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

// for hadoop assert
import static org.junit.Assert.*; 

/**
 * @brief A colorcount method that implements 
 * a BSP-LRT style 
 */
public class colorcount_HJ {


    
    
    // ----------------------------- for graph  -----------------------------

    //local graph 
    Graph g;
    // vertice number of local graph
    int num_verts_graph;
    //color val for each vertex on local graph
    int[] colors_g;
    // max vertice abs id of global graph
    private int max_abs_id;
    // ----------------------------- for templates  -----------------------------
    //global template
    Graph t;
    //equals to subtemplate size
    int num_colors;
    
    //partitioning a template into a chain of sub templates
    partitioner part;
    //partitioned subtemplates
    Graph[] subtemplates;
    // number of subtempaltes
    int subtemplate_count;
    // vertices num of each subtemplate 
    int[] num_verts_table;
    // vertice num of current subtemplate
    private int num_verts_sub_ato;
    // num of color comb num for current subtemplate
    private int num_combinations_ato;

    // ----------------------------- for dynamic programming  -----------------------------
    //store counts in dynamic programming table
    dynamic_table_array dt;

    // current active and passive template id for debug use
    private int active_child;
    private int passive_child;

    
    // ----------------------------- for combination colorset index  -----------------------------

    //stores the combination of color sets
    int[][] choose_table;
    // temp index sets to construct comb_num_indexes 
    int[][][][] index_sets;
    // temp index sets to construct comb_num_indexes 
    int[][][][][] color_sets;
    // record comb num values for active and passive children 
    int[][][][] comb_num_indexes;
    // record comb num values for each subtemplate and each color combination
    int[][] comb_num_indexes_set;

    // ----------------------------- for multi-threading counting -----------------------------

    //thread barrier
    // private AtomicInteger barrierCounter = new AtomicInteger(0);
    private CyclicBarrier barrier;
    // for mutual locking
    private ReentrantLock lock; 

    private int thread_num = 24;
    private int core_num = 24;
    private String affinity = "compact";

    private int[] chunks;
    private float[] cc_ato; 
    //store the local counts for last template
    private float[] count_local_root; 
    //store the local counts for last template
    private float[] count_comm_root; 

    // total counts on local graph
    private float full_count_ato = 0;
    // cumulated total counts from all iterations
    private double cumulate_count_ato = 0;

    // ----------------------------- for communication  -----------------------------
    //harp communicator
    private SCCollectiveMapper mapper;

    private int mapper_num = 1;
    private int local_mapper_id;
    // adding adj_abs_v to each mapper
    private Set<Integer>[] comm_mapper_vertex;

    // private Table<IntArray> recv_vertex_table;
    // harp table that stores vertex sent to other mappers
    private Table<IntArray> send_vertex_table;
    // harp table that regroups send/recv abs_v_id 
    private Table<IntArray> comm_vertex_table;
    // harp table that regroups send/recv color counts  
    private Table<SCSet> comm_data_table;

    //comm adj_abs_id for each local rel_v_id 
    private int[][] update_map;
    // the size of comm adj_abs_id for each local rel_v_id 
    private int[] update_map_size; 
    // mapping an arbitary abs_v_id to its belonged mapper id
    private int[] abs_v_to_mapper;
    // mapping an arbitary abs_v_id to the relative position in updating queues 
    private int[] abs_v_to_queue;
    // start/end counts pos of each adj_v_id counts on each mapper
    private int[][] update_queue_pos;
    // adj_v_id counts on each mapper
    private float[][] update_queue_counts;

    // ---------------------------- for label and other func ----------------------------
    int[] labels_g;
    int[] labels_t;
    boolean labeled;
    double[] final_vert_counts;
    boolean do_graphlet_freq = false;
    boolean do_vert_output = false;

    // -------------------------------- Misc --------------------------------
    
    // record computation time 
    private long start_comp = 0;
    // avg computation time of all iterations
    private long time_comp = 0;
    // record communication time, including time to prepare comm queues 
    private long start_comm = 0;
    // avg communication time of all iterations
    private long time_comm = 0;
    private long start_misc = 0;

    //number of iteration
    int num_iter;
    int cur_iter;

    final Logger LOG = Logger.getLogger(colorcount_HJ.class);
    boolean verbose = false;
    
    /**
     * @brief initialize local graph 
     *
     * @param local_graph 
     * @param global_max_v_id
     * @param thread_num
     * @param core_num
     * @param affinity
     * @param calc_auto
     * @param do_gdd
     * @param do_vert
     * @param verb
     */
    void init(Graph local_graph, int global_max_v_id, int thread_num, int core_num, String affinity, boolean do_gdd, boolean do_vert, boolean verb){

        // assign params
        this.g = local_graph;
        this.max_abs_id = global_max_v_id;
        this.thread_num = thread_num;
        this.core_num = core_num;
        this.affinity = affinity;
        this.do_graphlet_freq = do_gdd;
        this.do_vert_output = do_vert;
        this.verbose = verb;

        // init members 
        this.labels_g = this.g.labels;
        this.labeled = this.g.labeled;
        this.num_verts_graph = this.g.num_vertices();
        this.colors_g = new int[this.num_verts_graph];

        this.cc_ato = new float[this.thread_num];
        this.count_local_root = new float[this.thread_num];
        this.count_comm_root = new float[this.thread_num];

        this.dt = new dynamic_table_array();
        this.barrier = new CyclicBarrier(this.thread_num);

        if( do_graphlet_freq || do_vert_output){
            //ToDo for graphlet freq and vert output
        }

    }

    /**
     * @brief init members related to communications  
     *
     * @param mapper_id_vertex
     * @param mapper
     */
    void init_comm(int[] mapper_id_vertex, SCCollectiveMapper mapper) {

        // assign params
        this.mapper = mapper;
        //create abs mapping structure
        this.abs_v_to_mapper = mapper_id_vertex;

        // init members
        this.mapper_num = this.mapper.getNumWorkers();
        this.local_mapper_id = this.mapper.getSelfID();
        this.abs_v_to_queue = new int[this.max_abs_id + 1];
               
        // init comm mappers
        this.comm_mapper_vertex = new HashSet[this.mapper_num];
        for(int i=0; i<this.mapper_num; i++)
            this.comm_mapper_vertex[i] = new HashSet<>(); 

        //loop over all the adj of local graph verts
        //adj_array stores abs id 
        int[] adj_array = this.g.adjacencies();
        for(int i=0;i<adj_array.length;i++)
        {
            int adj_abs_id = adj_array[i];
            int mapper_id = this.abs_v_to_mapper[adj_abs_id];
            this.comm_mapper_vertex[mapper_id].add(new Integer(adj_abs_id));
        }

        this.update_map = new int[this.num_verts_graph][];
        for(int i=0;i<this.num_verts_graph;i++)
            this.update_map[i] = new int[this.g.out_degree(i)];

        this.update_map_size = new int[this.num_verts_graph];
        this.update_queue_pos = new int[this.mapper_num][];
        this.update_queue_counts = new float[this.mapper_num][];

        //convert set to arraylist
        this.comm_vertex_table = new Table<>(0, new IntArrPlus());
        this.send_vertex_table = new Table<>(0, new IntArrPlus());


        // prepareing send/recv information
        // the requested adj_abs_v queues will be sent to each mapper
        // ToDo: replace this by multi threading
        for(int i=0;i<this.mapper_num;i++)
        {
            // i is the mapper id from which local_mapper demands adj data
            if ( (i != this.local_mapper_id)  &&  this.comm_mapper_vertex[i].size() > 0 )
            {

                //create partition id, the upper 16 bits stores requested id
                //the lower 16 bits stores sender id (local id)
                int comm_id = ( (i << 16) | this.local_mapper_id );

                // retrieve communicated adj_abs_id
                ArrayList<Integer> temp_array = new ArrayList<>(comm_mapper_vertex[i]);
                int[] temp_array_primitive = ArrayUtils.toPrimitive(temp_array.toArray(new Integer[temp_array.size()]));

                //create mapping from adj_abs_id to relative t in update offset queue
                for(int j = 0; j< temp_array_primitive.length; j++)
                    this.abs_v_to_queue[temp_array_primitive[j]] = j;

                IntArray comm_array = new IntArray(temp_array_primitive, 0, temp_array_primitive.length);
                //table to communicate with other mappers
                this.comm_vertex_table.addPartition(new Partition<>(comm_id, comm_array));
            }

        }

        if (this.verbose)
            LOG.info("Start regroup comm_vertex_table");

        this.mapper.regroup("sc", "regroup-send-recv-vertex", this.comm_vertex_table, new SCPartitioner(this.mapper_num));

        if (this.verbose)
            LOG.info("Finish regroup comm_vertex_table");

        // pack the received requested adj_v_id information into send queues
        for(int comm_id : this.comm_vertex_table.getPartitionIDs())
        {
            int dst_mapper_id = comm_id & ( (1 << 16) -1 );
            // this shall equal to local_mapper_id
            int src_mapper_id = comm_id >>> 16;
            //create send table
            send_vertex_table.addPartition(new Partition(dst_mapper_id, comm_vertex_table.getPartition(comm_id).get()));

            if (this.verbose)
            {
                //check src id add assertion
                assertEquals("comm_vertex_table sender not matched ", this.local_mapper_id, src_mapper_id);
                LOG.info("Send from mapper: " + src_mapper_id + " to mapper: " + dst_mapper_id + "; partition size: " +
                        comm_vertex_table.getPartition(comm_id).get().get().length);
            }
            
        }

        //release memory
        for(int i=0; i<this.mapper_num; i++)
            this.comm_mapper_vertex[i] = null; 

        this.comm_mapper_vertex = null;
    }

    /**
     * @brief compute color counting in N iterations 
     *
     * @param template
     * @param N
     *
     * @return 
     */
    public double do_full_count(Graph template, int N){

        this.t = template;
        this.num_iter = N;
        this.labels_t = t.labels;

        // --------------------------- creating subtemplates and comb number index system --------------------------- 

        if(this.verbose){
            LOG.info("Begining partition...");
        }

        //partition the template into subtemplates 
        this.part = new partitioner(this.t, this.labeled, this.labels_t);
        this.part.sort_subtemplates();

        if(this.verbose){
            LOG.info("done partitioning");
        }

        //colors equals the num of vertices
        this.num_colors = this.t.num_vertices();
        //get array of subtemplates
        this.subtemplates = this.part.get_subtemplates();
        //subtemplates num
        this.subtemplate_count = this.part.get_subtemplate_count();

        //obtain the hash values table for each subtemplate and a combination of color sets
        create_tables();

        //initialize dynamic prog table, with subtemplate-vertices-color array
        this.dt.init(this.subtemplates, this.subtemplate_count, this.num_verts_graph, this.num_colors, this.max_abs_id);

        //vertice num of the full graph, huge
        this.chunks = divide_chunks(this.num_verts_graph, this.thread_num);   

        //triggering vtune profiling add command option for vtune 
        // java.nio.file.Path vtune_file = java.nio.file.Paths.get("vtune-flag.txt");
        // String flag_trigger = "Start training process and trigger vtune profiling.";
        // try{
        //     java.nio.file.Files.write(vtune_file, flag_trigger.getBytes());
        // }catch (IOException e)
        // {
        //    LOG.info("Failed to create vtune trigger flag");
        // }

        if(this.verbose){
            LOG.info("Starting Multi-threading Counting Iterations");
        }

        launchHabaneroApp( () -> forallChunked(0, this.thread_num-1, (threadIdx) -> {

        //set Java threads affinity
        BitSet bitSet = new BitSet(this.core_num);
        int thread_mask = 0;

        if (this.verbose && threadIdx == 0)
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

            // start the main loop of iterations
            for(int cur_itr = 0; cur_itr < this.num_iter; cur_itr++)
            {
                if (threadIdx == 0)
                    this.cur_iter = cur_itr;

                //start sampling colors
                this.barrier.await();

                if(verbose && threadIdx == 0){
                    LOG.info("Start Sampling Graph for Itr: " + cur_itr);
                    // this.start_comp = System.currentTimeMillis();
                    this.start_misc = System.currentTimeMillis();
                }

                Random rand = new Random(System.currentTimeMillis());
                //sampling the vertices of full graph g
                for (int i = this.chunks[threadIdx]; i < this.chunks[threadIdx+1]; ++i){
                    this.colors_g[i] = rand.nextInt(this.num_colors) ;
                }

                this.barrier.await();
                if(this.verbose && threadIdx == 0){
                    LOG.info("Finish Sampling Graph for Itr: " + cur_itr + "; use time: " + (System.currentTimeMillis() - this.start_misc) + "ms");
                    // this.time_comp += (System.currentTimeMillis() - this.start_comp);
                }

                // start doing counting
                for( int s = this.subtemplate_count -1; s > 0; --s)
                {

                    if (threadIdx == 0)
                    {
                        //get num_vert of subtemplate s
                        this.num_verts_sub_ato = this.num_verts_table[s];

                        if(this.verbose)
                            LOG.info("Initing Subtemplate "+ s + ", t verts: " + num_verts_sub_ato);

                        int a = this.part.get_active_index(s);
                        int p = this.part.get_passive_index(s);

                        if(this.verbose)
                            LOG.info("Subtemplate: " + s + "; active_idx: " + a + "; passive_idx: " + p);

                        this.dt.init_sub(s, a, p);
                    }

                    this.barrier.await();

                    if(this.verbose && threadIdx == 0)
                    {
                        LOG.info("Start Counting Local Graph Subtemplate "+ s);
                        // this.start_comp = System.currentTimeMillis();
                    }

                    //hit the bottom of subtemplate chain, dangling template node
                    if( this.num_verts_sub_ato == 1){

                        if ( s == this.subtemplate_count - 1)
                        {
                            init_table_node_HJ(s, threadIdx, this.chunks);
                        }
                        else
                        {
                            if (threadIdx == 0)
                                dt.set_to_table(this.subtemplate_count - 1, s);
                        }

                    }else{
                        colorful_count_HJ(s, threadIdx, this.chunks);
                    }

                    this.barrier.await();

                    if(this.verbose && threadIdx == 0)
                    {
                        LOG.info("Finish Counting Local Graph Subtemplate "+ s);
                        // this.time_comp += (System.currentTimeMillis() - this.start_comp);
                    }

                    //start communication part single thread 
                    // only for subtemplates size > 1, having neighbours on other mappers
                    // only if more than one mapper, otherwise all g verts are local
                    if (this.mapper_num > 1 && this.num_verts_sub_ato > 1)
                    {
                        //single thread communication
                        if (threadIdx == 0)
                            regroup_comm_single(s);

                        this.barrier.await();

                        // multi-threaded updates comm data
                        update_comm(s, threadIdx, this.chunks);

                        this.barrier.await();

                        if (threadIdx == 0)
                        {
                            // clean up memory
                            // this.dt.clear_comm_counts();
                            for(int k = 0; k< this.mapper_num;k++)
                            {
                                this.update_queue_pos[k] = null;
                                this.update_queue_counts[k] = null;
                            }

                            this.comm_data_table = null;
                        }

                    }

                    // printout results for sub s
                    this.barrier.await();

                    if (threadIdx == 0)
                    {
                        int a = this.part.get_active_index(s);
                        int p = this.part.get_passive_index(s);

                        //release the template with size > 1
                        if( a != SCConstants.NULL_VAL)
                        {
                            // do not free the dangling template node
                            if (this.num_verts_table[a] > 1)
                                this.dt.clear_sub(a);
                        }
                        if(p != SCConstants.NULL_VAL)
                        {
                            if (this.num_verts_table[p] > 1)
                                this.dt.clear_sub(p);
                        }
                    }

                    this.barrier.await();
                }

                if(verbose && threadIdx == 0)
                    LOG.info("Done with initialization. Doing full count");

                // do the count for the full template
                if (threadIdx == 0)
                {
                    this.num_verts_sub_ato = this.num_verts_table[0];
                    int a = this.part.get_active_index(0);
                    int p = this.part.get_passive_index(0);

                    if (this.verbose)
                        LOG.info("Subtemplate 0 ; active_idx: " + a + "; passive_idx: " + p);

                    dt.init_sub(0, a, p);

                }

                this.barrier.await();
                colorful_count_HJ(0, threadIdx, chunks);
                this.barrier.await();

                //comm and add the communicated counts to full_count_ato
                // only for subtemplates size > 1, having neighbours on other mappers
                // only if more than one mapper, otherwise all g verts are local
                if (this.num_verts_sub_ato > 1 && this.mapper_num > 1)
                {

                    //single thread communication
                    if (threadIdx == 0)
                        regroup_comm_single(0);

                    this.barrier.await();
                    update_comm(0, threadIdx, this.chunks);
                    this.barrier.await();

                    if (threadIdx == 0)
                    {
                        // clean up memory
                        // this.dt.clear_comm_counts();
                        for(int k = 0; k< this.mapper_num ;k++)
                        {
                            this.update_queue_pos[k] = null;
                            this.update_queue_counts[k] = null;
                        }

                        this.comm_data_table = null;
                    }
                }

                this.barrier.await();
                // printout results for last sub 
                if (threadIdx == 0 )
                {
                    float sum_count = 0.0f;
                    for(int k = 0; k< this.thread_num; k++)
                    {
                        sum_count += this.count_local_root[k];
                        sum_count += this.count_comm_root[k];
                    }

                    this.full_count_ato = sum_count;

                    LOG.info("Finish update comm counts for last subtemplate: " 
                            +  "; total counts: " + sum_count);
                }

                this.barrier.await();

                if (threadIdx == 0)
                {
                    int a = this.part.get_active_index(0);
                    int p = this.part.get_passive_index(0);

                    if (a != SCConstants.NULL_VAL)
                        this.dt.clear_sub(a);
                    if (p != SCConstants.NULL_VAL)
                        this.dt.clear_sub(p);

                    //free the first dangling template node
                    this.dt.clear_sub(subtemplate_count - 1);
                }

                //add counts from every iteration
                if (threadIdx == 0)
                {
                    this.cumulate_count_ato += this.full_count_ato;
                }

            }


        } catch (InterruptedException | BrokenBarrierException e) {
            LOG.info("Catch barrier exception in itr: " + this.cur_iter);
            e.printStackTrace();
        }

        }));


        //----------------------- end of color_counting -----------------

        double final_count = cumulate_count_ato / (double) this.num_iter;

        //free memory
        this.send_vertex_table = null;
        this.comm_vertex_table = null;
        this.update_map = null;
        this.colors_g = null;

        delete_tables();
        this.part.clear_temparrays();

        return final_count;

    }

    /**
     * @brief divide local graph vertices into multi-threading chunks
     *
     * @param total
     * @param partition
     *
     * @return 
     */
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
            num_combinations_ato = this.choose_table[num_verts_sub_ato][num_verts_a];

        }

        count_local_root[threadIdx] = 0.0f;

        barrier.await();

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


                //loop overall its neighbours
                int nbr_comm_itr = 0;
                for(int i = 0; i < end; ++i)
                {

                    int adj_i = g.get_relative_v_id(adjs_abs[i]);

                    //how to determine whether adj_i is in the current passive table
                    if( adj_i >=0 && dt.is_vertex_init_passive(adj_i)){
                        valid_nbrs[valid_nbrs_count++] = adj_i;
                    }

                    // preparation for communication 
                    // replace this snippet with more efficient data structure
                    // replace Int2ObjectMap with an array of ArrayList<Integer>
                    if (this.mapper_num > 1 && adj_i < 0)
                        this.update_map[v][nbr_comm_itr++] = adjs_abs[i];
                }

                if (this.mapper_num > 1)
                    this.update_map_size[v] = nbr_comm_itr;

                if(valid_nbrs_count != 0){

                    //add counts on local nbs
                    //number of colors for template s
                    //different from num_combinations_ato, which is for active child
                    int num_combinations_verts_sub = this.choose_table[num_colors][num_verts_sub_ato];

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
                                }
                            }
                        }

                        if( color_count > 0.0){
                        
                            if(do_graphlet_freq || do_vert_output)
                                final_vert_counts[v] += (double)color_count;

                            if(s != 0)
                                dt.set(v, comb_num_indexes_set[s][n], color_count);
                            else
                            {
                                //last template store counts into count_local_root
                                count_local_root[threadIdx] += color_count;
                            }
                        }

                    }
                }
            }


        }

        valid_nbrs = null;

        barrier.await();

        if (threadIdx == 0)
        {

            //reduction
            for(int i = 0; i < this.thread_num; ++i){
            }


        }

        barrier.await();
    }

    /**
     * @brief obtain the hash values table for each subtemplate and a combination of color sets
     *
     * @return 
     */
    private void create_tables(){
        //C_n^k compute the unique number of color set choices 
        //two dimensional
        this.choose_table = Util.init_choose_table(this.num_colors);
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
            this.choose_table[i] = null;
        this.choose_table = null;

        delete_comb_num_system_indexes();
        num_verts_table = null;
    }

    /**
     * @brief record vertices number of each subtemplate
     *
     * @return 
     */
    private void create_num_verts_table(){
        this.num_verts_table = new int[this.subtemplate_count];
        for(int s = 0; s < this.subtemplate_count; ++s){
            this.num_verts_table[s] = this.subtemplates[s].num_vertices();
        }
    }

    /**
     * @brief create color index for each combination of different set size
     *
     * @return 
     */
    private void create_all_index_sets(){

        //first dim (up to) how many colors
        this.index_sets = new int[this.num_colors][][][];

        for(int i = 0; i < (this.num_colors -1 ); ++i){

            int num_vals = i + 2;

            this.index_sets[i] = new int[num_vals -1][][];

            // second dim, for up to num_vals colors, has different set sizes
            for(int j = 0; j < (num_vals - 1); ++j){

                int set_size = j + 1;

                int num_combinations = Util.choose(num_vals, set_size);
                // third dim, for a set size, how many combinations from a given num of colors
                this.index_sets[i][j] = new int[num_combinations][];

                //set start from 1 to set_size
                //init set in increase order
                int[] set = Util.init_permutation(set_size);

                for(int k = 0; k < num_combinations; ++k){

                    // fourth dim, for a combination, having a set_size of color values
                    this.index_sets[i][j][k] = new int[set_size];

                    for(int p = 0; p < set_size; ++p){
                        this.index_sets[i][j][k][p] = set[p] - 1;
                    }

                    // permutate the color set
                    Util.next_set(set, set_size, num_vals);
                }
                set = null;
            }
        }
    }


    /**
     * @brief create color sets for all subtemplates
     *
     * @return 
     */
    private void create_all_color_sets(){

        //first dim, num of subtemplates
        this.color_sets = new int[this.subtemplate_count][][][][];

        for(int s = 0; s < this.subtemplate_count; ++s){

            int num_verts_sub = this.subtemplates[s].num_vertices();

            if( num_verts_sub > 1){

                //determine how many sets in a subtemplate
                //choose num vertices of subtemplate from colors
                int num_sets = Util.choose(this.num_colors, num_verts_sub);

                //second dim, num of sets in a subtemplate 
                this.color_sets[s] = new int[num_sets][][][];

                //init permutation in colorset
                int[] colorset = Util.init_permutation(num_verts_sub);

                for(int n = 0; n < num_sets; ++n){

                    int num_child_combs = num_verts_sub - 1;
                    //third dim, for a subtemplate, a set, how many child combinations
                    this.color_sets[s][n] = new int[num_child_combs][][];

                    for(int c = 0; c < num_child_combs; ++c){
                        int num_verts_1 = c + 1;
                        int num_verts_2 = num_verts_sub - num_verts_1;

                        int[][] index_set_1 = this.index_sets[num_verts_sub-2][num_verts_1-1];
                        int[][] index_set_2 = this.index_sets[num_verts_sub-2][num_verts_2-1];

                        int num_child_sets = Util.choose(num_verts_sub, c+1);
                        this.color_sets[s][n][c] = new int[num_child_sets][];

                        for(int i = 0; i < num_child_sets; ++i){

                            this.color_sets[s][n][c][i] = new int[num_verts_sub];

                            for(int j = 0; j < num_verts_1; ++j)
                                this.color_sets[s][n][c][i][j] = colorset[index_set_1[i][j]];

                            for(int j = 0; j < num_verts_2; ++j)
                                this.color_sets[s][n][c][i][j+num_verts_1] = colorset[index_set_2[i][j]];
                        }
                    }
                    Util.next_set(colorset, num_verts_sub, this.num_colors);
                }

                colorset = null;
            }
        }
    }


    /**
     * @brief convert colorset combination into numeric values (hash function)
     *
     * @return 
     */
    private void create_comb_num_system_indexes(){

        this.comb_num_indexes = new int[2][this.subtemplate_count][][];
        this.comb_num_indexes_set = new int[this.subtemplate_count][];

        // each subtemplate
        for(int s = 0; s < this.subtemplate_count; ++s){

            int num_verts_sub = this.subtemplates[s].num_vertices();
            int num_combinations_s = Util.choose(this.num_colors, num_verts_sub);

            if( num_verts_sub > 1){
                //for active and passive children  
                this.comb_num_indexes[0][s] = new int[num_combinations_s][];
                this.comb_num_indexes[1][s] = new int[num_combinations_s][];
            }

            this.comb_num_indexes_set[s] = new int[num_combinations_s];

            int[] colorset_set = Util.init_permutation(num_verts_sub);

            //loop over each combination instance
            for(int n = 0; n < num_combinations_s; ++n){

                //get the hash value for a colorset instance
                //Util.get_color_index the impl of hash function
                this.comb_num_indexes_set[s][n] = Util.get_color_index(colorset_set, num_verts_sub);

                if( num_verts_sub > 1){

                    int num_verts_a = this.part.get_num_verts_active(s);
                    int num_verts_p = this.part.get_num_verts_passive(s);

                    int[] colors_a;
                    int[] colors_p;
                    int[][] colorsets = this.color_sets[s][n][num_verts_a-1];

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

                        this.comb_num_indexes[0][s][n][a] = color_index_a;
                        this.comb_num_indexes[1][s][n][p] = color_index_p;
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


    /**
     * @brief free up memory space 
     *
     * @return 
     */
    private void delete_all_color_sets(){

        for(int s = 0; s < this.subtemplate_count; ++s){
            int num_verts_sub = this.subtemplates[s].num_vertices();
            if( num_verts_sub > 1) {
                int num_sets = Util.choose(this.num_colors, num_verts_sub);

                for (int n = 0; n < num_sets; ++n) {
                    int num_child_combs = num_verts_sub - 1;
                    for (int c = 0; c < num_child_combs; ++c) {
                        int num_child_sets = Util.choose(num_verts_sub, c + 1);
                        for (int i = 0; i < num_child_sets; ++i) {
                            this.color_sets[s][n][c][i] = null;
                        }
                        this.color_sets[s][n][c] = null;
                    }
                    this.color_sets[s][n] = null;
                }
                this.color_sets[s] = null;
            }
        }

        this.color_sets = null;
    }


    /**
     * @brief free up memory space
     *
     * @return 
     */
    private void delete_all_index_sets(){

        for (int i = 0; i < (this.num_colors-1); ++i) {
            int num_vals = i + 2;
            for (int j = 0; j < (num_vals-1); ++j) {
                int set_size = j + 1;
                int num_combinations = Util.choose(num_vals, set_size);
                for (int k = 0; k < num_combinations; ++k) {
                    this.index_sets[i][j][k] = null;
                }
                this.index_sets[i][j] = null;
            }
            this.index_sets[i] = null;
        }
        this.index_sets = null;
    }

    /**
     * @brief regroup communication
     * single threaded
     * TODO: parallelize the sending partition
     *
     * @param sub_id
     *
     * @return 
     */
    private void regroup_comm_single(int sub_id)
    {
        if (this.verbose)
        {
            LOG.info("Start prepare comm for subtemplate: " + sub_id);
            this.start_comm = System.currentTimeMillis();
            this.start_misc = System.currentTimeMillis();
        }

        //prepare the sending partitions
        this.comm_data_table = new Table<>(0, new SCSetCombiner());
        for(int send_id : this.send_vertex_table.getPartitionIDs())
        {
            //pack colorset_idx and count into a 64 bits double
            int comm_id = ( (send_id << 16) | this.local_mapper_id );

            int[] comm_vert_list = this.send_vertex_table.getPartition(send_id).get().get();
            //for each vert find the colorsets and counts in methods of dynamic_table_array
            int comb_len = this.dt.get_num_color_set(this.part.get_passive_index(sub_id)); 

            SCSet comm_data = compress_send_data(comm_vert_list, comb_len);
            this.comm_data_table.addPartition(new Partition<>(comm_id, comm_data));
        }

        if (this.verbose)
        {
            LOG.info("Finish prepare comm for subtemplate: " + sub_id + "; use time: " +
                    (System.currentTimeMillis() - this.start_misc) + "ms");
        }

        //start the regroup communication
        if (this.verbose)
        {
            LOG.info("Start regroup comm for subtemplate: " + sub_id);
            this.start_misc = System.currentTimeMillis();
        }

        this.mapper.regroup("sc", "regroup counts data", this.comm_data_table, new SCPartitioner(this.mapper_num));

        if (this.verbose)
        {
            LOG.info("Finish regroup comm for subtemplate: " + sub_id + "; use time: " +
                    (System.currentTimeMillis() - this.start_misc) + "ms");
        }

        //update local g counts by adj from each other mapper
        for(int comm_id : this.comm_data_table.getPartitionIDs())
        {
            int update_id = ( comm_id & ( (1 << 16) -1 ) );

            if (this.verbose)
            {
                LOG.info("Local Mapper: " + this.local_mapper_id + " recv from remote mapper id: " + update_id);
            }

            // update vert list accounts for the adj vert may be used to update local v
            SCSet scset = this.comm_data_table.getPartition(comm_id).get();
            this.update_queue_pos[update_id] = scset.get_v_offset();
            this.update_queue_counts[update_id] = scset.get_counts_data();
        }

        if (this.verbose)
        {
            this.time_comm += (System.currentTimeMillis() - this.start_comm);
        }
    }


    /**
     * @brief upate received comm adj counts on local graph 
     * multi-threaded 
     *
     * @param sub_id
     * @param threadIdx
     * @param chunks
     *
     * @return 
     */
    private void update_comm(int sub_id, int threadIdx, int[] chunks) throws BrokenBarrierException, InterruptedException
    {

        if (verbose && threadIdx == 0)
        {
            active_child = part.get_active_index(sub_id);
            passive_child = part.get_passive_index(sub_id);
            // LOG.info("Active Child: " + active_child + "; Passive Child: " + passive_child + " for sub: " + sub_id);
            LOG.info("Start updating remote counts on local vertex");
            this.start_comm = System.currentTimeMillis();
        }

        count_comm_root[threadIdx] = 0.0f;

        barrier.await();

        int num_combinations_verts_sub = this.choose_table[num_colors][num_verts_table[sub_id]];
        int active_index = part.get_active_index(sub_id);
        int num_verts_a = num_verts_table[active_index];

        // colorset combinations from active child
        // combination of colors for active child
        int num_combinations_active_ato = this.choose_table[num_verts_table[sub_id]][num_verts_a];

        barrier.await();
        // start update 
        // first loop over local v
        for (int v = chunks[threadIdx]; v < chunks[threadIdx + 1]; ++v) 
        {
            if (dt.is_vertex_init_active(v))
            {
                int adj_list_size = this.update_map_size[v];
                // store the abs adj id for v
                int[] adj_list = this.update_map[v]; 
                float[] counts_a = dt.get_active(v);

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
                                    color_count_local += (count_a*adj_counts_list[start_pos + comb_indexes_p[p]]);
                                }

                            }

                        }

                    }

                    if (color_count_local > 0.0)
                    {
                        if (sub_id != 0)
                            dt.update_comm(v, comb_num_indexes_set[sub_id][n], color_count_local);
                        else
                            count_comm_root[threadIdx] += color_count_local;
                    }

                }

            }

        }


        barrier.await();

        if (verbose && threadIdx == 0)
        {
            LOG.info("Finish updating remote counts on local vertex");
            this.time_comm += (System.currentTimeMillis() - this.start_comm);
        }
    }

    /**
     * @brief compress local vert data for remote mappers
     * single thread
     *
     * @param vert_list
     * @param num_comb_max
     *
     * @return 
     */
    public SCSet compress_send_data(int[] vert_list, int num_comb_max) 
    {

        if (this.verbose)
        {
            // LOG.info("Start compressing local vertices counts for remote mappers");
            this.start_comm = System.currentTimeMillis();
        }

        int v_num = vert_list.length;
        int[] v_offset = new int[v_num + 1];

        //to be trimed
        //counts_idx_tmp is not required, can be removed
        float[] counts_data_tmp = new float[num_comb_max*v_num];

        int count_num = 0;
        for(int i = 0; i< v_num; i++)
        {
            v_offset[i] = count_num;
            //get the abs vert id
            int comm_vert_id = vert_list[i];
            int rel_vert_id = this.g.get_relative_v_id(comm_vert_id); 

            //if comm_vert_id is not in local graph
            if (rel_vert_id < 0 || (this.dt.is_vertex_init_passive(rel_vert_id) == false))
                continue;

            float[] counts_arry = null; 

            // not retrieved
            if (this.dt.get_passive(rel_vert_id) != null)
            {
                counts_arry = this.dt.get_passive(rel_vert_id);
            }
            else
            {
                LOG.info("ERROR: null passive counts array");
                counts_arry = new float[num_comb_max];
            }

            //check length 
            if (counts_arry.length != num_comb_max)
                LOG.info("ERROR: comb_max and passive counts len not matched");

            System.arraycopy(counts_arry, 0, counts_data_tmp, count_num, counts_arry.length);
            count_num += counts_arry.length;
        }

        v_offset[v_num] = count_num;
        //trim the tmp array
        float[] counts_data = new float[count_num];
        System.arraycopy(counts_data_tmp, 0, counts_data, 0, count_num);

        counts_data_tmp = null;
        SCSet set = new SCSet(v_num, count_num, v_offset, counts_data);

        if (this.verbose)
        {
            // LOG.info("Finish compressing local vertices counts for remote mappers");
            this.time_comm += (System.currentTimeMillis() - this.start_comm);
        }

        return set;
    }

    private void print_counts(int sub_id, int threadIdx, int[] chunks) throws BrokenBarrierException, InterruptedException
    {

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

    }

    public long get_comm_time()
    {
        return this.time_comm;
    }
    
}
