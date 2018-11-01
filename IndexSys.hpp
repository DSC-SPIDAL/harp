#ifndef _INDEX_SYS_H
#define _INDEX_SYS_H

#include "Graph.hpp"
#include "DivideTemplates.hpp"
#include <vector>

using namespace std;

class IndexSys 
{
    public:

        IndexSys() 
        {
            _comb_table = nullptr;
            _sub_vert_num = nullptr;
            _comb_table = nullptr;
            _index_tmp = nullptr;
            _colors_tmp = nullptr;
            _i_sub_c_to_counts = nullptr;
            _i_sub_c_split_to_counts = nullptr;
            _i_sub_c_split_to_counts_vec = nullptr;
            _i_sub_precomp_to_counts = nullptr;
            _divider = nullptr;
            _effecAuxIdx = nullptr;
        }

        ~IndexSys() { release(); }

        void initialization(int color_num, int sub_len, Graph** sub_tps, DivideTemplates* divider);
        void release();
        int comb_calc(int a, int b);
        int** getSubCToCount() { return _i_sub_c_to_counts; }
        int* getSubsSize() {return _sub_vert_num;}
        int** getCombTable() {return _comb_table;}
        int**** getSplitToCountTable() {return _i_sub_c_split_to_counts;}
        int*** getSplitToCountVecTable() {return _i_sub_c_split_to_counts_vec;}
        int** getCombToCountTable() {return _i_sub_c_to_counts;}
        std::vector<int>* getEffectiveAuxIndices() {return _effecAuxIdx;}

    private:

        void perm_set_init(int*& perm_set, int size);
        void perm_set_next(int*& perm_set, int size, int colors);       
        void gen_index();
        void gen_colors();
        void gen_comb_hash_table();
        void releaseColorSets();
        void releaseIndexSets();
        int get_color_hash(int* perm_set, int size);


        // total number of colors
        int _color_num;
        // store vert num of each sub
        int* _sub_vert_num;

        // length of sub-template array
        int _sub_len;
        // pointers to the array of sub-templates
        Graph** _sub_tps;
        

        // store pre-computed combination values
        // \choose{a, b}
        int** _comb_table;

        // tmp container
        int**** _index_tmp;
        int***** _colors_tmp;
        
        // hash table given sub and comb id, 
        // find the index in counts table
        int** _i_sub_c_to_counts;

        // hash table given sub and comb id,
        // find the main and aux splits index in counts table 
        int**** _i_sub_c_split_to_counts; 
        int*** _i_sub_c_split_to_counts_vec; 
        // a new index sytem, given sub, return 
        // all of the comb index in precompute buf
        int** _i_sub_precomp_to_counts;

        // store the effective indices of aux indices for each subs
        std::vector<int>* _effecAuxIdx;
        
        DivideTemplates* _divider;

};

#endif
