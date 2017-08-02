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
	private int isom;
	private int sizeTemplate;
	private String templateFile;
	private String wholeTemplateName;
	// private ArrayList<SCSubJob> subjoblist;
	private Random rand = new Random();
	private int numMaxThreads;
	private int numThreads;
	private int numIteration;
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
    	LOG.info("init templateFile");
    	LOG.info(templateFile);

    	numThreads =configuration.getInt(SCConstants.NUM_THREADS_PER_NODE, 10);
        numIteration =configuration.getInt(SCConstants.NUM_ITERATION, 10);
        LOG.info("Subgraph Counting Iteration: " + numIteration);

    	// numMaxThreads = Runtime.getRuntime().availableProcessors();
        // if(numMaxThreads < numThreads){//if the numMaxThreads is less than numThreads, use numMaxThreads
        	// numThreads = numMaxThreads;
        // }

        // LOG.info("numMaxTheads: "+numMaxThreads+";numThreads:"+numThreads);
        // Constants.THREAD_NUM = numThreads;
        // Constants.CORE_NUM = 24;
        // Constants.THD_AFFINITY = "compact";
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
        graph_count.init(g_part, false, false, false, true);

        // ------------------- generate communication information -------------------
        // send/recv num and verts 
        graph_count.init_comm(mapper_id_vertex, this);
        
        LOG.info("Finish graph_count initialization");

        // --------------------- start counting ---------------------
		long computation_start = System.currentTimeMillis();

        double full_count = 0.0;
        full_count = graph_count.do_full_count(t, numIteration);

		long computation_end = System.currentTimeMillis();
        LOG.info("Computation time: " + (computation_end - computation_start) + " ms");

        // --------------- allreduce the final count from all mappers ---------------
        //
        Table<DoubleArray> final_count_table = new Table<>(0, new DoubleArrPlus());
        DoubleArray final_count_array = DoubleArray.create(1, false);
    
        final_count_array.get()[0] = full_count;
        final_count_table.addPartition(new Partition<>(0, final_count_array));
    
        this.allreduce("sc", "get-final-count", final_count_table);

        double final_count = final_count_table.getPartition(0).get().get()[0];

        //formula to compute the prob 
        int num_colors = t.num_vertices();
        boolean calculate_automorphisms = false;

        double prob_colorful = Util.factorial(num_colors) /
                ( Util.factorial(num_colors - t.num_vertices()) * Math.pow(num_colors, t.num_vertices()) );

        int num_auto = calculate_automorphisms ? Util.count_automorphisms(t): 1;
        final_count = Math.floor(final_count / (prob_colorful * num_auto) + 0.5);

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

		    for (int i = 0; i < numThreads; i++) {
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
                    + graphData.getNumPartitions() + "; local max v id: " + max_v_id);

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

	// private int getModelSize(ColorCountPairsKVTable[] model){
	// 	int modelSize = 0;
	// 	for(int i = 0; i <model.length; i++){
	// 		modelSize += model[i].getNumPartitions();
	// 	}
	// 	return modelSize;
	// }

	//TODO: try to abolish tables which won't be used anymore. No effect?
	// private void tryAbolish(SCSubJob subjob, Map<String, ColorCountPairsKVTable[]> dataModelMap){
	// 	for(SCSubJob ssj: subjoblist){
	// 		if(ssj.getSubJobID().equals(subjob.getActiveChild())){
	// 			ssj.referedNum--;
	// 		}
	// 		if(ssj.getSubJobID().equals(subjob.getPassiveChild())){
	// 			ssj.referedNum--;
	// 		}
	// 		if(ssj.referedNum == 0 && dataModelMap.containsKey(ssj.getSubJobID())){//abolish
	// 			LOG.info("Before abolishing: Total Memory (bytes): " + " "
	// 					+ Runtime.getRuntime().totalMemory()
	// 					+ ", Free Memory (bytes): "
	// 					+ Runtime.getRuntime().freeMemory());
    //
	// 			dataModelMap.remove(ssj.getSubJobID());
	// 			LOG.info("Abolished table "+ssj.getSubJobID());
	// 		}
	// 	}
	// }

	//local aggregate the counts
	// private long localAggregate (ColorCountPairsKVTable[] subMatchingModel){
	// 	long count = 0;
    //
	// 	List<AggregationTask> tasks = new LinkedList<>();
	// 	for (int i = 0; i < numThreads; i++) {
	// 		tasks.add(new AggregationTask());
	// 	}
	// 	// doTasks(cenPartitions, output, tasks);
	// 	DynamicScheduler<ColorCountPairs,  Long, AggregationTask>
	// 			compute = new DynamicScheduler<>(tasks);
	// 	compute.start();
	// 	for( int i = 0; i < subMatchingModel.length; i++) {
	// 		ColorCountPairsKVTable subMatchingTable = subMatchingModel[i];
	// 		for (int parID : subMatchingTable.getPartitionIDs()) {
	// 			ColorCountPairs ccp = subMatchingTable.getVal(parID);
	// 			compute.submit(ccp);
	// 		}
    //
	// 	}
	// 	Long output=null;
	// 	while(compute.hasOutput()){
	// 		output = compute.waitForOutput();
	// 		if(output != null){
	// 			count += output;
	// 		}
	// 	}
	// 	compute.stop();
	// 	return count;
	// }

	//clone table
	// private void cloneTable(ColorCountPairsKVTable[] curModel, ColorCountPairsKVTable[] newModel){
	// 	LOG.info("[BEGIN] clone table");
    //
	// 	List<CloneTask> tasks = new LinkedList<>();
	// 	for (int i = 0; i < numThreads; i++) {
	// 		tasks.add(new CloneTask());
	// 	}
	// 	// doTasks(cenPartitions, output, tasks);
	// 	StaticScheduler<Partition<ColorCountPairsKVPartition>,  CloneTask.CloneTaskOutput, CloneTask>
	// 			compute = new StaticScheduler<>(tasks);
	// 	for(int i = 0; i < curModel.length; i++) {
	// 		ColorCountPairsKVTable curTable = curModel[i];
	// 		for (Partition<ColorCountPairsKVPartition> par : curTable.getPartitions()) {
	// 			compute.submit(i % numThreads, par);
	// 		}
	// 	}
	// 	compute.start();
	// 	int sliceId = 0;
	// 	int partitionPerSlice = getModelSize(curModel) / numModelSlices
	// 			+ ( getModelSize(curModel) % numModelSlices == 0? 0 : 1);
	// 	CloneTask.CloneTaskOutput output=null;
	// 	compute.pause();//wait for completion
	// 	for(int i = 0; i < numThreads; i++) {
	// 		while (compute.hasOutput(i)) {
	// 			output = compute.waitForOutput(i);
	// 			if (output != null) {
	// 				if (newModel[sliceId].getNumPartitions() >= partitionPerSlice) {
	// 					sliceId++;
	// 				}
	// 				newModel[sliceId].addKeyVal(output.key, output.colorCountPairs);
	// 			}
	// 		}
	// 	}
	// 	compute.stop();
    //
	// 	LOG.info("[END] clone table");
	// }

	//subtemplate matching in MultiThread way
	// private ColorCountPairsKVTable[] matchSubTemplateMultiThread(Table<IntArray> graphData, Map<String, ColorCountPairsKVTable[]> dataModelMap, SCSubJob subjob){
	// 	LOG.info("[BEGIN] SCCollectiveMapper.matchSubTemplateMultiThread" );
    //
	// 	ColorCountPairsKVTable[] modelTable = new ColorCountPairsKVTable[numModelSlices];
	// 	for(int i = 0; i < numModelSlices; i++){
	// 		modelTable[i] = new ColorCountPairsKVTable(10*(i+1));//ensure different ids
	// 	}
    //
	// 	ColorCountPairsKVTable[] passiveChild = dataModelMap.get(subjob.getPassiveChild());
	// 	ColorCountPairsKVTable[] activeChild;
	// 	if(subjob.getActiveChild().equals(subjob.getPassiveChild())){
	// 		//if the active and passive children are the same one
	// 		//clone an activeChild
	// 		activeChild = new ColorCountPairsKVTable[numModelSlices];
	// 		for(int i = 0; i < numModelSlices; i++){
	// 			activeChild[i] = new ColorCountPairsKVTable(11*(i+1));//ensure different ids
	// 		}
	// 		cloneTable(dataModelMap.get(subjob.getPassiveChild()),activeChild);
	// 		//this activeChild won't do rotation
	// 	}else{
	// 		activeChild = dataModelMap.get(subjob.getActiveChild());
	// 	}
	// 	LOG.info("active child = "+ subjob.getActiveChild()+"; size="+getModelSize(activeChild)
	// 			+"; passiveChild ="+subjob.getPassiveChild()+";size="+getModelSize(passiveChild));
    //
	// 	int numWorkers = this.getNumWorkers();
	// 	LOG.info("numWorkers: "+numWorkers+"; numMaxTheads: "+numMaxThreads+";numThreads:"+numThreads);
	// 	int numColSplits = 1;
    //
	// 	LOG.info("[BEGIN] SCCollectiveMapper.matchSubTemplateMultiThread.Rotator." );
	// 	Rotator<ColorCountPairsKVPartition> rotator =
	// 			new Rotator<>(passiveChild, numColSplits,
	// 					false, this, null, "subgraph-" + subjob.getSubJobID());
	// 	rotator.start();
    //
	// 	//compose
	// 	List<SubMatchingTask> tasks = new LinkedList<>();
	// 	for (int i = 0; i < numThreads; i++) {
	//         	tasks.add(new SubMatchingTask(graphData,passiveChild[0]));
	// 	}
	// 	DynamicScheduler<Partition<ColorCountPairsKVPartition>, SubMatchingTask.SubMatchingTaskOutput, SubMatchingTask>
	// 	compute = new DynamicScheduler<>(tasks);
    //
	// 	long computeTime = 0;
	// 	long rotatorBegin = System.currentTimeMillis();
	// 	for(int j = 0; j < numWorkers; j++){
	// 		LOG.info("[BEGIN] SCCollectiveMapper.matchSubTemplateMultiThread. Round " +j);
	// 		for(int k = 0; k < numModelSlices; k++){
	// 			LOG.info("try to get partition");
	// 			List<Partition<ColorCountPairsKVPartition>>[] receivedPassiveChild =
	// 					rotator.getSplitMap(k);
	// 			LOG.info("get partition");
    //
	// 			long computateBegin = System.currentTimeMillis();
	// 			//update tasks
	// 			for (int i = 0; i < numThreads; i++) {
	// 				compute.getTasks().get(i).setPassiveChild(passiveChild[k]);
	// 			}
	// 			LOG.info("task updated");
	// 			//compute.start();
	// 			//submit
	// 			LOG.info("submitting tasks");
	// 			for(int i = 0; i < activeChild.length; i++) {
	// 				compute.submitAll(activeChild[i].getPartitions());
	// 			}
	// 			LOG.info("submitted; Start tasks");
	// 			//start tasks here
	// 			compute.start();
	// 			SubMatchingTask.SubMatchingTaskOutput output=null;
	// 			while(compute.hasOutput()){
	// 				output = compute.waitForOutput();
	// 				if(output != null){
	// 					modelTable[k].addKeyVal(output.key, output.colorCountPairs);
	// 				}
	// 			}
	// 			compute.pause();
	// 			LOG.info("tasks finished");
	// 			long computateEnd = System.currentTimeMillis();
	// 			computeTime += computateEnd - computateBegin;
	// 			rotator.rotate(k);
	// 		}
	// 		LOG.info("[END] SCCollectiveMapper.matchSubTemplateMultiThread. Round " +j);
	// 	}
	// 	compute.stop();
	// 	rotator.stop();
	// 	long rotatorEnd = System.currentTimeMillis();
	// 	long rotatorTotal = rotatorEnd - rotatorBegin;
    //
	// 	LOG.info("[END] SCCollectiveMapper.matchSubTemplateMultiThread.Rotator: "
	// 			+"computation: " + computeTime + "ms; communication:" + (rotatorTotal - computeTime) + "ms" );
    //
	// 	LOG.info("[END] SCCollectiveMapper.matchSubTemplateMultiThread" );
	// 	return modelTable;
	// }


	//color the graph in MultiThread way
	// private ColorCountPairsKVTable[] colorGraphMultiThread(Table<IntArray> graphData ){
	// 	LOG.info("[BEGIN] SCCollectiveMapper.colorGraphMultiThread " );
    //
	// 	ColorCountPairsKVTable[] colorTable =  new ColorCountPairsKVTable[numModelSlices];
	// 	for(int i = 0; i < numModelSlices; i++){
	// 		colorTable[i] = new ColorCountPairsKVTable(9*(i+1));
	// 	}
    //
	// 	Collection<Partition<IntArray>> graphPartitions = graphData.getPartitions();
	//     List<ColorTask> tasks = new LinkedList<>();
	//     for (int i = 0; i < numThreads; i++) {
	//     	tasks.add(new ColorTask(numColor, rand));
	//     }
	//     // doTasks(cenPartitions, output, tasks);
	//     DynamicScheduler<Partition<IntArray>,  ColorTask.ColorTaskOutput, ColorTask>
	//     	compute = new DynamicScheduler<>(tasks);
	//     compute.start();
    //
	//     for (Partition<IntArray> partition : graphPartitions) {
	//     	compute.submit(partition);
	//     }
	// 	int modelSliceId = 0;
	//     int partitionPerSlice = graphData.getNumPartitions() / numModelSlices
	// 							+ ( graphData.getNumPartitions() % numModelSlices == 0? 0 : 1);
	// 	ColorTask.ColorTaskOutput output=null;
	//     while(compute.hasOutput()){
	//     	output = compute.waitForOutput();
	//     	if(output != null){
	//     		if( colorTable[modelSliceId].getNumPartitions() >= partitionPerSlice){
	//     			modelSliceId++;
	// 			}
	// 			colorTable[modelSliceId].addKeyVal(output.partitionId, output.colorCountPairs);
	//     	}
	//     }
	//     compute.stop();
    //
	//     for(int i = 0; i < numModelSlices; i++){
	// 	LOG.info("model slice id: "+i+"; mode size: " + colorTable[i].getNumPartitions());
	//     }
    //
	//     LOG.info("[END] SCCollectiveMapper.colorGraphMultiThread");
    //
	// 	return colorTable;
	// }

	//color the graph
	// private ColorCountPairsKVTable colorGraph(Table<IntArray> graphData ){
	// 	ColorCountPairsKVTable table = new ColorCountPairsKVTable(8);
	// 	for(int ID: graphData.getPartitionIDs()){
	// 		//LOG.info("for ID:=" + ID);
	// 		//int colorBit = SCUtils.power(2, rand.nextInt(numColor));
	// 		int colorBit = SCUtils.power(2, ID % numColor );
	// 		ColorCountPairs ccp = new ColorCountPairs();
	// 		ccp.addAPair(colorBit, 1);
	// 		table.addKeyVal(ID, ccp);
	// 	}
	// 	return table;
	// }

	//load template
	// private void init(String template){
    //
	// 	subjoblist = new ArrayList<SCSubJob>();
    //
	// 	try {
    //         String line;
    //         File f = new File(template);
    //         BufferedReader fReader = new BufferedReader(new FileReader(f));
    //         while ((line = fReader.readLine()) != null) {
    //             String[] st = line.split(" ");
    //             if(st.length<=1){
    //             	continue;
    //             }
    //             if (st[0].contains("final")) { // total count
    //             	// like final u5-1 5 2
    //             	SCSubJob subJob = new SCSubJob();
    //             	isom = Integer.parseInt(st[3]);
    //             	sizeTemplate = Integer.parseInt(st[2]);
    //             	subJob.setSubJobID(st[0]);
    //             	subJob.setActiveChild(st[1]);
    //             	wholeTemplateName = st[1];
    //             	subJob.setPassiveChild(null);
    //                 subjoblist.add(subJob);
    //
    //             	// new a subjob, or update an exsiting subjob
    //             	boolean flag=false;
    //             	for(SCSubJob ssj: subjoblist){
    //             		if (ssj.getSubJobID().equals(st[1])){
    //             			ssj.referedNum ++;
    //             			flag=true;
    //             			break;
    //             		}
    //             	}
    //             	if(flag==false){
    //             		SCSubJob ssj = new SCSubJob();
    //             		ssj.setSubJobID(st[1]);
    //             		ssj.referedNum ++;
    //             		subjoblist.add(ssj);
    //             	}
    //
    //
    //
    //             } else if (st[0].equals("i")) { // random coloring
    //             	// like i graph 5
    //                 numColor = Integer.parseInt(st[2]);
    //
    //                 boolean flag=false;
    //             	for(SCSubJob ssj: subjoblist){
    //             		if (ssj.getSubJobID().equals(st[0])){
    //             			flag=true;
    //             			ssj.setActiveChild(null);
    //             			ssj.setPassiveChild(null);
    //             			break;
    //             		}
    //             	}
    //             	if(flag==false){
    //             		SCSubJob subJob = new SCSubJob();
    //                     subJob.setSubJobID(st[0]);
    //                     subJob.setActiveChild(null);
    //                 	subJob.setPassiveChild(null);
    //                 	 subjoblist.add(subJob);
    //             	}
    //
    //             } else {
    //             	// like u5-1 u3-1 u2
    //                 String activeChild = st[1];
    //                 String passiveChild = st[2];
    //
    //                 boolean flag=false;
    //             	for(SCSubJob ssj: subjoblist){
    //             		if (ssj.getSubJobID().equals(st[0])){
    //             			flag=true;
    //             			ssj.setActiveChild(activeChild);
    //             			ssj.setPassiveChild(passiveChild);
    //             			break;
    //             		}
    //             	}
    //             	if(flag==false){
    //             		  SCSubJob subJob = new SCSubJob();
    //                       subJob.setSubJobID(st[0]);
    //                       subJob.setActiveChild(activeChild);
    //                       subJob.setPassiveChild(passiveChild);
    //                       subjoblist.add(subJob);
    //             	}
    //
    //             	flag=false;
    //             	for(SCSubJob ssj: subjoblist){
    //             		if (ssj.getSubJobID().equals(activeChild)){
    //             			flag=true;
    //             			ssj.referedNum++;
    //             			break;
    //             		}
    //             	}
    //             	if(flag==false){
    //             		  SCSubJob subJob = new SCSubJob();
    //                       subJob.setSubJobID(activeChild);
    //                       subJob.referedNum++;
    //                       subjoblist.add(subJob);
    //             	}
    //
    //             	flag=false;
    //             	for(SCSubJob ssj: subjoblist){
    //             		if (ssj.getSubJobID().equals(passiveChild)){
    //             			flag=true;
    //             			ssj.referedNum++;
    //             			break;
    //             		}
    //             	}
    //             	if(flag==false){
    //             		  SCSubJob subJob = new SCSubJob();
    //                       subJob.setSubJobID(passiveChild);
    //                       subJob.referedNum++;
    //                       subjoblist.add(subJob);
    //             	}
    //             }
    //         }
    //         fReader.close();
    //
    //     } catch (IOException e) {
    //         // TODO Auto-generated catch block
    //         e.printStackTrace();
    //     }
	// }

	// private ArrayList<SCSubJob> topologySort(ArrayList<SCSubJob> subjoblist){
	// 	ArrayList<SCSubJob> res = new ArrayList<SCSubJob>();
	// 	Set<String> uniqueSet = new HashSet<String>();
	// 	int size =  subjoblist.size();
	// 	for(int j=0; j<size; j++){
	// 		for(int i = 0; i < size; i++){
	// 			SCSubJob scsjob = subjoblist.get(i);
	// 			if(!uniqueSet.contains(scsjob.getSubJobID())){
	// 				if( (scsjob.getActiveChild() == null || uniqueSet.contains(scsjob.getActiveChild()))
	// 						&& 
	// 					((scsjob.getPassiveChild() == null) ||  uniqueSet.contains(scsjob.getPassiveChild())) ) {
	// 					res.add(scsjob);
	// 					uniqueSet.add(scsjob.getSubJobID());
	// 				}
	// 			}
	// 		}
	// 	}
	// 	return res;
	// }

	//final counting
	// private int finalCounting(Table<IntArray> wholeMatchingTable){
	// 	int count = 0;
	// 	for(Partition<IntArray> parWholeMatching: wholeMatchingTable.getPartitions()){
	// 		for(int i = parWholeMatching.get().start()+1; i<parWholeMatching.get().size(); i+=2){
	// 			count += parWholeMatching.get().get()[i];
	// 		}
	// 	}
    //
	// 	count /= isom;
	// 	count /= SCUtils.Prob(numColor, sizeTemplate);
	// 	
	// 	
	// 	return count;
	// }
	
	//print for debugging
	// private void printTable( ColorCountPairsKVTable table){
	// 	for(Partition<ColorCountPairsKVPartition> par: table.getPartitions())
	// 	 {
	// 		int key = par.id();
	// 		 ColorCountPairs ccp = par.get().getVal(key);
	// 		 System.out.print(key+"\t");
	// 		 for(int i = 0; i<ccp.getColors().size(); i++)
	// 			 System.out.print(ccp.getColors().get(i)+","+ccp.getCounts().get(i)+",");
	// 		 System.out.println();
	// 	}
	// }
}
