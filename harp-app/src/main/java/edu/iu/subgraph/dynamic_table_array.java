/*
 * Copyright 2013-2017 Indiana University
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.iu.subgraph;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import org.apache.log4j.Logger;
import java.util.*;

public class dynamic_table_array extends dynamic_table{
    // subtemplate-vertex-colorset
    private float[][][] table;
    // vertex-colorset
    private float[][] cur_table;
    // vertex-colorset
    private float[][] cur_table_active;
    // vertex-colorset
    private float[][] cur_table_passive;

    //indexed by relative v id
    private Int2ObjectOpenHashMap<int[]> comm_colorset_idx;
    private Int2ObjectOpenHashMap<float[]> comm_colorset_counts;

    int cur_sub;

    final Logger LOG = Logger.getLogger(dynamic_table_array.class);

    @Override
    public void init(Graph[] subtemplates, int num_subtemplates, int num_vertices, int num_colors) {
        this.subtemplates = subtemplates;
        this.num_subs = num_subtemplates;
        //num of vertices of full graph 
        this.num_verts = num_vertices;
        this.num_colors = num_colors;

        this.comm_colorset_idx = new Int2ObjectOpenHashMap<>();
        this.comm_colorset_counts = new Int2ObjectOpenHashMap<>();

        //obtain the table,choose j color from i color, the num of combinations
        init_choose_table();
        //obtain color sets for each subtemplate
        init_num_colorsets();

        // the core three-dimensional arrays in algorithm
        table = new float[ this.num_subs ][][];

        assert(table != null);

        is_sub_inited = new boolean[ this.num_subs];

        assert (is_sub_inited != null);

        for(int s = 0; s < this.num_subs; ++s){
            is_sub_inited[s] = false;
        }

        is_inited = false;

    }

    @Override
    public void init_sub(int subtemplate) {
        this.table[subtemplate] = new float[ this.num_verts][];
        assert (table[subtemplate] != null);

        cur_table = table[subtemplate];
        cur_sub = subtemplate;

        /*
        #ifdef _OPENMP
        #pragma omp parallel for
        #endif
        for(int v = 0; v < num_verts; ++v){
            cur_table[v] = null;
        }
        */

        is_sub_inited[subtemplate] = true;

    }

    public void init_sub(int subtemplate, int active_child, int passive_child){
        if( active_child != Constants.NULL_VAL && passive_child != Constants.NULL_VAL){
            cur_table_active = table[active_child];
            cur_table_passive = table[passive_child];
        }else{
            cur_table_active = null;
            cur_table_passive = null;
        }

        if(subtemplate != 0){
            init_sub(subtemplate);
        }
    }

    @Override
    public void clear_sub(int subtemplate) {

        if (table[subtemplate] != null)
        {

            for(int v = 0; v < num_verts; ++v){
                if( table[subtemplate][v] != null){
                    table[subtemplate][v] = null;
                }
            }

            if( is_sub_inited[subtemplate]){
                table[subtemplate] = null;
            }

            is_sub_inited[subtemplate] = false;

        }
    }

    @Override
    public void clear_table() {
        for( int s = 0; s < num_subs; s++){
            if( is_sub_inited[s]){
                for(int v = 0; v < num_verts; ++v){
                    if ( table[s][v] != null){
                        table[s][v] = null;
                    }
                }
                table[s] = null;
                is_sub_inited[s] = false;
            }
        }

        table = null;
        is_sub_inited = null;
    }


    public float get(int subtemplate, int vertex, int comb_num_index){
        if( table[subtemplate][vertex] != null){
            float retval = table[subtemplate][vertex][comb_num_index];
            return retval;
        }else{
            return 0.0f;
        }
    }

    public float get_active(int vertex, int comb_num_index){
        if( cur_table_active[vertex] != null){
            return cur_table_active[vertex][comb_num_index];
        }else{
            return 0.0f;
        }
    }

    public float[] get_active(int vertex){
        return cur_table_active[vertex];
    }

    public float get_passive(int vertex, int comb_num_index){
        if( cur_table_passive[vertex] != null){
            return cur_table_passive[vertex][comb_num_index];
        }else{
            return 0.0f;
        }
    }

    //float* get(int subtemplate, int vertex) return table[subtemplate][vertex];
    //float* get_active(int vertex) return cur_table_active[vertex];
    //float* get_passive(int vertex) return cur_table_passive[vertex];

    public void set(int subtemplate, int vertex, int comb_num_index, float count){
        if( table[subtemplate][vertex] != null){

            table[subtemplate][vertex] = new float[ num_colorsets[subtemplate] ];
            assert(cur_table[vertex] != null);

            for(int c = 0; c < num_colorsets[subtemplate]; ++c){
                table[subtemplate][vertex][c] = 0.0f;
            }
        }

        table[subtemplate][vertex][comb_num_index]  = count;
    }


    public void set(int vertex, int comb_num_index, float count){
        if( cur_table[vertex] == null){
            cur_table[vertex] = new float[ num_colorsets[cur_sub] ];

            assert(cur_table[vertex] != null );

            for( int c = 0; c < num_colorsets[cur_sub]; ++c) {
                cur_table[vertex][c] = 0.0f;
            }
        }

        cur_table[vertex][comb_num_index] = count;
    }

    public void update_comm(int vertex, int comb_num_index, float count){

        if( cur_table[vertex] != null){
            cur_table[vertex][comb_num_index] += count;
        }
    }

    @Override
    public boolean is_init() {
        return this.is_inited;
    }

    @Override
    public boolean is_sub_init(int subtemplate) {
        return this.is_sub_inited[subtemplate];
    }

    public boolean is_vertex_init_active(int vertex){
        if( cur_table_active[vertex] != null)
            return true;
        else
            return false;
    }

    public boolean is_vertex_init_passive(int vertex){
        if(cur_table_passive[vertex] != null)
            return true;
        else
            return false;
    }

    public SCSet compress_send_data(int[] vert_list, int num_comb_max, Graph g) 
    {

        int v_num = vert_list.length;
        int[] v_offset = new int[v_num + 1];

        //to be trimed
        int[] counts_idx_tmp = new int[num_comb_max*v_num];
        float[] counts_data_tmp = new float[num_comb_max*v_num];

        int count_num = 0;

        // LOG.info("Start compressing send data v_num: " + v_num);
        for(int p = 0; p< v_num; p++)
        {
            v_offset[p] = count_num;
            //get the abs vert id
            int comm_vert_id = vert_list[p];
            int rel_vert_id = g.get_relative_v_id(comm_vert_id); 

            //if comm_vert_id is not in local graph
            if (rel_vert_id < 0 || (is_vertex_init_passive(rel_vert_id) == false))
                continue;

            int[] idx_arry = this.comm_colorset_idx.get(rel_vert_id); 
            float[] counts_arry = this.comm_colorset_counts.get(rel_vert_id); 

            if (idx_arry == null || counts_arry == null)
            {
                //create the entry and put it to table
                //create tmp array which will be trimed later
                int[] tmp_idx_arry = new int[num_comb_max];
                float[] tmp_counts_arry = new float[num_comb_max];

                int tmp_itr = 0;
                for(int q = 0; q< num_comb_max; q++)
                {
                    // float count_v = get(cur_sub, rel_vert_id, q);
                    float count_v = get_passive(rel_vert_id, q);
                    // if (count_v > 0.0f)
                    // {
                        tmp_idx_arry[tmp_itr] = q;
                        tmp_counts_arry[tmp_itr] = count_v;
                        tmp_itr++;
                    // }
                }

                //trim the tmp array
                if (tmp_itr > 0)
                {
                    idx_arry = new int[tmp_itr];
                    counts_arry = new float[tmp_itr];
                    System.arraycopy(tmp_idx_arry, 0, idx_arry, 0, tmp_itr);
                    System.arraycopy(tmp_counts_arry, 0, counts_arry, 0, tmp_itr);

                    tmp_idx_arry = null;
                    tmp_counts_arry = null;

                    this.comm_colorset_idx.put(rel_vert_id, idx_arry);
                    this.comm_colorset_counts.put(rel_vert_id, counts_arry);

                }
            }

            if (idx_arry != null && counts_arry != null)
            {
                //combine the idx_arry 
                System.arraycopy(idx_arry, 0, counts_idx_tmp, count_num, idx_arry.length);
                System.arraycopy(counts_arry, 0, counts_data_tmp, count_num, counts_arry.length);
                count_num += idx_arry.length;
            }

        }

        // LOG.info("Finish compressing send data");

        v_offset[v_num] = count_num;
        //trim the tmp array
        int[] counts_idx = new int[count_num];
        float[] counts_data = new float[count_num];
        System.arraycopy(counts_idx_tmp, 0, counts_idx, 0, count_num);
        System.arraycopy(counts_data_tmp, 0, counts_data, 0, count_num);

        counts_idx_tmp = null;
        counts_data_tmp = null;

        SCSet set = new SCSet(v_num, count_num, v_offset, counts_idx, counts_data);

        return set;

    }

    public void clear_comm_counts() {
        this.comm_colorset_idx.clear();
        this.comm_colorset_counts.clear();
    }

    public void free_comm_counts() {
        this.comm_colorset_idx = null;
        this.comm_colorset_counts = null;
    }
}
