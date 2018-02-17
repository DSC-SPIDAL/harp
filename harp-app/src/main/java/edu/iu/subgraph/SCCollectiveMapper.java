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

import edu.iu.dymoro.Rotator;
import edu.iu.harp.example.IntArrPlus;
import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.IntArray;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.resource.LongArray;
import edu.iu.harp.schdynamic.DynamicScheduler;
import edu.iu.harp.schstatic.StaticScheduler;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.CollectiveMapper;

import java.io.*;
import java.util.*;

/*
 * use key-object; in-memory
 * Instead of using allgather, using rotation
 */
public class SCCollectiveMapper  extends CollectiveMapper<String, String, Object, Object> {
	private int numMappers;
	private int numColor;
	private String templateFile;
	private Random rand = new Random();
	private int numThreads;
    private int harpThreads; //always use the maximum hardware threads to load in data and convert data
	private int numIteration;
    private int numCores;
    private int tpc;
    private long send_array_limit;
    private boolean rotation_pipeline;
    private String affinity;
	boolean useLocalMultiThread;
	int numModelSlices; // number of slices for pipeline optimization
    private int vert_num_count =0;
    private int vert_num_count_total = 0;
    private int adj_len = 0;
    private int max_v_id = 0;
    private Graph t;
    private Table<IntArray> abs_ids_table;
    private int[] mapper_id_vertex; 

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {

		LOG.info("start setup");

		Configuration configuration = context.getConfiguration();
    	numMappers = configuration.getInt(SCConstants.NUM_MAPPERS, 10);
    	templateFile = configuration.get(SCConstants.TEMPLATE_PATH);
    	useLocalMultiThread = configuration.getBoolean(SCConstants.USE_LOCAL_MULTITHREAD, true);
    	rotation_pipeline = configuration.getBoolean(SCConstants.ROTATION_PIPELINE, true);

    	LOG.info("init templateFile");
    	LOG.info(templateFile);

    	numThreads =configuration.getInt(SCConstants.THREAD_NUM, 10);
        numCores = configuration.getInt(SCConstants.CORE_NUM, 24);
        affinity = configuration.get(SCConstants.THD_AFFINITY);
        tpc = configuration.getInt(SCConstants.TPC, 2);

        //always use the maximum hardware threads to load in data and convert data 
        harpThreads = Runtime.getRuntime().availableProcessors();
        LOG.info("Num Threads " + numThreads);
        LOG.info("Num harp load data threads " + harpThreads);

        send_array_limit = (configuration.getInt(SCConstants.SENDLIMIT, 250))*1024L*1024L;

        numIteration =configuration.getInt(SCConstants.NUM_ITERATION, 10);
        LOG.info("Subgraph Counting Iteration: " + numIteration);

		numModelSlices = 2;
	}

    protected void mapCollective( KeyValReader reader, Context context) throws IOException, InterruptedException {
		LOG.info("Start collective mapper" );
		this.logMemUsage();
		LOG.info("Memory Used: "+ (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));

        long startTime = System.currentTimeMillis();
        LinkedList<String> vFiles = getVFiles(reader);
        try {
            runSC(vFiles, context.getConfiguration(), context);
        } catch (Exception e) {
            LOG.error("Fail to run Subgraph Counting.", e);
        }
        LOG.info("Total execution time: "
                + (System.currentTimeMillis() - startTime));
		
	}

    

    private void runSC(final LinkedList<String> vFilePaths,
            final Configuration configuration,
            final Context context) throws Exception {

		Configuration conf = context.getConfiguration();

        // ------------------------- read in graph data -------------------------
        //
		LOG.info("Start read Graph Data");
		long readGraphbegintime = System.currentTimeMillis();

        Graph g_part = new Graph();
		readGraphDataMultiThread(conf, vFilePaths, g_part);

		long readGraphendtime=System.currentTimeMillis();
		LOG.info("Loaded local graph verts: " + g_part.num_vertices()+"; takes time: " + (readGraphendtime- readGraphbegintime)+"ms");

		// this.logMemUsage();
		// LOG.info("Memory Used: "+ (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
		// logGCTime();
        // ------------------------- read in template data -------------------------
        Graph t = new Graph();
        readTemplate(templateFile, context, t);
        LOG.info("Finish load templateFile, num_verts: " + t.num_vertices() + "; edges: " + t.num_edges());

		// ---------------  main computation ----------------------------------
        colorcount_HJ graph_count = new colorcount_HJ();
        graph_count.init(this, context, g_part, max_v_id, numThreads, numCores, tpc, affinity, false, false, true);

        // ------------------- generate communication information -------------------
        // send/recv num and verts 
        if (this.getNumWorkers() > 1)
        {
            graph_count.init_comm(mapper_id_vertex, send_array_limit, rotation_pipeline);
            LOG.info("Finish graph_count initialization");
        }

        // --------------------- start counting ---------------------
		long computation_start = System.currentTimeMillis();

        double full_count = 0.0;
        full_count = graph_count.do_full_count(t, numIteration);

		long computation_end = System.currentTimeMillis();
        long local_count_time = (computation_end - computation_start);
        long local_comm_time = graph_count.get_comm_time();
        long local_sync_time = graph_count.get_sync_time();

        Table<DoubleArray> time_table = new Table<>(0, new DoubleArrPlus());
        DoubleArray time_array = DoubleArray.create(3, false);
        time_array.get()[0] = (double)local_count_time;
        time_array.get()[1] = (double)local_comm_time;
        time_array.get()[2] = (double)local_sync_time;

        time_table.addPartition(new Partition<>(0, time_array));

        this.allreduce("sc", "get-time", time_table);

        double global_count_time = time_table.getPartition(0).get().get()[0]/this.getNumWorkers(); 
        double global_comm_time = time_table.getPartition(0).get().get()[1]/this.getNumWorkers(); 
        double global_sync_time = time_table.getPartition(0).get().get()[2]/this.getNumWorkers(); 

        LOG.info("Total Counting time: " + global_count_time + " ms" + "; Avg per itr: " + 
                (global_count_time/(double)numIteration) + " ms");


        LOG.info("Total comm time: " + global_comm_time + " ms" + "; Avg per itr: "
                + (global_comm_time/(double)numIteration) + " ms");

        LOG.info("Total sync waiting time: " + global_sync_time + " ms" + "; Avg per itr: "
                + (global_sync_time/(double)numIteration) + " ms");

        LOG.info("Time Ratio: Comm: " + (global_comm_time/global_count_time)*100 + " %; Waiting: " 
                + (global_sync_time)/global_count_time*100 + " %; Local Computation: "
                + (global_count_time - global_sync_time - global_comm_time)/global_count_time*100 + " %");


        // --------------- allreduce the final count from all mappers ---------------
        //
        Table<DoubleArray> final_count_table = new Table<>(0, new DoubleArrPlus());
        DoubleArray final_count_array = DoubleArray.create(1, false);

        final_count_array.get()[0] = full_count;
        final_count_table.addPartition(new Partition<>(0, final_count_array));

        this.allreduce("sc", "get-final-count", final_count_table);

        full_count = final_count_table.getPartition(0).get().get()[0];

        //formula to compute the prob 
        int num_colors = t.num_vertices();
        boolean calculate_automorphisms = true;

        double prob_colorful = Util.factorial(num_colors) /
                ( Util.factorial(num_colors - t.num_vertices()) * Math.pow(num_colors, t.num_vertices()) );

        int num_auto = calculate_automorphisms ? Util.count_automorphisms(t): 1;
        double final_count = Math.floor(full_count / (prob_colorful * num_auto) + 0.5);

        LOG.info("Finish counting local color count: " + full_count + "; final alll count: " + final_count);

		//-------------------------------------------------------------------
        //
    }

    private LinkedList<String>
        getVFiles(final KeyValReader reader)
        throws IOException, InterruptedException {
        final LinkedList<String> vFiles =
            new LinkedList<>();
        while (reader.nextKeyValue()) {
            final String value =
                reader.getCurrentValue();
            LOG.info("File: " + value);
            vFiles.add(value);
        }
        return vFiles;
    }
    
    /**
     * @brief read in data and store them in Fascia structure
     *
     * @param conf
     * @param vFilePaths
     *
     * @return 
     */
	private void readGraphDataMultiThread( Configuration conf, List<String> vFilePaths, Graph g){

			LOG.info("[BEGIN] SCCollectiveMapper.readGraphDataMultiThread" );

			Table<IntArray> graphData = new Table<>(0, new IntArrPlus());
		    List<GraphLoadTask> tasks = new LinkedList<>();

		    for (int i = 0; i < harpThreads; i++) {
		    	tasks.add(new GraphLoadTask(conf));
		    }

		    DynamicScheduler<String, ArrayList<Partition<IntArray>>, GraphLoadTask> compute
		    	= new DynamicScheduler<>(tasks);

		    compute.start();

		    for (String filename : vFilePaths) {
		    	compute.submit(filename);
		    }

		    ArrayList<Partition<IntArray>> output=null;
		    while (compute.hasOutput()) {
		    	output = compute.waitForOutput();
		    	if(output != null){
		    		ArrayList<Partition<IntArray>> partialGraphDataList = output;
		    		for(Partition<IntArray> partialGraph:partialGraphDataList )
		    		{
		    			graphData.addPartition(partialGraph);
		    		}
		    	}
		    }

		    compute.stop();
		    LOG.info("[END] SCCollectiveMapper.readGraphDataMultiThread" );

            //get num vert and size of adjacent array
            vert_num_count = 0;
            adj_len = 0;
            max_v_id = 0;

            for(int p=0;p<tasks.size();p++)
            {
                vert_num_count += tasks.get(p).get_vert_num_local();
                adj_len += tasks.get(p).get_adjacent_local_num();

                int max_id_local = tasks.get(p).get_max_v_id();
                max_v_id = max_id_local > max_v_id ? max_id_local: max_v_id; 
            }

            //debug
            LOG.info("vert num count local: " + vert_num_count + " total partitions: " 
                    + graphData.getNumPartitions() + "; local max v id: " + max_v_id + "; total local nbrs num: " + adj_len);

            vert_num_count = graphData.getNumPartitions();
		 
            //allreduce to get the total vert number 
            Table<IntArray> table_max_id = new Table<>(0, new IntArrMax());
            IntArray array_max_id = IntArray.create(1, false);
    
            array_max_id.get()[0] = max_v_id;
            table_max_id.addPartition(new Partition<>(0, array_max_id));
    
            this.allreduce("sc", "get-max-v-id", table_max_id);

            max_v_id = table_max_id.getPartition(0).get().get()[0];

            LOG.info("Max vertex id of full graph: " + max_v_id);
    
            table_max_id.release();
            table_max_id = null;

            //initialize the Graph class
            g.initGraph(vert_num_count, max_v_id, adj_len, graphData);

            //communication allgather to get all global ids
            abs_ids_table = new Table<>(0, new IntArrPlus());
            IntArray abs_ids_array = new IntArray(g.get_abs_v_ids(), 0, g.num_vertices());
            abs_ids_table.addPartition(new Partition<>(this.getSelfID(), abs_ids_array));

            this.allgather("sc", "collect all abs ids", abs_ids_table);

            //create an label array to store mapper ids info
            mapper_id_vertex = new int[max_v_id+1];
            for(int p=0; p<this.getNumWorkers();p++)
            {
                // mapper_vertex_array stores abs ids from each mapper
                int[] mapper_vertex_array = abs_ids_table.getPartition(p).get().get();
                for(int q=0;q<mapper_vertex_array.length;q++)
                    mapper_id_vertex[mapper_vertex_array[q]] = p;
            }

            LOG.info("Finish creating mapper-vertex mapping array");
            abs_ids_table = null;

	}

    /**
     * @brief read in template file
     *
     * @param template
     *
     * @return 
     */
    private void readTemplate(String templateFile, Context context, Graph t) {

        FSDataInputStream in = null;
        BufferedReader fReader = null;

        try {

		    Configuration conf = context.getConfiguration();


            Path path = new Path(templateFile);
            String template_file = path.toUri().toString();

            Path template_input = new Path(template_file);

            FileSystem fs = template_input.getFileSystem(conf);
            in = fs.open(template_input);
            fReader = new BufferedReader(new InputStreamReader(in), 1048576);

            // BufferedReader fReader = new BufferedReader(new FileReader(f_t));

            int n_g, m_g;

            //get number of nodes (vertice)
            n_g = Integer.parseInt( fReader.readLine() );
            //get number of edges
            m_g = Integer.parseInt( fReader.readLine() );

            LOG.info("templateFile verts: " + n_g + "; edges: " + m_g);

            int[] srcs_g = new int[m_g];
            int[] dsts_g = new int[m_g];

            for(int p = 0; p < m_g; ++p){
                String[] src_dst = fReader.readLine().split("\\s+");
                srcs_g[p] = Integer.parseInt(src_dst[0]  );
                dsts_g[p] = Integer.parseInt(src_dst[1] );
            }

            fReader.close();
            in.close();

            t.initTemplate(n_g, m_g, srcs_g, dsts_g);

            srcs_g = null;
            dsts_g = null;

        } catch (IOException e) {
            e.printStackTrace();
            if (fReader != null)
            {
                try {
                    fReader.close();
                } catch (Exception e1) {
                }

            }

            if (in != null)
            {

                try {
                    in.close();
                } catch (Exception e1) {
                }

            }

            LOG.info("Failed in reading in templateFile");
        }

    }

	
}
