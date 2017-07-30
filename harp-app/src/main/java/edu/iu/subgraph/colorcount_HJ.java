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

import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.Random;

// using HJlib 
import static edu.rice.hj.Module0.launchHabaneroApp;
import static edu.rice.hj.Module1.forallChunked;

//for BSP-LRT
import java.util.*;
import java.util.BitSet;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
//threads affinity
import net.openhft.affinity.Affinity;

// for triggering vtune 
import java.nio.file.*;
import java.io.IOException;

/**
 * @brief A colorcount method that implements 
 * a BSP-LRT style 
 */
public class colorcount_HJ {

    final Logger LOG = Logger.getLogger(colorcount_HJ.class);

    Graph g;
    Graph t;

    //may not use them 
    int[] labels_g;
    int[] labels_t;

    //randomly assigned color values to each vertex of 
    //graph
    int[] colors_g;
    boolean labeled;

    //partitioned subtemplates
    Graph[] subtemplates;

    int subtemplate_count;

    int num_colors;
    int num_iter;
    int cur_iter;

    //for dynamic programming ?
    dynamic_table_array dt;
    //for partitioning a template
    partitioner part;

    // what is this ?
    int[][] choose_table;
    // temp index sets to construct comb_num_indexes 
    int[][][][] index_sets;
    // temp index sets to construct comb_num_indexes 
    int[][][][][] color_sets;
    // record comb num values for active and passive children 
    int[][][][] comb_num_indexes;
    // record comb num values for each subtemplate and each color combination
    int[][] comb_num_indexes_set;
    // vertices num of each subtemplate 
    int[] num_verts_table;
    // what is this ?
    int num_verts_graph;
    int max_degree;

    // what is this ?
    double[] final_vert_counts;
    boolean do_graphlet_freq;
    boolean do_vert_output;
    boolean calculate_automorphisms;
    boolean verbose;

    int set_count;
    int total_count;
    int read_count;

    //thread bindings
    //cps is core per socket
    // int cps = 12; 
    // int cps = 18; 

    //thread barrier
    // private AtomicInteger barrierCounter = new AtomicInteger(0);
    private CyclicBarrier barrier;
    private int num_verts_sub_ato;
    private int num_combinations_ato;

    private int[] set_count_loop_ato; 
    private int[] total_count_loop_ato; 
    private int[] read_count_loop_ato; 
    private float[] cc_ato; 
    private int retval_ato = 0;
    private float full_count_ato = 0;
    private double count_ato = 0;

    //hardcode cores per node on Juliet low-end node
    // private int cpn = 24;  

    //inovoked in Fascia.java
    void init(Graph full_graph, boolean calc_auto, boolean do_gdd, boolean do_vert, boolean verb){
        g = full_graph;
        num_verts_graph = g.num_vertices();
        labels_g = g.labels;
        labeled = g.labeled;
        do_graphlet_freq = do_gdd;
        do_vert_output = do_vert;
        calculate_automorphisms = calc_auto;
        verbose = verb;

        //initialize dynamic table
        dt = new dynamic_table_array();
        barrier = new CyclicBarrier(Constants.THREAD_NUM);

        if( do_graphlet_freq || do_vert_output){
            //Skipped
        }
    }

    // the outer iteration
    double do_full_count(Graph template, int N){

        num_iter = N;
        t = template;

        labels_t = t.labels;

        if(verbose){
            LOG.info("Begining partition...");
        }

        //partition the template into subtemplates 
        part = new partitioner(t, labeled, labels_t);
        part.sort_subtemplates();

        if(verbose){
            LOG.info("done partitioning");
        }

        //colors equals the num of vertices
        num_colors = t.num_vertices();
        //get array of subtemplates
        subtemplates = part.get_subtemplates();
        //subtemplates num
        subtemplate_count = part.get_subtemplate_count();

        //obtain the hash values table for each subtemplate and a combination of color sets
        create_tables();

        //initialize dynamic prog table, with subtemplate-vertices-color array
        dt.init(subtemplates, subtemplate_count, g.num_vertices(), num_colors);

        //determine max out degree for the data graph
        int max_out_degree = 0;
        for(int i =0; i < num_verts_graph; i++){
            int out_degree_i = g.out_degree(i);
            if(out_degree_i > max_out_degree){
                max_out_degree = out_degree_i;
            }
        }
        max_degree = max_out_degree;

        if(verbose){
            LOG.info("n "+ num_verts_graph + ", max degree " + max_out_degree);
        }

        //begin the counting 
        //launch LRT threads
        count_ato = 0.0;
        Lock lock = new ReentrantLock();

        //vertice num of the full graph, huge
        int num_verts = g.num_vertices();
        int[] chunks = divide_chunks(num_verts, Constants.THREAD_NUM);   

        //triggering vtune profiling
        // java.nio.file.Path vtune_file = java.nio.file.Paths.get("vtune-flag.txt");
        // String flag_trigger = "Start training process and trigger vtune profiling.";
        // try{
        //     java.nio.file.Files.write(vtune_file, flag_trigger.getBytes());
        // }catch (IOException e)
        // {
        //    LOG.info("Failed to create vtune trigger flag");
        // }

        launchHabaneroApp( () -> forallChunked(0, Constants.THREAD_NUM-1, (threadIdx) -> {

            //set Java threads affinity
            BitSet bitSet = new BitSet(Constants.CORE_NUM);
            int thread_mask = 0;

            if (Constants.THD_AFFINITY == "scatter")
            {
                //implement threads bind by core round-robin
                thread_mask = threadIdx%Constants.CORE_NUM; 

            }else
            {
                //default affinity compact
                //implement a compact bind, 2 threads a core
                int tpn = 2*Constants.CORE_NUM;
                thread_mask = threadIdx%tpn;
                thread_mask /= 2;
            }

            bitSet.set(thread_mask);
            Affinity.setAffinity(bitSet);

            try{

                for(int cur_itr = 0; cur_itr < N; cur_itr++)
                {
                    if (threadIdx == 0)
                    {
                        colors_g = new int[num_verts];
                    }

                    barrier.await();

                    //start sampling colors
                    long elt = 0;
                    if(verbose && threadIdx == 0){
                        elt = System.currentTimeMillis();
                    }

                    Random rand = new Random();

                    //sampling the vertices of full graph g
                    for (int p = chunks[threadIdx]; p < chunks[threadIdx+1]; ++p){
                        colors_g[p] = rand.nextInt(num_colors) ;
                    }

                    barrier.await();
                    // if (threadIdx == 0)
                    //     LOG.info("Itr "+cur_itr + " Finish sampling");

                    // start doing counting
                    for( int s = subtemplate_count -1; s > 0; --s){

                        if (threadIdx == 0)
                        {
                            set_count = 0;
                            total_count = 0;
                            read_count = 0;
                            num_verts_sub_ato = num_verts_table[s];

                            if(verbose && threadIdx == 0)
                                LOG.info("Initing with sub "+ s + ", verts: " + num_verts_sub_ato );

                            int a = part.get_active_index(s);
                            int p = part.get_passive_index(s);

                            dt.init_sub(s, a, p);

                        }

                        barrier.await();

                        elt = 0;
                        //hit the bottom
                        if( num_verts_sub_ato == 1){

                            if(verbose && threadIdx == 0){
                                elt = System.currentTimeMillis();
                            }

                            //counting the bottom
                            //using relative v_ids
                            init_table_node_HJ(s, threadIdx, chunks);
                            barrier.await();

                            if(verbose && threadIdx == 0){
                                elt = System.currentTimeMillis() - elt;
                                LOG.info("s " + s +", init_table_node "+ elt + " ms");
                            }

                        }else{

                            if(verbose && threadIdx == 0){
                                elt = System.currentTimeMillis();
                            }

                            //counting non-bottom subtemplates
                            //using absolute v_ids
                            colorful_count_HJ(s, threadIdx, chunks);
                            barrier.await();

                            if(verbose && threadIdx == 0){
                                elt = System.currentTimeMillis() - elt;
                                LOG.info("s " + s +", array time "+ elt + " ms");
                            }
                        }

                        if(verbose && threadIdx == 0){
                            if( num_verts != 0){
                                double ratio1  =(double) set_count / (double) num_verts;
                                double ratio2 = (double) read_count /(double) num_verts;
                                LOG.info("  Sets: "+ set_count + " Total: "+num_verts + "  Ratio: " + ratio1);
                                LOG.info("  Reads: "+read_count + " Total: "+num_verts + "  Ratio: " + ratio2);
                            }else{
                                LOG.info("  Sets: "+ set_count + " Total: "+num_verts );
                                LOG.info("  Reads: "+read_count + " Total: "+num_verts);
                            }
                        }

                        if (threadIdx == 0)
                        {
                            int a = part.get_active_index(s);
                            int p = part.get_passive_index(s);

                            if( a != Constants.NULL_VAL)
                                dt.clear_sub(a);
                            if(p != Constants.NULL_VAL)
                                dt.clear_sub(p);
                        }

                    }

                    if(verbose && threadIdx == 0)
                        LOG.info("Done with initialization. Doing full count");

                    // do the count for the full template
                    if (threadIdx == 0)
                    {
                        full_count_ato = 0;
                        set_count = 0;
                        total_count = 0;
                        read_count = 0;

                        int a = part.get_active_index(0);
                        int p = part.get_passive_index(0);
                        dt.init_sub(0, a, p);

                    }
                    
                    barrier.await();

                    elt = 0;

                    if(verbose && threadIdx == 0) 
                        elt= System.currentTimeMillis();

                    full_count_ato = colorful_count_HJ(0, threadIdx, chunks);

                    barrier.await();

                    if(verbose && threadIdx == 0)
                    {
                        elt = System.currentTimeMillis() - elt;
                        LOG.info("s 0, array time " + elt + "ms");
                    }

                    if (threadIdx == 0)
                    {
                        int a = part.get_active_index(0);
                        int p = part.get_passive_index(0);
                        colors_g = null;
                        dt.clear_sub(a);
                        dt.clear_sub(p);
                    }
                    

                    if(verbose && threadIdx == 0){
                        if( num_verts != 0){
                            double ratio1  =(double) set_count / (double) num_verts;
                            double ratio2 = (double) read_count /(double) num_verts;
                            LOG.info("  Non-zero: "+ set_count + " Total: "+num_verts + "  Ratio: " + ratio1);
                            LOG.info("  Reads: "+read_count + " Total: "+num_verts + "  Ratio: " + ratio2);
                        }else{
                            LOG.info("  Non-zero: "+ set_count + " Total: "+num_verts );
                            LOG.info("  Reads: "+read_count + " Total: "+num_verts);
                        }

                        LOG.info("Full count: " + full_count_ato);

                    }

                    //add counts from every iteration
                    if (threadIdx == 0)
                    {
                        count_ato += full_count_ato;
                    }

                    //finish one iteration
                    if(verbose && threadIdx == 0){
                        elt = System.currentTimeMillis() - elt;
                        LOG.info("Itr "+cur_itr + " Time for count: "+ elt +" ms");
                    }

                }


            } catch (InterruptedException | BrokenBarrierException e) {
                LOG.info("Catch barrier exception in itr: " + cur_iter);
                e.printStackTrace();
            }

        }));

        // for(cur_iter = 0; cur_iter < N; cur_iter++){
        //
        //     long elt = 0;
        //     if(verbose){
        //         elt = System.currentTimeMillis();
        //     }
        //     //get template counts
        //     count += template_count();
        //     if(verbose){
        //         elt = System.currentTimeMillis() - elt;
        //         LOG.info("Time for count: "+ elt +" ms");
        //     }
        // }

        //----------------------- end of color_counting -----------------

        double final_count = count_ato / (double) N;

        //formula to compute the prob 
        // double prob_colorful = Util.factorial(num_colors) /
        //         ( Util.factorial(num_colors - t.num_vertices()) * Math.pow(num_colors, t.num_vertices()) );
        //
        // int num_auto = calculate_automorphisms ? Util.count_automorphisms(t): 1;
        // final_count = Math.floor(final_count / (prob_colorful * num_auto) + 0.5);
        //
        // if(verbose){
        //     LOG.info("Probability colorful: " + prob_colorful);
        //     LOG.info("Num automorphisms: " + num_auto );
        //     LOG.info("Final count: " + final_count);
        // }
        //
        // if(do_graphlet_freq || do_vert_output){
        //     System.err.println("do_gdd or do_vert skipped");
        // }

        delete_tables();
        part.clear_temparrays();

        return final_count;

    }

    int[] divide_chunks(int total, int partition){
        int chunks[] = new int[partition+1];
        chunks[0] = 0;
        int remainder = total % partition;
        int basic_size = total / partition;
        for(int i = 1; i <= partition; ++i) {
            chunks[i] = chunks[i - 1] + basic_size + (remainder > 0 ? 1 : 0);
            --remainder;
        }

        return chunks;
    }

    double[] get_vert_counts(){
        return final_vert_counts;
    }

    // This does a single counting for a given templates
    //Return the full scaled count for the template on the whole graph
    private double template_count(){
        //do random coloring for full graph
        Random rand = new Random();

        //vertice num of the full graph, huge
        int num_verts = g.num_vertices();
        colors_g = new int[num_verts];


        //coloring the graph. (replaced by multi-threading code below)
        /*
        for(int v = 0; v < num_verts; ++v){
            colors_g[v] = rand.nextInt(num_colors) ;
        }
        */

        //multi-threading
        //recording the start and end index
        //num_verts assigned to each thread
        int[] chunks = divide_chunks(num_verts, Constants.THREAD_NUM);

        //coloring the full graph (all the vertices)
        Thread[] threads = new Thread[Constants.THREAD_NUM];
        for(int t = 0; t < Constants.THREAD_NUM; ++t){
            final int index = t;
            threads[t] = new Thread(){
                public void run(){
                    for (int v = chunks[index]; v < chunks[index+1]; ++v){
                        colors_g[v] = rand.nextInt(num_colors) ;
                    }
                }
            };
            threads[t].start();
        }

        //waiting for threads to die
        for(int t =0 ; t < Constants.THREAD_NUM; ++t){
            try {
                threads[t].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        //start doing the counting, starting at bottom of partition tree
        // then go through all of the subtemplates except for the primary
        // since that's handled a bt differently
        for( int s = subtemplate_count -1; s > 0; --s){
            set_count = 0;
            total_count = 0;
            read_count = 0;
            int num_verts_sub = num_verts_table[s];

            if(verbose)
                LOG.info("Initing with sub "+ s + ", verts: " + num_verts_sub );

            int a = part.get_active_index(s);
            int p = part.get_passive_index(s);

            dt.init_sub(s, a, p);

            long elt = 0;
            //hit the bottom
            if( num_verts_sub == 1){
                if(verbose){
                    elt = System.currentTimeMillis();
                }
                init_table_node(s);
                if(verbose){
                    elt = System.currentTimeMillis() - elt;
                    LOG.info("s " + s +", init_table_node "+ elt + " ms");
                }
            }else{
                if(verbose){
                    elt = System.currentTimeMillis();
                }
                colorful_count(s);

                if(verbose){
                    elt = System.currentTimeMillis() - elt;
                    LOG.info("s " + s +", array time "+ elt + " ms");
                }
            }

            if(verbose){
                 if( num_verts != 0){
                     double ratio1  =(double) set_count / (double) num_verts;
                     double ratio2 = (double) read_count /(double) num_verts;
                     LOG.info("  Sets: "+ set_count + " Total: "+num_verts + "  Ratio: " + ratio1);
                     LOG.info("  Reads: "+read_count + " Total: "+num_verts + "  Ratio: " + ratio2);
                }else{
                    LOG.info("  Sets: "+ set_count + " Total: "+num_verts );
                    LOG.info("  Reads: "+read_count + " Total: "+num_verts);
                }
            }
            if( a != Constants.NULL_VAL)
                dt.clear_sub(a);
            if(p != Constants.NULL_VAL)
                dt.clear_sub(p);
        }

        if(verbose)
            LOG.info("Done with initialization. Doing full count");

        // do the count for the full template
        float full_count = 0;
        set_count = 0;
        total_count = 0;
        read_count = 0;
        long elt = 0;

        int a = part.get_active_index(0);
        int p = part.get_passive_index(0);
        dt.init_sub(0, a, p);

        if(verbose) elt= System.currentTimeMillis();
        full_count = colorful_count(0);

        if(verbose){
            elt = System.currentTimeMillis() - elt;
            LOG.info("s 0, array time " + elt + "ms");
        }
        colors_g = null;
        dt.clear_sub(a);
        dt.clear_sub(p);

        if(verbose){
            if( num_verts != 0){
                double ratio1  =(double) set_count / (double) num_verts;
                double ratio2 = (double) read_count /(double) num_verts;
                LOG.info("  Non-zero: "+ set_count + " Total: "+num_verts + "  Ratio: " + ratio1);
                LOG.info("  Reads: "+read_count + " Total: "+num_verts + "  Ratio: " + ratio2);
            }else{
                LOG.info("  Non-zero: "+ set_count + " Total: "+num_verts );
                LOG.info("  Reads: "+read_count + " Total: "+num_verts);
            }
            LOG.info("Full count: " + full_count);
        }

        return (double)full_count;
    }


    //s is subtemplate index
    private void init_table_node(int s){

        //replace unlabeled implementation with multi-threading
        if( !labeled) {
            int[] chunks = divide_chunks(num_verts_graph, Constants.THREAD_NUM);
            Thread[] threads = new Thread[Constants.THREAD_NUM];
            for (int t = 0; t < Constants.THREAD_NUM; ++t) {
                final int index = t;
                threads[t] = new Thread() {
                    public void run() {
                        for (int v = chunks[index]; v < chunks[index + 1]; ++v) {
                            //get the randomly assigned color value
                            int n = colors_g[v];
                            dt.set(v, comb_num_indexes_set[s][n], 1.0f);
                        }
                    }
                };
                threads[t].start();
            }
            //waiting for threads to die
            for(int t =0 ; t < Constants.THREAD_NUM; ++t){
                try {
                    threads[t].join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            set_count = num_verts_graph;

        }else{
            int set_count_loop  = 0;
            int[] labels_sub = part.get_labels(s);
            int label_s = labels_sub[0];
            for (int v = 0; v < num_verts_graph; ++v){
                int n = colors_g[v];
                int label_g = labels_g[v];
                if (label_g == label_s) {
                    dt.set(v, comb_num_indexes_set[s][n], 1.0f);
                    set_count_loop++;
                }
            }
            set_count = set_count_loop;
        }

    }


    private void init_table_node_HJ(int s, int threadIdx, int[] chunks) throws BrokenBarrierException, InterruptedException {

        //replace unlabeled implementation with multi-threading
        if( !labeled) {

            //initialization the bottom, using relative v_id
            for (int v = chunks[threadIdx]; v < chunks[threadIdx + 1]; ++v) {
                //get the randomly assigned color value
                int n = colors_g[v];
                dt.set(v, comb_num_indexes_set[s][n], 1.0f);
            }

            barrier.await();

            if (threadIdx == 0)
                set_count = num_verts_graph;

        }else{

            if (threadIdx == 0)
            {

                int set_count_loop  = 0;
                int[] labels_sub = part.get_labels(s);
                int label_s = labels_sub[0];
                for (int v = 0; v < num_verts_graph; ++v){
                    int n = colors_g[v];
                    int label_g = labels_g[v];
                    if (label_g == label_s) {
                        dt.set(v, comb_num_indexes_set[s][n], 1.0f);
                        set_count_loop++;
                    }
                }
                set_count = set_count_loop;
            }
            
        }

    }

    private float colorful_count(int s){

        //get vert num of subtemplate
        int num_verts_sub = subtemplates[s].num_vertices();

        //get vert num of active child 
        int active_index = part.get_active_index(s);
        int num_verts_a = num_verts_table[active_index];
        // colorset combinations from active child
        int num_combinations = choose_table[num_verts_sub][num_verts_a];


        long elt = System.currentTimeMillis();

        int[] chunks = divide_chunks(num_verts_graph, Constants.THREAD_NUM);

        Thread[] threads = new Thread[Constants.THREAD_NUM];
        final int[] set_count_loop = new int[Constants.THREAD_NUM];
        final int[] total_count_loop = new int[Constants.THREAD_NUM];
        final int[] read_count_loop = new int[Constants.THREAD_NUM];

        final float[] cc = new float[Constants.THREAD_NUM];

        for (int t = 0; t < Constants.THREAD_NUM; ++t) {
            final int index = t;
            threads[t] = new Thread() {
                public void run() {

                    int[] valid_nbrs = new int[max_degree];
                    assert(valid_nbrs != null);
                    int valid_nbrs_count = 0;

                    // each thread for a chunk
                    for (int v = chunks[index]; v < chunks[index + 1]; ++v) {

                        valid_nbrs_count = 0;

                        //table v must be initialized
                        if( dt.is_vertex_init_active(v)){

                            int[] adjs = g.adjacent_vertices(v);
                            int end = g.out_degree(v);
                            float[] counts_a = dt.get_active(v);

                            ++read_count_loop[index];

                            for(int i = 0; i < end; ++i){
                                int adj_i = adjs[i];
                                if(dt.is_vertex_init_passive(adj_i)){
                                    valid_nbrs[valid_nbrs_count++] = adj_i;
                                }
                            }

                            if(valid_nbrs_count != 0){

                                int num_combinations_verts_sub = choose_table[num_colors][num_verts_sub];

                                for(int n = 0; n < num_combinations_verts_sub; ++n){

                                    float color_count = 0.0f;

                                    int[] comb_indexes_a = comb_num_indexes[0][s][n];
                                    int[] comb_indexes_p = comb_num_indexes[1][s][n];

                                    int p = num_combinations -1;
                                    for(int a = 0; a < num_combinations; ++a, --p){
                                        float count_a = counts_a[comb_indexes_a[a]];
                                        if( count_a > 0){
                                            for(int i = 0; i < valid_nbrs_count; ++i){
                                                color_count += count_a * dt.get_passive(valid_nbrs[i], comb_indexes_p[p]);
                                                ++read_count_loop[index];
                                            }
                                        }
                                    }

                                    if( color_count > 0.0){
                                        cc[index] += color_count;
                                        ++set_count_loop[index];

                                        if(s != 0)
                                            dt.set(v, comb_num_indexes_set[s][n], color_count);
                                        else if(do_graphlet_freq || do_vert_output)
                                            final_vert_counts[v] += (double)color_count;
                                    }
                                    ++total_count_loop[index];
                                }
                            }
                        }


                    }
                    valid_nbrs = null;
                }
            };

            threads[t].start();
        }
        
        //waiting for threads to die
        for(int t =0 ; t < Constants.THREAD_NUM; ++t){
            try {
                threads[t].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        set_count = 0;
        total_count= 0;
        read_count = 0;
        float retval = 0;
        //reduction
        for(int i = 0; i < Constants.THREAD_NUM; ++i){
            set_count += set_count_loop[i];
            total_count += total_count_loop[i];
            read_count += read_count_loop[i];
            retval += cc[i];
        }

        elt = System.currentTimeMillis() - elt;
        //System.out.println("time: "+ elt +"ms");
        return retval;
    }

    /**
     * @brief counting
     *
     * @param s
     * @param threadIdx
     * @param chunks
     *
     * @return 
     */
    private float colorful_count_HJ(int s, int threadIdx, int[] chunks) throws BrokenBarrierException, InterruptedException {

        //get vert num of subtemplate
        if (threadIdx == 0)
        {
            num_verts_sub_ato = subtemplates[s].num_vertices();

            //get vert num of active child 
            int active_index = part.get_active_index(s);
            int num_verts_a = num_verts_table[active_index];

            // colorset combinations from active child
            num_combinations_ato = choose_table[num_verts_sub_ato][num_verts_a];

            set_count_loop_ato = new int[Constants.THREAD_NUM];
            total_count_loop_ato = new int[Constants.THREAD_NUM];
            read_count_loop_ato = new int[Constants.THREAD_NUM];
            cc_ato = new float[Constants.THREAD_NUM];
        }

        barrier.await();

        long elt = System.currentTimeMillis();

        int[] valid_nbrs = new int[max_degree];
        assert(valid_nbrs != null);
        int valid_nbrs_count = 0;

        // each thread for a chunk
        // loop by relative v_id
        for (int v = chunks[threadIdx]; v < chunks[threadIdx + 1]; ++v) {

            // v is relative v_id from 0 to num_verts -1 
            valid_nbrs_count = 0;

            //table v must be initialized
            if( dt.is_vertex_init_active(v)){

                //adjs is absolute v_id
                int[] adjs_abs = g.adjacent_vertices(v);
                int end = g.out_degree(v);
                float[] counts_a = dt.get_active(v);

                ++read_count_loop_ato[threadIdx];

                for(int i = 0; i < end; ++i){
                    int adj_i = g.get_relative_v_id(adjs_abs[i]);

                    if( adj_i >=0 && dt.is_vertex_init_passive(adj_i)){
                        valid_nbrs[valid_nbrs_count++] = adj_i;
                    }
                }

                if(valid_nbrs_count != 0){

                    int num_combinations_verts_sub = choose_table[num_colors][num_verts_sub_ato];

                    for(int n = 0; n < num_combinations_verts_sub; ++n){

                        float color_count = 0.0f;

                        int[] comb_indexes_a = comb_num_indexes[0][s][n];
                        int[] comb_indexes_p = comb_num_indexes[1][s][n];

                        int p = num_combinations_ato -1;
                        for(int a = 0; a < num_combinations_ato; ++a, --p){
                            float count_a = counts_a[comb_indexes_a[a]];
                            if( count_a > 0){
                                for(int i = 0; i < valid_nbrs_count; ++i){
                                    color_count += count_a * dt.get_passive(valid_nbrs[i], comb_indexes_p[p]);
                                    ++read_count_loop_ato[threadIdx];
                                }
                            }
                        }

                        if( color_count > 0.0){
                            cc_ato[threadIdx] += color_count;
                            ++set_count_loop_ato[threadIdx];

                            if(s != 0)
                                dt.set(v, comb_num_indexes_set[s][n], color_count);
                            else if(do_graphlet_freq || do_vert_output)
                                final_vert_counts[v] += (double)color_count;
                        }
                        ++total_count_loop_ato[threadIdx];
                    }
                }
            }


        }
        valid_nbrs = null;

        barrier.await();

        if (threadIdx == 0)
        {

            set_count = 0;
            total_count= 0;
            read_count = 0;

            retval_ato = 0;

            //reduction
            for(int i = 0; i < Constants.THREAD_NUM; ++i){
                set_count += set_count_loop_ato[i];
                total_count += total_count_loop_ato[i];
                read_count += read_count_loop_ato[i];
                retval_ato += cc_ato[i];
            }

            set_count_loop_ato = null;
            total_count_loop_ato = null;
            read_count_loop_ato = null;
            cc_ato = null;

        }

        barrier.await();
        // float retval = 0;
        // elt = System.currentTimeMillis() - elt;
        //System.out.println("time: "+ elt +"ms");
        return retval_ato;
    }

    private void create_tables(){
        //C_n^k compute the unique number of color set choices 
        //two dimensional
        choose_table = Util.init_choose_table(num_colors);
        //record vertices number of each subtemplate
        create_num_verts_table();
        //create color index for each combination of different set size
        create_all_index_sets();
        //create color sets for all subtemplates
        create_all_color_sets();
        //convert colorset combination into numeric values (hash function)
        create_comb_num_system_indexes();
        //free up memory space 
        delete_all_color_sets();
        //free up memory space
        delete_all_index_sets();
    }

    private void delete_tables(){
        for(int i = 0; i <= num_colors; ++i)
            choose_table[i] = null;
        choose_table = null;

        delete_comb_num_system_indexes();
        num_verts_table = null;
    }

    //record vertices number of each subtemplate
    private void create_num_verts_table(){
        num_verts_table = new int[subtemplate_count];
        for(int s = 0; s < subtemplate_count; ++s){
            num_verts_table[s] = subtemplates[s].num_vertices();
        }
    }

    private void create_all_index_sets(){

        //first dim (up to) how many colors
        index_sets = new int[num_colors][][][];

        for(int i = 0; i < (num_colors -1 ); ++i){

            int num_vals = i + 2;

            index_sets[i] = new int[num_vals -1][][];

            // second dim, for up to num_vals colors, has different set sizes
            for(int j = 0; j < (num_vals - 1); ++j){

                int set_size = j + 1;

                int num_combinations = Util.choose(num_vals, set_size);
                // third dim, for a set size, how many combinations from a given num of colors
                index_sets[i][j] = new int[num_combinations][];

                //set start from 1 to set_size
                //init set in increase order
                int[] set = Util.init_permutation(set_size);

                for(int k = 0; k < num_combinations; ++k){

                    // fourth dim, for a combination, having a set_size of color values
                    index_sets[i][j][k] = new int[set_size];

                    for(int p = 0; p < set_size; ++p){
                        index_sets[i][j][k][p] = set[p] - 1;
                    }

                    // permutate the color set
                    Util.next_set(set, set_size, num_vals);
                }
                set = null;
            }
        }
    }


    private void create_all_color_sets(){

        //first dim, num of subtemplates
        color_sets = new int[subtemplate_count][][][][];

        for(int s = 0; s < subtemplate_count; ++s){

            int num_verts_sub = subtemplates[s].num_vertices();

            if( num_verts_sub > 1){

                //determine how many sets in a subtemplate
                //choose num vertices of subtemplate from colors
                int num_sets = Util.choose(num_colors, num_verts_sub);

                //second dim, num of sets in a subtemplate 
                color_sets[s] = new int[num_sets][][][];

                //init permutation in colorset
                int[] colorset = Util.init_permutation(num_verts_sub);

                for(int n = 0; n < num_sets; ++n){

                    int num_child_combs = num_verts_sub - 1;
                    //third dim, for a subtemplate, a set, how many child combinations
                    color_sets[s][n] = new int[num_child_combs][][];

                    for(int c = 0; c < num_child_combs; ++c){
                        int num_verts_1 = c + 1;
                        int num_verts_2 = num_verts_sub - num_verts_1;

                        int[][] index_set_1 = index_sets[num_verts_sub-2][num_verts_1-1];
                        int[][] index_set_2 = index_sets[num_verts_sub-2][num_verts_2-1];

                        int num_child_sets = Util.choose(num_verts_sub, c+1);
                        color_sets[s][n][c] = new int[num_child_sets][];

                        for(int i = 0; i < num_child_sets; ++i){
                            color_sets[s][n][c][i] = new int[num_verts_sub];

                            for(int j = 0; j < num_verts_1; ++j)
                                color_sets[s][n][c][i][j] = colorset[index_set_1[i][j]];

                            for(int j = 0; j < num_verts_2; ++j)
                                color_sets[s][n][c][i][j+num_verts_1] = colorset[index_set_2[i][j]];
                        }
                    }
                    Util.next_set(colorset, num_verts_sub, num_colors);
                }
                colorset = null;
            }
        }
    }


    //hashtable like comb_num_system
    private void create_comb_num_system_indexes(){

        comb_num_indexes = new int[2][subtemplate_count][][];
        comb_num_indexes_set = new int[subtemplate_count][];

        // each subtemplate
        for(int s = 0; s < subtemplate_count; ++s){

            int num_verts_sub = subtemplates[s].num_vertices();
            int num_combinations_s = Util.choose(num_colors, num_verts_sub);

            if( num_verts_sub > 1){
                //for active and passive children  
                comb_num_indexes[0][s] = new int[num_combinations_s][];
                comb_num_indexes[1][s] = new int[num_combinations_s][];
            }

            comb_num_indexes_set[s] = new int[num_combinations_s];

            int[] colorset_set = Util.init_permutation(num_verts_sub);

            //loop over each combination instance
            for(int n = 0; n < num_combinations_s; ++n){

                //get the hash value for a colorset instance
                //Util.get_color_index the impl of hash function
                comb_num_indexes_set[s][n] = Util.get_color_index(colorset_set, num_verts_sub);

                if( num_verts_sub > 1){

                    int num_verts_a = part.get_num_verts_active(s);
                    int num_verts_p = part.get_num_verts_passive(s);

                    int[] colors_a;
                    int[] colors_p;
                    int[][] colorsets = color_sets[s][n][num_verts_a-1];

                    int num_combinations_a= Util.choose(num_verts_sub, num_verts_a);
                    comb_num_indexes[0][s][n] = new int[num_combinations_a];
                    comb_num_indexes[1][s][n] = new int[num_combinations_a];

                    int p = num_combinations_a - 1;
                    for(int a = 0; a < num_combinations_a; ++a, --p){
                        colors_a = colorsets[a];
                        //are they the same?
                        // colors_p = colorsets[p] + num_verts_a;
                        colors_p = new int[num_verts_p];
                        System.arraycopy(colorsets[p], num_verts_a, colors_p, 0, num_verts_p);

                        int color_index_a = Util.get_color_index(colors_a, num_verts_a);
                        int color_index_p = Util.get_color_index(colors_p, num_verts_p);

                        comb_num_indexes[0][s][n][a] = color_index_a;
                        comb_num_indexes[1][s][n][p] = color_index_p;
                    }
                }

                //permutate the colorset_set
                Util.next_set(colorset_set, num_verts_sub, num_colors);

            }
            colorset_set = null;
        }
    }

    private void delete_comb_num_system_indexes(){
        for(int s = 0; s < subtemplate_count; ++s){
            int num_verts_sub = subtemplates[s].num_vertices();
            int num_combinations_s = Util.choose(num_colors, num_verts_sub);

            for(int n = 0; n < num_combinations_s; ++n){
                if(num_verts_sub > 1){
                    comb_num_indexes[0][s][n] = null;
                    comb_num_indexes[1][s][n] = null;
                }
            }

            if(num_verts_sub > 1){
                comb_num_indexes[0][s] = null;
                comb_num_indexes[1][s] = null;
            }

            comb_num_indexes_set[s]= null;
        }
        comb_num_indexes[0]= null;
        comb_num_indexes[1] = null;
        comb_num_indexes= null;
        comb_num_indexes_set = null;

    }


    private void delete_all_color_sets(){
        for(int s = 0; s < subtemplate_count; ++s){
            int num_verts_sub = subtemplates[s].num_vertices();
            if( num_verts_sub > 1) {
                int num_sets = Util.choose(num_colors, num_verts_sub);

                for (int n = 0; n < num_sets; ++n) {
                    int num_child_combs = num_verts_sub - 1;
                    for (int c = 0; c < num_child_combs; ++c) {
                        int num_child_sets = Util.choose(num_verts_sub, c + 1);
                        for (int i = 0; i < num_child_sets; ++i) {
                            color_sets[s][n][c][i] = null;
                        }
                        color_sets[s][n][c] = null;
                    }
                    color_sets[s][n] = null;
                }
                color_sets[s] = null;
            }
        }
        color_sets = null;
    }


    private void delete_all_index_sets(){

        for (int i = 0; i < (num_colors-1); ++i) {
            int num_vals = i + 2;
            for (int j = 0; j < (num_vals-1); ++j) {
                int set_size = j + 1;
                int num_combinations = Util.choose(num_vals, set_size);
                for (int k = 0; k < num_combinations; ++k) {
                    index_sets[i][j][k] = null;
                }
                index_sets[i][j] = null;
            }
            index_sets[i] = null;
        }
        index_sets = null;
    }

    /**
     * @brief synchronize threads 
     *
     * @return 
     */
    // private void barrier()
    //     throws BrokenBarrierException, InterruptedException {
    //     barrierCounter.compareAndSet(Constants.THREAD_NUM, 0);
    //     barrierCounter.incrementAndGet();
    //     while (barrierCounter.get() != Constants.THREAD_NUM) {
    //         ;
    //     }
    // }

}
