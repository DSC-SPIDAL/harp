#ifndef CSCGRAPH_H
#define CSCGRAPH_H

#include <stdint.h>
#include <cstdlib>
#include <fstream>
#include <iostream>
#include <cstring>
#include <stdio.h>
#include <omp.h>
#include "Helper.hpp"

#ifdef __INTEL_COMPILER
// use avx intrinsics
#include "immintrin.h"
#include "zmmintrin.h"
#endif

using namespace std;

template<class idxType, class valType>
class CSCGraph
{
    public:

        CSCGraph(): _isDirected(false), _isOneBased(false), _numEdges(-1), _numVertices(-1), _nnZ(-1), 
        _edgeVal(nullptr), _indexRow(nullptr), _indexCol(nullptr),
        _degList(nullptr), _numsplits(0), _splitsRowIds(nullptr), _splitsColIds(nullptr), _splitsVals(nullptr), 
        _isDistri(false), _nprocs(0), _myrank(0), _vOffset(0), _vNLocal(0), 
        _sendCounts(nullptr), _recvCounts(nullptr),_sendDispls(nullptr), _recvDispls(nullptr),
        _sendBufLen(0), _recvBufLen(0){}


        CSCGraph(bool isDistri, int nprocs, int myrank): _isDirected(false), _isOneBased(false), _numEdges(-1), _numVertices(-1), _nnZ(-1), 
        _edgeVal(nullptr), _indexRow(nullptr), _indexCol(nullptr), 
        _degList(nullptr), _numsplits(0), _splitsRowIds(nullptr), _splitsColIds(nullptr), _splitsVals(nullptr),
        _isDistri(isDistri), _nprocs(nprocs), _myrank(myrank), _vOffset(0), _vNLocal(0), 
        _sendCounts(nullptr), _recvCounts(nullptr),_sendDispls(nullptr), _recvDispls(nullptr) ,
        _sendBufLen(0), _recvBufLen(0){}

        ~CSCGraph () {

            if (_edgeVal != nullptr)
                free(_edgeVal);

            if (_indexRow != nullptr)
                free(_indexRow);

            if (_indexCol != nullptr)
                free(_indexCol);

            if (_splitsRowIds != nullptr)
                delete[] _splitsRowIds;

            if (_splitsColIds != nullptr)
                delete[] _splitsColIds; 

            if (_splitsVals != nullptr)
                delete[] _splitsVals; 

            if (_sendCounts)
                delete[] _sendCounts;

            if (_recvCounts)
                delete[] _recvCounts;

            if (_sendDispls)
                delete[] _sendDispls;

            if (_recvDispls)
                delete[] _recvDispls;

        }

        valType* getEdgeVals(idxType colId) {return _edgeVal + _indexCol[colId]; }
        idxType* getRowIdx(idxType colId) {return _indexRow + _indexCol[colId]; }
        idxType getColLen(idxType colId) {return _indexCol[colId+1] - _indexCol[colId]; }

        idxType getNumVertices() {return  _numVertices;} 
        idxType getNNZDistri() {return _indexCol[_vNLocal]; }

#ifdef DISTRI
        idxType getNNZ() {return _indexCol[_vNLocal]; }
#else
        idxType getNNZ() {return _indexCol[_numVertices]; }
#endif

        idxType* getIndexRow() {return _indexRow;}
        idxType* getIndexCol() {return _indexCol;}
        valType* getNNZVal() {return _edgeVal;} 

        idxType* getDegList() {return _degList;}

        void createFromEdgeListFile(idxType numVerts, idxType numEdges, 
                idxType* srcList, idxType* dstList, bool isBenchmark = false);       

        void splitCSC(idxType numsplits);
        void spmvNaiveSplit(valType* x, valType* y, idxType numThds);
        double spmmSplitExp(valType* x, valType* y, idxType xColNum, idxType numThds);
        void spmmSplit(valType* x, valType* y, idxType xColNum, idxType numThds);

        void serialize(ofstream& outputFile);
        void deserialize(ifstream& inputFile);

        // for distributed env
        bool isDistributed () { return _isDistri; }
        idxType getVNLocal() {return _vNLocal;}
        idxType getVOffset() {return _vOffset;}
        idxType getSendBuflen() { return _sendBufLen; }
        idxType getRecvBuflen() { return _recvBufLen; }

        idxType* getSendCounts() {return _sendCounts; }
        idxType* getRecvCounts() {return _recvCounts; }

        idxType* getSendDispls() {return _sendDispls; }
        idxType* getRecvDispls() {return _recvDispls; }

        void prepComBuf(); 
        void cscReduce(valType* input, valType* output, idxType batchSize,
                idxType* recvMetaC, idxType* recvMetaDispls);


    private:
        /* data */

        bool _isDirected;
        bool _isOneBased;
        idxType _numEdges;
        idxType _numVertices;
        idxType _nnZ;
        valType* _edgeVal;
        idxType* _indexRow;
        idxType* _indexCol;
        idxType* _degList;
        idxType _numsplits;

        std::vector<idxType>* _splitsRowIds;
        std::vector<idxType>* _splitsColIds;
        std::vector<valType>* _splitsVals;

        // for distributed version
        bool _isDistri;
        int _nprocs;
        int _myrank;
        idxType _vOffset; // offset of local vertices 
        idxType _vNLocal; // local number of vertices

        idxType* _sendCounts; // num of vertices to send
        idxType* _recvCounts; // num of vertices to recv
        idxType* _sendDispls; // displacement of sending vertices 
        idxType* _recvDispls; // displacement of receiving vertices 
        idxType _sendBufLen;
        idxType _recvBufLen;


};

template<class idxType, class valType>
void CSCGraph<idxType, valType>::createFromEdgeListFile(idxType numVerts, idxType numEdges, idxType* srcList, idxType* dstList, bool isBenchmark) 
{
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

    _degList = (idxType*) malloc(_numVertices*sizeof(idxType)); 

#pragma omp parallel for num_threads(omp_get_max_threads())
    for (int i = 0; i < _numVertices; ++i) {
        _degList[i] = 0;
    }

    // build the degree list
#pragma omp parallel for
    for(idxType i = 0; i< _numEdges; i++)
    {
        idxType srcId = srcList[i];
        idxType dstId = dstList[i];

#pragma omp atomic
        _degList[dstId]++;

        // non-directed graph
        if (!_isDirected)
        {
#pragma omp atomic
        _degList[srcId]++;
        }
    }

    // calculate the col index of CSC (offset)
    // column partitioned
    // size numVerts + 1
#ifdef DISTRI
    _indexCol = (idxType*)malloc((_vNLocal+1)*sizeof(idxType));
#else
    _indexCol = (idxType*)malloc((_numVertices+1)*sizeof(idxType));
#endif

    _indexCol[0] = 0;

#ifdef DISTRI
    for(idxType i=1; i<= _vNLocal;i++)
        _indexCol[i] = _indexCol[i-1] + _degList[i+_vOffset-1]; 

#else
    for(idxType i=1; i<= _numVertices;i++)
        _indexCol[i] = _indexCol[i-1] + _degList[i-1]; 
#endif

    // create the row index and val 
#ifdef DISTRI
    _nnZ = _indexCol[_vNLocal];
#else
    _nnZ = _indexCol[_numVertices];
#endif

#ifdef DISTRI
    _indexRow = (idxType*)malloc(_indexCol[_vNLocal]*sizeof(idxType));
    std::vector<idxType> indexRowVec(_indexCol[_vNLocal]);
    _edgeVal = (valType*)malloc(_indexCol[_vNLocal]*sizeof(valType));
#pragma omp parallel for num_threads(omp_get_max_threads())
    for (int i = 0; i < _indexCol[_vNLocal]; ++i) {
        _edgeVal[i] = 0;
    }

#else
    _indexRow = (idxType*)malloc(_indexCol[_numVertices]*sizeof(idxType));
    std::vector<idxType> indexRowVec(_indexCol[_numVertices]);
    _edgeVal = (valType*)malloc(_indexCol[_numVertices]*sizeof(valType));
#pragma omp parallel for num_threads(omp_get_max_threads())
    for (int i = 0; i < _indexCol[_numVertices]; ++i) {
        _edgeVal[i] = 0;
    }

#endif

#ifdef DISTRI

    for(idxType i = 0; i< _numEdges; i++)
    {
        idxType srcId = srcList[i];
        idxType dstId = dstList[i];

        idxType vLocalId = dstId - _vOffset;
        if (vLocalId >=0 && vLocalId < _vNLocal)
        {
            indexRowVec[_indexCol[vLocalId]] = srcId;
            _edgeVal[(_indexCol[vLocalId])++] = 1.0;
        }

        // non-directed graph
        if (!_isDirected)
        {
            idxType vLocalIdSrc = srcId - _vOffset;
            if (vLocalIdSrc >= 0 && vLocalIdSrc < _vNLocal)
            {
                indexRowVec[_indexCol[vLocalIdSrc]] = dstId;
                _edgeVal[(_indexCol[vLocalIdSrc])++] = 1.0;
            }
        }
    }

    // recover the indexRow 
#pragma omp parallel for
    for(idxType i=0; i<_vNLocal;i++)
        _indexCol[i] -= _degList[i+_vOffset];

    // sort the row id for each col
    // no need to sort the val in adjacency matrix
#pragma omp parallel for
    for (idxType i = 0; i < _vNLocal; ++i) {

        // is this thread safe ?
        std::sort(indexRowVec.begin()+_indexCol[i], indexRowVec.begin()+_indexCol[i+1]);
    }

    std::copy(indexRowVec.begin(), indexRowVec.end(), _indexRow);

#else

    for(idxType i = 0; i< _numEdges; i++)
    {
        idxType srcId = srcList[i];
        idxType dstId = dstList[i];

        indexRowVec[_indexCol[dstId]] = srcId;
        _edgeVal[(_indexCol[dstId])++] = 1.0;

        // non-directed graph
        if (!_isDirected)
        {
            indexRowVec[_indexCol[srcId]] = dstId;
            _edgeVal[(_indexCol[srcId])++] = 1.0;
        }
    }

    // recover the indexRow 
#pragma omp parallel for
    for(idxType i=0; i<_numVertices;i++)
        _indexCol[i] -= _degList[i];

    // sort the row id for each col
    // no need to sort the val in adjacency matrix
#pragma omp parallel for
    for (idxType i = 0; i < _numVertices; ++i) {

        // is this thread safe ?
        std::sort(indexRowVec.begin()+_indexCol[i], indexRowVec.begin()+_indexCol[i+1]);
    }

    std::copy(indexRowVec.begin(), indexRowVec.end(), _indexRow);

#endif

}

template<class idxType, class valType>
void CSCGraph<idxType, valType>::splitCSC(idxType numsplits)
{
    _numsplits = numsplits;
    if (_splitsRowIds != nullptr)
        delete[] _splitsRowIds;

    if (_splitsColIds != nullptr)
        delete[] _splitsColIds;

    if (_splitsVals != nullptr)
        delete[] _splitsVals;

    _splitsRowIds = new std::vector<idxType>[_numsplits];
    _splitsColIds = new std::vector<idxType>[_numsplits];
    _splitsVals = new std::vector<valType>[_numsplits];

#ifdef DISTRI

    idxType perpiece = _numVertices / _numsplits;

    for (idxType i=0; i < _vNLocal; ++i)
    {
        for (idxType j = _indexCol[i]; j < _indexCol[i+1]; ++j)
        {
            // already sorted
            idxType rowid = _indexRow[j];
            idxType owner = std::min(rowid / perpiece, (_numsplits-1));
            _splitsColIds[owner].push_back(i);
            _splitsRowIds[owner].push_back(rowid);
            _splitsVals[owner].push_back(_edgeVal[j]);
        }
    }

#else
    idxType perpiece = _numVertices / _numsplits;

    for (idxType i=0; i < _numVertices; ++i)
    {
        for (idxType j = _indexCol[i]; j < _indexCol[i+1]; ++j)
        {
            // already sorted
            idxType rowid = _indexRow[j];
            idxType owner = std::min(rowid / perpiece, (_numsplits-1));
            _splitsColIds[owner].push_back(i);
            _splitsRowIds[owner].push_back(rowid);
            _splitsVals[owner].push_back(_edgeVal[j]);
        }
    }

#endif

}

template<class idxType, class valType>
void CSCGraph<idxType, valType>::spmvNaiveSplit(valType* x, valType* y, idxType numThds)
{
#ifdef DISTRI

    // split CSC spmv
#pragma omp parallel for num_threads(numThds) 
    for (idxType s = 0; s < _numsplits; ++s) {

        std::vector<idxType>* localRowIds = &(_splitsRowIds[s]);
        std::vector<idxType>* localColIds = &(_splitsColIds[s]);
        std::vector<valType>* localVals = &(_splitsVals[s]);
        idxType localSize = localRowIds->size();

// here the usage of simd will cause write conflict on rowid
        for (idxType j = 0; j < localSize; ++j) {
            idxType colid = (*localColIds)[j];
            idxType rowid = (*localRowIds)[j];
            valType val = (*localVals)[j];
            y[rowid] += (val*x[colid]);
        }
    }

#else

    // split CSC spmv
#pragma omp parallel for num_threads(numThds) 
    for (idxType s = 0; s < _numsplits; ++s) {

        std::vector<idxType>* localRowIds = &(_splitsRowIds[s]);
        std::vector<idxType>* localColIds = &(_splitsColIds[s]);
        std::vector<valType>* localVals = &(_splitsVals[s]);
        idxType localSize = localRowIds->size();

// here the usage of simd will cause write conflict on rowid
        for (idxType j = 0; j < localSize; ++j) {
            idxType colid = (*localColIds)[j];
            idxType rowid = (*localRowIds)[j];
            valType val = (*localVals)[j];
            y[rowid] += (val*x[colid]);
        }
    }

#endif
}

// sparse matrix dense matrix (multiple dense vectors) 
template<class idxType, class valType>
void CSCGraph<idxType, valType>::spmmSplit(valType* x, valType* y, idxType xColNum, idxType numThds)
{
    // doing the computation
#pragma omp parallel for num_threads(numThds) 
    for (idxType s = 0; s < _numsplits; ++s) {

        std::vector<idxType>* localRowIds = &(_splitsRowIds[s]);
        std::vector<idxType>* localColIds = &(_splitsColIds[s]);
        // std::vector<valType>* localVals = &(_splitsVals[s]);
        idxType localSize = localRowIds->size();

        for (idxType j = 0; j < localSize; ++j) 
        {
            idxType colid = (*localColIds)[j];
            idxType rowid = (*localRowIds)[j];
            // valType val = (*localVals)[j];

            valType* readBufPtr = x + colid*xColNum;
            valType* writeBufPtr = y + rowid*xColNum;

#ifdef __INTEL_COMPILER
        __assume_aligned(readBufPtr, 64);
        __assume_aligned(writeBufPtr, 64);
#else
        __builtin_assume_aligned(readBufPtr, 64);
        __builtin_assume_aligned(writeBufPtr, 64);
#endif

#ifdef __AVX512F__ 
            // unrolled by 16 float
            int n16 = xColNum & ~(16-1); 
            __m512 tmpzero = _mm512_set1_ps (0);
            __mmask16 mask = (1 << (xColNum - n16)) - 1;

            __m512 vecX;
            __m512 vecY;
            __m512 vecBuf;

            for (int k = 0; k < n16; k+=16)
            {
                vecX = _mm512_load_ps (&(readBufPtr[k]));
                vecY = _mm512_load_ps (&(writeBufPtr[k]));
                vecBuf = _mm512_add_ps(vecX, vecY);
                _mm512_store_ps(&(writeBufPtr[k]), vecBuf);
            }

            if (n16 < xColNum)
            {
                vecX = _mm512_mask_load_ps(tmpzero, mask, &(readBufPtr[n16]));
                vecY = _mm512_mask_load_ps(tmpzero, mask, &(writeBufPtr[n16]));
                vecBuf = _mm512_add_ps(vecX, vecY);
                _mm512_mask_store_ps(&(writeBufPtr[n16]), mask, vecBuf);
            }

#else
            // compiler auto vectorization
// #pragma omp simd
#pragma omp simd aligned(writeBufPtr, readBufPtr: 64)
            for (int k = 0; k < xColNum; ++k) {
                writeBufPtr[k] += (readBufPtr[k]); 
            }
#endif


        }
    }

}

// sparse matrix dense matrix (multiple dense vectors) 
// used in benchmarking
template<class idxType, class valType>
double CSCGraph<idxType, valType>::spmmSplitExp(valType* x, valType* y, idxType xColNum, idxType numThds)
{
    double startTime = 0.0;
    double conversionTime = 0.0;
    double computeTime = 0.0;

    // data format conversion
    // creating the first buffer
    valType* readBuf = (valType*) _mm_malloc(_numVertices*xColNum*sizeof(valType), 64);
    valType* writeBuf = (valType*) _mm_malloc(_numVertices*xColNum*sizeof(valType), 64);

    startTime = utility::timer();
    // convert x from column majored to row majord
#pragma omp parallel for
    for (int i = 0; i < _numVertices; ++i) {
        for (int j = 0; j < xColNum; ++j) {
            readBuf[i*xColNum+j] = x[j*_numVertices+i];
        }
    }

    conversionTime += (utility::timer() - startTime);
    startTime = utility::timer();

    // doing the computation
#pragma omp parallel for num_threads(numThds) 
    for (idxType s = 0; s < _numsplits; ++s) 
    {

        std::vector<idxType>* localRowIds = &(_splitsRowIds[s]);
        std::vector<idxType>* localColIds = &(_splitsColIds[s]);
        // std::vector<valType>* localVals = &(_splitsVals[s]);
        idxType localSize = localRowIds->size();


        for (idxType j = 0; j < localSize; ++j) 
        {
            idxType colid = (*localColIds)[j];
            idxType rowid = (*localRowIds)[j];
            // valType val = (*localVals)[j];

            valType* readBufPtr = readBuf + colid*xColNum;
            valType* writeBufPtr = writeBuf + rowid*xColNum;

#ifdef __INTEL_COMPILER
            __assume_aligned(readBufPtr, 64);
            __assume_aligned(writeBufPtr, 64);
#else
            __builtin_assume_aligned(readBufPtr, 64);
            __builtin_assume_aligned(writeBufPtr, 64);
#endif

// #ifdef __AVX512F__ 
//             // unrolled by 16 float
//             int n16 = xColNum & ~(16-1); 
//             __m512 tmpzero = _mm512_set1_ps (0);
//             __mmask16 mask = (1 << (xColNum - n16)) - 1;
//
//             __m512 vecX;
//             __m512 vecY;
//             __m512 vecBuf;
//
//             for (int k = 0; k < n16; k+=16)
//             {
//                 vecX = _mm512_load_ps (&(readBufPtr[k]));
//                 vecY = _mm512_load_ps (&(writeBufPtr[k]));
//                 vecBuf = _mm512_add_ps(vecX, vecY);
//                 _mm512_store_ps(&(writeBufPtr[k]), vecBuf);
//             }
//
//             if (n16 < xColNum)
//             {
//                 vecX = _mm512_mask_load_ps(tmpzero, mask, &(readBufPtr[n16]));
//                 vecY = _mm512_mask_load_ps(tmpzero, mask, &(writeBufPtr[n16]));
//                 vecBuf = _mm512_add_ps(vecX, vecY);
//                 _mm512_mask_store_ps(&(writeBufPtr[n16]), mask, vecBuf);
//             }
//
// #else
            // compiler auto vectorization
#pragma omp simd aligned(writeBufPtr, readBufPtr: 64)
            for (int k = 0; k < xColNum; ++k) {
                writeBufPtr[k] += readBufPtr[k]; 
            }
// #endif

        }
    }

    computeTime += (utility::timer() - startTime);
    startTime = utility::timer();

    // convert writeBuf back to y 
#pragma omp parallel for
    for (int i = 0; i < _numVertices; ++i) {
        for (int j = 0; j < xColNum; ++j) {
            y[j*_numVertices+i] = writeBuf[i*xColNum+j];
        }
    }   

    conversionTime += (utility::timer() - startTime);

    _mm_free(readBuf);
    _mm_free(writeBuf);

    printf("Compute Time: %f sec, Conversion Time: %f sec\n", 
            computeTime, conversionTime);
    std::fflush(stdout);           

    return computeTime;
}


template<class idxType, class valType>
void CSCGraph<idxType, valType>::serialize(ofstream& outputFile)
{
#ifdef DISTRI
    outputFile.write((char*)&_numEdges, sizeof(idxType));
    outputFile.write((char*)&_numVertices, sizeof(idxType));
    outputFile.write((char*)&_vNLocal, sizeof(idxType));
    outputFile.write((char*)&_vOffset, sizeof(idxType));
    outputFile.write((char*)_degList, _numVertices*sizeof(idxType));
    outputFile.write((char*)_indexCol, (_vNLocal+1)*sizeof(idxType));
    outputFile.write((char*)_indexRow, (_indexCol[_vNLocal])*sizeof(idxType));
    outputFile.write((char*)_edgeVal, (_indexCol[_vNLocal])*sizeof(valType));
#else
    outputFile.write((char*)&_numEdges, sizeof(idxType));
    outputFile.write((char*)&_numVertices, sizeof(idxType));
    outputFile.write((char*)_degList, _numVertices*sizeof(idxType));
    outputFile.write((char*)_indexCol, (_numVertices+1)*sizeof(idxType));
    outputFile.write((char*)_indexRow, (_indexCol[_numVertices])*sizeof(idxType));
    outputFile.write((char*)_edgeVal, (_indexCol[_numVertices])*sizeof(valType));
#endif
}
        
template<class idxType, class valType>
void CSCGraph<idxType, valType>::deserialize(ifstream& inputFile)
{
#ifdef DISTRI

    inputFile.read((char*)&_numEdges, sizeof(idxType));
    inputFile.read((char*)&_numVertices, sizeof(idxType));
    inputFile.read((char*)&_vNLocal, sizeof(idxType));
    inputFile.read((char*)&_vOffset, sizeof(idxType));

    _degList = (idxType*) malloc (_numVertices*sizeof(idxType)); 
    inputFile.read((char*)_degList, _numVertices*sizeof(idxType));

    _indexCol = (idxType*) malloc ((_vNLocal+1)*sizeof(idxType)); 
    inputFile.read((char*)_indexCol, (_vNLocal+1)*sizeof(idxType));

    _indexRow = (idxType*) malloc (_indexCol[_vNLocal]*sizeof(idxType)); 
    inputFile.read((char*)_indexRow, (_indexCol[_vNLocal])*sizeof(idxType));

    _edgeVal = (valType*) malloc ((_indexCol[_vNLocal])*sizeof(valType)); 
    inputFile.read((char*)_edgeVal, (_indexCol[_vNLocal])*sizeof(valType));

    _nnZ = _indexCol[_vNLocal];

    printf("CSC Format Total vertices is : %d\n", _numVertices);
    printf("CSC Format Total Edges is : %d\n", _numEdges);
    printf("CSC Format Local vertices is : %d\n", _vNLocal);
    std::fflush(stdout); 

#else

    inputFile.read((char*)&_numEdges, sizeof(idxType));
    inputFile.read((char*)&_numVertices, sizeof(idxType));

    _degList = (idxType*) malloc (_numVertices*sizeof(idxType)); 
    inputFile.read((char*)_degList, _numVertices*sizeof(idxType));

    _indexCol = (idxType*) malloc ((_numVertices+1)*sizeof(idxType)); 
    inputFile.read((char*)_indexCol, (_numVertices+1)*sizeof(idxType));

    _indexRow = (idxType*) malloc (_indexCol[_numVertices]*sizeof(idxType)); 
    inputFile.read((char*)_indexRow, (_indexCol[_numVertices])*sizeof(idxType));

    _edgeVal = (valType*) malloc ((_indexCol[_numVertices])*sizeof(valType)); 
    inputFile.read((char*)_edgeVal, (_indexCol[_numVertices])*sizeof(valType));

    _nnZ = _indexCol[_numVertices];

    printf("CSC Format Total vertices is : %d\n", _numVertices);
    printf("CSC Format Total Edges is : %d\n", _numEdges);
    std::fflush(stdout); 

#endif
}

template<class idxType, class valType>
void CSCGraph<idxType, valType>::prepComBuf()
{
#ifdef DISTRI

    std::cout<<"start prep csc" <<std::endl;

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

    std::cout<<"Finish prep csc" <<std::endl;

#endif

}

/**
 * @brief 
 *
 * @tparam idxType
 * @tparam valType
 * @param input: length _vert_num, row_majored
 * @param output: length _local_vert_num, colum_majored
 * @param batchSize
 * @param sendMetaC
 * @param sendMetaDispls
 * @param recvMetaC
 * @param recvMetaDispls
 */
template<class idxType, class valType>
void CSCGraph<idxType, valType>::cscReduce(valType* input, valType* output, idxType batchSize,idxType* recvMetaC, idxType* recvMetaDispls)
{

#ifdef DISTRI
    // a row_majored tmpBuf
    valType* tmpBuf = new valType[_vNLocal*batchSize];

    // determine the meta counts and displacements
    for (int i = 0; i < _nprocs; ++i) {
        recvMetaC[i] = _recvCounts[i]*batchSize; 
    }

    recvMetaDispls[0] = 0;
    for (int i = 1; i < _nprocs; ++i) {
        recvMetaDispls[i] = recvMetaDispls[i-1] + recvMetaC[i-1]; 
    }

    // debug
    //std::cout<<"Rank: " << _myrank << " finish recvMeta" << std::endl;

    // mpi reduce 
    for (int i = 0; i < _nprocs; ++i) {

        MPI_Reduce((const void *)(input+recvMetaDispls[i]), (void *)tmpBuf, recvMetaC[i], MPI_FLOAT, 
               MPI_SUM, i, MPI_COMM_WORLD);

        // convert tmpbuf to output (row-majored to column majored)
        if (i == _myrank)
        {

#pragma omp parallel for num_threads(omp_get_max_threads())
           for (int k = 0; k < _vNLocal; ++k) {
                 for (int j = 0; j < batchSize; ++j) {
                    output[j*_vNLocal+k] = tmpBuf[k*batchSize+j];
                }
            }
        }
    }

    delete[] tmpBuf;

#endif
}

#endif
