#include "DataTableColMajor.hpp"
#include <cstring>
#include <stdlib.h>

using namespace std;

void DataTableColMajor::initDataTable(Graph* subTempsList, IndexSys* indexer, int subsNum, int colorNum, idxType vertsNum, 
        int thdNum)
{

    _subTempsList = subTempsList; 
    _indexer = indexer;
    _subsNum = subsNum;
    _colorNum = colorNum;
    _vertsNum = vertsNum;
    _thdNum = thdNum;

    _dataTable = (float***) malloc (_subsNum*sizeof(float**));
    for(int i=0;i<_subsNum;i++)
        _dataTable[i] = nullptr;

    _isSubInited = (bool*) malloc (_subsNum*sizeof(bool));
    for(int i=0;i<_subsNum;i++)
        _isSubInited[i] = false;

    _tableLen = (int*) malloc (_subsNum*sizeof(int));
    for (int i = 0; i < _subsNum; ++i) {
       _tableLen[i] = _indexer->comb_calc(_colorNum, _subTempsList[i].get_vert_num()); 
    }

    // copy vals in parallel and vectorization with length _vertNum
    _blockSizeBasic = (_vertsNum)/_thdNum;
    _blockSize = (int*) malloc(_thdNum*sizeof(int));
    for(int i=0;i<_thdNum-1;i++)
        _blockSize[i] = _blockSizeBasic;

    // remains
    _blockSize[_thdNum-1] = ( (_vertsNum%_thdNum == 0) ) ? _blockSizeBasic : (_blockSizeBasic + (_vertsNum%_thdNum)); 

    _isInited = true;
}

void DataTableColMajor::initSubTempTable(int subsId)
{

    int lenCur = _tableLen[subsId];
    _dataTable[subsId] = (float**)malloc(lenCur*sizeof(float*));
    _curTable = _dataTable[subsId];
    _curSubId = subsId;

    // initialize and allocate the memory
#pragma omp parallel for
    for (int i = 0; i < lenCur; ++i) 
    {

#ifdef __INTEL_COMPILER
       _curTable[i] = (float*) _mm_malloc(_vertsNum*sizeof(float), 64); 
#else
       _curTable[i] = (float*) aligned_alloc(64, _vertsNum*sizeof(float)); 
#endif

       std::memset(_curTable[i], 0, _vertsNum*sizeof(float));
    }

    _isSubInited[subsId] = true;

}

void DataTableColMajor::initSubTempTable(int subsId, int mainId, int auxId)
{
    if (mainId != DUMMY_VAL && auxId != DUMMY_VAL) {
        _curMainTable = _dataTable[mainId];
        _curAuxTable = _dataTable[auxId];
        _curMainLen = _tableLen[mainId];
        _curAuxLen = _tableLen[auxId];
    }
    else
    {
        _curMainTable = NULL;
        _curAuxTable = NULL;
        _curMainLen = 0;
        _curAuxLen = 0;
    }

    if (subsId != 0) {
       initSubTempTable(subsId); 
    }

}

void DataTableColMajor::cleanSubTempTable(int subsId)
{
    if (_dataTable[subsId] != NULL)
    {
#pragma omp parallel for 
        for (int i = 0; i < _tableLen[subsId]; ++i) 
        {
            if (_dataTable[subsId][i] != nullptr)
            {
#ifdef __INTEL_COMPILER
                _mm_free(_dataTable[subsId][i]);
#else
                free(_dataTable[subsId][i]);
#endif
            }
        }
    }

    if (_isSubInited[subsId] && _dataTable[subsId] != nullptr) {
        free(_dataTable[subsId]);
        _dataTable[subsId] = nullptr;
    }

    _isSubInited[subsId] = false;
}

void DataTableColMajor::cleanTable()
{
    for (int i = 0; i < _subsNum; ++i) {
       cleanSubTempTable(i); 
    }

    free(_dataTable);
    free(_isSubInited);
    free(_tableLen);
    free(_blockSize);

}

void DataTableColMajor::setTableArray(int subsId, int colIdx, float*& vals)
{
    // update the colIdx array by vals
    float* obj = _dataTable[subsId][colIdx];
    updateArrayVec(vals, obj);
}

void DataTableColMajor::setCurTableArray(int colIdx, float*& vals)
{
    float* obj = _curTable[colIdx];
    updateArrayVec(vals, obj);
}

void DataTableColMajor::setCurTableArrayZero(int colIdx)
{
    std::memset(_curTable[colIdx], 0, _vertsNum*sizeof(float));
}

void DataTableColMajor::setMainArray(int colIdx, float*& vals)
{
    float* obj = _curMainTable[colIdx];
    updateArrayVec(vals, obj);
}

void DataTableColMajor::setAuxArray(int colIdx, float*& vals)
{
    float* obj = _curAuxTable[colIdx];
    updateArrayVec(vals, obj);
}

void DataTableColMajor::updateArrayVec(float*& src, float*& dst)
{
    float** blockPtrObj = new float*[_thdNum];
    float** blockPtrVals = new float*[_thdNum];
    blockPtrObj[0] = dst;
    blockPtrVals[0] = src;

    for (int i = 1; i < _thdNum; ++i) {
        blockPtrObj[i] = blockPtrObj[i-1] + _blockSizeBasic; 
        blockPtrVals[i] = blockPtrVals[i-1] + _blockSizeBasic;
    }

#pragma omp parallel for schedule(static) num_threads(_thdNum)
    for(int i=0; i<_thdNum; i++)
    {

        float* blockPtrObjLocal = blockPtrObj[i]; 
        float* blockPtrValsLocal = blockPtrVals[i]; 
        int blockSizeLocal = _blockSize[i];

#ifdef __INTEL_COMPILER
        __assume_aligned(blockPtrObjLocal, 64);           
        __assume_aligned(blockPtrValsLocal, 64);           
#else
        __builtin_assume_aligned(blockPtrObjLocal, 64);           
        __builtin_assume_aligned(blockPtrValsLocal, 64);           
#endif

#ifdef __INTEL_COMPILER
#pragma simd 
#else
#pragma GCC ivdep
#endif
        for(int j=0; j<blockSizeLocal;j++)
            blockPtrObjLocal[j] = blockPtrValsLocal[j];
    }

    delete[] blockPtrObj;
    delete[] blockPtrVals;

}


/**
 * @brief elemente-wised array-FMA
 *
 * @param dst
 * @param a
 * @param b
 */
void DataTableColMajor::arrayWiseFMA(float*& dst, float*& a, float*& b)
{
    float** blockPtrDst = new float*[_thdNum];
    float** blockPtrA = new float*[_thdNum];
    float** blockPtrB = new float*[_thdNum];

    blockPtrDst[0] = dst; 
    blockPtrA[0] = a;
    blockPtrB[0] = b;

    for (int i = 1; i < _thdNum; ++i) {
        blockPtrDst[i] = blockPtrDst[i-1] + _blockSizeBasic; 
        blockPtrA[i] = blockPtrA[i-1] + _blockSizeBasic;
        blockPtrB[i] = blockPtrB[i-1] + _blockSizeBasic;
    }

#pragma omp parallel for schedule(static) num_threads(_thdNum)
    for(int i=0; i<_thdNum; i++)
    {

        float* blockPtrDstLocal = blockPtrDst[i]; 
        float* blockPtrALocal = blockPtrA[i]; 
        float* blockPtrBLocal = blockPtrB[i]; 
        int blockSizeLocal = _blockSize[i];

#ifdef __INTEL_COMPILER
        __assume_aligned(blockPtrDstLocal, 64);           
        __assume_aligned(blockPtrALocal, 64);           
        __assume_aligned(blockPtrBLocal, 64);           
#else
        __builtin_assume_aligned(blockPtrDstLocal, 64);           
        __builtin_assume_aligned(blockPtrALocal, 64);           
        __builtin_assume_aligned(blockPtrBLocal, 64);           
#endif

#ifdef __INTEL_COMPILER
#pragma simd 
#else
#pragma GCC ivdep
#endif
        for(int j=0; j<blockSizeLocal;j++)
            blockPtrDstLocal[j] += blockPtrALocal[j]*blockPtrBLocal[j];
    }

    delete[] blockPtrDst;
    delete[] blockPtrA;
    delete[] blockPtrB;

}

void DataTableColMajor::countCurBottom(int*& idxCToC, int*& colorVals)
{
#pragma omp parallel for
    for(idxType v=0; v<_vertsNum; v++)
    {
        int* idxCToCLocal = idxCToC;
        int* colorValsLocal = colorVals;
        float** curTableLocal = _curTable;
        int idxLocal = idxCToCLocal[colorValsLocal[v]];
        curTableLocal[idxLocal][v] = 1.0; 
    }
}
