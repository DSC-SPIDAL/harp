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
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Random;

import edu.iu.harp.example.IntArrPlus;
import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.IntArray;
import edu.iu.harp.resource.DoubleArray;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

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
    // int max_degree;

    // what is this ?
    double[] final_vert_counts;
    boolean do_graphlet_freq;
    boolean do_vert_output;
    boolean calculate_automorphisms;
    boolean verbose;

    int set_count;
    int total_count;
    int read_count;

    //thread barrier
    // private AtomicInteger barrierCounter = new AtomicInteger(0);
    private CyclicBarrier barrier;
    private int num_verts_sub_ato;
    private int num_combinations_ato;

    private int[] set_count_loop_ato; 
    private int[] total_count_loop_ato; 
    private int[] read_count_loop_ato; 
    private float[] cc_ato; 
    //for debug
    private float[] cc_ato_test; 
    private float[] cc_ato_cur_sum; 
    private int retval_ato = 0;
    private float full_count_ato = 0;
    private double count_ato = 0;

    private float comm_count_de;
    private float local_count_de;

    //for comm send/recv vertex abs id
    private Set<Integer>[] comm_mapper_vertex;
    private Table<IntArray> recv_vertex_table;
    private Table<IntArray> send_vertex_table;
    private Table<IntArray> comm_vertex_table;

    private Table<SCSet> comm_data_table;

    private SCCollectiveMapper mapper;

    private int workerNum;
    private int max_abs_id;

    private int passive_len;
    private int active_child;
    private int passive_child;

    private int error_check_num = 0;
    private int error_check_num2 = 0;

    //comm abs_adj id for each local rel v
    private int[][] update_map;
    // the size of comm abs_adj for each local rel_v 
    private int[] update_map_size; 
    // mapping an arbitary abs_v_id to its belonged mapper id
    private int[] abs_v_to_mapper;
    // mapping an arbitary abs_v_id to the relative position in updating queues 
    private int[] abs_v_to_queue;
    //update counts from communicated data 
    private int[][] update_queue_pos;
    private float[][] update_queue_counts;

    private final ReentrantLock lock = new ReentrantLock();

    private int thread_num = 24;
    private int core_num = 24;
    private String affinity = "compact";

    private long update_start = 0;

    //inovoked in Fascia.java
    void init(Graph full_graph, int max_v_id, int thread_num, int core_num, String affinity, boolean calc_auto, boolean do_gdd, boolean do_vert, boolean verb){

        g = full_graph;
        num_verts_graph = g.num_vertices();
        labels_g = g.labels;
        labeled = g.labeled;
        do_graphlet_freq = do_gdd;
        do_vert_output = do_vert;
        calculate_automorphisms = calc_auto;
        verbose = verb;

        this.max_abs_id = max_v_id;
        this.thread_num = thread_num;
        this.core_num = core_num;
        this.affinity = affinity;

        this.cc_ato = new float[this.thread_num];
        this.cc_ato_test = new float[this.thread_num];
        this.cc_ato_cur_sum = new float[this.thread_num];

        //initialize dynamic table
        dt = new dynamic_table_array();
        barrier = new CyclicBarrier(this.thread_num);

        if( do_graphlet_freq || do_vert_output){
            //Skipped
        }
    }

    void init_comm(int[] mapper_id_vertex, SCCollectiveMapper mapper) {

        this.mapper = mapper;
        
        int n_mapper = mapper.getNumWorkers();
        comm_mapper_vertex = new HashSet[n_mapper];
        for(int p=0; p<n_mapper; p++)
            comm_mapper_vertex[p] = new HashSet<>(); 

        //create abs mapping structure
        this.abs_v_to_mapper = mapper_id_vertex;
        abs_v_to_queue = new int[this.max_abs_id + 1];

        //create update structure
        update_map = new int[g.num_vertices()][];
        // int max_deg = g.max_degree();
        for(int p=0;p<g.num_vertices();p++)
            update_map[p] = new int[g.out_degree(p)];

        update_map_size = new int[g.num_vertices()];

        this.update_queue_pos = new int[mapper.getNumWorkers()][];
        this.update_queue_counts = new float[mapper.getNumWorkers()][];
        
        //loop over all the adj of local graph verts
        //adj_array stores abs id 
        int[] adj_array = g.adjacencies();
        for(int p=0;p<adj_array.length;p++)
        {
            int adj_abs_id = adj_array[p];
            int mapper_id = mapper_id_vertex[adj_abs_id];
            comm_mapper_vertex[mapper_id].add(new Integer(adj_abs_id));
        }

        //convert set to arraylist
        comm_vertex_table = new Table<>(0, new IntArrPlus());
        recv_vertex_table = new Table<>(0, new IntArrPlus());
        send_vertex_table = new Table<>(0, new IntArrPlus());

        int localID = mapper.getSelfID();
        this.workerNum = mapper.getNumWorkers();

        for(int p=0;p<n_mapper;p++)
        {
            //create partition id, the upper 16 bits stores receiver id
            //the lower 16 bits stores sender id
            if ( (p != localID) &&  comm_mapper_vertex[p].size() > 0 )
            {
                int comm_id = ( (p << 16) | localID );
                //set to arraylist
                ArrayList<Integer> temp_array = new ArrayList<>(comm_mapper_vertex[p]);
                //arraylist to int[]
                int[] temp_array_primitive = ArrayUtils.toPrimitive(temp_array.toArray(new Integer[temp_array.size()]));

                //create mapping from adj_abs_id to relative t in update offset queue
                for(int t = 0; t< temp_array_primitive.length; t++)
                    this.abs_v_to_queue[temp_array_primitive[t]] = t;

                int[] temp_array_primitive_copy = temp_array_primitive.clone();
                IntArray comm_array = new IntArray(temp_array_primitive, 0, temp_array_primitive.length);
                IntArray comm_array_copy = new IntArray(temp_array_primitive_copy, 0, temp_array_primitive_copy.length);

                //list of partitions to be recieved from other mappers
                recv_vertex_table.addPartition(new Partition<>(p, comm_array_copy));

                //table to communicate with other mappers
                comm_vertex_table.addPartition(new Partition<>(comm_id, comm_array));
            }

        }

        LOG.info("Start regroup comm table");
        mapper.regroup("sc", "regroup-send-recv-vertex", comm_vertex_table, new SCPartitioner(mapper.getNumWorkers()));
        LOG.info("Finish regroup comm table");

        //check the communicated table content
        for(int comm_id : comm_vertex_table.getPartitionIDs())
        {
            LOG.info("Worker ID: " + localID + "; sender id: " + (comm_id >>> 16) + "; receiver id: " + ( comm_id & ( (1 << 16) -1 ) ));
            LOG.info("Partition Size: " + comm_vertex_table.getPartition(comm_id).get().get().length);
            //create send table
            send_vertex_table.addPartition(new Partition(( comm_id & ( (1 << 16) -1 ) ), comm_vertex_table.getPartition(comm_id).get()));
        }

        //finish
        comm_mapper_vertex = null;
        LOG.info("Finish comm table: Recv Partitoin num: " + recv_vertex_table.getNumPartitions() + "; Send Partition num: " + send_vertex_table.getNumPartitions());
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
        dt.init(subtemplates, subtemplate_count, g.num_vertices(), num_colors, max_abs_id);

        //begin the counting 
        //launch LRT threads
        count_ato = 0.0;

        //vertice num of the full graph, huge
        int num_verts = g.num_vertices();
        int[] chunks = divide_chunks(num_verts, this.thread_num);   

        //triggering vtune profiling
        // java.nio.file.Path vtune_file = java.nio.file.Paths.get("vtune-flag.txt");
        // String flag_trigger = "Start training process and trigger vtune profiling.";
        // try{
        //     java.nio.file.Files.write(vtune_file, flag_trigger.getBytes());
        // }catch (IOException e)
        // {
        //    LOG.info("Failed to create vtune trigger flag");
        // }

        launchHabaneroApp( () -> forallChunked(0, this.thread_num-1, (threadIdx) -> {

            //set Java threads affinity
            BitSet bitSet = new BitSet(this.core_num);
            int thread_mask = 0;

        if (threadIdx == 0)
        {
            LOG.info("Set up threads affinity: Core Num: " + this.core_num + 
                "; Total Threads: " + this.thread_num + "; affinity: " + this.affinity);
        }

        if (this.affinity == "scatter")
        {
            //implement threads bind by core round-robin
            thread_mask = threadIdx%this.core_num; 

        }else
        {
            //default affinity compact
            //implement a compact bind, 2 threads a core
            int tpn = 2*this.core_num;
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

                if (threadIdx == 0)
                {
                    Random rand = new Random(System.currentTimeMillis());
                    //sampling all the local g graphs
                    for(int p=0;p<num_verts; p++)
                        colors_g[p] = rand.nextInt(num_colors) ;

                }

                //sampling the vertices of full graph g
                // for (int p = chunks[threadIdx]; p < chunks[threadIdx+1]; ++p){
                //     colors_g[p] = rand.nextInt(num_colors) ;
                // }

                barrier.await();
                // if (threadIdx == 0)
                //     LOG.info("Itr "+cur_itr + " Finish sampling");

                // start doing counting
                for( int s = subtemplate_count -1; s > 0; --s){

                    //debug
                    // float local_count_de = 0.0f;
                    // float comm_count_de = 0.0f;

                    if (threadIdx == 0)
                    {
                        set_count = 0;
                        total_count = 0;
                        read_count = 0;

                        //get num_vert of subtemplate s
                        num_verts_sub_ato = num_verts_table[s];

                        if(verbose && threadIdx == 0)
                            LOG.info("Initing with sub "+ s + ", verts: " + num_verts_sub_ato );

                        int a = part.get_active_index(s);
                        int p = part.get_passive_index(s);

                        LOG.info("Subtemplate: " + s + "; active_idx: " + a + "; passive_idx: " + p);

                        //assign active and passive subtemplates to 
                        //s and create the table[s] for g verts
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
                        //check
                        // LOG.info("Bottom init: " + s);
                        if ( s == subtemplate_count - 1)
                        {
                            init_table_node_HJ(s, threadIdx, chunks);
                            barrier.await();
                        }
                        else
                        {
                            if (threadIdx == 0)
                                dt.set_to_table(subtemplate_count - 1, s);

                            barrier.await();
                        }

                        
                        if(verbose && threadIdx == 0){
                            elt = System.currentTimeMillis() - elt;
                            LOG.info("s " + s +", init_table_node "+ elt + " ms");
                        }

                    }else{

                        if(verbose && threadIdx == 0){
                            elt = System.currentTimeMillis();
                        }

                        //counting non-bottom subtemplates
                        //using rel v_ids
                        // local_count_de = colorful_count_HJ(s, threadIdx, chunks);
                        colorful_count_HJ(s, threadIdx, chunks);

                        barrier.await();

                        if(verbose && threadIdx == 0){
                            local_count_de = 0.0f;
                            for(int k=0; k< this.thread_num; k++)
                                local_count_de += cc_ato[k];

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



                    //start communication part single thread 
                    barrier.await();

                    //single thread for comm
                        // only for subtemplates size > 1, having neighbours on other mappers
                        // only if more than one mapper, otherwise all g verts are local
                        if (num_verts_sub_ato > 1 && mapper.getNumWorkers() > 1)
                        {

                            if (threadIdx == 0)
                            {

                                LOG.info("Start prepare regroup comm for subtemplate: " + s);
                                long reg_prep_start = System.currentTimeMillis();

                                //prepare the sending partitions
                                comm_data_table = new Table<>(0, new SCSetCombiner());
                                //pack colorset_idx and count into a 64 bits double
                                int localID = mapper.getSelfID();
                                //send_vertex_table
                                for(int send_id : send_vertex_table.getPartitionIDs())
                                {
                                    int comm_id = ( (send_id << 16) | localID );

                                    //not all of vert in the comm_vert_list required
                                    int[] comm_vert_list = send_vertex_table.getPartition(send_id).get().get();
                                    //for each vert find the colorsets and counts in methods of dynamic_table_array
                                    int comb_len = dt.get_num_color_set(part.get_passive_index(s)); 
                                    // int num_combinations_verts_compress = choose_table[num_colors][num_verts_table[part.get_passive_index(s)]];
                                    passive_len = comb_len;

                                    SCSet comm_data = dt.compress_send_data(comm_vert_list, comb_len, g);
                                    comm_data_table.addPartition(new Partition<>(comm_id, comm_data));
                                }

                                LOG.info("Finish prepare regroup comm for subtemplate: " + s + 
                                        "; time used: " + (System.currentTimeMillis() - reg_prep_start));

                                LOG.info("Start regroup comm for subtemplate: " + s);
                                long reg_start = System.currentTimeMillis();
                                //start the regroup communication
                                mapper.regroup("sc", "regroup counts data", comm_data_table, new SCPartitioner(mapper.getNumWorkers()));
                                LOG.info("Finish regroup comm for subtemplate: " 
                                        + s + "; time used: " + (System.currentTimeMillis() - reg_start));

                                //start append comm counts to local dt data structure
                                LOG.info("Start update comm counts for subtemplate: " + s);

                                //update local g counts by adj from each other mapper
                                for(int comm_id : comm_data_table.getPartitionIDs())
                                {
                                    int update_id = ( comm_id & ( (1 << 16) -1 ) );
                                    LOG.info("Worker ID: " + localID + "; Recv from worker id: " + update_id);
                                    // update vert list accounts for the adj vert may be used to update local v
                                    // int[] update_vert_list = recv_vertex_table.getPartition(update_id).get().get();
                                    SCSet scset = comm_data_table.getPartition(comm_id).get();
                                    this.update_queue_pos[update_id] = scset.get_v_offset();
                                    this.update_queue_counts[update_id] = scset.get_counts_data();
                                    // ??? comb_num of the cur subtemplate or active child one ?
                                    // LOG.info("update list len: " + update_vert_list.length );
                                }
                            }


                            barrier.await();

                            if (threadIdx == 0)
                                update_start = System.currentTimeMillis();

                            // comm_count_de = update_comm(s, threadIdx, chunks);
                            update_comm(s, threadIdx, chunks);
                            barrier.await();
                            

                            if (threadIdx == 0)
                            {
                                //check active child total counts

                                
                                // comm_count_de  = 0.0f;
                                // for(int k = 0; k< this.thread_num; k++)
                                // {
                                //     LOG.info("Active child of : " + s + " idx: " + k + " is: " + cc_ato_test[k]);
                                //     comm_count_de += cc_ato_test[k];
                                // }
                                // LOG.info("Total counts of Active child of sub: " + s + " is " + comm_count_de);
                                //
                                // comm_count_de  = 0.0f;
                                // for(int k = 0; k< this.thread_num; k++)
                                // {
                                //     LOG.info("Remote Update Template: " + s + " idx: " + k + " is: " + cc_ato[k]);
                                //     comm_count_de += cc_ato[k];
                                // }
                                // LOG.info("Local counts of updated of sub: " + s + " is " + comm_count_de);
                                //
                                // comm_count_de  = 0.0f;
                                // for(int k = 0; k< this.thread_num; k++)
                                // {
                                //     LOG.info("Cur Table counts after update: " + s + " idx: " + k + " is: " + cc_ato_cur_sum[k]);
                                //     comm_count_de += cc_ato_cur_sum[k];
                                // }

                                // LOG.info("Total counts of cur sub: " + s + " after update is " + comm_count_de);

                                
                                // clean up memory
                                dt.clear_comm_counts();
                                for(int k = 0; k< mapper.getNumWorkers();k++)
                                {
                                    this.update_queue_pos[k] = null;
                                    this.update_queue_counts[k] = null;
                                }

                                comm_data_table = null;
                            }

                        }

                    
                        // printout results for sub s
                        barrier.await();
                        print_counts(s, threadIdx, chunks);
                        // if (threadIdx == 0)
                        // {
                        //     print_counts_single(s);
                        // }
                            
                        barrier.await();

                        if (threadIdx == 0 )
                        {
                            float sum_count = 0.0f;
                            for(int k = 0; k< this.thread_num; k++)
                                sum_count += cc_ato[k];

                            LOG.info("Finish update comm counts for subtemplate: " 
                                        + s + "; total counts: " + sum_count);
                        }

                    //print out the total counts for subtemplate s from all mappers
                    // if (num_verts_sub_ato > 1 && threadIdx == 0)
                    // {
                    //     //allreduce to get total counts for subtemplae s
                    //     if ( mapper.getNumWorkers() > 1  )
                    //     {
                    //         //allreduce
                    //         Table<DoubleArray> sub_count_table = new Table<>(0, new DoubleArrPlus());
                    //         DoubleArray sub_count_array = DoubleArray.create(2, false);
                    //
                    //         sub_count_array.get()[0] = local_count_de ;
                    //         sub_count_array.get()[1] = comm_count_de ;
                    //
                    //         sub_count_table.addPartition(new Partition<>(0, sub_count_array));
                    //
                    //         mapper.allreduce("sc", "get-sub-count", sub_count_table);
                    //         double sub_count_local = sub_count_table.getPartition(0).get().get()[0];
                    //         double sub_count_remote = sub_count_table.getPartition(0).get().get()[1];
                    //
                    //         // LOG.info("Total count for subtemplate: " + s + " is: " + sub_count);
                    //         LOG.info("For subtemplate: " + s + " local count is: " +sub_count_local + "; remote count is: " + sub_count_remote
                    //                 + "; total count is: " + (sub_count_local + sub_count_remote) );
                    //
                    //     }
                    //     else
                    //     {
                    //         LOG.info("For subtemplate: " + s + " local count is: " +local_count_de + "; remote count is: " + comm_count_de
                    //                 + "; total count is: " + (local_count_de + comm_count_de) );
                    //     }
                    //
                    // }

                    barrier.await();

                    if (threadIdx == 0)
                    {
                        int a = part.get_active_index(s);
                        int p = part.get_passive_index(s);

                        // if( a != SCConstants.NULL_VAL)
                        //     dt.clear_sub(a);
                        // if(p != SCConstants.NULL_VAL)
                        //     dt.clear_sub(p);
                    }

                    barrier.await();
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

                    // num_verts_sub_ato = num_verts_table[0];
                    int a = part.get_active_index(0);
                    int p = part.get_passive_index(0);

                    LOG.info("Subtemplate 0 ; active_idx: " + a + "; passive_idx: " + p);
                    dt.init_sub(0, a, p);

                }

                barrier.await();

                elt = 0;

                if(verbose && threadIdx == 0) 
                    elt= System.currentTimeMillis();

                // full_count_ato = colorful_count_HJ(0, threadIdx, chunks);
                colorful_count_HJ(0, threadIdx, chunks);

                barrier.await();

                // if(verbose && threadIdx == 0)
                // {
                //     full_count_ato = 0.0f;
                //     for(int k=0; k< this.thread_num; k++)
                //     {
                //         full_count_ato += cc_ato[k];
                //     }
                //
                //     elt = System.currentTimeMillis() - elt;
                //     LOG.info("s 0, array time " + elt + "ms");
                // }

                // float added_last = 0.0f;

                //comm and add the communicated counts to full_count_ato
                barrier.await();
                
                num_verts_sub_ato = num_verts_table[0];


                // only for subtemplates size > 1, having neighbours on other mappers
                // only if more than one mapper, otherwise all g verts are local
                if (num_verts_sub_ato > 1 && mapper.getNumWorkers() > 1)
                {

                    if (threadIdx == 0)
                    {

                        LOG.info("Start prepare regroup comm for last subtemplate");
                        long reg_prep_start = System.currentTimeMillis();

                        //prepare the sending partitions
                        comm_data_table = new Table<>(0, new SCSetCombiner());
                        //pack colorset_idx and count into a 64 bits double
                        int localID = mapper.getSelfID();
                        //send_vertex_table
                        for(int send_id : send_vertex_table.getPartitionIDs())
                        {
                            int comm_id = ( (send_id << 16) | localID );

                            //not all of vert in the comm_vert_list required
                            int[] comm_vert_list = send_vertex_table.getPartition(send_id).get().get();
                            //for each vert find the colorsets and counts in methods of dynamic_table_array
                            int comb_len = dt.get_num_color_set(part.get_passive_index(0)); 
                            // int num_combinations_verts_compress = choose_table[num_colors][num_verts_table[part.get_passive_index(s)]];
                            SCSet comm_data = dt.compress_send_data(comm_vert_list, comb_len, g);
                            comm_data_table.addPartition(new Partition<>(comm_id, comm_data));
                        }

                        //debug
                        // LOG.info("passive of 0 is template: " + part.get_passive_index(0) + " vert num: " + num_verts_table[part.get_passive_index(0)]);

                        // LOG.info("Finish prepare regroup comm for last subtemplate; time used: " + (System.currentTimeMillis() - reg_prep_start));

                        // LOG.info("Start regroup comm for last subtemplate");
                        long reg_start = System.currentTimeMillis();
                        //start the regroup communication
                        mapper.regroup("sc", "regroup counts data", comm_data_table, new SCPartitioner(mapper.getNumWorkers()));
                        LOG.info("Finish regroup comm for last subtemplate: " 
                                + "; time used: " + (System.currentTimeMillis() - reg_start));

                        //start append comm counts to local dt data structure
                        LOG.info("Start update comm counts for subtemplate:");

                        //update local g counts by adj from each other mapper
                        for(int comm_id : comm_data_table.getPartitionIDs())
                        {
                            int update_id = ( comm_id & ( (1 << 16) -1 ) );
                            LOG.info("Worker ID: " + localID + "; Recv from worker id: " + update_id);
                            // update vert list accounts for the adj vert may be used to update local v
                            // int[] update_vert_list = recv_vertex_table.getPartition(update_id).get().get();
                            SCSet scset = comm_data_table.getPartition(comm_id).get();
                            this.update_queue_pos[update_id] = scset.get_v_offset();
                            this.update_queue_counts[update_id] = scset.get_counts_data();
                            // ??? comb_num of the cur subtemplate or active child one ?
                            // LOG.info("update list len: " + update_vert_list.length );
                        }

                    }
                    // int num_combinations_verts_sub = choose_table[num_colors][num_verts_table[s]];
                    barrier.await();

                    if (threadIdx == 0)
                        update_start = System.currentTimeMillis();

                    update_comm(0, threadIdx, chunks);
                    barrier.await();

                    if (threadIdx == 0)
                    {
                        //check error num
                        // LOG.info("Error num: " + error_check_num);
                        // LOG.info("Error num2: " + error_check_num2);

                        // comm_count_de  = 0.0f;
                        // for(int k = 0; k< this.thread_num; k++)
                        // {
                        //     LOG.info("Active child of last sub: " + " idx: " + k + " is: " + cc_ato_test[k]);
                        //     comm_count_de += cc_ato_test[k];
                        // }
                        // LOG.info("Total counts of Active child of last sub: " + " is " + comm_count_de);
                        //
                        // comm_count_de = 0.0f;
                        // for(int k=0;k<this.thread_num;k++)
                        // {
                        //     LOG.info("Remote Update Last Template: " + " idx: " + k + " is: " + cc_ato[k]);
                        //     comm_count_de += cc_ato[k];
                        // }
                        //
                        // LOG.info("Local counts of updated of last sub: " + " is " + comm_count_de);
                        //
                        // comm_count_de  = 0.0f;
                        // for(int k = 0; k< this.thread_num; k++)
                        // {
                        //     LOG.info("Last Table counts after update: " +  " idx: " + k + " is: " + cc_ato_cur_sum[k]);
                        //     comm_count_de += cc_ato_cur_sum[k];
                        // }
                        //
                        // LOG.info("Total counts of last sub: " + " after update is " + comm_count_de);
                        //
                        // LOG.info("Finish update comm counts for last subtemplate: " 
                        //         + "; time used: " + (System.currentTimeMillis() - update_start));

                        // clean up memory
                        dt.clear_comm_counts();
                        for(int k = 0; k< mapper.getNumWorkers();k++)
                        {
                            this.update_queue_pos[k] = null;
                            this.update_queue_counts[k] = null;
                        }

                        comm_data_table = null;
                    }
                }

                // printout results for last sub 
                if (threadIdx == 0)
                    LOG.info("Start counting final templates counts");

                barrier.await();
                // if (threadIdx == 0)
                //     print_counts_single(0);
                print_counts(0, threadIdx, chunks);
                barrier.await();

                if (threadIdx == 0)
                    LOG.info("Finish counting final templates counts");

                if (threadIdx == 0 )
                {
                    float sum_count = 0.0f;
                    for(int k = 0; k< this.thread_num; k++)
                            sum_count += cc_ato[k];

                    full_count_ato = sum_count;

                    LOG.info("Finish update comm counts for last subtemplate: " 
                            +  "; total counts: " + sum_count);
                }

                barrier.await();

                if (threadIdx == 0)
                {
                    int a = part.get_active_index(0);
                    int p = part.get_passive_index(0);
                    colors_g = null;
                    dt.clear_sub(a);
                    dt.clear_sub(p);
                }

                //check last subtemplate counts
                // if (threadIdx == 0)
                // {
                //     //allreduce to get total counts for subtemplae s
                //     if ( mapper.getNumWorkers() > 1  )
                //     {
                //         //allreduce
                //         Table<DoubleArray> sub_count_table = new Table<>(0, new DoubleArrPlus());
                //         DoubleArray sub_count_array = DoubleArray.create(2, false);
                //
                //         sub_count_array.get()[0] = full_count_ato;
                //         sub_count_array.get()[1] = comm_count_de;
                //
                //         sub_count_table.addPartition(new Partition<>(0, sub_count_array));
                //
                //         mapper.allreduce("sc", "get-sub-count", sub_count_table);
                //         double full_count_ato_all = sub_count_table.getPartition(0).get().get()[0];
                //         double added_last_all = sub_count_table.getPartition(0).get().get()[1];
                //
                //         // LOG.info("Total count for subtemplate: " + s + " is: " + sub_count);
                //         LOG.info("For last subtemplate local count is: " +full_count_ato_all + "; remote count is: " + added_last_all
                //                 + "; total count is: " + (full_count_ato_all + added_last_all) );
                //
                //     }
                //     else
                //     {    
                //         LOG.info("For last subtemplate local count is: " +full_count_ato + "; remote count is: " + comm_count_de
                //                 + "; total count is: " + (full_count_ato + comm_count_de) );
                //     }
                //
                //     full_count_ato += comm_count_de;
                //
                // }

                // if(verbose && threadIdx == 0){
                //     if( num_verts != 0){
                //         double ratio1  =(double) set_count / (double) num_verts;
                //         double ratio2 = (double) read_count /(double) num_verts;
                //         LOG.info("  Non-zero: "+ set_count + " Total: "+num_verts + "  Ratio: " + ratio1);
                //         LOG.info("  Reads: "+read_count + " Total: "+num_verts + "  Ratio: " + ratio2);
                //     }else{
                //         LOG.info("  Non-zero: "+ set_count + " Total: "+num_verts );
                //         LOG.info("  Reads: "+read_count + " Total: "+num_verts);
                //     }
                //
                //     LOG.info("Full count: " + full_count_ato);
                //
                // }

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


        //----------------------- end of color_counting -----------------

        double final_count = count_ato / (double) N;

        //free memory
        dt.free_comm_counts();
        recv_vertex_table = null;
        send_vertex_table = null;
        comm_vertex_table = null;
        update_map = null;

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

    private void init_table_node_HJ(int s, int threadIdx, int[] chunks) throws BrokenBarrierException, InterruptedException {

        //replace unlabeled implementation with multi-threading
        if( !labeled) {

            //initialization the bottom, using relative v_id
            for (int v = chunks[threadIdx]; v < chunks[threadIdx + 1]; ++v) {
                //get the randomly assigned color value
                int n = colors_g[v];
                //put counts into curTable for rel v id 
                //template s and color n
                dt.set(v, comb_num_indexes_set[s][n], 1.0f);
            }

            barrier.await();

            if (threadIdx == 0)
                LOG.info("Bottom inited subtemplate: " + dt.cur_sub);

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

    /**
     * @brief counting
     *
     * @param s
     * @param threadIdx
     * @param chunks
     *
     * @return 
     */
    private void colorful_count_HJ(int s, int threadIdx, int[] chunks) throws BrokenBarrierException, InterruptedException {

        if (threadIdx == 0)
        {
            //get vert num of subtemplate
            num_verts_sub_ato = subtemplates[s].num_vertices();

            //get vert num of active child 
            int active_index = part.get_active_index(s);
            int num_verts_a = num_verts_table[active_index];

            // colorset combinations from active child
            // combination of colors for active child
            num_combinations_ato = choose_table[num_verts_sub_ato][num_verts_a];

            set_count_loop_ato = new int[this.thread_num];
            total_count_loop_ato = new int[this.thread_num];
            read_count_loop_ato = new int[this.thread_num];
            // cc_ato = new float[this.thread_num];
            // for(int k=0;k<this.thread_num;k++)
                // cc_ato[k] = 0.0f;
        }

        //emtpy cc_ato
        cc_ato[threadIdx] = 0.0f;

        barrier.await();

        long elt = System.currentTimeMillis();

        int[] valid_nbrs = new int[g.max_degree()];
        assert(valid_nbrs != null);
        int valid_nbrs_count = 0;

        // each thread for a chunk
        // loop by relative v_id
        for (int v = chunks[threadIdx]; v < chunks[threadIdx + 1]; ++v) 
        {

            // v is relative v_id from 0 to num_verts -1 
            valid_nbrs_count = 0;

            // check vertex v is initialized in active child table
            // all the local verts already initialized at the bottom of subtemplate chain
            if( dt.is_vertex_init_active(v)){

                //adjs is absolute v_id
                int[] adjs_abs = g.adjacent_vertices(v);
                int end = g.out_degree(v);

                //indexed by comb number
                //counts of v at active child
                float[] counts_a = dt.get_active(v);

                ++read_count_loop_ato[threadIdx];

                //loop overall its neighbours
                int nbr_comm_itr = 0;
                for(int i = 0; i < end; ++i)
                {

                    int adj_i = g.get_relative_v_id(adjs_abs[i]);

                    //how to determine whether adj_i is in the current passive table
                    if( adj_i >=0 && dt.is_vertex_init_passive(adj_i)){
                        // if adj_i < 0, it is in other mappers, count them later
                        // adj_i must be initialized in passive template
                        // valid_nbrs stores rel neighbour ids at local mapper
                        valid_nbrs[valid_nbrs_count++] = adj_i;
                    }

                    // preparation for communication 
                    // replace this snippet with more efficient data structure
                    // replace Int2ObjectMap with an array of ArrayList<Integer>
                    if (workerNum > 1 && adj_i < 0)
                        this.update_map[v][nbr_comm_itr++] = adjs_abs[i];
                }

                this.update_map_size[v] = nbr_comm_itr;

                if(valid_nbrs_count != 0){

                    //add counts on local nbs

                    //number of colors for template s
                    //different from num_combinations_ato, which is for active child
                    int num_combinations_verts_sub = choose_table[num_colors][num_verts_sub_ato];

                    // for a specific vertex v initialized on active child
                    // first loop on different color_combs of cur subtemplate
                    for(int n = 0; n < num_combinations_verts_sub; ++n){

                        float color_count = 0.0f;

                        // more details
                        int[] comb_indexes_a = comb_num_indexes[0][s][n];
                        int[] comb_indexes_p = comb_num_indexes[1][s][n];

                        int p = num_combinations_ato -1;

                        // second loop on different color_combs of active/passive children
                        // a+p == num_combinations_ato 
                        // (total colorscombs for both of active child and pasive child)
                        for(int a = 0; a < num_combinations_ato; ++a, --p){

                            float count_a = counts_a[comb_indexes_a[a]];

                            if( count_a > 0){

                                //third loop on different valid nbrs
                                for(int i = 0; i < valid_nbrs_count; ++i){
                                    //validated nbrs already checked to be on passive child
                                    color_count += count_a * dt.get_passive(valid_nbrs[i], comb_indexes_p[p]);
                                    ++read_count_loop_ato[threadIdx];
                                }
                            }
                        }

                        if( color_count > 0.0){
                            cc_ato[threadIdx] += color_count;
                            ++set_count_loop_ato[threadIdx];

                            //also add counts to the current subtemplate s 
                            dt.set(v, comb_num_indexes_set[s][n], color_count);

                            if(do_graphlet_freq || do_vert_output)
                                final_vert_counts[v] += (double)color_count;

                            // if(s != 0)
                            //     dt.set(v, comb_num_indexes_set[s][n], color_count);
                            // else if(do_graphlet_freq || do_vert_output)
                            //     final_vert_counts[v] += (double)color_count;
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
            for(int i = 0; i < this.thread_num; ++i){
                set_count += set_count_loop_ato[i];
                total_count += total_count_loop_ato[i];
                read_count += read_count_loop_ato[i];
                // retval_ato += cc_ato[i];
            }

            set_count_loop_ato = null;
            total_count_loop_ato = null;
            read_count_loop_ato = null;
            // cc_ato = null;

        }

        barrier.await();
        // float retval = 0;
        // elt = System.currentTimeMillis() - elt;
        //System.out.println("time: "+ elt +"ms");
        // return retval_ato;
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

    private void update_comm(int sub_id, int threadIdx, int[] chunks) throws BrokenBarrierException, InterruptedException
    {

        if (threadIdx == 0)
        {
            // cc_ato = new float[this.thread_num];
            // cc_ato_test = new float[this.thread_num];
            // cc_ato_cur_sum = new float[this.thread_num];
            // retval_ato = 0.0f;
            active_child = part.get_active_index(sub_id);
            LOG.info("Active Child: " + active_child + " for sub: " + sub_id);
        }

        //empty the cc_ato vals
        cc_ato[threadIdx] = 0.0f;
        cc_ato_test[threadIdx] = 0.0f;
        cc_ato_cur_sum[threadIdx] = 0.0f;

        barrier.await();
        // float added_count = 0.0f;

        int num_combinations_verts_sub = choose_table[num_colors][num_verts_table[sub_id]];
        int active_index = part.get_active_index(sub_id);
        int num_verts_a = num_verts_table[active_index];

        // colorset combinations from active child
        // combination of colors for active child
        int num_combinations_active_ato = choose_table[num_verts_table[sub_id]][num_verts_a];

        if (threadIdx == 0)
        {
            LOG.info("Debug subtmp: " + sub_id + "; cur vert: " + num_verts_table[sub_id] +
                    "; cur ver comb: " +num_combinations_verts_sub + "; active comb: "
                    +num_combinations_active_ato);
        }

        //debug try to test all counts for active child
        // barrier.await();
        //
        // for (int v = chunks[threadIdx]; v < chunks[threadIdx + 1]; ++v) 
        // {
        //     float[] counts_a_test = dt.get_active(v);
        //     if (counts_a_test != null)
        //     {
        //         for(int x = 0; x< counts_a_test.length; x++)
        //             cc_ato_test[threadIdx] += counts_a_test[x];
        //     }
        // }

        barrier.await();
        // start update 
        // first loop over local v
        // for(int v = 0; v < num_verts_graph; v++ )
        for (int v = chunks[threadIdx]; v < chunks[threadIdx + 1]; ++v) 
        {
            if (dt.is_vertex_init_active(v))
            {
                int adj_list_size = this.update_map_size[v];
                // store the abs adj id for v
                int[] adj_list = this.update_map[v]; 
                float[] counts_a = dt.get_active(v);
                // float[] counts_a = dt.get_table(active_child, v);
                //debug
                // if (counts_a == null)
                // {
                //    error_check_num++;
                //    continue;
                // }

                //second loop over comb_num for cur subtemplate
                for(int n = 0; n< num_combinations_verts_sub; n++)
                {
                    float color_count_local = 0.0f;

                    // more details
                    int[] comb_indexes_a = comb_num_indexes[0][sub_id][n];
                    int[] comb_indexes_p = comb_num_indexes[1][sub_id][n];

                    // for passive 
                    int p = num_combinations_active_ato -1;

                    // third loop over comb_num for active/passive subtemplates
                    for(int a = 0; a < num_combinations_active_ato; ++a, --p)
                    {
                        float count_a = counts_a[comb_indexes_a[a]];
                        if (count_a > 0)
                        {
                            //check
                            // if (sub_id == 8 && count_a > 1.0f)
                                // error_check_num2++;

                            // fourth loop over nbrs
                            for(int i = 0; i< adj_list_size; i++)
                            {
                                int adj_id = adj_list[i]; 
                                int adj_offset = this.abs_v_to_queue[adj_id];
                                int[] adj_offset_list = this.update_queue_pos[this.abs_v_to_mapper[adj_id]];
                                int start_pos = adj_offset_list[adj_offset];
                                int end_pos = adj_offset_list[adj_offset + 1];

                                float[] adj_counts_list = this.update_queue_counts[this.abs_v_to_mapper[adj_id]];

                                
                                //check if nbr on passive child 
                                if (start_pos != end_pos)
                                {

                                    //check 
                                    // if (sub_id == 0 && adj_counts_list[start_pos + comb_indexes_p[p]] > 1.0f)
                                        // LOG.info("Error last passive val: " +adj_counts_list[start_pos + comb_indexes_p[p]]);

                                    color_count_local += (count_a*adj_counts_list[start_pos + comb_indexes_p[p]]);
                                }

                            }

                        }

                    }

                    if (color_count_local > 0.0)
                    {
                        dt.update_comm(v, comb_num_indexes_set[sub_id][n], color_count_local);
                        cc_ato[threadIdx] += color_count_local;
                            // added_count += color_count_local;
                    }

                }

            }

        }

        if (threadIdx == 0)
            LOG.info("Finish updated work for sub: " + sub_id);

        //debug try to test all counts for current table 
        // barrier.await();
        
        // for (int v = chunks[threadIdx]; v < chunks[threadIdx + 1]; ++v) 
        // {
        //     float[] counts_a_test_cur = dt.get_table(sub_id, v);
        //     if (counts_a_test_cur != null)
        //     {
        //         for(int x = 0; x< counts_a_test_cur.length; x++)
        //             cc_ato_cur_sum[threadIdx] += counts_a_test_cur[x];
        //     }
        // }


        barrier.await();
        // return added_count;
    }

    private void print_counts(int sub_id, int threadIdx, int[] chunks) throws BrokenBarrierException, InterruptedException
    {

        // barrier.await();
        cc_ato[threadIdx] = 0.0f;
        
        for (int v = chunks[threadIdx]; v < chunks[threadIdx + 1]; ++v) 
        {
            float[] counts_a_test_cur = dt.get_table(sub_id, v);
            if (counts_a_test_cur != null)
            {
                for(int x = 0; x< counts_a_test_cur.length; x++)
                    cc_ato[threadIdx] += counts_a_test_cur[x];
            }
        }

        // barrier.await();
    }

    private void print_counts_single(int sub_id) 
    {

        // cc_ato[threadIdx] = 0.0f;
        float output_c = 0.0f;

        for (int v = 0; v < g.num_vertices(); ++v) 
        {
            float[] counts_a_test_cur = dt.get_table(sub_id, v);
            if (counts_a_test_cur != null)
            {
                for(int x = 0; x< counts_a_test_cur.length; x++)
                    output_c += counts_a_test_cur[x];
                    // cc_ato[threadIdx] += counts_a_test_cur[x];
            }
        }

        LOG.info("Output c for sub: " + sub_id + " is " + output_c);
    }
}
