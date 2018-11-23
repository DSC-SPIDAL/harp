#ifndef EDGE_LIST_H
#define EDGE_LIST_H

#include <cstring>
#include <cstdlib>
#include <stdlib.h>
#include <stdio.h>
#include <fstream>
#include <cstring>

#include "radix/pvector.h"
#include "radix/commons/graph.h"

using namespace std;

class EdgeList
{
    public:
        
        typedef int32_t idxType;

        EdgeList(): _numEdges(-1), _numVertices(-1), 
        _srcList(nullptr), _dstList(nullptr){}

        EdgeList(string fileName, int noVerticesNum = 0) 
        {
            if (noVerticesNum)
                readfromfileNoVerticesNum(fileName);
            else
                readfromfile(fileName);
        } 

        ~EdgeList() {
            if (_srcList != nullptr)
                delete[] _srcList;

            if (_dstList != nullptr)
                delete[] _dstList;
        }
    
        idxType getNumEdges() {return _numEdges;}
        idxType getNumVertices() {return _numVertices;}
        idxType* getSrcList() {return _srcList;}
        idxType* getDstList() {return _dstList;}

        // for radix
        void convertToRadixList(pvector<EdgePair<int32_t, int32_t> >& List);
        
    private:

        void readfromfile(string fileName);
        void readfromfileNoVerticesNum(string fileName);

        idxType _numEdges;
        idxType _numVertices;
        idxType* _srcList;
        idxType* _dstList;

};

#endif
