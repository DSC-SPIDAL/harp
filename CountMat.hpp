#ifndef __COUNT_MAT__ 
#define __COUNT_MAT__

#include <stdlib.h>
#include <stdio.h>
#include <cstring>
// #include <string.h>

#include "Graph.hpp"
#include "CSRGraph.hpp"
#include "CSCGraph.hpp"
#include "DivideTemplates.hpp"
#include "IndexSys.hpp"
#include "DataTableColMajor.hpp"

using namespace std;

class CountMat {

    public:

        typedef int32_t idxType;
        // typedef int idxType;
        typedef float valType;

        CountMat(): _graph(nullptr), _templates(nullptr), _subtmp_array(nullptr), _colors_local(nullptr), 
        _bufVec(nullptr), _bufMatY(nullptr), _bufMatCols(-1), _bufVecLeaf(nullptr), _spmvTime(0), _eMATime(0), 
        _isPruned(1), _isScaled(0), _useSPMM(0), _peakMemUsage(0), _spmvElapsedTime(0), _fmaElapsedTime(0), _spmvFlops(0),
        _spmvMemBytes(0), _fmaFlops(0), _fmaMemBytes(0), _vtuneStart(-1), _calculate_automorphisms(false){} 

        void initialization(CSRGraph& graph, int thd_num, int itr_num, int isPruned, int useSPMM, int vtuneStart=-1,
                bool calculate_automorphisms = false);

        double compute(Graph& templates, bool isEstimate = false);

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

            if (_bufMatY != nullptr)
            {
#ifdef __INTEL_COMPILER
                _mm_free(_bufMatY); 
#else
                free(_bufMatY); 
#endif
            }

            if (_bufVecLeaf != nullptr) 
            {
                if (_useSPMM == 0)
                {
                    for (int i = 0; i < _color_num; ++i) 
                    {
#ifdef __INTEL_COMPILER
                        _mm_free(_bufVecLeaf[i]); 
#else
                        free(_bufVecLeaf[i]); 
#endif                   
                    }

                }
                else
                {
#ifdef __INTEL_COMPILER
                        _mm_free(_bufVecLeaf[0]); 
#else
                        free(_bufVecLeaf[0]); 
#endif                   

                }

                free(_bufVecLeaf);
            }

            

        }

        double estimateMemComm();
        double estimateMemCommNonPruned();

        double estimateFlops();
        double estimateFlopsNonPruned();

        double estimateTemplate();

        int automorphismNum();

    private:

        int calcAutomorphismRecursive(Graph& t, std::vector<int>& mappingID, std::vector<int>& restID);
        int calcAutomorphismZero(Graph& t, std::vector<int>& mappingID);

        double colorCounting();
        double sumVec(valType* input, idxType len);
        void scaleVec(valType* input, idxType len, double scale);
        double countNonBottomePruned(int subsId);
        double countNonBottomePrunedSPMM(int subsId);
        double countNonBottomeOriginal(int subsId);
        void colorInit();
        // trace the process mem usage
        void process_mem_usage(double& resident_set);
        void printSubTemps();
        void estimatePeakMemUsage();


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
        float* _bufMatY;
        int _bufMatCols;

        float** _bufVecLeaf;
        double _spmvTime;
        double _eMATime;
        int _isPruned;
        int _isScaled;
        int _useSPMM;
        double _peakMemUsage;
        bool _calculate_automorphisms;

        double _spmvFlops;
        double _spmvMemBytes;
        double _fmaFlops;
        double _fmaMemBytes;
        double _spmvElapsedTime;
        double _fmaElapsedTime;

        int _vtuneStart;
};

#endif
