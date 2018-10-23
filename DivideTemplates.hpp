// divide the templates into a chain of 
// subtemplates for dynamic programming used
// in color-coding
//
#ifndef __DIVIDETEMPLATES_H__
#define __DIVIDETEMPLATES_H__

#include "Graph.hpp"
#include "Helper.hpp"
#include <vector>

using namespace std;

class DivideTemplates
{
    public:

        DivideTemplates() 
        {
            tmp_subtps = NULL;
            all_subtps = NULL;
            tp_valid = NULL;
            dummy_val = 93620;
        }

        ~DivideTemplates() { release(); }

        void DivideTp(Graph& tp);

        void sort_tps();

        // accessors
        Graph* get_subtps() {return all_subtps;}
        int get_subtps_num() {return total_subtp;}
        int get_main_node_idx(int id) {return node_main[id]; }
        int get_aux_node_idx(int id) {return node_aux[id]; }
        int get_main_node_vert_num(int sub) {return all_subtps[node_main[sub]].get_vert_num();}
        int get_aux_node_vert_num(int sub) {return all_subtps[node_aux[sub]].get_vert_num();}
        int get_tp_valid(int sub) {return tp_valid[sub];}

        void release();

    private:

        void initial();
        void divide(int stp, int root);
        void finalize();
        void split(int& sub, int& root, int& new_main, int& new_aux);
        int split_nodes(int& sub, int& root, int& rest);
        void check_val(int& root, vector<int>& src_list, vector<int>& dst_list);
        int get_max_val(vector<int>& v1, vector<int>& v2);

        void set_nodes(vector<int>& nodes_list, int sub, int node);
        

        Graph* tmp_subtps;
        Graph* all_subtps;
        Graph cur_subtp;

        vector<int> node_main;
        vector<int> node_aux;
        vector<int> parents;

        int cur_index;
        int total_subtp;
        bool* tp_valid;
        // int dummy_val = 3245493620;
        int dummy_val;
        // int dummy_val = DUMMY_VAL;


};

#endif
