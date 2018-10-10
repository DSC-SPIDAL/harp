#ifndef __COUNT_H__
#define __COUNT_H__

#include <stdlib.h>
#include <stdio.h>
#include <cstring>

#include "Graph.hpp"
#include "DivideTemplates.hpp"
#include "CountsTable.hpp"

using namespace std;

class Count {

    public:

        Count() 
        {
            _graph = NULL;
            _templates=NULL;
            _subtmp_array=NULL;
            _colors_local=NULL;
            _appro_vert_counts=NULL;
        }

        void initialization(Graph& graph, int thd_num, int itr_num);

        double compute(Graph& templates);

        ~Count() {}

    private:

        // local graph data
        Graph* _graph;
        int _vert_num;

        // templates and sub-temps chain
        Graph* _templates; 
        Graph* _subtmp_array;

        // local coloring for each verts
        int* _colors_local;

        // iterations
        int _itr_num;
        int _itr;

        // omp threads num
        int _thd_num;

        // divide the templates into sub-templates
        DivideTemplates div_tp;
        // CountsTable table;


        // index system
        
        // counts container
        double* _appro_vert_counts;
        
};

#endif
