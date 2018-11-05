#ifndef __COUNT_MAT__ 
#define __COUNT_MAT__

#include <stdlib.h>
#include <stdio.h>
#include <cstring>

#include "Graph.hpp"
#include "CSRGraph.hpp"
#include "DivideTemplates.hpp"
#include "IndexSys.hpp"
#include "DataTableColMajor.hpp"

using namespace std;

class CountMat {

    public:

        typedef int32_t idxType;
        typedef float valType;

        CountMat(): _graph(nullptr), _templates(nullptr), _subtmp_array(nullptr), _colors_local(nullptr), _bufVec(nullptr), _bufVecThd(nullptr), _spmvTime(0), _eMATime(0), _isPruned(1) {} 

        void initialization(CSRGraph& graph, int thd_num, int itr_num, int isPruned);

        double compute(Graph& templates);

        ~CountMat() 
        {
            if (_colors_local != nullptr)
                free(_colors_local);

            if (_bufVec != nullptr) 
            {
#ifdef __INTEL_COMPILER
                _mm_free(_bufVec); 
#else
                free(_bufVec); 
#endif
            }

            if (_bufVecThd != nullptr)
            {
                for (int i = 0; i < _thd_num; ++i) {
                    if (_bufVecThd[i] != nullptr) 
                    {
#ifdef __INTEL_COMPILER
                        _mm_free(_bufVecThd[i]);
#else
                        free(_bufVecThd[i]); 
#endif
                    }
                }
                free(_bufVecThd);
            }
        }

    private:

        double colorCounting();
        double countNonBottomePruned(int subsId);
        double countNonBottomeOriginal(int subsId);
        void colorInit();
        int factorial(int n);

        // local graph data
        CSRGraph* _graph;
        idxType _vert_num;

        // templates and sub-temps chain
        Graph* _templates; 
        Graph* _subtmp_array;
        int _total_sub_num;
        // total color num equals to the size of template
        int _color_num;

        // local coloring for each verts
        int* _colors_local;

        // iterations
        int _itr_num;
        int _itr;

        // omp threads num
        int _thd_num;

        // divide the templates into sub-templates
        DivideTemplates div_tp;

        // counts table
        DataTableColMajor _dTable;

        // index system
        IndexSys indexer;
        
        float* _bufVec;
        float** _bufVecThd;
        double _spmvTime;
        double _eMATime;
        int _isPruned;
};

#endif
