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
package edu.iu.daal_subgraph;

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

    private int max_abs_vid;

    int cur_sub;

    final Logger LOG = Logger.getLogger(dynamic_table_array.class);

    @Override
    public void init(Graph[] subtemplates, int num_subtemplates, int num_vertices, int num_colors, int max_abs_vid) {
        this.subtemplates = subtemplates;
        this.num_subs = num_subtemplates;
        //num of vertices of full graph 
        this.num_verts = num_vertices;
        this.num_colors = num_colors;
        this.max_abs_vid = max_abs_vid;

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

        is_inited = true;

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
        if( active_child != SCConstants.NULL_VAL && passive_child != SCConstants.NULL_VAL){
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

    public float[] get_table(int subtemplate, int vertex)
    {
        return table[subtemplate][vertex];
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

    public float[] get_passive(int vertex){
        return cur_table_passive[vertex];
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

        if( table[subtemplate][vertex] == null){

            table[subtemplate][vertex] = new float[ num_colorsets[subtemplate] ];
            assert(table[subtemplate][vertex] != null);

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

    //shall deal with the uninit vertex in local
    public void update_comm(int vertex, int comb_num_index, float count){

        if( cur_table[vertex] == null){
            cur_table[vertex] = new float[ num_colorsets[cur_sub] ];

            assert(cur_table[vertex] != null );

            for( int c = 0; c < num_colorsets[cur_sub]; ++c) {
                cur_table[vertex][c] = 0.0f;
            }
        }

        cur_table[vertex][comb_num_index] += count;
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


    public int get_num_color_set(int s) 
    {
        if (num_colorsets != null )
            return num_colorsets[s];
        else
            return 0;
    }

    public void set_to_table(int s, int d)
    {
        table[d] = table[s]; 
    }
}
