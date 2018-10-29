#ifndef DATATABLE_H
#define DATATABLE_H

#include "Graph.hpp"
#include "IndexSys.hpp"
#include "Helper.hpp"

using namespace std;

class DataTabble
{
    public:

        DataTabble() {
            _subTempsList = NULL;
            _dataTable = NULL;
            _tableLen = NULL;
            _curTable = NULL;
            _curMainTable = NULL;
            _curAuxTable = NULL;
            _indexer = NULL;
        }

        ~DataTabble(){ cleanTable();}

        void initDataTable(Graph* subTempsList, IndexSys* indexer, int subsNum, int colorNum, int vertsNum);
        void initSubTempTable(int subsId);
        void initSubTempTable(int subsId, int mainId, int auxId);
        void cleanSubTempTable(int subsId);
        void cleanTable();

        float getTableCell(int subsId, int vertId, int combIdx);
        float getMainCell(int vertId, int combIdx);
        float getAuxCell(int vertId, int combIdx);
        float* getTableArray(int subsId, int vertId);
        float* getMainArray(int vertId);
        float* getAuxArray(int vertId);

        void setTableCell(int subsId, int vertId, int combIdx, float val);
        void setCurTableCell(int vertId, int combIdx, float val);

        int getMainLen(){ return _curMainLen;}
        int getAuxLen(){ return _curAuxLen; }
        int getTableLen(int subsId) { return _tableLen[subsId]; }
        bool isInited() { return _isInited; }
        bool isSubInited(int subsId) { return _isSubInited[subsId]; }
        bool isVertInitMain(int vertId) {return (_curMainTable[vertId] != NULL) ? true: false;}
        bool isVertInitAux(int vertId) {return (_curAuxTable[vertId] != NULL) ? true: false;}

    private:

        Graph* _subTempsList;
        IndexSys* _indexer;

        int _colorNum;
        int _subsNum;
        int _vertsNum;
        bool _isInited;
        bool* _isSubInited;

        float*** _dataTable;
        int* _tableLen;
        int _curMainLen;
        int _curAuxLen;
        int _curSubId;

        float** _curTable;
        float** _curMainTable;
        float** _curAuxTable;
    
};



#endif /* ifndef DATATABLE_H */
