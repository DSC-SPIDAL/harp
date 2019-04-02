#ifndef CSRGRAPH_H
#define CSRGRAPH_H

#include <stdint.h>
#include <cstdlib>
#include <fstream>
#include <algorithm>
#include <iostream>

#ifndef GPU
#include "SpDM3/include/spmat.h"
#include "mkl.h"
// for RCM reordering
#include "SpMP/CSR.hpp"
#endif

#ifdef GPU
#include <cuda_runtime.h>
#include "cusparse.h"
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
#ifndef GPU
        CSRGraph(): _isDirected(false), _isOneBased(false), _numEdges(-1), _numVertices(-1), _nnZ(-1),  
            _edgeVal(nullptr), _indexRow(nullptr), _indexCol(nullptr), 
            _degList(nullptr), _useMKL(false), _useRcm(false), _rcmMat(nullptr), _rcmMatR(nullptr) {}
#else
        CSRGraph(): _isDirected(false), _isOneBased(false), _numEdges(-1), _numVertices(-1), _nnZ(-1),  
            _edgeVal(nullptr), _indexRow(nullptr), _indexCol(nullptr), 
            _edgeValDev(nullptr), _indexRowDev(nullptr), _indexColDev(nullptr), _handle(0), _descr(0), _cudaAlpha(1.0), _cudaBeta(0.0), _gpuBlkSize(512), 
            _degList(nullptr), _useMKL(false), _useRcm(false) {}
#endif

        ~CSRGraph(){
            if (_edgeVal != nullptr)
                free(_edgeVal);

            if (_indexRow != nullptr)
                free(_indexRow);

            if (_indexCol != nullptr)
                free(_indexCol);
#ifndef GPU
            if (_rcmMatR != nullptr)
                delete _rcmMatR;
#else
            if (_edgeValDev != nullptr)
                cudaFree(_edgeValDev);

            if (_indexRowDev != nullptr)
                cudaFree(_indexRowDev);

            if (_indexColDev != nullptr)
                cudaFree(_indexColDev);
#endif
        }

        valType* getEdgeVals(idxType rowId)
        {
#ifndef GPU
            return (_rcmMatR != nullptr) ? (_rcmMatR->svalues + _rcmMatR->rowptr[rowId]) : (_edgeVal + _indexRow[rowId]); 
#else
            return (_edgeVal + _indexRow[rowId]); 
#endif
        }

        idxType* getColIdx(idxType rowId)
        {
#ifndef GPU
            return (_rcmMatR != nullptr ) ? (_rcmMatR->colidx + _rcmMatR->rowptr[rowId]) : (_indexCol + _indexRow[rowId]);
#else
            return (_indexCol + _indexRow[rowId]);
#endif
        }

        idxType getRowLen(idxType rowId)
        {
#ifndef GPU
            return (_rcmMatR != nullptr ) ? (_rcmMatR->rowptr[rowId + 1] - _rcmMatR->rowptr[rowId]) : (_indexRow[rowId+1] - _indexRow[rowId]);
#else
            return (_indexRow[rowId+1] - _indexRow[rowId]);
#endif
        }

        void SpMVNaive(valType* x, valType* y);
        void SpMVNaiveFull(valType* x, valType* y);
        void SpMVNaiveScale(valType* x, valType* y, float scale);
        void SpMVNaive(valType* x, valType* y, int thdNum);
        void SpMVNaiveFull(valType* x, valType* y, int thdNum);
        void SpMVMKL(valType* x, valType* y, int thdNum);
        void SpMVMKLHint(int callNum);

#ifdef GPU
        void setGPUBlkSize(int size) { _gpuBlkSize = size; }
        void cudaSpMV(valType* xInput, valType* yOutput);
        void cudaSpMVCuSparse(valType* xInput, valType* yOutput);
        void cudaSpMMCuSparse(valType* xInput, valType* yOutput, idxType batchSize);
#endif

#ifndef GPU
        idxType getNumVertices() {return (_rcmMatR != nullptr) ? _rcmMatR->m : _numVertices;} 
#else
        idxType getNumVertices() {return _numVertices;} 
#endif

#ifndef GPU
        idxType getNNZ() {return (_rcmMatR != nullptr) ? _rcmMatR->rowptr[_rcmMatR->m] : _indexRow[_numVertices]; }
        idxType* getIndexRow() {return (_rcmMatR != nullptr) ? _rcmMatR->rowptr : _indexRow;}
        idxType* getIndexCol() {return (_rcmMatR != nullptr) ? _rcmMatR->colidx : _indexCol;}
        valType* getNNZVal() {return (_rcmMatR != nullptr) ? _rcmMatR->svalues : _edgeVal;} 
#else
        idxType getNNZ() {return _indexRow[_numVertices]; }
        idxType* getIndexRow() {return _indexRow;}
        idxType* getIndexCol() {return _indexCol;}
        valType* getNNZVal() {return _edgeVal;} 
#endif
         
        void createFromEdgeListFile(idxType numVerts, idxType numEdges, 
                idxType* srcList, idxType* dstList, bool useMKL = false, bool useRcm = false, bool isBenchmark = false);       

        // make the csr format from 0 based index to 1 based index
        // used by csrmm of MKL
        void makeOneIndex();

        idxType* getDegList() {return _degList;}

        void serialize(ofstream& outputFile);
        void deserialize(ifstream& inputFile, bool useMKL = false, bool useRcm = false);

#ifndef GPU
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

#ifdef GPU
        valType* _edgeValDev;
        idxType* _indexRowDev;
        idxType* _indexColDev;
        cusparseStatus_t _status;
        cusparseHandle_t _handle;
        cusparseMatDescr_t _descr;
        // used in CuSparse
        valType _cudaAlpha;
        valType _cudaBeta;
        int _gpuBlkSize;
#endif

        idxType* _degList;
#ifndef GPU
        sparse_matrix_t _mklA;
        matrix_descr _descA;
#endif
        bool _useMKL;

#ifndef GPU
        SpMP::CSR* _rcmMat;
        SpMP::CSR* _rcmMatR;
#endif
        bool _useRcm;
};

#endif
