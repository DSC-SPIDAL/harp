#ifndef CSRGRAPH_H
#define CSRGRAPH_H

#include <stdint.h>
#include <cstdlib>
#include <fstream>

#include "SpDM3/include/spmat.h"

using namespace std;

// store the edgelist graph data into a CSR format (non-symmetric)
// use float type for entry and avoid the type conversion with count data
class CSRGraph
{
    public:

        typedef int32_t idxType;
        typedef float valType;

        CSRGraph(): _isDirected(false), _isOneBased(false), _numEdges(-1), _numVertices(-1), _nnZ(-1), 
            _edgeVal(nullptr), _indexRow(nullptr), _indexCol(nullptr), 
            _degList(nullptr) {}

        ~CSRGraph(){
            if (_edgeVal != nullptr)
                free(_edgeVal);

            if (_indexRow != nullptr)
                free(_indexRow);

            if (_indexCol != nullptr)
                free(_indexCol);
        }

        valType* getEdgeVals(idxType rowId)
        {
            return (_edgeVal + _indexRow[rowId]); 
        }

        idxType* getColIdx(idxType rowId)
        {
            return (_indexCol + _indexRow[rowId]);
        }

        idxType getRowLen(idxType rowId)
        {
            return (_indexRow[rowId+1] - _indexRow[rowId]);
        }

        void SpMVNaive(valType* x, valType* y);
        void SpMVNaiveScale(valType* x, valType* y, float scale);
        void SpMVNaive(valType* x, valType* y, int thdNum);
        void SpMVMKL(valType* x, valType* y, int thdNum);

        idxType getNumVertices() {return _numVertices;} 

        // idxType getNNZ() {return _indexRow[_numVertices];}
        idxType getNNZ() {return _nnZ;}
        idxType* getIndexRow() {return _indexRow;}
        idxType* getIndexCol() {return _indexCol;}
        valType* getNNZVal() {return _edgeVal;} 
         
        void createFromEdgeListFile(idxType numVerts, idxType numEdges, 
                idxType* srcList, idxType* dstList);       

        // make the csr format from 0 based index to 1 based index
        // used by csrmm of MKL
        void makeOneIndex();

        void serialize(ofstream& outputFile);
        void deserialize(ifstream& inputFile);

        void fillSpMat(spdm3::SpMat<int, float> &smat);

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
};

#endif
