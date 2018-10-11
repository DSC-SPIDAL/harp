// the implementation of the color-coding algorithms
// Author: Langshi Chen

#include "Count.hpp"
#include <cassert>

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
    _color_num = _templates->get_vert_num();

    printf("Start subtemplates Dividing\n");
    std::fflush(stdout);

    div_tp.DivideTp(*(_templates));
    div_tp.sort_tps();

    _subtmp_array = div_tp.get_subtps();
    _total_sub_num = div_tp.get_subtps_num();

    //check the sub vert num
    // debug
    // for(int s=0;s<_total_sub_num;s++)
    // {
    //     int vert_self = _subtmp_array[s].get_vert_num();
    //     if (vert_self > 1)
    //     {
    //         int main_leaf = div_tp.get_main_node_vert_num(s);
    //         int aux_leaf = div_tp.get_aux_node_vert_num(s);
    //
    //         printf("Vert: %d, main: %d, aux: %d\n", vert_self, main_leaf, aux_leaf);
    //         std::fflush(stdout);
    //         assert((main_leaf + aux_leaf == vert_self));
    //
    //     }
    // }
    
    printf("Finish subtemplates Dividing\n");
    std::fflush(stdout);

    // create the index tables
    indexer.initialization(_color_num, _total_sub_num, &_subtmp_array, &div_tp);

    printf("Finish creating indexer\n");
    std::fflush(stdout);

    
}
