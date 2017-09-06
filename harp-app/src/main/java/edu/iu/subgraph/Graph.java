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

import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.IntArray;
import it.unimi.dsi.fastutil.ints.IntArrays;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Arrays;

public class Graph {

   
    private int num_verts;

    private int v_max_id;

    private int num_edges;
    private int max_deg;
    private int adj_len;

    int[] adjacency_array;
    int[] degree_list;

    // absolute v_id
    int[] vertex_ids;
    // from absolute v_id to relative v_id
    int[] vertex_local_ids;

    int[] labels = null;
    boolean labeled = false;

    protected static final Log LOG = LogFactory
			    .getLog(Graph.class);

    void initGraph(int num_verts, int v_max_id, int adj_len, Table<IntArray> adj_table) 
    {//{{{

        this.num_verts = num_verts;
        this.v_max_id = v_max_id;
        this.adjacency_array = new int[adj_len];
        //global v_id starts from 1
        this.vertex_local_ids = new int[v_max_id+1];
        for(int p=0;p<v_max_id+1;p++)
            this.vertex_local_ids[p] = -1;

        //undirected graph, num edges equals adj array size
        this.num_edges = adj_len;

        this.degree_list = new int[this.num_verts + 1];
        degree_list[0] = 0;

        //global vertex ids
        vertex_ids = new int[this.num_verts];
        adj_table.getPartitionIDs().toArray(vertex_ids);

        //increase order
        IntArrays.quickSort(vertex_ids, 0, this.num_verts);

        //debug
        // for(int i=this.num_verts-1;i>this.num_verts-11;i--)
            // LOG.info("vert ids sort: " + vertex_ids[i]);

        int[] temp_deg_list = new int[this.num_verts];
        max_deg = 0;

        for(int p=0;p<this.num_verts;p++)
        {
            //get out a vertex 
            int v_id = vertex_ids[p];

            //setup the mapping between global v id and local v id
            vertex_local_ids[v_id] = p;

            int[] v_adj = adj_table.getPartition(v_id).get().get();
            temp_deg_list[p] = v_adj.length;

            max_deg = v_adj.length > max_deg? v_adj.length: max_deg;
        
            degree_list[p + 1] = degree_list[p] + temp_deg_list[p];
            //copy adj array 
            System.arraycopy(v_adj, 0, adjacency_array, degree_list[p], v_adj.length);
        }
            
        temp_deg_list = null;

    }//}}}

    void initTemplate(int n, int m, int[] srcs, int[] dsts)
    {
        this.num_verts = n;
        this.num_edges = 2*m;
        this.max_deg = 0;

        this.adjacency_array = new int[2*m];

        this.vertex_local_ids = new int[num_verts+1];

        degree_list = new int[num_verts + 1];
        degree_list[0] = 0;

        int[] temp_deg_list = new int[num_verts];

        for(int i = 0; i < num_verts; ++i){
            temp_deg_list[i] = 0;
        }

        for(int i = 0; i < m; ++i){

            temp_deg_list[srcs[i]]++;
            temp_deg_list[dsts[i]]++;

            vertex_local_ids[srcs[i]] = srcs[i];
            vertex_local_ids[dsts[i]] = dsts[i];

        }

        for(int  i = 0; i < num_verts; ++i){
            max_deg = temp_deg_list[i] > max_deg? temp_deg_list[i]: max_deg;
        }

        //definition of accumulated degree_list
        for(int i = 0; i < num_verts; ++i){
            degree_list[i + 1] = degree_list[i] + temp_deg_list[i];
        }

        System.arraycopy(degree_list, 0, temp_deg_list, 0, num_verts);

        //the structure of adjacency array 
        //undirected graph 
        for(int i = 0; i < m; ++i){
            adjacency_array[ temp_deg_list[srcs[i]]++ ] = dsts[i];
            adjacency_array[ temp_deg_list[dsts[i]]++ ] = srcs[i];
        }

        temp_deg_list = null;

    }

    /**
     * @brief get adj array from relative v id
     *
     * @param v
     *
     * @return 
     */
    int[] adjacent_vertices(int v){
        //return (&adjacency_array[degree_list[v]]);
        //this doesn't return original array.
        //Be aware of the difference
        return Arrays.copyOfRange(adjacency_array, degree_list[v], degree_list[v+1]);

    }

    /**
     * @brief get adj vertices by absolute v_id
     *
     * @param v
     *
     * @return 
     */
    int[] adjacent_vertices_abs(int v){
        //return (&adjacency_array[degree_list[v]]);
        //this doesn't return original array.
        //Be aware of the difference
        int local_id = this.vertex_local_ids[v];
        if (local_id >= 0)
            return Arrays.copyOfRange(adjacency_array, degree_list[local_id], degree_list[local_id+1]);
        else
            return null;
    }

    int get_relative_v_id(int v_abs) {

        return vertex_local_ids[v_abs]; 
    }

    // v is rel id
    // v start from 0
    int out_degree(int v){
        return degree_list[v + 1] - degree_list[v];
    }

    // v is abs id
    int out_degree_abs(int v){
        int local_id = this.vertex_local_ids[v];
        if (local_id >= 0)
            return degree_list[local_id + 1] - degree_list[local_id];
        else
            return 0;
    }

    int[] adjacencies(){
        return adjacency_array;
    }

    int[] degrees(){
        return degree_list;
    }

    int num_vertices(){
        return num_verts;
    }

    int v_max_id(){
        return v_max_id;
    }

    int num_edges(){
        return num_edges;
    }

    int max_degree(){
        return max_deg;
    }

    int[] get_abs_v_ids() {
        return vertex_ids;
    }

    void clear(){
        adjacency_array = null;
        degree_list = null;
    }

}
