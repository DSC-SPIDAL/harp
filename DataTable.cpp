#include "DataTable.hpp"
#include <cstring>
#include <stdlib.h>

using namespace std;

void DataTabble::initDataTable(Graph* subTempsList, IndexSys* indexer, int subsNum, int colorNum, int vertsNum)
{

    _subTempsList = subTempsList; 
    _indexer = indexer;
    _subsNum = subsNum;
    _colorNum = colorNum;
    _vertsNum = vertsNum;

    _dataTable = (float***) malloc (_subsNum*sizeof(float**));
    for(int i=0;i<_subsNum;i++)
        _dataTable[i] = NULL;

    _isSubInited = (bool*) malloc (_subsNum*sizeof(bool));
    for(int i=0;i<_subsNum;i++)
        _isSubInited[i] = false;

    _tableLen = (int*) malloc (_subsNum*sizeof(int));
    for (int i = 0; i < _subsNum; ++i) {
       _tableLen[i] = _indexer->comb_calc(_colorNum, _subTempsList[i].get_vert_num()); 
    }
    
    _isInited = true;
}

void DataTabble::initSubTempTable(int subsId)
{

    _dataTable[subsId] = (float**)malloc(_vertsNum*sizeof(float*));
    _curTable = _dataTable[subsId];
    _curSubId = subsId;

#pragma omp parallel for
    for (int i = 0; i < _vertsNum; ++i) {
       _curTable[i] = NULL; 
    }

    _isSubInited[subsId] = true;
}

void DataTabble::initSubTempTable(int subsId, int mainId, int auxId)
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

void DataTabble::cleanSubTempTable(int subsId)
{
    if (_dataTable[subsId] != NULL)
    {
#pragma omp parallel for 
        for (int v = 0; v < _vertsNum; ++v) {
            if (_dataTable[subsId][v] != NULL)
            {
#ifdef __INTEL_COMPILER
                _mm_free(_dataTable[subsId][v]);
#else
                free(_dataTable[subsId][v]);
#endif
            }
        }
    }

    if (_isSubInited[subsId] && _dataTable[subsId] != NULL) {
        free(_dataTable[subsId]);
        _dataTable[subsId] = NULL;
    }

    _isSubInited[subsId] = false;
}

void DataTabble::cleanTable()
{
    for (int i = 0; i < _subsNum; ++i) {
       cleanSubTempTable(i); 
    }

    free(_dataTable);
    free(_isSubInited);
    free(_tableLen);

}

float DataTabble::getTableCell(int subsId, int vertId, int combIdx)
{
    if (_dataTable[subsId][vertId]) {
        return _dataTable[subsId][vertId][combIdx];
    }
    else
        return 0.0;
}

float DataTabble::getMainCell(int vertId, int combIdx)
{

    if (_curMainTable[vertId]) {
        return _curMainTable[vertId][combIdx];
    }
    else
        return 0.0;
}

float DataTabble::getAuxCell(int vertId, int combIdx)
{
    if (_curAuxTable[vertId]) {
        return _curAuxTable[vertId][combIdx];
    }
    else
        return 0.0;
}

float* DataTabble::getTableArray(int subsId, int vertId)
{
    return _dataTable[subsId][vertId];
}

float* DataTabble::getMainArray(int vertId)
{
    return _curMainTable[vertId];
}

float* DataTabble::getAuxArray(int vertId)
{
    return _curAuxTable[vertId];
}

void DataTabble::setTableCell(int subsId, int vertId, int combIdx, float val)
{
    if (_dataTable[subsId][vertId] == NULL) {
#ifdef __INTEL_COMPILER
        _dataTable[subsId][vertId] = (float*) _mm_malloc(_tableLen[subsId]*sizeof(float),64);
#else
        _dataTable[subsId][vertId] = (float*) aligned_alloc(64, _tableLen[subsId]*sizeof(float));
#endif
        std::memset(_dataTable[subsId][vertId], 0, _tableLen[subsId]*sizeof(float));
    }

    _dataTable[subsId][vertId][combIdx] = val;
}

void DataTabble::setCurTableCell(int vertId, int combIdx, float val)
{
    if (_curTable[vertId] == NULL) {

#ifdef __INTEL_COMPILER
        _curTable[vertId] = (float*) _mm_malloc(_tableLen[_curSubId]*sizeof(float), 64);
#else
        _curTable[vertId] = (float*) aligned_alloc(64, _tableLen[_curSubId]*sizeof(float));
#endif
        std::memset(_curTable[vertId], 0, _tableLen[_curSubId]*sizeof(float));
    }

    _curTable[vertId][combIdx] = val;
}
