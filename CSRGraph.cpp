#include "CSRGraph.hpp"
#include <cstring>
#include <cstdlib>
#include <stdio.h>
#include "mkl.h"

using namespace std;

// create a CSR graph from edge list 
// TODO: add suppor to distributed env
void CSRGraph::createFromEdgeListFile(CSRGraph::idxType numVerts, CSRGraph::idxType numEdges, 
                CSRGraph::idxType* srcList, CSRGraph::idxType* dstList, bool useMKL, bool useRcm, bool isBenchmark)
{/*{{{*/

    _numEdges = numEdges;
    _numVertices = numVerts;

#ifdef DISTRI

    _vNLocal = (_numVertices + _nprocs - 1)/_nprocs;
    _vOffset = _myrank*_vNLocal;

    if (_myrank == _nprocs -1)
    {
        // adjust the last rank
        _vNLocal = _numVertices - _vNLocal*(_nprocs -1);
    }

    printf("nprocs: %d, rank: %d, vTotal is: %d, _vNLocal is : %d, _vOffset is: %d\n", _nprocs, _myrank, _numVertices, _vNLocal, _vOffset);
    std::fflush(stdout); 
#endif

    // here first maintains a global degList even for distributed env 
    // TODO: implement a parallel MPI/IO 
    _degList = (CSRGraph::idxType*) malloc(_numVertices*sizeof(CSRGraph::idxType)); 

#pragma omp parallel for num_threads(omp_get_max_threads())
    for (int i = 0; i < _numVertices; ++i) {
        _degList[i] = 0;
    }

    // build the degree list
#pragma omp parallel for
    for(CSRGraph::idxType i = 0; i< _numEdges; i++)
    {
        CSRGraph::idxType srcId = srcList[i];
        CSRGraph::idxType dstId = dstList[i];

#pragma omp atomic
        _degList[srcId]++;

        // non-directed graph
        if (!_isDirected)
        {
#pragma omp atomic
        _degList[dstId]++;
        }
    }

    // calculate the row index of CSR (offset)
    // size _vNLocal + 1
    // local vertices 
#ifdef DISTRI
    _indexRow = (CSRGraph::idxType*)malloc((_vNLocal+1)*sizeof(CSRGraph::idxType));
#else
    _indexRow = (CSRGraph::idxType*)malloc((_numVertices+1)*sizeof(CSRGraph::idxType));
#endif

    _indexRow[0] = 0;

#ifdef DISTRI
    for(CSRGraph::idxType i=1; i<= _vNLocal;i++)
        _indexRow[i] = _indexRow[i-1] + _degList[i+_vOffset-1]; 
#else
    for(CSRGraph::idxType i=1; i<= _numVertices;i++)
        _indexRow[i] = _indexRow[i-1] + _degList[i-1]; 
#endif

    // create the col index and val 
#ifdef DISTRI
    _nnZ = _indexRow[_vNLocal];
#else
    _nnZ = _indexRow[_numVertices];
#endif

#ifdef DISTRI
    _indexCol = (CSRGraph::idxType*)malloc
        (_indexRow[_vNLocal]*sizeof(CSRGraph::idxType));

    _edgeVal = (CSRGraph::valType*)malloc
        (_indexRow[_vNLocal]*sizeof(CSRGraph::valType));
#pragma omp parallel for num_threads(omp_get_max_threads())
    for (int i = 0; i < _indexRow[_vNLocal]; ++i) {
        _edgeVal[i] = 0;
    }

#else
    _indexCol = (CSRGraph::idxType*)malloc
        (_indexRow[_numVertices]*sizeof(CSRGraph::idxType));

    _edgeVal = (CSRGraph::valType*)malloc
        (_indexRow[_numVertices]*sizeof(CSRGraph::valType));

#pragma omp parallel for num_threads(omp_get_max_threads())
    for (int i = 0; i < _indexRow[_numVertices]; ++i) {
        _edgeVal[i] = 0;
    }
#endif

#ifdef DISTRI
    // only load in the src/dst within the local vertex range
    for(CSRGraph::idxType i = 0; i< _numEdges; i++)
    {

        CSRGraph::idxType srcId = srcList[i];
        CSRGraph::idxType dstId = dstList[i];

        idxType vLocalId = srcId - _vOffset;
        if (vLocalId >= 0 && vLocalId < _vNLocal)
        {
            _indexCol[_indexRow[vLocalId]] = dstId;
            _edgeVal[(_indexRow[vLocalId])++] = 1.0;
        }

        // non-directed graph
        if (!_isDirected)
        {
            idxType vLocalIdDst = dstId - _vOffset;
            if (vLocalIdDst >= 0 && vLocalIdDst < _vNLocal)
            {
                _indexCol[_indexRow[vLocalIdDst]] = srcId;
                _edgeVal[(_indexRow[vLocalIdDst])++] = 1.0;
            }
        }
    }

    // recover the indexRow 
 #pragma omp parallel for
    for(CSRGraph::idxType i=0; i<_vNLocal;i++)
        _indexRow[i] -= _degList[i+_vOffset];

#else

    for(CSRGraph::idxType i = 0; i< _numEdges; i++)
    {

        CSRGraph::idxType srcId = srcList[i];
        CSRGraph::idxType dstId = dstList[i];

        _indexCol[_indexRow[srcId]] = dstId;
        _edgeVal[(_indexRow[srcId])++] = 1.0;

        // non-directed graph
        if (!_isDirected)
        {
            _indexCol[_indexRow[dstId]] = srcId;
            _edgeVal[(_indexRow[dstId])++] = 1.0;
        }
    }

    // recover the indexRow 
#pragma omp parallel for
    for(CSRGraph::idxType i=0; i<_numVertices;i++)
        _indexRow[i] -= _degList[i];

#endif 

    _useMKL = useMKL;
    _useRcm = useRcm;

    if (!isBenchmark)
    {
        // re-ordering the input graph by using RCM
        // comment out this to disbale re-ordering
        if (_useRcm)
        {
#ifdef DISTRI
            printf("Error, rcm reordering not supported in distributed mode\n");
            std::fflush(stdout); 
#else
            rcmReordering();
#endif
        }
        // initialize the mkl mat data structure
        // comment out this to disable using MKL spmv kernel
        if (_useMKL)
           createMKLMat();
    }

}/*}}}*/


void CSRGraph::SpMVNaive(valType* x, valType* y)
{

#pragma omp parallel for
    for(idxType i = 0; i<_numVertices; i++)
    {
        valType sum = 0.0;

        idxType rowLen = getRowLen(i);
        idxType* rowColIdx = getColIdx(i);

        #pragma omp simd reduction(+:sum) 
        for(idxType j=0; j<rowLen;j++)
            sum += (x[rowColIdx[j]]);

        y[i] = sum;
    }
}

void CSRGraph::SpMVNaiveFull(valType* x, valType* y)
{

#pragma omp parallel for
    for(idxType i = 0; i<_numVertices; i++)
    {
        valType sum = 0.0;

        idxType rowLen = getRowLen(i);
        valType* rowElem = getEdgeVals(i); 
        idxType* rowColIdx = getColIdx(i);

        #pragma omp simd reduction(+:sum) 
        for(idxType j=0; j<rowLen;j++)
            sum += rowElem[j] * (x[rowColIdx[j]]);

        y[i] = sum;
    }
}

void CSRGraph::SpMVNaiveScale(valType* x, valType* y, float scale)
{

#pragma omp parallel for
    for(idxType i = 0; i<_numVertices; i++)
    {
        double sum = 0.0;

        idxType rowLen = getRowLen(i);
        valType* rowElem = getEdgeVals(i); 
        idxType* rowColIdx = getColIdx(i);

        #pragma omp simd reduction(+:sum) 
        for(idxType j=0; j<rowLen;j++)
            sum += rowElem[j]*((double)scale*(x[rowColIdx[j]]));

        y[i] = (float)sum;
    }
}

// used in computation
void CSRGraph::SpMVNaive(valType* x, valType* y, int thdNum)
{

#pragma omp parallel for num_threads(thdNum)
    for(idxType i = 0; i<_numVertices; i++)
    {
        valType sum = 0.0;

        idxType rowLen = getRowLen(i);
        idxType* rowColIdx = getColIdx(i);

        #pragma omp simd reduction(+:sum) 
        for(idxType j=0; j<rowLen;j++)
            sum += (x[rowColIdx[j]]);

        y[i] = sum;
    }
}

void CSRGraph::SpMVNaiveFullDistri(valType* x, valType* y, int thdNum)
{

#pragma omp parallel for num_threads(thdNum)
    for(idxType i = 0; i<_vNLocal; i++)
    {
        valType sum = 0.0;

        idxType rowLen = getRowLen(i);
        valType* rowElem = getEdgeVals(i); 
        idxType* rowColIdx = getColIdx(i);

        #pragma omp simd reduction(+:sum) 
        for(idxType j=0; j<rowLen;j++)
            sum += rowElem[j] * (x[rowColIdx[j]]);

        y[i] = sum;
    }
}

void CSRGraph::SpMVNaiveFull(valType* x, valType* y, int thdNum)
{

#pragma omp parallel for num_threads(thdNum)
    for(idxType i = 0; i<_numVertices; i++)
    {
        valType sum = 0.0;

        idxType rowLen = getRowLen(i);
        valType* rowElem = getEdgeVals(i); 
        idxType* rowColIdx = getColIdx(i);

        #pragma omp simd reduction(+:sum) 
        for(idxType j=0; j<rowLen;j++)
            sum += rowElem[j] * (x[rowColIdx[j]]);

        y[i] = sum;
    }
}

void CSRGraph::SpMVMKLDistri(valType* x, valType* y, int thdNum)
{
    if (_useMKL)
    {
        mkl_sparse_s_mv(SPARSE_OPERATION_NON_TRANSPOSE, 1, _mklA, _descA, x, 0, y);
    }
    else
    {
        SpMVNaiveFullDistri(x,y,thdNum);
    }

}

void CSRGraph::SpMVMKL(valType* x, valType* y, int thdNum)
{
    if (_useMKL)
    {
        mkl_sparse_s_mv(SPARSE_OPERATION_NON_TRANSPOSE, 1, _mklA, _descA, x, 0, y);
    }
    else
    {
        SpMVNaive(x,y,thdNum);
    }

    // mkl_set_num_threads(thdNum);
    // const char tran = 'N';
    // mkl_cspblas_scsrgemv(&tran, &_numVertices, _edgeVal, _indexRow, _indexCol, x, y);
}

void CSRGraph::SpMMMKL(valType* x, valType* y, idxType m, idxType n, int thdNum)
{
    if (_useMKL)
    {
        mkl_sparse_s_mm(SPARSE_OPERATION_NON_TRANSPOSE, 1, _mklA, _descA, SPARSE_LAYOUT_COLUMN_MAJOR, 
            x, n, m, 0, y, m);
    }
    
}

void CSRGraph::SpMVMKLHint(int callNum)
{
    mkl_sparse_set_mv_hint(_mklA, SPARSE_OPERATION_NON_TRANSPOSE, _descA, callNum);
    mkl_sparse_optimize(_mklA);
}

void CSRGraph::SpMMMKLHint(int mCols, int callNum)
{
    mkl_sparse_set_mm_hint(_mklA, SPARSE_OPERATION_NON_TRANSPOSE, _descA, SPARSE_LAYOUT_COLUMN_MAJOR, mCols, callNum);
    mkl_sparse_optimize(_mklA);
}

void CSRGraph::serialize(ofstream& outputFile)
{
#ifdef DISTRI
    outputFile.write((char*)&_numEdges, sizeof(idxType));
    outputFile.write((char*)&_numVertices, sizeof(idxType));
    outputFile.write((char*)&_vNLocal, sizeof(idxType));
    outputFile.write((char*)&_vOffset, sizeof(idxType));
    outputFile.write((char*)_degList, _numVertices*sizeof(idxType));
    outputFile.write((char*)_indexRow, (_vNLocal+1)*sizeof(idxType));
    outputFile.write((char*)_indexCol, (_indexRow[_vNLocal])*sizeof(idxType));
    outputFile.write((char*)_edgeVal, (_indexRow[_vNLocal])*sizeof(valType));
#else
    outputFile.write((char*)&_numEdges, sizeof(idxType));
    outputFile.write((char*)&_numVertices, sizeof(idxType));
    outputFile.write((char*)_degList, _numVertices*sizeof(idxType));
    outputFile.write((char*)_indexRow, (_numVertices+1)*sizeof(idxType));
    outputFile.write((char*)_indexCol, (_indexRow[_numVertices])*sizeof(idxType));
    outputFile.write((char*)_edgeVal, (_indexRow[_numVertices])*sizeof(valType));
#endif
}

void CSRGraph::deserialize(ifstream& inputFile, bool useMKL, bool useRcm)
{

#ifdef DISTRI
    inputFile.read((char*)&_numEdges, sizeof(idxType));
    inputFile.read((char*)&_numVertices, sizeof(idxType));
    inputFile.read((char*)&_vNLocal, sizeof(idxType));
    inputFile.read((char*)&_vOffset, sizeof(idxType));

    _degList = (idxType*) malloc (_numVertices*sizeof(idxType)); 
    inputFile.read((char*)_degList, _numVertices*sizeof(idxType));

    _indexRow = (idxType*) malloc ((_vNLocal+1)*sizeof(idxType)); 
    inputFile.read((char*)_indexRow, (_vNLocal+1)*sizeof(idxType));

    _indexCol = (idxType*) malloc ((_indexRow[_vNLocal])*sizeof(idxType)); 
    inputFile.read((char*)_indexCol, (_indexRow[_vNLocal])*sizeof(idxType));

    _edgeVal = (valType*) malloc ((_indexRow[_vNLocal])*sizeof(valType)); 
    inputFile.read((char*)_edgeVal, (_indexRow[_vNLocal])*sizeof(valType));

    _nnZ = _indexRow[_vNLocal];

#else
    inputFile.read((char*)&_numEdges, sizeof(idxType));
    inputFile.read((char*)&_numVertices, sizeof(idxType));

    _degList = (idxType*) malloc (_numVertices*sizeof(idxType)); 
    inputFile.read((char*)_degList, _numVertices*sizeof(idxType));

    _indexRow = (idxType*) malloc ((_numVertices+1)*sizeof(idxType)); 
    inputFile.read((char*)_indexRow, (_numVertices+1)*sizeof(idxType));

    _indexCol = (idxType*) malloc ((_indexRow[_numVertices])*sizeof(idxType)); 
    inputFile.read((char*)_indexCol, (_indexRow[_numVertices])*sizeof(idxType));

    _edgeVal = (valType*) malloc ((_indexRow[_numVertices])*sizeof(valType)); 
    inputFile.read((char*)_edgeVal, (_indexRow[_numVertices])*sizeof(valType));

    _nnZ = _indexRow[_numVertices];
#endif

    _useMKL = useMKL;
    _useRcm = useRcm;

#ifdef DISTRI
    printf("CSR Format Total vertices is : %d\n", _numVertices);
    printf("CSR Format Total Edges is : %d\n", _numEdges);
    printf("CSR Format Local vertices is : %d\n", _vNLocal);
    printf("CSR Format Local vertices offset is : %d\n", _vOffset);
    std::fflush(stdout); 
#else
    printf("CSR Format Total vertices is : %d\n", _numVertices);
    printf("CSR Format Total Edges is : %d\n", _numEdges);
    std::fflush(stdout); 
#endif

    // comment out this to disable the re-ordering
    if (_useRcm)
    {
#ifdef DISTRI
        printf("Error, rcm reordering not supported in distributed mode\n");
        std::fflush(stdout); 
#else
        rcmReordering();
#endif
    }

    // comment out this to disable the mkl spmv kernel
    if (_useMKL)
        createMKLMat();

}

void CSRGraph::toASCII(string fileName)
{
    ofstream outputFile;
    outputFile.open(fileName);
    std::cout<<"m: "<<_numVertices<<" n: "<<_numVertices<<" nnz: "<<_indexRow[_numVertices]<<std::endl;

    outputFile<<_numVertices<<" "<<_numVertices<<" "<<_indexRow[_numVertices]<<std::endl;
    for (int i = 0; i < _numVertices; ++i) {

        for (int j = _indexRow[i]; j < _indexRow[i+1]; ++j) {
            outputFile<<(i+1)<<" "<<(_indexCol[j]+1)<<" "<<_edgeVal[j]<<std::endl; 
        }
    }
    outputFile.close();
}

void CSRGraph::makeOneIndex()
{
    if (!_isOneBased)
    {

#ifdef DISTRI

#pragma omp parallel for
        for (int i = 0; i < _indexRow[_vNLocal]; ++i) {
           _indexCol[i]++; 
        }

#pragma omp parallel for 
        for (int i = 0; i < _vNLocal+1; ++i) {
           _indexRow[i]++; 
        }

#else

#pragma omp parallel for
        for (int i = 0; i < _indexRow[_numVertices]; ++i) {
           _indexCol[i]++; 
        }

#pragma omp parallel for 
        for (int i = 0; i < _numVertices+1; ++i) {
           _indexRow[i]++; 
        }
#endif        
        _isOneBased = true;
    }
}

// fill the spdm3 CSR format (CSR and zero based)
void CSRGraph::fillSpMat(spdm3::SpMat<int, float> &smat)
{
    smat.SetNNZ(_nnZ);
    smat.SetRows(_numVertices);
    smat.SetCols(_numVertices);

    smat.SetHeadptrs(_indexRow);
    smat.SetIndices(_indexCol);
    smat.SetValues(_edgeVal);

}

void CSRGraph::createMKLMat()
{
    printf("Create MKL format for input graph\n");
    std::fflush(stdout);

    sparse_status_t stat;  
    
    if (_rcmMatR != nullptr)
    {
        stat = mkl_sparse_s_create_csr(&_mklA, SPARSE_INDEX_BASE_ZERO, _rcmMatR->m, _rcmMatR->m,
            _rcmMatR->rowptr, _rcmMatR->rowptr + 1, _rcmMatR->colidx, _rcmMatR->svalues);
    }
    else
    {
#ifdef DISTRI
        printf("_vNLocal of rank: %d is: %d\n", _myrank, _vNLocal);
        std::fflush(stdout);

        stat = mkl_sparse_s_create_csr(&_mklA, SPARSE_INDEX_BASE_ZERO, _vNLocal, _numVertices,
            _indexRow, _indexRow + 1, _indexCol, _edgeVal);
#else
        stat = mkl_sparse_s_create_csr(&_mklA, SPARSE_INDEX_BASE_ZERO, _numVertices, _numVertices,
            _indexRow, _indexRow + 1, _indexCol, _edgeVal);
#endif
    }
    
    if (SPARSE_STATUS_SUCCESS != stat) {
        fprintf(stderr, "Failed to create mkl csr\n");
        return;
    }

    _descA.type = SPARSE_MATRIX_TYPE_GENERAL;
    _descA.diag = SPARSE_DIAG_NON_UNIT;

}

void CSRGraph::rcmReordering()
{
    _rcmMat = new SpMP::CSR(_numVertices, _numVertices, _indexRow, _indexCol, _edgeVal);
    int* perm = (int*)_mm_malloc(_rcmMat->m*sizeof(int), 64);
    int* inversePerm = (int*) _mm_malloc(_rcmMat->m*sizeof(int), 64);
    _rcmMat->getRCMPermutation(perm, inversePerm);
    _rcmMatR = _rcmMat->permute(perm, inversePerm, false, true);
    if (_rcmMatR != nullptr)
    {
        printf("Reordering sccussful\n");
        std::fflush(stdout);
    }

    _mm_free(perm);
    _mm_free(inversePerm);
}

/**
 * @brief prepare the sending and receiving buffer of 
 * distributed communication
 */
void CSRGraph::prepComBuf()
{
    // this function is to use the 

}

























