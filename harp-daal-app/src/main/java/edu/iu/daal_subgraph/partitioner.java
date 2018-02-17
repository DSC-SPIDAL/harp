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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class partitioner {

    //a chain of created subtemplates
    private Graph[] subtemplates_create;
    private Graph[] subtemplates;
    private Graph subtemplate;

    private List<Integer> active_children;
    private List<Integer> passive_children;
    private List<Integer> parents;
    private List<Integer> cut_edge_labels;
    private List<int[]> label_maps;

    private int current_creation_index;
    private int subtemplate_count;

    private boolean[] count_needed;
    private boolean labeled;

    public partitioner(){}

    public partitioner(Graph t, boolean label, int[] label_map){
        init_arrays();
        labeled = label;
        subtemplates_create[0] = t;
        //start from 1
        current_creation_index = 1;

        if(labeled){
            label_maps.add(label_map);
        }
        parents.add(SCConstants.NULL_VAL);

        int root = 0;

        //recursively partition the template
        partition_recursive(0, root);

        fin_arrays();

    }

    void clear_temparrays(){
        subtemplates = null;
        count_needed = null;
    }

    //Do a bubble sort based on each subtemplate's parents' index
    //This is a simple way to organize the partition tree for use
    // with dt.init_sub() and memory management of dynamic table
    public void sort_subtemplates(){
        boolean swapped;
        Graph temp_g;
        do{
            swapped = false;
            for(int i = 2; i < subtemplate_count; ++i){
                if( parents.get(i) < parents.get(i-1)){

                    temp_g = subtemplates[i];
                    int temp_pr = parents.get(i);
                    int temp_a = active_children.get(i);
                    int temp_p = passive_children.get(i);
                    int[] temp_l = null;
                    if(labeled)
                        temp_l = label_maps.get(i);

                    //if either have children, need to update their parents
                    if( active_children.get(i) != SCConstants.NULL_VAL)
                        parents.set( active_children.get(i), i-1 );

                    if(active_children.get(i-1) != SCConstants.NULL_VAL)
                        parents.set( active_children.get(i-1), i );

                    if( passive_children.get(i) != SCConstants.NULL_VAL)
                        parents.set( passive_children.get(i), i-1 );

                    if( passive_children.get(i-1) != SCConstants.NULL_VAL)
                        parents.set( passive_children.get(i-1), i );

                    // need to update their parents
                    if( active_children.get( parents.get(i)) == i)
                        active_children.set( parents.get(i), i-1 );
                    else if( passive_children.get( parents.get(i)) == i )
                        passive_children.set( parents.get(i), i-1 );

                    if( active_children.get( parents.get(i-1)) == i-1)
                        active_children.set( parents.get(i-1), i );
                    else if( passive_children.get( parents.get(i-1)) == i-1 )
                        passive_children.set( parents.get(i-1), i );

                    // make the switch
                    subtemplates[i] = subtemplates[i-1];
                    parents.set(i, parents.get(i-1));
                    active_children.set(i, active_children.get(i-1));
                    passive_children.set(i, passive_children.get(i-1));
                    if(labeled)
                        label_maps.set(i, label_maps.get(i-1));

                    subtemplates[i-1] = temp_g;
                    parents.set(i-1, temp_pr);
                    active_children.set(i-1, temp_a);
                    passive_children.set(i-1, temp_p);
                    if(labeled)
                        label_maps.set(i-1, temp_l);

                    swapped = true;
                }
            }
        }while(swapped);

    }

    boolean sub_count_needed(int s){ return count_needed[s];}

    Graph[] get_subtemplates(){return subtemplates; }
    int get_subtemplate_count(){ return subtemplate_count; }
    int[] get_labels(int s){
        if (labeled)
            return label_maps.get(s);
        else
            return null;
    }

    int get_active_index(int a){
        return active_children.get(a);
    }
    int get_passive_index(int p){
        return passive_children.get(p);
    }
    int get_num_verts_active(int s){
        return subtemplates[ active_children.get(s)].num_vertices();
    }
    int get_num_verts_passive(int s){
        return subtemplates[ passive_children.get(s)].num_vertices();
    }

    private void partition_recursive(int s, int root){

        //split the current subtemplate using the current root
        int[] roots = split(s, root);

        //set the parent/child tree structure
        int a = current_creation_index - 2;
        int p = current_creation_index - 1;
        set_active_child(s, a);
        set_passive_child(s, p);
        set_parent(a, s);
        set_parent(p, s);

        //specify new roots and continue partitioning
        int num_verts_a = subtemplates_create[a].num_vertices();
        int num_verts_p = subtemplates_create[p].num_vertices();

        if( num_verts_a > 1){
            int activeRoot = roots[0];
            partition_recursive(a, activeRoot);
        }else{
            set_active_child(a, SCConstants.NULL_VAL);
            set_passive_child(a, SCConstants.NULL_VAL);
        }
        if( num_verts_p > 1){
            int passiveRoot = roots[1];
            partition_recursive(p, passiveRoot);
        }else{
            set_active_child(p, SCConstants.NULL_VAL);
            set_passive_child(p, SCConstants.NULL_VAL);
        }

    }

    private int[] split(int s, int root){
        //get new root
        //get adjacency vertices of the root vertex
        int[] adjs = subtemplates_create[s].adjacent_vertices(root);

        //get the first adjacent vertex
        int u = adjs[0];

        //split this subtemplate between root and node u
        //create a subtemplate rooted at root
        int active_root = split_sub(s, root, u);
        //create a subtemplate rooted at u
        int passive_root = split_sub(s, u, root);

        int[] retval = new int[2];
        retval[0] = active_root;
        retval[1] = passive_root;
        return retval;

    }

    private int split_sub(int s, int root, int other_root){
        subtemplate = subtemplates_create[s];
        int[] labels_sub = null;

        if(labeled)
            labels_sub = label_maps.get(s);

        //source and destination arrays for edges
        List<Integer> srcs = new ArrayList();
        List<Integer> dsts = new ArrayList();

        //get the previous vertex to avoid backtracking
        int previous_vertex = other_root;

        //loop through the rest of the vertices
        //if a new edge is found, add it

        List<Integer> next = new ArrayList<>();
        //start from the root
        //record all the edges in template execpt for other root branch
        next.add(root);
        int cur_next = 0;
        while( cur_next < next.size()){
            int u = next.get( cur_next ++ );
            int[] adjs = subtemplate.adjacent_vertices(u);
            int end = subtemplate.out_degree(u);

            //loop over all the adjacent vertices of u
            for(int i = 0; i < end; i++){
                int v = adjs[i];
                boolean add_edge = true;

                //avoiding add repeated edges
                for(int j = 0; j < dsts.size(); j++){
                    if(  srcs.get(j) == v && dsts.get(j) == u){
                        add_edge = false;
                        break;
                    }
                }

                if( add_edge && v != previous_vertex){
                    srcs.add(u);
                    dsts.add(v);
                    next.add(v);
                }
            }
        }

        //if empty, just add the single vert;
        int n;
        int m;
        int[] labels = null;

        if( srcs.size() > 0){
            m = srcs.size();
            n = m + 1;

            if(labeled){
                //extract_uniques
                Set<Integer> unique_ids = new HashSet<>();
                unique_ids.addAll(srcs);
                labels = new int[unique_ids.size()];
            }
            check_nums(root, srcs, dsts, labels, labels_sub);

        }else{
            //single node
            m = 0;
            n = 1;
            srcs.add(0);

            if( labeled){
                labels = new int[1];
                labels[0] = labels_sub[root];
            }
        }

        //convert list<Interger> to int[] 
        int[] srcs_array = Util.dynamic_to_static(srcs);
        int[] dsts_array = Util.dynamic_to_static(dsts);

        //create a subtemplate
        subtemplates_create[current_creation_index].initTemplate(n, m, srcs_array, dsts_array);
        current_creation_index++;

        if(labeled)
            label_maps.add(labels);

        return srcs.get(0);

    }

    // Initialize dynamic arrays and graph aray
    private void init_arrays(){
        subtemplates_create = new Graph[SCConstants.CREATE_SIZE];
        for(int i = 0; i < subtemplates_create.length; ++i)
            subtemplates_create[i] = new Graph();

        parents = new ArrayList<Integer>();
        active_children = new ArrayList<Integer>();
        passive_children = new ArrayList<Integer>();
        label_maps = new ArrayList<int[]>();
    }

    //Finalize arrays
    //Delete the creation array and make a final one of appropriate size
    private void fin_arrays(){
        subtemplate_count = current_creation_index;
        subtemplates = new Graph[subtemplate_count];

        for(int i = 0; i < subtemplate_count; ++i){
            subtemplates[i] = subtemplates_create[i];
        }

        subtemplates_create = null;

        count_needed = new boolean[subtemplate_count];
        for(int i = 0; i < subtemplate_count; ++i){
            count_needed[i] = true;
        }

    }


    /**
     * Check nums 'closes the gaps' in the srcs and dsts arrays
     * Can't initialize a graph with edges (0,2),(0,4),(2,5)
     * This changes the edges to (0,1),(0,2),(1,3)
     *
     * This reassign vertex id in the subtemplate
    */
    private void check_nums(int root, List<Integer> srcs, List<Integer> dsts, int[] labels, int[] labels_sub){
        int maximum = Util.get_max(srcs, dsts);
        int size = srcs.size();

        int[] mappings = new int[maximum + 1];

        for(int i = 0; i < maximum + 1; ++i){
            mappings[i] = -1;
        }

        int new_map = 0;
        mappings[root] = new_map ++;

        for(int i = 0; i < size; ++i){
            if( mappings[ srcs.get(i) ] == -1)
                mappings[ srcs.get(i) ] = new_map++;

            if( mappings[ dsts.get(i) ] == -1 )
                mappings[ dsts.get(i) ] = new_map++;
        }

        for(int i = 0; i < size; ++i){
            srcs.set(i, mappings[srcs.get(i)]);
            dsts.set(i, mappings[dsts.get(i)]);
        }

        if( labeled ){
            for (int i = 0; i < maximum; ++i){
                labels[ mappings[i] ] = labels_sub[i];
            }
        }
    }


    private void set_active_child(int s, int a){
        while( active_children.size() <= s)
            active_children.add(SCConstants.NULL_VAL);

        active_children.set(s, a);
    }


    private void set_passive_child(int s, int p){
        while( passive_children.size() <= s)
            passive_children.add(SCConstants.NULL_VAL);

        passive_children.set(s,p);
    }

    private void set_parent(int c, int p){
        while( parents.size() <= c )
            parents.add(SCConstants.NULL_VAL);

        parents.set(c, p);
    }

}
