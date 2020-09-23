#include "CSRGraph.hpp"
#include <cstring>
#include <cstdlib>
#include <stdio.h>
#include <unistd.h>
#include <fcntl.h>
#include <omp.h>

#ifndef NEC
#include "mkl.h"
#endif

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

 // std::memset(_edgeVal, 0, _indexRow[_numVertices]*sizeof(CSRGraph::valType));

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

        //#pragma omp simd reduction(+:sum) 
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

        //#pragma omp simd reduction(+:sum) 
        for(idxType j=0; j<rowLen;j++)
            sum += rowElem[j] * (x[rowColIdx[j]]);

        y[i] = sum;
    }
}

void CSRGraph::SpMVMKLDistri(valType* x, valType* y, int thdNum)
{
    SpMVNaiveFullDistri(x,y,thdNum);

}

void CSRGraph::SpMVMKL(valType* x, valType* y, int thdNum)
{
    SpMVNaive(x,y,thdNum);
}

void CSRGraph::SpMMMKL(valType* x, valType* y, idxType m, idxType n, idxType k,  int thdNum)
{
#ifndef NEC
    if (_useMKL)
    {
        mkl_sparse_s_mm(SPARSE_OPERATION_NON_TRANSPOSE, 1, _mklA, _descA, SPARSE_LAYOUT_COLUMN_MAJOR, 
            x, n, k, 0, y, m);
    }
#endif
}

void CSRGraph::SpMVMKLHint(int callNum)
{
#ifndef NEC
    mkl_sparse_set_mv_hint(_mklA, SPARSE_OPERATION_NON_TRANSPOSE, _descA, callNum);
    mkl_sparse_optimize(_mklA);
#endif
}

void CSRGraph::SpMMMKLHint(int mCols, int callNum)
{
#ifndef NEC
    mkl_sparse_set_mm_hint(_mklA, SPARSE_OPERATION_NON_TRANSPOSE, _descA, SPARSE_LAYOUT_COLUMN_MAJOR, mCols, callNum);
    mkl_sparse_optimize(_mklA);
#endif
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

void CSRGraph::deserialize(int inputFile, bool useMKL, bool useRcm)
{

#ifdef DISTRI
    read(inputFile, (char*)&_numEdges, sizeof(idxType));
    read(inputFile, (char*)&_numVertices, sizeof(idxType));
    read(inputFile, (char*)&_vNLocal, sizeof(idxType));
    read(inputFile, (char*)&_vOffset, sizeof(idxType));

    _degList = (idxType*) malloc (_numVertices*sizeof(idxType)); 
    read(inputFile, (char*)_degList, _numVertices*sizeof(idxType));

    _indexRow = (idxType*) malloc ((_vNLocal+1)*sizeof(idxType)); 
    read(inputFile, (char*)_indexRow, (_vNLocal+1)*sizeof(idxType));

    _indexCol = (idxType*) malloc ((_indexRow[_vNLocal])*sizeof(idxType)); 
    read(inputFile, (char*)_indexCol, (_indexRow[_vNLocal])*sizeof(idxType));

    _edgeVal = (valType*) malloc ((_indexRow[_vNLocal])*sizeof(valType)); 
    read(inputFile, (char*)_edgeVal, (_indexRow[_vNLocal])*sizeof(valType));

    _nnZ = _indexRow[_vNLocal];

#else
    read(inputFile, (char*)&_numEdges, sizeof(idxType));
    read(inputFile, (char*)&_numVertices, sizeof(idxType));

    _degList = (idxType*) malloc (_numVertices*sizeof(idxType)); 
    read(inputFile, (char*)_degList, _numVertices*sizeof(idxType));

    _indexRow = (idxType*) malloc ((_numVertices+1)*sizeof(idxType)); 
    read(inputFile, (char*)_indexRow, (_numVertices+1)*sizeof(idxType));

    _indexCol = (idxType*) malloc ((_indexRow[_numVertices])*sizeof(idxType)); 
    read(inputFile, (char*)_indexCol, (_indexRow[_numVertices])*sizeof(idxType));

    _edgeVal = (valType*) malloc ((_indexRow[_numVertices])*sizeof(valType)); 
    read(inputFile, (char*)_edgeVal, (_indexRow[_numVertices])*sizeof(valType));

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

#ifndef NEC
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
#endif

void CSRGraph::createMKLMat()
{
#ifndef NEC
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
#endif
}

void CSRGraph::rcmReordering()
{
#ifndef NEC
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
#endif
}

/**
 * @brief prepare the sending and receiving buffer of 
 * distributed communication
 */
void CSRGraph::prepComBuf()
{

#ifdef DISTRI

    _sendCounts = new idxType[_nprocs];
    _recvCounts = new idxType[_nprocs];
    
    _sendDispls = new idxType[_nprocs];
    _recvDispls = new idxType[_nprocs];

    for (int i = 0; i < _nprocs; ++i) {
       _sendCounts[i] = _vNLocal;
    }

    _recvCounts[_myrank] = 0;
    //alltoall and get 
    MPI_Alltoall((const void *)_sendCounts, 1, MPI_INT,
                 (void *)_recvCounts, 1, MPI_INT, MPI_COMM_WORLD);

    // debug check print 
    for (int i = 0; i < _nprocs; ++i) {
        std::cout<<"Rank: "<< _myrank << " sending to rank:" << i << " is " << _sendCounts[i]<< std::endl;
        std::cout<<"Rank: "<< _myrank << " receiving from rank:" << i << " is " << _recvCounts[i]<< std::endl;
    }   

    _sendDispls[0] = 0;
    _recvDispls[0] = 0;
    for (int i = 1; i < _nprocs; ++i) {
        _sendDispls[i] = _sendDispls[i-1] + _sendCounts[i-1];
        _recvDispls[i] = _recvDispls[i-1] + _recvCounts[i-1];
    }

    for (int i = 0; i < _nprocs; ++i) {
       _sendBufLen += _sendCounts[i]; 
       _recvBufLen += _recvCounts[i];
    }

#endif
}


void CSRGraph::assembleSendBuf(valType* data, valType* sendbuf, idxType dataLen, 
                idxType batchSize, idxType* metaC, idxType* metaDispls)
{
#ifdef DISTRI
    if (sendbuf)
    {
        // determine the meta counts and displacements
        for (int i = 0; i < _nprocs; ++i) {
            metaC[i] = _sendCounts[i]*batchSize; 
        }

        metaDispls[0] = 0;
        for (int i = 1; i < _nprocs; ++i) {
           metaDispls[i] = metaDispls[i-1] + metaC[i-1]; 
        }
        
        // column majored copy data to sendbuf in parallel
        for (int i = 0; i < _nprocs; ++i) {
#pragma omp parallel for 
                for (int j = 0; j < batchSize; ++j) {
                    std::copy(data + j*dataLen, data + j*dataLen + 
                            _sendCounts[i], sendbuf+metaDispls[i]+j*_sendCounts[i]);
                }
            
        } 
    }
#endif
}

void CSRGraph::csrAlltoAll(valType* sendbuf, valType* recvbuf, idxType bufLen, idxType batchSize, idxType* sendMetaC,
                idxType* sendMetaDispls, idxType* recvMetaC, idxType* recvMetaDispls)
{
#ifdef DISTRI
    if (recvbuf)
    {

        // determine the meta counts and displacements
        for (int i = 0; i < _nprocs; ++i) {
            recvMetaC[i] = _recvCounts[i]*batchSize; 
        }

        recvMetaDispls[0] = 0;
        for (int i = 1; i < _nprocs; ++i) {
           recvMetaDispls[i] = recvMetaDispls[i-1] + recvMetaC[i-1]; 
        }

        // launch mpi_alltoallv
        MPI_Alltoallv((const void *)sendbuf, (const int*) sendMetaC,
                  (const int *)sendMetaDispls, MPI_FLOAT, (void *)recvbuf,
                  (const int *)recvMetaC, (const int *)recvMetaDispls, MPI_FLOAT,
                  MPI_COMM_WORLD);

    }
#endif

}


void CSRGraph::reorgBuf(valType* input, valType* output, idxType batchSize, idxType* recvMetaC, idxType* recvMetaDispls)
{
#ifdef DISTRI
    for (int i = 0; i < _nprocs; ++i) {
#pragma omp parallel for 
        for (int j = 0; j < batchSize; ++j) {
            std::copy(input+recvMetaDispls[i]+j*_recvCounts[i], input+recvMetaDispls[i]+j*_recvCounts[i]
                    + _recvCounts[i], output+j*_numVertices+_recvDispls[i]);
        }
    }

#endif
}






















