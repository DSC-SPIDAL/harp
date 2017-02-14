package edu.iu.lda;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.CollectiveMapper;

import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.example.IntArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.resource.IntArray;

import edu.umd.cloud9.math.Gamma;

/**
 * The number of mappers will be the same with numOfTopics
 * Every mapper task deals with one topic.
 * The topic it associated is the id of the mapper, which is this.getSelfID()
 *
 */
public class LDAMapper extends CollectiveMapper<String, String, Object, Object> {
	
	int numOfTerms;
	int numOfTopics;
	int numOfDocs;
	int numOfIterations;
	String metafile;
	Table<DoubleArray> alphaTable;
	Table<IntArray> dataTable ;
	Table<DoubleArray> globalLogPhiTable = null;
	Table<DoubleArray> logPhiTable = null;
	Table<DoubleArray> totalAlphaSufficientStatisticsTable = null;
	double[] logNormalizer ;
	
	Table<DoubleArray> logBeta = null;
	double[] tempLogBeta = null;
	
	Table<DoubleArray> gammaTable = null;
	
	Table<DoubleArray> logLikelihoodTable = null;
	
	
	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		
		Configuration configuration = context.getConfiguration();
		numOfTerms =configuration.getInt(Constants.NUM_OF_TERMS, Integer.MAX_VALUE);
		numOfTopics =configuration.getInt(Constants.NUM_OF_TOPICS, 20);
		numOfDocs =configuration.getInt(Constants.NUM_OF_DOCS, 20);
		numOfIterations = configuration.getInt(Constants.NUM_OF_ITERATIONS, 1);
		metafile = configuration.get(Constants.META_DATA_FILE_NAME,"metadata");
	}
	
	@Override
	protected void mapCollective( KeyValReader reader, Context context) throws IOException, InterruptedException {
	
		List<String> docFiles = new ArrayList<String>();
	    while (reader.nextKeyValue()) {
	    	String value = reader.getCurrentValue();
	    	docFiles.add(value);
	    }
	    Configuration conf = context.getConfiguration();
		runLDA(docFiles, conf, context);
	}

	private void runLDA(List<String> docFiles, Configuration conf, Context context) throws IOException {
		//load files
		dataTable = new Table<>(0, new IntArrPlus());
		loadData(dataTable, docFiles, numOfTerms, conf);
		Utils.printIntTable(dataTable);
		
		
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
		for(int termIndex=0; termIndex < numOfTerms; termIndex++){
			double[] tempBeta = new double[numOfTopics];
			for (int i = 0; i < numOfTopics; i++) {
				//tempBeta[i] = Math.log(2 * Math.random() / numOfTerms + Math.random());
				tempBeta[i]=0.1;
				if(termIndex == 0)
				{
					logNorm[i]  =  tempBeta[i];
				}else
				logNorm[i] = Utils.logAdd(logNorm[i] , tempBeta[i]);
			}
			Partition<DoubleArray> termPartition = new Partition<DoubleArray>(termIndex, new DoubleArray(tempBeta, 0, numOfTopics));
			logBeta.addPartition(termPartition);
		}
		for(int termIndex=0; termIndex < numOfTerms; termIndex++){
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
		
		gammaTable = new Table<>(0, new DoubleArrPlus());
		globalLogPhiTable = new Table<>(0, new DoubleArrLogPlus());
		logPhiTable = new Table<>(0, new DoubleArrLogPlus());
		
		logLikelihoodTable = new Table<>(0, new DoubleArrPlus());
		Partition<DoubleArray> logLikelihoodPartition = new Partition<DoubleArray>(0, new DoubleArray(new double[1], 0, 1));
		logLikelihoodTable.addPartition(logLikelihoodPartition);
		
		//init logNormalizer
		logNormalizer= new double[numOfTopics];
		
		//iterations 
		double lastLogLikelihood = 0.0;
		for(int iter = 0; iter < numOfIterations; iter++){
			 
			double logLikelihood = oneIteration(iter);
			System.out.println("logLikelihood: " + logLikelihood);
			
			if(Math.abs((lastLogLikelihood - logLikelihood) / lastLogLikelihood) <= Constants.LOG_LIKELIHOOD_THRESHOLD) {
				break;
			}
			lastLogLikelihood = logLikelihood;
		}
	}
	
	/**
	 * 
	 * @return LogLikelihood
	 */
	private double oneIteration(int iterationID){
		
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
		
		int cnt = 0;
		for(Partition<IntArray> ap : dataTable.getPartitions()){// for docs in this mapper
			cnt ++;
			int[] theDoc = ap.get().get();
			//System.out.println("now deal with doc_" + ap.id());
			
			//likelihoodAlpha
			double[] alpha = alphaTable.getPartition(0).get().get();
			double alphaLnGammaSum = 0;
			double alphaSum = 0;
			for (int i = 0; i < numOfTopics; i++) {
				alphaSum += alpha[i];
				alphaLnGammaSum += Gamma.lngamma(alpha[i]);
			}
			double likelihoodAlphaInOneDoc = Gamma.lngamma(alphaSum) - alphaLnGammaSum;
			
			//update gamma and phi
			
			//reuse gamma vectors
			double[] tempGamma = null;
			if(gammaTable.getPartition(ap.id()) == null){
				tempGamma = new double[numOfTopics];
				Partition<DoubleArray> gammaPartition = new Partition<DoubleArray>(ap.id(), new DoubleArray(tempGamma, 0, numOfTopics));
				gammaTable.addPartition(gammaPartition);
			}else{
				tempGamma = gammaTable.getPartition(ap.id()).get().get();
			}
			
			//initialize gamma
			int sumOfWords = 0;
			for(int i=0; i <  theDoc.length; i++){
				sumOfWords += theDoc[i];
			}
		    for (int i = 0; i < numOfTopics; i++) {
		        tempGamma[i] = alpha[i] + 1.0f * sumOfWords / numOfTopics;
		    }
			

		    
		    double[] logPhi = null;
		    double[] updateLogGamma = new double[numOfTopics];
		    double likelihoodPhiInOneDoc = 0;
		    int gammaUpdateIterationCount = 1;
		    do {
		    	likelihoodPhiInOneDoc = 0;
		    	
		    	 for (int i = 0; i < numOfTopics; i++) {
		    	        tempGamma[i] = Gamma.digamma(tempGamma[i]);
		    	        updateLogGamma[i] = Math.log(alpha[i]);
		    	 }
		    	
		    	 for(int termID=0; termID < theDoc.length; termID++){
		    		 // acquire the corresponding beta vector for this term
		    	        if (logPhiTable.getPartition(termID) != null) {
		    	          //    existing object
		    	          logPhi = logPhiTable.getPartition(termID).get().get();
		    	        } else {
		    	          logPhi = new double[numOfTopics];
		    	          Partition<DoubleArray> logPhiPartition = new Partition<DoubleArray>(termID, new DoubleArray(logPhi, 0, numOfTopics));
		    	          logPhiTable.addPartition(logPhiPartition);
		    	        }

		    	        int termCounts = theDoc[termID];
		    	        
		    	        tempLogBeta = logBeta.getPartition(termID).get().get();
		    

		    	        likelihoodPhiInOneDoc += updatePhi(numOfTopics, termCounts, tempLogBeta, tempGamma, logPhi, updateLogGamma);
		    	        
		    	 }
		    	 for (int i = 0; i < numOfTopics; i++) {
		    	        tempGamma[i] = Math.exp(updateLogGamma[i]);
		    	 }
		    	 gammaUpdateIterationCount++;
		    } while (gammaUpdateIterationCount < Constants.MAX_GAMMA_ITERATIONS);
		    

		   
		    // compute the sum of gamma vector
		    double sumGamma = 0;
		    double likelihoodGammaInOneDoc = 0;
		    for (int i = 0; i < numOfTopics; i++) {
		      sumGamma += tempGamma[i];
		      likelihoodGammaInOneDoc += Gamma.lngamma(tempGamma[i]);
		    }
		    likelihoodGammaInOneDoc -= Gamma.lngamma(sumGamma);
		    double documentLogLikelihood = likelihoodAlphaInOneDoc + likelihoodGammaInOneDoc + likelihoodPhiInOneDoc;

		    LogLikelihood[0] += documentLogLikelihood;
		    
		    double digammaSumGamma = Gamma.digamma(sumGamma);
		    
		    if(cnt == 1){
		    	for (int i = 0; i < numOfTopics; i++) {
		    		totalAlphaSufficientStatistics[i] = Gamma.digamma(tempGamma[i]) - digammaSumGamma;
		    	}
		    }else{
		    	for (int i = 0; i < numOfTopics; i++) {
		    		totalAlphaSufficientStatistics[i] += Gamma.digamma(tempGamma[i]) - digammaSumGamma;
		    	}
		    }
		    
		    //aggregate logphitable
		    logPhi = null;
		    double[] tempLogPhi = null;
		    for(int termID=0; termID < theDoc.length; termID++){
		    	logPhi = logPhiTable.getPartition(termID).get().get();
		    	if( globalLogPhiTable.getPartition(termID) == null){
		    		tempLogPhi = new double[numOfTopics];
					Partition<DoubleArray> phiPartition = new Partition<DoubleArray>(termID, new DoubleArray(tempLogPhi, 0, numOfTopics));
					globalLogPhiTable.addPartition(phiPartition);
				}else{
					tempLogPhi = globalLogPhiTable.getPartition(termID).get().get();
				}
		    	if(cnt == 1){
		    		for(int k =0; k < numOfTopics; k++){
		    			tempLogPhi[k] = logPhi[k];
		    		}
		    	}else{
		    		for(int k =0; k < numOfTopics; k++){
		    	    	tempLogPhi[k] = Utils.logAdd(tempLogPhi[k],logPhi[k]);
		    		}
		    	}
		    }
		}// end for docs

		//allreduce globalLogPhiTable;
		allreduce("LDA_log_phi_table", "allreduce_log_phi_table_"+iterationID, globalLogPhiTable);
		


		// update logBeta;  from globalLogPhiTable
		
		for(int i=0; i<numOfTopics; i++){
			logNormalizer[i] = Double.NEGATIVE_INFINITY;
		}
		double[] tempLogPhi = null;
		double[] tempLogBeta = null;
		for(Partition<DoubleArray> par : globalLogPhiTable.getPartitions()){// this is termID
			tempLogBeta = logBeta.getPartition(par.id()).get().get();
			tempLogPhi = par.get().get();
			for(int i=0; i<numOfTopics; i++){
				tempLogBeta[i] = tempLogPhi[i];
				logNormalizer[i] = Utils.logAdd(logNormalizer[i], tempLogPhi[i]);
			}
		}

		
		for(Partition<DoubleArray> par : logBeta.getPartitions()){
			tempLogBeta =par.get().get();
			for(int i=0; i<numOfTopics; i++){
				tempLogBeta[i] -=  logNormalizer[i];
			}
		} 
		
		//allreduce totalAlphaSufficientStatistics.
		allreduce("LDA_alpha_sufficient_table", "allreduce_alpha_sufficient_table_"+iterationID, totalAlphaSufficientStatisticsTable);
		// update alpha
		double[] alphaSufficientStatistics = totalAlphaSufficientStatisticsTable.getPartition(0).get().get();
		double[] alphaVector = alphaTable.getPartition(0).get().get();
		//will new double[]; so put the double array to table
		alphaVector = updateVectorAlpha(numOfTopics, numOfDocs, alphaVector,
                 alphaSufficientStatistics);
		alphaTable.free();
		Partition<DoubleArray> alphaPartition = new Partition<DoubleArray>(0, new DoubleArray(alphaVector, 0, numOfTopics));
		alphaTable.addPartition(alphaPartition); 
		
		//allreduce loglikelihood
		allreduce("LDA_log_likelihood_table", "allreduce_log_likelihood_table_"+iterationID, logLikelihoodTable);
		
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
	private void loadData(Table<IntArray> data, List<String> fileNames,  int numOfTerms, Configuration conf) throws IOException{
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
	    
//	    for (Map.Entry<String, Integer> entry : metaMap.entrySet()) {  
//	        System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());  
//	    }  
//		
		
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
			int partitionId = metaMap.get(dPath.getName());
			while((line = br.readLine()) != null){
				vector = line.split("\\s+");
				if(vector.length != numOfTerms){
					throw new IOException();
				}else{
					int[] aDoc = new int[numOfTerms];
					for(int i=0; i<numOfTerms; i++){
						aDoc[i] = Integer.parseInt(vector[i]);
					}
					
					Partition<IntArray> ap = new Partition<IntArray>(partitionId, new IntArray(aDoc, 0, numOfTerms));
					data.addPartition(ap);
				}
				partitionId ++;
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


