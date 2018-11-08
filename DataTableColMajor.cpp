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

    _blockPtrDst = (float**) malloc(_thdNum*sizeof(float*));
    _blockPtrA = (float**) malloc(_thdNum*sizeof(float*));
    _blockPtrB = (float**) malloc(_thdNum*sizeof(float*));
    for (int i = 0; i < _thdNum; ++i) {
       _blockPtrDst = nullptr; 
       _blockPtrA = nullptr; 
       _blockPtrB = nullptr; 
    }

    _isInited = true;
}

void DataTableColMajor::initSubTempTable(int subsId)
{

    if (_subTempsList[subsId].get_vert_num() > 1 || subsId == _subsNum -1)
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
    else
    {
        #ifdef VERBOSE
           printf("Link to the last subtemplate\n"); 
           std::fflush(stdout);
        #endif
        
        // point to the last sub-template
        _dataTable[subsId] = _dataTable[_subsNum - 1];
        _curTable = _dataTable[subsId];
        _curSubId = subsId;

        _isSubInited[subsId] = true;
    }

    // debug check the total memory usage (GB) 
    // printf("Vert num: %d, curLen: %d, mem: %f GB\n", _vertsNum, lenCur, ((double)(_vertsNum*lenCur)*4/1024/1024/1024));
    // std::fflush(stdout);

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
        _curMainTable = nullptr;
        _curAuxTable = nullptr;
        _curMainLen = 0;
        _curAuxLen = 0;
    }

    if (subsId != 0) {
       initSubTempTable(subsId); 
    }

}

void DataTableColMajor::cleanSubTempTable(int subsId, bool isBottom)
{
    if (_subTempsList[subsId].get_vert_num() > 1 && isBottom)
    {
        if (_dataTable[subsId] != nullptr)
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
}

void DataTableColMajor::cleanTable()
{
    for (int i = 0; i < _subsNum-1; ++i) {
       cleanSubTempTable(i, false); 
    }

    // clean bottom template 
    cleanSubTempTable(_subsNum-1, true); 

    free(_dataTable);
    free(_isSubInited);
    free(_tableLen);
    free(_blockSize);

    free(_blockPtrDst);
    free(_blockPtrA);
    free(_blockPtrB);
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
    _blockPtrA[0] = dst;
    _blockPtrB[0] = src;

    for (int i = 1; i < _thdNum; ++i) {
        _blockPtrA[i] = _blockPtrA[i-1] + _blockSizeBasic; 
        _blockPtrB[i] = _blockPtrB[i-1] + _blockSizeBasic;
    }

#pragma omp parallel for schedule(static) num_threads(_thdNum)
    for(int i=0; i<_thdNum; i++)
    {

        float* blockPtrObjLocal = _blockPtrA[i]; 
        float* blockPtrValsLocal = _blockPtrB[i]; 
        int blockSizeLocal = _blockSize[i];

#pragma omp simd aligned(blockPtrObjLocal, blockPtrValsLocal: 64)
        for(int j=0; j<blockSizeLocal;j++)
            blockPtrObjLocal[j] = blockPtrValsLocal[j];
    }

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
    _blockPtrDst[0] = dst; 
    _blockPtrA[0] = a;
    _blockPtrB[0] = b;

    for (int i = 1; i < _thdNum; ++i) {
        _blockPtrDst[i] = _blockPtrDst[i-1] + _blockSizeBasic; 
        _blockPtrA[i] = _blockPtrA[i-1] + _blockSizeBasic;
        _blockPtrB[i] = _blockPtrB[i-1] + _blockSizeBasic;
    }

#pragma omp parallel for schedule(static) num_threads(_thdNum)
    for(int i=0; i<_thdNum; i++)
    {

        float* blockPtrDstLocal = _blockPtrDst[i]; 
        float* blockPtrALocal = _blockPtrA[i]; 
        float* blockPtrBLocal = _blockPtrB[i]; 
        int blockSizeLocal = _blockSize[i];

#pragma omp simd aligned(blockPtrDstLocal, blockPtrALocal, blockPtrBLocal: 64)
        for(int j=0; j<blockSizeLocal;j++)
            blockPtrDstLocal[j] = blockPtrDstLocal[j] + blockPtrALocal[j]*blockPtrBLocal[j];
    }

}

void DataTableColMajor::arrayWiseFMANaive(float* dst, float* a, float* b)
{

#pragma omp parallel for simd schedule(static) aligned(dst, a, b: 64)
    for (int i = 0; i < _vertsNum; ++i) {
        dst[i] = dst[i] + a[i]*b[i]; 
    }

}

void DataTableColMajor::arrayWiseFMANaive(float* dst, float* a, float* b, int thdNum)
{

#pragma omp parallel for simd schedule(static) num_threads(thdNum) aligned(dst, a, b: 64)
    for (int i = 0; i < _vertsNum; ++i) {
        dst[i] = dst[i] + a[i]*b[i]; 
    }

}

void DataTableColMajor::countCurBottom(int*& idxCToC, int*& colorVals)
{
    if (_curSubId == _subsNum - 1)
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
    
}
