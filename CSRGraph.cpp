#include "CSRGraph.hpp"
#include <cstring>
#include <cstdlib>
#include <stdio.h>
#include "mkl.h"

using namespace std;

// create a CSR graph from edge list 
void CSRGraph::createFromEdgeListFile(CSRGraph::idxType numVerts, CSRGraph::idxType numEdges, 
                CSRGraph::idxType* srcList, CSRGraph::idxType* dstList)
{/*{{{*/

    _numEdges = numEdges;
    _numVertices = numVerts;

    _degList = (CSRGraph::idxType*) malloc(_numVertices*sizeof(CSRGraph::idxType)); 
    std::memset(_degList, 0, _numVertices*sizeof(CSRGraph::idxType));

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
    // size numVerts + 1
    _indexRow = (CSRGraph::idxType*)malloc((_numVertices+1)*sizeof(CSRGraph::idxType));
    _indexRow[0] = 0;
    for(CSRGraph::idxType i=1; i<= _numVertices;i++)
        _indexRow[i] = _indexRow[i-1] + _degList[i-1]; 

    // create the col index and val 
    _nnZ = _indexRow[_numVertices];

    _indexCol = (CSRGraph::idxType*)malloc
        (_indexRow[_numVertices]*sizeof(CSRGraph::idxType));

    _edgeVal = (CSRGraph::valType*)malloc
        (_indexRow[_numVertices]*sizeof(CSRGraph::valType));

    std::memset(_edgeVal, 0, _indexRow[_numVertices]*sizeof(CSRGraph::valType));

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

    // initialize the mkl mat data structure
    // comment out this to disable using MKL spmv kernel
    createMKLMat();

}/*}}}*/


void CSRGraph::SpMVNaive(valType* x, valType* y)
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

void CSRGraph::SpMVNaive(valType* x, valType* y, int thdNum)
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

void CSRGraph::SpMVMKLHint(int callNum)
{
    mkl_sparse_set_mv_hint(_mklA, SPARSE_OPERATION_NON_TRANSPOSE, _descA, callNum);
    mkl_sparse_optimize(_mklA);
}

void CSRGraph::serialize(ofstream& outputFile)
{
    outputFile.write((char*)&_numEdges, sizeof(idxType));
    outputFile.write((char*)&_numVertices, sizeof(idxType));
    outputFile.write((char*)_degList, _numVertices*sizeof(idxType));
    outputFile.write((char*)_indexRow, (_numVertices+1)*sizeof(idxType));
    outputFile.write((char*)_indexCol, (_indexRow[_numVertices])*sizeof(idxType));
    outputFile.write((char*)_edgeVal, (_indexRow[_numVertices])*sizeof(valType));
}

void CSRGraph::deserialize(ifstream& inputFile)
{
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

    printf("Total vertices is : %d\n", _numVertices);
    printf("Total Edges is : %d\n", _numEdges);
    std::fflush(stdout); 
}

void CSRGraph::makeOneIndex()
{
    if (!_isOneBased)
    {

#pragma omp parallel for
        for (int i = 0; i < _indexRow[_numVertices]; ++i) {
           _indexCol[i]++; 
        }

#pragma omp parallel for 
        for (int i = 0; i < _numVertices+1; ++i) {
           _indexRow[i]++; 
        }
        
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
    sparse_status_t stat = mkl_sparse_s_create_csr(
            &_mklA, SPARSE_INDEX_BASE_ZERO, _numVertices, _numVertices,
            _indexRow, _indexRow + 1,
            _indexCol, _edgeVal);

    if (SPARSE_STATUS_SUCCESS != stat) {
        fprintf(stderr, "Failed to create mkl csr\n");
        return;
    }

    _descA.type = SPARSE_MATRIX_TYPE_GENERAL;
    _descA.diag = SPARSE_DIAG_NON_UNIT;

    _useMKL = true;
}

