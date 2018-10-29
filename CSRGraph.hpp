#ifndef CSRGRAPH_H
#define CSRGRAPH_H

#include <stdint.h>
#include <cstdlib>

using namespace std;

// store the edgelist graph data into a CSR format (non-symmetric)
// use float type for entry and avoid the type conversion with count data
class CSRGraph
{
    public:

        typedef int32_t idxType;
        typedef float valType;

        CSRGraph(): _isDirected(false), _numEdges(-1), _numVertices(-1), 
            _edgeVal(nullptr), _indexRow(nullptr), _indexCol(nullptr), 
            _degList(nullptr){}

        // construct CSRGraph from edgelist file
        CSRGraph(idxType numEdges, idxType numVerts, 
                idxType* srcList, idxType* dstList): _isDirected(false)
        {
            createFromEdgeListFile(numEdges, numVerts, srcList, dstList);
        }

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

        idxType getNumVertices() {return _numVertices;} 

    private:

        void createFromEdgeListFile(idxType numVerts, idxType numEdges, 
                idxType* srcList, idxType* dstList);

        bool _isDirected;
        idxType _numEdges;
        idxType _numVertices;
        valType* _edgeVal;
        idxType* _indexRow;
        idxType* _indexCol;
        idxType* _degList;
};

#endif
