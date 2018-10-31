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

void CSRGraph::SpMVMKL(valType* x, valType* y, int thdNum)
{
    // mkl_set_num_threads(thdNum);
    const char tran = 'N';
    mkl_cspblas_scsrgemv(&tran, &_numVertices, _edgeVal, _indexRow, _indexCol, x, y);
}


