package edu.iu.lda;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.CollectiveMapper;

import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.example.IntArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Partitioner;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.resource.IntArray;
import edu.iu.harp.schdynamic.DynamicScheduler;
import edu.umd.cloud9.math.Gamma;

/**
 * The number of mappers will be the same with numOfTopics
 * Every mapper task deals with one topic.
 * The topic it associated is the id of the mapper, which is this.getSelfID()
 *
 * Use dynamic scheduling for computation.
 * 
 * data should be in sparse matrix.
 * 
 */
public class LDAMapperDyn extends CollectiveMapper<String, String, Object, Object> {
	
	int numOfTermsInCorpus;
	int numOfTopics;
	int numOfDocs;
	int numOfIterations;
	int numOfThreads;
	String metafile;
	Table<DoubleArray> alphaTable;

	List<OneDoc> docList;
	HashSet<Integer> wordIdListInThisMapper;
	
	Table<DoubleArray> localLogPhiTable = null;
	//point to the same partitions with localLogPhiTable. But use different combiner. 
	//So in pull, it will directly get values from global, not the combined results from local and global.
	Table<DoubleArray> localLogPhiTableToReceiveGlobal=null;
	Table<DoubleArray> globalLogPhiTable = null;
 
	Table<DoubleArray> logNormalizerTable = null;
	
	Table<DoubleArray> totalAlphaSufficientStatisticsTable = null;
	
	Table<DoubleArray> logBeta = null;
	
	Table<DoubleArray> logLikelihoodTable = null;
	
	
	
	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		
		Configuration configuration = context.getConfiguration();
		numOfTermsInCorpus =configuration.getInt(Constants.NUM_OF_TERMS, Integer.MAX_VALUE);
		numOfTopics =configuration.getInt(Constants.NUM_OF_TOPICS, 20);
		numOfDocs =configuration.getInt(Constants.NUM_OF_DOCS, 20);
		numOfIterations = configuration.getInt(Constants.NUM_OF_ITERATIONS, 1);
		numOfThreads = configuration.getInt(Constants.NUM_OF_THREADS, 1);
		metafile = configuration.get(Constants.META_DATA_FILE_NAME,"metadata");
		docList = new ArrayList<OneDoc> ();
		wordIdListInThisMapper = new HashSet();
	}
	
	@Override
	protected void mapCollective( KeyValReader reader, Context context) throws IOException, InterruptedException {
		long beginTime = System.currentTimeMillis();
		List<String> docFiles = new ArrayList<String>();
	    while (reader.nextKeyValue()) {
	    	String value = reader.getCurrentValue();
	    	docFiles.add(value);
	    }
	    Configuration conf = context.getConfiguration();
		runLDA(docFiles, conf, context);
		long endOfOneIteration = System.currentTimeMillis();
		System.out.println("total running time(ms): "+(endOfOneIteration-beginTime));
		context.write(null, "total running time(ms): "+(endOfOneIteration-beginTime));
	}

	private void runLDA(List<String> docFiles, Configuration conf, Context context) throws IOException {
		//load files
		loadData(docList,wordIdListInThisMapper, docFiles, conf);
		//Utils.printIntTable(docList);
		
		
		// initialize alpha	
		alphaTable = new Table<>(0, new DoubleArrPlus());
		double[] alphaVector = new double[numOfTopics];
		for(int i=0; i< numOfTopics; i++){
			alphaVector[i] = 1e-3;
		}
		Partition<DoubleArray> alphaPartition = new Partition<DoubleArray>(0, new DoubleArray(alphaVector, 0, numOfTopics));
		alphaTable.addPartition(alphaPartition);
		
		// initialize beta
		logBeta = new Table<>(0, new DoubleArrLogPlus());
		double[] logNorm = new double[numOfTopics];
		
		int cnt = 0;
		for(int termIndex = 0; termIndex < numOfTermsInCorpus; termIndex++)
		{ 
			double[] tempBeta = new double[numOfTopics];
			for (int i = 0; i < numOfTopics; i++) {
				//tempBeta[i] = Math.log(2 * Math.random() / numOfTermsInCorpus + Math.random());
				tempBeta[i]=0.1;
				if(cnt == 0)
				{
					logNorm[i]  =  tempBeta[i];
				}else
					logNorm[i] = Utils.logAdd(logNorm[i] , tempBeta[i]);
			}
			Partition<DoubleArray> termPartition = new Partition<DoubleArray>(termIndex, new DoubleArray(tempBeta, 0, numOfTopics));
			logBeta.addPartition(termPartition);
			cnt++;
		}
		
		for(int termIndex = 0; termIndex < numOfTermsInCorpus; termIndex++)
		{
			double[] tb = logBeta.getPartition(termIndex).get().get();
			for (int i = 0; i < numOfTopics; i++) {
				tb[i] -= logNorm[i] ;
			}
		}
		
		// new  tables
		
		totalAlphaSufficientStatisticsTable = new Table<>(0, new DoubleArrPlus());
		double[] totalAlphaSufficientStatistics = new double[numOfTopics];
		Partition<DoubleArray> tasPartition = new Partition<DoubleArray>(0, new DoubleArray(totalAlphaSufficientStatistics, 0, numOfTopics));
		totalAlphaSufficientStatisticsTable.addPartition(tasPartition);
		 
		localLogPhiTable = new Table<>(0, new DoubleArrLogPlus());
		localLogPhiTableToReceiveGlobal = new Table<>(0, new DoubleArrGet());
		globalLogPhiTable = new Table<>(0, new DoubleArrLogPlus());
		logLikelihoodTable = new Table<>(0, new DoubleArrPlus());
		Partition<DoubleArray> logLikelihoodPartition = new Partition<DoubleArray>(0, new DoubleArray(new double[1], 0, 1));
		logLikelihoodTable.addPartition(logLikelihoodPartition);

		logNormalizerTable = new Table<>(0, new DoubleArrLogPlus());
		double[] logNormalizer = new double[numOfTopics];
		Partition<DoubleArray> lnpar = new Partition<DoubleArray>(0, new DoubleArray(logNormalizer, 0, numOfTopics));
		logNormalizerTable.addPartition(lnpar); 
		
		//init task
		
		List<TrainingTask> tsks = new LinkedList<>();
		for (int i = 0; i < numOfThreads; i++) {
			tsks.add(new TrainingTask(numOfTopics, alphaTable, logBeta));
		}
		 
		DynamicScheduler<OneDoc, TrainingTaskOutput, TrainingTask> trainingCompute =
		      new DynamicScheduler<>(tsks);
		trainingCompute.start();
		
		//iterations 
		double lastLogLikelihood = 0.0;
		
		
		if(this.isMaster()){
			try {
				context.write(null, new Text("iteration\ttime elapsed(ms)\tloglikelihood"));
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		long beginTime = System.currentTimeMillis();
		
		for(int iter = 0; iter < numOfIterations; iter++){
			
			double logLikelihood = oneIteration(iter,trainingCompute );
			
			System.out.println("logLikelihood: " + logLikelihood);
			long endOfOneIteration = System.currentTimeMillis();
			System.out.println("time elapsed(ms): "+(endOfOneIteration-beginTime));

			if(this.isMaster()){
				try {
					context.write(null, new Text(iter+"\t"+(endOfOneIteration-beginTime)+"\t"+logLikelihood));
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	
			}
			
			if(Math.abs((lastLogLikelihood - logLikelihood) / lastLogLikelihood) <= Constants.LOG_LIKELIHOOD_THRESHOLD) {
				break;
			}
			lastLogLikelihood = logLikelihood;
		}
		
		trainingCompute.stop();
	}
	
	 
	
	/**
	 * 
	 * @return LogLikelihood
	 */
	private double oneIteration(int iterationID, DynamicScheduler<OneDoc, TrainingTaskOutput, TrainingTask> trainingCompute){
		
		//iterations; (can be multi-threading)
		// for all d in docs:
		//     for all v in terms:
		//         for all k in topics:
		//             update phi(v,k) = beta_{v,k} * exp( digamma( gamma_{d,k} ));
		//         end for
		//
		//         normalize row phi_v,*, such that sum_over_k(phi_{v,k}) = 1;
		//
		//         update sigma = sigma + w_{v} * phi_v
		//     end for
		//     update row vector gamma_{d,*} = alpha + sigma
		//  *
		//  *
		//  *
		// end for
		System.out.println("iteration: "+ iterationID);
		double[] LogLikelihood = logLikelihoodTable.getPartition(0).get().get();
		LogLikelihood[0] = 0;
		double[] totalAlphaSufficientStatistics = totalAlphaSufficientStatisticsTable.getPartition(0).get().get();
		localLogPhiTable.release();

		// for docs in this mapper
		trainingCompute.submitAll(docList);
		
		TrainingTaskOutput output;
		int cnt=0;
		while (trainingCompute.hasOutput()) {
			output = trainingCompute.waitForOutput();
			if(output != null){
			    cnt++;
	    		//get output results; 
				Table<DoubleArray> logPhiTable = output.logPhiTable;
				
				double[] alphaSufficientStatistics = output.alphaSufficientStatistics;
				LogLikelihood[0] += output.likelihood;
				
				 //aggregate logphitable
				double[] logPhi;
				
			    double[] tempLogPhi = null;
			    for(int termID: logPhiTable.getPartitionIDs()){
			    	logPhi = logPhiTable.getPartition(termID).get().get();
			    	if( localLogPhiTable.getPartition(termID) == null){
			    		tempLogPhi = new double[numOfTopics];
			    		for(int i=0; i<numOfTopics; i++){
			    			tempLogPhi[i]=Double.NEGATIVE_INFINITY;
						}
						Partition<DoubleArray> phiPartition = new Partition<DoubleArray>(termID, new DoubleArray(tempLogPhi, 0, numOfTopics));
						localLogPhiTable.addPartition(phiPartition);
					}else{
						tempLogPhi = localLogPhiTable.getPartition(termID).get().get();
					}
			    	
			    	for(int k =0; k < numOfTopics; k++){
			        	tempLogPhi[k] = Utils.logAdd(tempLogPhi[k],logPhi[k]);
			   		}
			    }
			    
			    if(cnt == 1){
			    	for (int i = 0; i < numOfTopics; i++) {
			    		totalAlphaSufficientStatistics[i] =alphaSufficientStatistics[i];
			    	}
			    }else{
			    	for (int i = 0; i < numOfTopics; i++) {
			    		totalAlphaSufficientStatistics[i] += alphaSufficientStatistics[i];
			    	}
			    }
	    	}
	    }
		System.out.println("before push");
		//sync
		globalLogPhiTable.release();
		push("LDA_log_phi_table", "push_log_phi_table_"+iterationID, localLogPhiTable, globalLogPhiTable , new Partitioner(this.getNumWorkers()));
	 
		// update logBeta;  from localLogPhiTable
		double[] logNormalizer = logNormalizerTable.getPartition(0).get().get();
		for(int i=0; i<numOfTopics; i++){
			logNormalizer[i] = Double.NEGATIVE_INFINITY;
		}
		double[] tempLogPhi = null;
		for(Partition<DoubleArray> par : globalLogPhiTable.getPartitions()){// this is termID
			tempLogPhi = par.get().get();
			for(int i=0; i<numOfTopics; i++){
				logNormalizer[i] = Utils.logAdd(logNormalizer[i], tempLogPhi[i]);
			}
		} 
		System.out.println("after push; before allreduce");
		allreduce("LDA_log_normalizer_table", "allreudce_log_normalizer_"+iterationID, logNormalizerTable);
		logNormalizer = logNormalizerTable.getPartition(0).get().get();
		
		for(Partition<DoubleArray> par : globalLogPhiTable.getPartitions()){// this is termID
			tempLogPhi = par.get().get();
			for(int i = 0; i < numOfTopics; i++){
				tempLogPhi[i] -=  logNormalizer[i];
			}
		} 
		
		//pull operation is incremental, but ids need to be kept.
		//here use another table with different combiner, to get data from global table.
		localLogPhiTableToReceiveGlobal = new Table<>(0, new DoubleArrGet());
		for(Partition<DoubleArray> par : localLogPhiTable.getPartitions()){
			localLogPhiTableToReceiveGlobal.addPartition(par);
		}
		System.out.println("after allreduce; before pull");
		pull("LDA_log_phi_table", "pull_log_phi_table_"+iterationID, localLogPhiTableToReceiveGlobal, globalLogPhiTable, true);
		
		logBeta.release();
		double[] tempLogBeta = null;
		for(Partition<DoubleArray> par : localLogPhiTable.getPartitions()){
			if(logBeta.getPartition(par.id())==null){
				tempLogBeta = new double[numOfTopics];
				Partition<DoubleArray> ap = new Partition<DoubleArray>(par.id(), new DoubleArray(tempLogBeta, 0, numOfTopics));
				logBeta.addPartition(ap);
			}else{
				tempLogBeta = logBeta.getPartition(par.id()).get().get();
			}
			double[] localLogPhiPar = par.get().get();
			for(int i=0; i<numOfTopics; i++){
				tempLogBeta[i] = localLogPhiPar[i];
			}
		}
		System.out.println("after pull; before allreduce");
	/*	System.out.println("logBeta after normalization");
		Utils.printDoubleTable(logBeta);
		*/
		//allreduce totalAlphaSufficientStatistics
		allreduce("LDA_alpha_sufficient_table", "allreduce_alpha_sufficient_table_"+iterationID, totalAlphaSufficientStatisticsTable);
		// update alpha
		double[] alphaSufficientStatistics = totalAlphaSufficientStatisticsTable.getPartition(0).get().get();
		double[] alphaVector = alphaTable.getPartition(0).get().get();
		//will new double[]; so put the double array to table
		alphaVector = updateVectorAlpha(numOfTopics, numOfDocs, alphaVector, alphaSufficientStatistics);
		alphaTable.release();
		Partition<DoubleArray> alphaPartition = new Partition<DoubleArray>(0, new DoubleArray(alphaVector, 0, numOfTopics));
		alphaTable.addPartition(alphaPartition);
		/*System.out.println("print alpha_table");
		Utils.printDoubleTable(alphaTable);
		*/
		System.out.println("after allreduce alpha; before allreduce loglikelihood");
		//allreduce loglikelihood
		allreduce("LDA_log_likelihood_table", "allreduce_log_likelihood_table_"+iterationID, logLikelihoodTable);
		System.out.println("after allreduce");
		return LogLikelihood[0];
	}
	
	 /**
	   * This method updates the hyper-parameter alpha vector of the topic Dirichlet prior, which is an
	   * asymmetric Dirichlet prior.
	   * 
	   * @param numberOfTopics the number of topics
	   * @param numberOfDocuments the number of documents in this corpus
	   * @param alphaVector the current alpha vector
	   * @param alphaSufficientStatistics the alpha sufficient statistics collected from the corpus
	   * @return
	   */
	  public double[] updateVectorAlpha(int numberOfTopics, int numberOfDocuments, 
			  double[] alphaVector, double[] alphaSufficientStatistics) {
		  double[] alphaVectorUpdate = new double[numberOfTopics];
		  double[] alphaGradientVector = new double[numberOfTopics];
		  double[] alphaHessianVector = new double[numberOfTopics];

		  int alphaUpdateIterationCount = 0;

	    // update the alpha vector until converge
	    boolean keepGoing = true;
	    try {
	      int decay = 0;

	      double alphaSum = 0;
	      for (int j = 0; j < numberOfTopics; j++) {
	        alphaSum += alphaVector[j];
	      }

	      while (keepGoing) {
	        double sumG_H = 0;
	        double sum1_H = 0;

	        for (int i = 0; i < numberOfTopics; i++) {
	          // compute alphaGradient
	          alphaGradientVector[i] = numberOfDocuments
	              * (Gamma.digamma(alphaSum) - Gamma.digamma(alphaVector[i]))
	              + alphaSufficientStatistics[i];

	          // compute alphaHessian
	          alphaHessianVector[i] = -numberOfDocuments * Gamma.trigamma(alphaVector[i]);

	          if (alphaGradientVector[i] == Double.POSITIVE_INFINITY
	              || alphaGradientVector[i] == Double.NEGATIVE_INFINITY) {
	            throw new ArithmeticException("Invalid ALPHA gradient matrix...");
	          }

	          sumG_H += alphaGradientVector[i] / alphaHessianVector[i];
	          sum1_H += 1 / alphaHessianVector[i];
	        }

	        double z = numberOfDocuments * Gamma.trigamma(alphaSum);
	        double c = sumG_H / (1 / z + sum1_H);

	        while (true) {
	          boolean singularHessian = false;

	          for (int i = 0; i < numberOfTopics; i++) {
	            double stepSize = Math.pow(Constants.DEFAULT_ALPHA_UPDATE_DECAY_FACTOR, decay)
	                * (alphaGradientVector[i] - c) / alphaHessianVector[i];
	            if (alphaVector[i] <= stepSize) {
	              // the current hessian matrix is singular
	              singularHessian = true;
	              break;
	            }
	            alphaVectorUpdate[i] = alphaVector[i] - stepSize;
	          }

	          if (singularHessian) {
	            // we need to further reduce the step size
	            decay++;

	            // recover the old alpha vector
	            alphaVectorUpdate = alphaVector;
	            if (decay > Constants.DEFAULT_ALPHA_UPDATE_MAXIMUM_DECAY) {
	              break;
	            }
	          } else {
	            // we have successfully update the alpha vector
	            break;
	          }
	        }

	        // compute the alpha sum and check for alpha converge
	        alphaSum = 0;
	        keepGoing = false;
	        for (int j = 0; j < numberOfTopics; j++) {
	          alphaSum += alphaVectorUpdate[j];
	          if (Math.abs((alphaVectorUpdate[j] - alphaVector[j]) / alphaVector[j]) >= Constants.DEFAULT_ALPHA_UPDATE_CONVERGE_THRESHOLD) {
	            keepGoing = true;
	          }
	        }

	        if (alphaUpdateIterationCount >= Constants.DEFAULT_ALPHA_UPDATE_MAXIMUM_ITERATION) {
	          keepGoing = false;
	        }

	        if (decay > Constants.DEFAULT_ALPHA_UPDATE_MAXIMUM_DECAY) {
	          break;
	        }

	        alphaUpdateIterationCount++;
	        alphaVector = alphaVectorUpdate;
	      }
	    } catch (IllegalArgumentException iae) {
	      System.err.println(iae.getMessage());
	      iae.printStackTrace();
	    } catch (ArithmeticException ae) {
	      System.err.println(ae.getMessage());
	      ae.printStackTrace();
	    }

	    return alphaVector;
	  }

	
	
	/** 
	 * And the dataset should come with a metadata file recording the beginning index of the docs.
	 * For example, we have a dataset of 10 docs, and we split it into two files: sample_data_part_0, sample_data_part_1
	 * Each file contains 5 docs.
	 * So the metadata is:
	 * sample_data_part_0 0
	 * sample_data_part_0 6
	 *
	 */
	private void loadData(List<OneDoc> docList, HashSet<Integer> wordIdListInThisMapper, List<String> fileNames, Configuration conf) throws IOException{
		//get metadata
		HashMap<String, Integer> metaMap = new HashMap<String, Integer>();
		FileSystem fs = FileSystem.get(conf);
		Path metafilepath = new Path(metafile);
	    FSDataInputStream in = fs.open(metafilepath);
	    BufferedReader br = new BufferedReader( new InputStreamReader(in));
	    String line="";
	    while((line = br.readLine()) != null){
	    	String[] str = line.split("\\s+");
	    	if(str.length != 2){
	    		throw new IOException();
	    	}
	    	metaMap.put(str[0], Integer.parseInt(str[1]));
	    }
	    
	  /*  for (Map.Entry<String, Integer> entry : metaMap.entrySet()) {  
	        System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());  
	    } */ 
		
		
		for(String filename: fileNames){
			Path dPath = new Path(filename);
			in = fs.open(dPath);
			br = new BufferedReader( new InputStreamReader(in));
			line="";
			String[] vector=null;
			System.out.println("path name: "+dPath.getName());
			System.out.println("file name: "+fileNames);
			if( !metaMap.containsKey(dPath.getName( )) ){
				throw new IOException();
			}
			int docId = metaMap.get(dPath.getName());
			while((line = br.readLine()) != null){
				
				OneDoc adoc = new OneDoc();
				
				vector = line.split("\\s+");
				adoc.docId = docId;
				adoc.numOfWords = vector.length;

				for(int i=0; i<vector.length; i++){
					String[] pair = vector[i].split(":");
					int wordId = Integer.parseInt(pair[0]);
					int occurrence = Integer.parseInt(pair[1]);
					adoc.idList.add(wordId);
					adoc.occurrenceList.add(occurrence);
					adoc.numOfTokens += occurrence ;
					
					if(!wordIdListInThisMapper.contains(wordId)){
						wordIdListInThisMapper.add(wordId);
					}
					
				}
				docList.add(adoc);
				docId ++;
			}
		}
	}		
	
	 
	 public static double updatePhi(int numberOfTopics, int termCounts, double[] logBeta,
		      double[] digammaGamma, double[] logPhi, double[] updateLogGamma) {
		    double convergePhi = 0;

		    // initialize the normalize factor and the phi vector
		    // phi is initialized in log scale
		    logPhi[0] = (logBeta[0] + digammaGamma[0]);
		    double normalizeFactor = logPhi[0];

		    // compute the K-dimensional vector phi iteratively
		    for (int i = 1; i < numberOfTopics; i++) {
		      logPhi[i] = (logBeta[i] + digammaGamma[i]);
		      normalizeFactor = Utils.logAdd(normalizeFactor, logPhi[i]);
		    }

		    for (int i = 0; i < numberOfTopics; i++) {
		      // normalize the K-dimensional vector phi scale the
		      // K-dimensional vector phi with the term count
		      logPhi[i] -= normalizeFactor;
		      convergePhi += termCounts * Math.exp(logPhi[i]) * (logBeta[i] - logPhi[i]);
		      logPhi[i] += Math.log(termCounts);

		      // update the K-dimensional vector gamma with phi
		      updateLogGamma[i] = Utils.logAdd(updateLogGamma[i], logPhi[i]);
		    }

		    return convergePhi;
	 }
	  
}


