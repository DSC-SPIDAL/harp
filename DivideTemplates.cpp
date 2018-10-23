// implementation of sub-templates divide

#include "DivideTemplates.hpp"
#include "Constants.hpp"
#include <cstring>

void DivideTemplates::DivideTp(Graph& tp)
{
    initial();
    tmp_subtps[0] = tp;
    cur_index = 1;

    parents.push_back(dummy_val);

    // a recursive process to divide the templates
    // into sub-templates
    divide(0, 0);
    finalize();

}

void DivideTemplates::initial()
{
    tmp_subtps = new Graph[1000];
    parents = vector<int>(0);
    node_main = vector<int>(0);
    node_aux = vector<int>(0);
}

/**
 * @brief recursively divide the template tree
 *
 * @param sub
 * @param root
 */
void DivideTemplates::divide(int sub, int root)
{/*{{{*/

    // split the cur sub-templates
    int root_main = 0;
    int root_aux = 0;

    printf("Start splitting\n");
    std::fflush(stdout);

    split(sub, root, root_main, root_aux);

    printf("Finish splitting\n");
    std::fflush(stdout);

    int main_index = cur_index - 2;
    int aux_index = cur_index -1;

    set_nodes(node_main, sub, main_index);
    set_nodes(node_aux, sub, aux_index);
    set_nodes(parents, main_index, sub);
    set_nodes(parents, aux_index, sub);

    printf("Finish setting nodes\n");
    std::fflush(stdout);

    int mainSize = tmp_subtps[main_index].get_vert_num();
    int auxSize = tmp_subtps[aux_index].get_vert_num();

    if ( mainSize > 1)
    {
        divide(main_index, root_main);
    }
    else
    {
        set_nodes(node_main, main_index, dummy_val);
        set_nodes(node_aux, main_index, dummy_val);
    }
    //
    if ( auxSize > 1)
    {
        divide(aux_index, root_aux);
    }
    else
    {
        set_nodes(node_main, aux_index, dummy_val);
        set_nodes(node_aux, aux_index, dummy_val);
    }

}/*}}}*/

void DivideTemplates::set_nodes(vector<int>& nodes_list, int sub, int node)
{/*{{{*/
    while( nodes_list.size() <= sub )
        nodes_list.push_back(dummy_val);

    nodes_list[sub] = node;
}/*}}}*/

void DivideTemplates::release()
{/*{{{*/
    if (tp_valid != NULL)
        delete[] tp_valid;

    if (all_subtps != NULL)
        delete[] all_subtps;

    tp_valid = NULL;
    all_subtps = NULL;
}/*}}}*/

void DivideTemplates::finalize()
{/*{{{*/
    total_subtp = cur_index;
    all_subtps = new Graph[total_subtp];
    
    for(int i=0;i<total_subtp;i++)
    {
        // copy
        all_subtps[i] = tmp_subtps[i];
        tmp_subtps[i].release();
    }

    delete[] tmp_subtps;

    

    tp_valid = new bool[total_subtp];
    for(int i=0;i<total_subtp;i++)
        tp_valid[i] = true;
}/*}}}*/

void DivideTemplates::split(int& sub, int& root, int& new_main, int& new_aux)
{/*{{{*/
    int* adjs = tmp_subtps[sub].get_adj_list(root);

    // choose the first neighbour as the new root
    // may change this for better performance
    int new_node = adjs[0];

    //debug
    printf("new node is %d\n", new_node);
    std::fflush(stdout);

    // start the split job
    new_main = split_nodes(sub, root, new_node);

    //debug
    printf("new main index is %d\n", new_main);
    std::fflush(stdout);

    new_aux = split_nodes(sub, new_node, root);
    
    //debug
    printf("new aux index is %d\n", new_aux);
    std::fflush(stdout);

}/*}}}*/

/**
 * @brief A function to divide the tree-like templates into a 
 * main and aux sub-trees
 *
 * @param sub: the sub-template id
 * @param root
 * @param rest
 *
 * @return 
 */
int DivideTemplates::split_nodes(int& sub, int& root, int& rest)
{/*{{{*/

   cur_subtp = tmp_subtps[sub]; 
   vector<int> src_list;
   vector<int> dst_list;
   
   int stale_vert = rest;
   vector<int> added_vert;

   added_vert.push_back(root);
   
   for(int i=0;i<added_vert.size();i++)
   {
       int* adj_tp = cur_subtp.get_adj_list(added_vert[i]);
       int deg_tp = cur_subtp.get_out_deg(added_vert[i]);

       for(int j=0;j< deg_tp;j++)
       {
           int vert_id = adj_tp[j];
           bool add_edge = true;
           for(int k=0;k<dst_list.size();k++)
           {
               if (src_list[k] == vert_id && dst_list[k] == added_vert[i])
               {
                   add_edge = false;
                   break;
               }
           }

           if ( add_edge && vert_id != stale_vert )
           {
               src_list.push_back(added_vert[i]);
               dst_list.push_back(vert_id);
               added_vert.push_back(vert_id);
           }
       }
   }

   // add single vert id
   int num_vert;
   int num_edge;

   if (src_list.size() > 0)
   {
       num_edge = src_list.size();
       num_vert = num_edge + 1;

       check_val(root, src_list, dst_list);
   }
   else
   {
       num_edge = 0;
       num_vert = 1;
       src_list.push_back(0);
   }

   int val_return = src_list[0];
   // int* src_copy = new int[src_list.size()];
   // int* dst_copy = new int[dst_list.size()];
   // std::memcpy(src_copy, src_list.data(), src_list.size()*sizeof(int));
   // std::memcpy(dst_copy, dst_list.data(), dst_list.size()*sizeof(int));

   //debug
   printf("build graph: num_vert is %d, num_edge is %d\n", num_vert, num_edge);
   std::fflush(stdout);
   for(int i= 0;i<src_list.size();i++)
       printf("src: %d\n", src_list[i]);

   for(int i= 0;i<dst_list.size();i++)
       printf("dst: %d\n", dst_list[i]);

   tmp_subtps[cur_index].build_graph(num_vert, num_edge, &src_list[0], &dst_list[0]);
   // tmp_subtps[cur_index].build_graph(num_vert, num_edge, src_copy, dst_copy);
   cur_index++;

   // delete[] src_copy;
   // delete[] dst_copy;

   return val_return;

}/*}}}*/

void DivideTemplates::check_val(int& root, vector<int>& src_list, vector<int>& dst_list)
{/*{{{*/
    int max_val = get_max_val(src_list, dst_list); 

    int* cur_maps = new int[max_val+1];
    for(int i=0;i<max_val+1;i++)
        cur_maps[i] = -1;


    cur_maps[root] = 0; 
    int next_maps = 1;

    for(int i=0;i<src_list.size();i++)
    {
        if (cur_maps[src_list[i]] == -1)
            cur_maps[src_list[i]] = next_maps++;

        if (cur_maps[dst_list[i]] == -1)
            cur_maps[dst_list[i]] = next_maps++;
    }

    for(int i=0;i<src_list.size();i++)
    {
        src_list[i] = cur_maps[src_list[i]];
        dst_list[i] = cur_maps[dst_list[i]];
    }

}/*}}}*/

int DivideTemplates::get_max_val(vector<int>& v1, vector<int>& v2)
{/*{{{*/
    int max_val = 0;
    for(int i=0;i<v1.size();i++)
    {
        max_val = v1[i] > max_val ? v1[i] : max_val;
        max_val = v2[i] > max_val ? v2[i] : max_val;
    }

    return max_val;
}/*}}}*/

/**
 * @brief conduct a bubble sort upon
 * the parents index
 */
void DivideTemplates::sort_tps()
{/*{{{*/

    bool is_running = true;
    bool is_swapped = false;
    Graph cur_tp;

    while(is_running)
    {

        is_swapped = false;

        for(int i=2;i<total_subtp;i++)
        {
            if (parents[i] < parents[i-1])
            {
                cur_tp = all_subtps[i];
                int cur_parent = parents[i];
                int cur_main = node_main[i];
                int cur_aux = node_aux[i];

                if (node_main[i] != dummy_val)
                    parents[node_main[i]] = (i-1);

                if (node_main[i-1] != dummy_val)
                    parents[node_main[i-1]] = i;
                
                if (node_aux[i] != dummy_val)
                    parents[node_aux[i]] = (i-1);

                if (node_aux[i-1] != dummy_val)
                    parents[node_aux[i-1]] = i;

                // upate the parents
                if (node_main[parents[i]] == i)
                    node_main[parents[i]] = (i-1);
                else if (node_aux[parents[i]] == i)
                    node_aux[parents[i]] = (i-1);

                if (node_main[parents[i-1]] == (i-1))
                    node_main[parents[i-1]] = i;
                else if (node_aux[parents[i-1]] == (i-1))
                    node_aux[parents[i-1]] = i;

                all_subtps[i] = all_subtps[i-1];
                parents[i] = parents[i-1];
                node_main[i] = node_main[i-1];
                node_aux[i] = node_aux[i-1];
                
                all_subtps[i-1] = cur_tp;
                parents[i-1] = cur_parent;
                node_main[i-1] = cur_main;
                node_aux[i-1] = cur_aux;

                is_swapped = true;
            }

        }

        if (!is_swapped)
            is_running = false;
    }

}/*}}}*/
