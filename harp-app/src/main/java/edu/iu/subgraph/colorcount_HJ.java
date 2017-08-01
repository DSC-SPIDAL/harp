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

    //for comm send/recv vertex abs id
    private Set<Integer>[] comm_mapper_vertex;
    private Table<IntArray> recv_vertex_table;
    private Table<IntArray> send_vertex_table;
    private Table<IntArray> comm_vertex_table;

    private Table<SCSet> comm_data_table;

    private SCCollectiveMapper mapper;

    private Int2ObjectOpenHashMap<ArrayList<Integer>> update_map;
    private final ReentrantLock lock = new ReentrantLock();
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

        update_map = new Int2ObjectOpenHashMap<>();

        //initialize dynamic table
        dt = new dynamic_table_array();
        barrier = new CyclicBarrier(Constants.THREAD_NUM);

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

        //loop over all the adj of graph
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

                        //debug
                        float local_count_de = 0.0f;
                        float comm_count_de = 0.0f;

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
                            local_count_de = colorful_count_HJ(s, threadIdx, chunks);

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

                        

                        //start communication part single thread 
                        barrier.await();

                        if (threadIdx == 0)
                        {
                            if (num_verts_sub_ato > 1 && mapper.getNumWorkers() > 1)
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
                                    int[] comm_vert_list = send_vertex_table.getPartition(send_id).get().get();
                                    //for each vert find the colorsets and counts in methods of dynamic_table_array
                                    int num_combinations_verts_compress = choose_table[num_colors][num_verts_table[part.get_passive_index(s)]];
                                    SCSet comm_data = dt.compress_send_data(comm_vert_list, num_combinations_verts_compress, g);
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
                                long update_start = System.currentTimeMillis();

                                for(int comm_id : comm_data_table.getPartitionIDs())
                                {
                                    int update_id = ( comm_id & ( (1 << 16) -1 ) );
                                    LOG.info("Worker ID: " + localID + "; Recv from worker id: " + update_id);
                                    int[] update_vert_list = recv_vertex_table.getPartition(update_id).get().get();
                                    int num_combinations_verts_sub = choose_table[num_colors][num_verts_table[s]];
                                    LOG.info("update list len: " + update_vert_list.length );

                                    comm_count_de += update_comm_data(update_vert_list, num_combinations_verts_sub, s, comm_data_table.getPartition(comm_id).get());
                                }

                                LOG.info("Finish update comm counts for subtemplate: " 
                                        + s + "; time used: " + (System.currentTimeMillis() - update_start));

                                dt.clear_comm_counts();
                                update_map.clear();

                                comm_data_table = null;

                            }

                        }

                        barrier.await();

                        //print out the total counts for subtemplate s from all mappers
                        if (num_verts_sub_ato > 1 && threadIdx == 0)
                        {
                            //allreduce to get total counts for subtemplae s
                            if ( mapper.getNumWorkers() > 1  )
                            {
                                //allreduce
                                Table<DoubleArray> sub_count_table = new Table<>(0, new DoubleArrPlus());
                                DoubleArray sub_count_array = DoubleArray.create(2, false);

                                sub_count_array.get()[0] = local_count_de ;
                                sub_count_array.get()[1] = comm_count_de ;

                                sub_count_table.addPartition(new Partition<>(0, sub_count_array));

                                mapper.allreduce("sc", "get-sub-count", sub_count_table);
                                double sub_count_local = sub_count_table.getPartition(0).get().get()[0];
                                double sub_count_remote = sub_count_table.getPartition(0).get().get()[1];

                                // LOG.info("Total count for subtemplate: " + s + " is: " + sub_count);
                                LOG.info("For subtemplate: " + s + " local count is: " +sub_count_local + "; remote count is: " + sub_count_remote
                                            + "; total count is: " + (sub_count_local + sub_count_remote) );

                            }
                            else
                            {
                                LOG.info("For subtemplate: " + s + " local count is: " +local_count_de + "; remote count is: " + comm_count_de
                                            + "; total count is: " + (local_count_de + comm_count_de) );
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

                        num_verts_sub_ato = num_verts_table[0];
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



                    //comm and add the communicated counts to full_count_ato
                    barrier.await();
                    if (threadIdx == 0)
                    {
                        if (num_verts_table[0] > 1 && mapper.getNumWorkers() > 1)
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
                                int[] comm_vert_list = send_vertex_table.getPartition(send_id).get().get();
                                //for each vert find the colorsets and counts in methods of dynamic_table_array
                                int num_combinations_verts_compress = choose_table[num_colors][num_verts_table[part.get_passive_index(0)]];
                                SCSet comm_data = dt.compress_send_data(comm_vert_list, num_combinations_verts_compress, g);
                                comm_data_table.addPartition(new Partition<>(comm_id, comm_data));
                            }

                            LOG.info("Finish prepare regroup comm for last subtemplate " + 
                                    "; time used: " + (System.currentTimeMillis() - reg_prep_start));
                            //
                            LOG.info("Start regroup comm for last subtemplate");
                            long reg_start = System.currentTimeMillis();
                            //start the regroup communication
                            mapper.regroup("sc", "regroup counts data", comm_data_table, new SCPartitioner(mapper.getNumWorkers()));
                            LOG.info("Finish regroup comm for last subtemplate" 
                                     + "; time used: " + (System.currentTimeMillis() - reg_start));

                            //start append comm counts to local dt data structure
                            LOG.info("Start update comm counts for last subtemplate");
                            long update_start = System.currentTimeMillis();

                            for(int comm_id : comm_data_table.getPartitionIDs())
                            {
                                int update_id = ( comm_id & ( (1 << 16) -1 ) );
                                LOG.info("Worker ID: " + localID + "; Recv from worker id: " + update_id);
                                int[] update_vert_list = recv_vertex_table.getPartition(update_id).get().get();
                                int num_combinations_verts_sub = choose_table[num_colors][num_verts_table[0]];
                                LOG.info("update list len: " + update_vert_list.length );

                                float added_counts = update_comm_data_full(update_vert_list, num_combinations_verts_sub, 0, comm_data_table.getPartition(comm_id).get());
                                full_count_ato += added_counts;
                            }

                            LOG.info("Finish update comm counts for last subtemplate" 
                                    + "; time used: " + (System.currentTimeMillis() - update_start));

                            dt.clear_comm_counts();
                            update_map.clear();
                            // dt.free_comm_counts();
                        }

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

            //table v must be initialized as active
            if( dt.is_vertex_init_active(v)){

                //adjs is absolute v_id
                int[] adjs_abs = g.adjacent_vertices(v);
                int end = g.out_degree(v);
                float[] counts_a = dt.get_active(v);

                ++read_count_loop_ato[threadIdx];

                //loop overall its neighbours
                for(int i = 0; i < end; ++i){
                    int adj_i = g.get_relative_v_id(adjs_abs[i]);

                    //how to determine whether adj_i is in the current passive table
                    if( adj_i >=0 && dt.is_vertex_init_passive(adj_i)){
                        valid_nbrs[valid_nbrs_count++] = adj_i;
                    }

                    if (adj_i < 0)
                    {
                        lock.lock();
                        //put v to the request list of abjs_abs[i]
                        //update_map is indexed by abs id of adj v
                        //contains array of rel id of local v
                        ArrayList<Integer> update_adj_arr = update_map.get(adjs_abs[i]);
                        if (update_adj_arr == null)
                        {
                            update_adj_arr = new ArrayList<>();
                            update_adj_arr.add(new Integer(v));
                            update_map.put(adjs_abs[i], update_adj_arr);
                        }
                        else
                        {
                            update_adj_arr.add(new Integer(v));
                        }

                        lock.unlock();
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

                            //also add counts to the last subtemplaes
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

    private float update_comm_data(int[] vert_list, int num_comb_max, int sub_id,  SCSet comm_set)
    {//{{{

        float added_count = 0.0f;

        int v_num = vert_list.length;
        if (comm_set.get_v_num() != v_num )
        {
            LOG.info("update vertex number not matched !!!");
            return added_count;
        }
        else
        {
            LOG.info("Debug: remote v_num: " + v_num);
        }

        int[] update_offset_array = comm_set.get_v_offset();
        int[] update_idx_array = comm_set.get_counts_idx();
        float[] update_counts_array = comm_set.get_counts_data();

        for(int p=0;p<v_num;p++)
        {
            //abs v id
            int update_abs_id = vert_list[p];

            int start_pos = update_offset_array[p];
            int end_pos = update_offset_array[p+1];

            if (end_pos != start_pos)
            {

                for(int q = 0; q< num_comb_max; q++)
                {

                    int[] comb_indexes_a = comb_num_indexes[0][sub_id][q];
                    int[] comb_indexes_p = comb_num_indexes[1][sub_id][q];

                    //num_combinations_ato is the same as that in colorful_count_HJ
                    int s = num_combinations_ato -1;

                    for(int t = 0; t < num_combinations_ato; ++t, --s){

                        //get the active vertex list
                        ArrayList<Integer> update_v_arr = update_map.get(update_abs_id);

                        if (update_v_arr != null)
                        {
                            for(int u = 0; u<update_v_arr.size(); u++)
                            {
                                int update_rel_id = update_v_arr.get(u).intValue(); 
                                float[] counts_active = dt.get_active(update_rel_id);
                        
                                float count_a = counts_active[comb_indexes_a[t]];
                                if( count_a > 0)
                                {
                                    //update count val
                                    float update_count = count_a*update_counts_array[start_pos + comb_indexes_p[s]];
                                    dt.update_comm(update_rel_id, comb_num_indexes_set[sub_id][q], update_count);
                                    added_count += update_count;
                                }

                            }

                        }
                        
                    }
                }
                
            }

        }

        return added_count;
    }//}}}
    private float update_comm_data_full(int[] vert_list, int num_comb_max, int sub_id,  SCSet comm_set)
    {//{{{

        float added_count = 0.0f;

        int v_num = vert_list.length;
        if (comm_set.get_v_num() != v_num )
        {
            LOG.info("update vertex number not matched !!!");
            return added_count;
        }
        else
        {
            LOG.info("Debug: remote v_num: " + v_num);
        }

        int[] update_offset_array = comm_set.get_v_offset();
        int[] update_idx_array = comm_set.get_counts_idx();
        float[] update_counts_array = comm_set.get_counts_data();

        for(int p=0;p<v_num;p++)
        {
            //abs v id
            int update_abs_id = vert_list[p];

            int start_pos = update_offset_array[p];
            int end_pos = update_offset_array[p+1];

            if (end_pos != start_pos)
            {

                for(int q = 0; q< num_comb_max; q++)
                {

                    int[] comb_indexes_a = comb_num_indexes[0][sub_id][q];
                    int[] comb_indexes_p = comb_num_indexes[1][sub_id][q];

                    //num_combinations_ato is the same as that in colorful_count_HJ
                    int s = num_combinations_ato -1;

                    for(int t = 0; t < num_combinations_ato; ++t, --s){

                        //get the active vertex list
                        ArrayList<Integer> update_v_arr = update_map.get(update_abs_id);

                        if (update_v_arr != null)
                        {
                            for(int u = 0; u<update_v_arr.size(); u++)
                            {
                                int update_rel_id = update_v_arr.get(u).intValue(); 
                                float[] counts_active = dt.get_active(update_rel_id);
                                //
                                float count_a = counts_active[comb_indexes_a[t]];
                                if( count_a > 0)
                                {
                                    //update count val
                                    // if ((start_pos + comb_indexes_p[s]) < update_counts_array.length)
                                        added_count += count_a*update_counts_array[start_pos + comb_indexes_p[s]];
                                    // dt.update_comm(update_rel_id, comb_num_indexes_set[sub_id][q], update_count);
                                }

                            }

                        }

                    }
                }
                
            }

        }

        return added_count;
    }//}}}

}
