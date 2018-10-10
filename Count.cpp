// the implementation of the color-coding algorithms
// Author: Langshi Chen

#include "Count.hpp"

using namespace std;

void Count::initialization(Graph& graph, int thd_num, int itr_num)
{
    _graph = &graph;
    _vert_num = _graph->get_vert_num();
    _thd_num = thd_num;
    _itr_num = itr_num;
}

double Count::compute(Graph& templates)
{
    _templates = &templates; 
    printf("Start subtemplates Dividing\n");

    div_tp.DivideTp(*(_templates));
    div_tp.sort_tps();

    printf("Finish subtemplates Dividing\n");
}
