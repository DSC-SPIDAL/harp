#ifndef __COUNT_H__
#define __COUNT_H__

#include <stdlib.h>
#include <stdio.h>
#include <cstring>

#include "Graph.hpp"
#include "DivideTemplates.hpp"
#include "IndexSys.hpp"
#include "DataTable.hpp"

using namespace std;

class Count {

    public:

        Count() 
        {
            _graph = NULL;
            _templates=NULL;
            _subtmp_array=NULL;
            _colors_local=NULL;
            _algoMode = 0;
            _vtuneStart = -1;
            _calculate_automorphisms = false;
            _spmvElapsedTime = 0;
            _fmaElapsedTime = 0;
        }

        void initialization(Graph& graph, int thd_num, int itr_num, int algoMode, int vtuneStart = -1, 
                bool calculate_automorphisms = false);

        double compute(Graph& templates, bool isEstimate = false);

        ~Count() {
            if (_colors_local != NULL)
                free(_colors_local);
        }

        int automorphismNum();

    private:

        int calcAutomorphismRecursive(Graph& t, std::vector<int>& mappingID, std::vector<int>& restID);
        int calcAutomorphismZero(Graph& t, std::vector<int>& mappingID);

        double colorCounting();
        double countNonBottomeFascia(int subsId);
        double countNonBottomeFasciaPruned(int subsId);
        double countNonBottomeVec(int subsId);
        double countNonBottomePruned(int subsId);
        double estimateMemComm();
        double estimateMemCommPruned();
        double estimateFlops();
        double estimateFlopsPruned();

        void colorInit();
        void colorInitSeq();
        int factorial(int n);

        // local graph data
        Graph* _graph;
        int _vert_num;
        int _max_deg;

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

        int _algoMode;
        int _vtuneStart;
        bool _calculate_automorphisms; 

        double _spmvElapsedTime;
        double _fmaElapsedTime;

        // divide the templates into sub-templates
        DivideTemplates div_tp;
        // CountsTable table;

        DataTabble _dTable;

        // index system
        IndexSys indexer;
        
};

#endif