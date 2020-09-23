#ifndef CSRGRAPH_H
#define CSRGRAPH_H

#include <stdint.h>
#include <cstdlib>
#include <fstream>
#include <algorithm>
#include <iostream>

#ifndef NEC
#include "SpDM3/include/spmat.h"
#include "mkl.h"
// for RCM reordering
#include "SpMP/CSR.hpp"
#endif

#ifdef DISTRI
#include <mpi.h>
#endif

using namespace std;

// store the edgelist graph data into a CSR format (non-symmetric)
// use float type for entry and avoid the type conversion with count data
class CSRGraph
{
    public:

        typedef int32_t idxType;
        // typedef int idxType;
        typedef float valType;

        CSRGraph(): _isDirected(false), _isOneBased(false), _numEdges(-1), _numVertices(-1), _nnZ(-1),  
            _edgeVal(nullptr), _indexRow(nullptr), _indexCol(nullptr), 
            _degList(nullptr), _useMKL(false), _useRcm(false), _isDistri(false),
            _nprocs(0), _myrank(0), _vOffset(0), _vNLocal(0), 
            _sendCounts(nullptr), _recvCounts(nullptr),_sendDispls(nullptr), _recvDispls(nullptr),
            _sendBufLen(0), _recvBufLen(0){}

        // for distributed version
        CSRGraph(bool isDistri, int nprocs, int myrank): _isDirected(false), _isOneBased(false), _numEdges(-1), _numVertices(-1), _nnZ(-1),  
            _edgeVal(nullptr), _indexRow(nullptr), _indexCol(nullptr), 
            _degList(nullptr), _useMKL(false), _useRcm(false), _isDistri(isDistri),
            _nprocs(nprocs), _myrank(myrank), _vOffset(0), _vNLocal(0), 
            _sendCounts(nullptr), _recvCounts(nullptr),_sendDispls(nullptr), _recvDispls(nullptr) ,
            _sendBufLen(0), _recvBufLen(0){}

        ~CSRGraph(){
            if (_edgeVal != nullptr)
                free(_edgeVal);

            if (_indexRow != nullptr)
                free(_indexRow);

            if (_indexCol != nullptr)
                free(_indexCol);

            if (_sendCounts)
                delete[] _sendCounts;

            if (_recvCounts)
                delete[] _recvCounts;

            if (_sendDispls)
                delete[] _sendDispls;

            if (_recvDispls)
                delete[] _recvDispls;

        }

        valType* getEdgeVals(idxType rowId)
        {
#ifndef NEC
            return (_rcmMatR != nullptr) ? (_rcmMatR->svalues + _rcmMatR->rowptr[rowId]) : (_edgeVal + _indexRow[rowId]); 
#else
            return (_edgeVal + _indexRow[rowId]); 
#endif
        }

        idxType* getColIdx(idxType rowId)
        {
#ifndef NEC
            return (_rcmMatR != nullptr ) ? (_rcmMatR->colidx + _rcmMatR->rowptr[rowId]) : (_indexCol + _indexRow[rowId]);
#else
            return (_indexCol + _indexRow[rowId]);
#endif
        }

        idxType getRowLen(idxType rowId)
        {
#ifndef NEC
            return (_rcmMatR != nullptr ) ? (_rcmMatR->rowptr[rowId + 1] - _rcmMatR->rowptr[rowId]) : (_indexRow[rowId+1] - _indexRow[rowId]);
#else
            return (_indexRow[rowId+1] - _indexRow[rowId]);
#endif
        }

        void SpMVNaive(valType* x, valType* y);
        void SpMVNaiveFull(valType* x, valType* y);
        void SpMVNaiveFullDistri(valType* x, valType* y, int thdNum);

        void SpMVNaiveScale(valType* x, valType* y, float scale);
        void SpMVNaive(valType* x, valType* y, int thdNum);
        void SpMVNaiveFull(valType* x, valType* y, int thdNum);
        void SpMVMKL(valType* x, valType* y, int thdNum);
        void SpMMMKL(valType* x, valType* y, idxType m, idxType n, idxType k, int thdNum);
        void SpMVMKLDistri(valType* x, valType* y, int thdNum);
        void SpMVMKLHint(int callNum);
        void SpMMMKLHint(int mCols, int callNum);

        // for distributed env
        bool isDistributed () { return _isDistri; }
        idxType getVNLocal() {return _vNLocal;}
        idxType getVOffset() {return _vOffset;}
        idxType getSendBuflen() { return _sendBufLen; }
        idxType getRecvBuflen() { return _recvBufLen; }

        void prepComBuf(); 
        void assembleSendBuf(valType* data, valType* sendbuf, idxType dataLen, 
                idxType batchSize, idxType* metaC, idxType* metaDispls);


        void csrAlltoAll(valType* sendbuf, valType* recvbuf, idxType bufLen, idxType batchSize, idxType* sendMetaC,
                idxType* sendMetaDispls, idxType* recvMetaC, idxType* recvMetaDispls);

        void reorgBuf(valType* input, valType* output, idxType batchSize, idxType* recvMetaC, idxType* recvMetaDispls);

        idxType getNumVertices() {return _numVertices;}

#ifdef DISTRI
        idxType getNNZ() {return _indexRow[_vNLocal]; }
#else
        idxType getNNZ() {return _indexRow[_numVertices]; }
#endif
        idxType getNNZDistri() {return _indexRow[_vNLocal]; }
        idxType* getIndexRow() {return _indexRow;}
        idxType* getIndexCol() {return _indexCol;}
        valType* getNNZVal() {return _edgeVal;}         
        void createFromEdgeListFile(idxType numVerts, idxType numEdges, 
                idxType* srcList, idxType* dstList, bool useMKL = false, bool useRcm = false, bool isBenchmark = false);       

        // make the csr format from 0 based index to 1 based index
        // used by csrmm of MKL
        void makeOneIndex();

        idxType* getDegList() {return _degList;}

        void serialize(ofstream& outputFile);
        void deserialize(int inputFile, bool useMKL = false, bool useRcm = false);

#ifndef NEC
        void fillSpMat(spdm3::SpMat<int, float> &smat);
#endif
        void rcmReordering();
        void createMKLMat();
        void toASCII(string fileName);

        bool useMKL() { return _useMKL; }

    private:

        bool _isDirected;
        bool _isOneBased;
        idxType _numEdges;
        idxType _numVertices;
        idxType _nnZ;
        valType* _edgeVal;
        idxType* _indexRow;
        idxType* _indexCol;
        idxType* _degList;
#ifndef NEC
        sparse_matrix_t _mklA;
        matrix_descr _descA;
#endif
        bool _useMKL;

#ifndef NEC
        SpMP::CSR* _rcmMat;
        SpMP::CSR* _rcmMatR;
#endif
        bool _useRcm;

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

#endif
