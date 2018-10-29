#include "EdgeList.hpp"
using namespace std;

void EdgeList::readfromfile(string fileName)
{
    string line;
    ifstream file_strm;
    file_strm.open(fileName.c_str());

    // get the vert num
    std::getline(file_strm, line);
    _numVertices = atoi(line.c_str());
    // get the edge num
    std::getline(file_strm, line);
    _numEdges = atoi(line.c_str());

    _srcList = new EdgeList::idxType[_numEdges];
    _dstList = new EdgeList::idxType[_numEdges];

    EdgeList::idxType max_id = 0;
    for(EdgeList::idxType j=0;j<_numEdges;j++)
    {
        std::getline(file_strm, line, ' ');
        _srcList[j] = atoi(line.c_str());
        max_id = (_srcList[j] > max_id) ? _srcList[j]: max_id;

        std::getline(file_strm, line);
        _dstList[j] = atoi(line.c_str());
        max_id = (_dstList[j] > max_id) ? _dstList[j]: max_id;
    }

    file_strm.close();

    if (max_id != _numVertices - 1)
    {
#ifdef VERBOSE
        // remove "holes"
        printf("Start remove holes; max_id: %d, n_g: %d\n", max_id, _numVertices); 
        std::fflush(stdout);
#endif

        EdgeList::idxType* v_id = new EdgeList::idxType[max_id+1];
        std::memset(v_id, 0, (max_id+1)*sizeof(EdgeList::idxType));

        for(EdgeList::idxType i=0;i<_numEdges;i++)
        {
            v_id[_srcList[i]] = 1;
            v_id[_dstList[i]] = 1;
        }

        EdgeList::idxType itr = 0;
        for(EdgeList::idxType i=0;i<max_id+1;i++)
        {
            if (v_id[i] == 1)
                v_id[i] = (itr++);
        }

        for(EdgeList::idxType i=0;i<_numEdges;i++)
        {
            _srcList[i] = v_id[_srcList[i]];
            _dstList[i] = v_id[_dstList[i]];
        }
#ifdef VERBOSE
        printf("Finish remove holes\n");
        std::fflush(stdout);
#endif
        delete[] v_id;
    }

}
