package edu.iu.lda;

import java.util.ArrayList;
import java.util.List;

import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.resource.IntArray;
import edu.iu.harp.schdynamic.Task;
import edu.umd.cloud9.math.Gamma;


class TrainingTaskOutput{
	int docId;
	double[] alphaSufficientStatistics;
	double likelihood;
	int numOfUniqueWords;
	Table<DoubleArray> logPhiTable = null;
	
	public TrainingTaskOutput(int docId, int numOfUniqueWords, int numOfTopics){
		alphaSufficientStatistics = new double[numOfTopics];
		this.logPhiTable =  new Table<>(0, new DoubleArrLogPlus());
		this.likelihood = 0.0;
		this.numOfUniqueWords = numOfUniqueWords;
		this.docId = docId;
	}
}
class OneDoc{
	List<Integer> idList;
	List<Integer> occurrenceList;
	int docId;
	int numOfWords;//number of unique words in this doc
	int numOfTokens;// number of total tokens in this doc
	public OneDoc(int docId, List<Integer> idList, List<Integer> occurrenceList, int numOfWords, int numOfTokens){
		this.idList = idList;
		this.occurrenceList = occurrenceList;
		this.docId = docId;
		this.numOfWords = numOfWords;
		this.numOfTokens = numOfTokens;
	}
	public OneDoc(){
		this.idList = new ArrayList<Integer>();
		this.occurrenceList =  new ArrayList<Integer>();
		this.docId= -1;
		this.numOfTokens=0;
		this.numOfTokens=0;
	}
	
}

public class TrainingTask implements Task<OneDoc, TrainingTaskOutput> {

	int numOfTopics;
	Table<DoubleArray> alphaTable;
	Table<DoubleArray> logPhiTable = null;
	Table<DoubleArray> gammaTable = null;
	Table<DoubleArray> logBeta = null;
	double[] alphaSufficientStatistics;;
	public TrainingTask(int nTopics, Table<DoubleArray> alphaTable, Table<DoubleArray> logBeta ){
		this.numOfTopics = nTopics;
		this.alphaTable = alphaTable;
		this.logBeta = logBeta;
		this.gammaTable = new Table<>(0, new DoubleArrPlus());
	}
	
	@Override
	public TrainingTaskOutput run(OneDoc doc) throws Exception {

		List<Integer> wordIdIntheDoc = doc.idList;
	//	System.out.println("in task: "+doc.docId);
		
		TrainingTaskOutput output = new TrainingTaskOutput(doc.docId, doc.numOfWords , numOfTopics);
		alphaSufficientStatistics = output.alphaSufficientStatistics;
		this.logPhiTable = output.logPhiTable;
		  
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
		if(gammaTable.getPartition(doc.docId) == null){
			tempGamma = new double[numOfTopics];
			Partition<DoubleArray> gammaPartition = new Partition<DoubleArray>(doc.docId, new DoubleArray(tempGamma, 0, numOfTopics));
			gammaTable.addPartition(gammaPartition);
		}else{
			tempGamma = gammaTable.getPartition(doc.docId).get().get();
		}
		
		//initialize gamma
	    for (int i = 0; i < numOfTopics; i++) {
	        tempGamma[i] = alpha[i] + 1.0f * doc.numOfTokens / numOfTopics;
	    }
		
	    double[] logPhi = null;
	    double[] updateLogGamma = new double[numOfTopics];
	    double likelihoodPhiInOneDoc = 0;
	    int gammaUpdateIterationCount = 1;
	    double[] tempLogBeta;
	    do {
	    	likelihoodPhiInOneDoc = 0;
	    	 for (int i = 0; i < numOfTopics; i++) {
	    	        tempGamma[i] = Gamma.digamma(tempGamma[i]);
	    	        updateLogGamma[i] = Math.log(alpha[i]);
	    	 }
	    	
	    	 for(int t=0; t < wordIdIntheDoc.size(); t++){
	    		 int termID = wordIdIntheDoc.get(t);
	    		 // acquire the corresponding beta vector for this term
	    	        if (logPhiTable.getPartition(termID) != null) {
	    	          //    existing object
	    	          logPhi = logPhiTable.getPartition(termID).get().get();
	    	        } else {
	    	          logPhi = new double[numOfTopics];
	    	          Partition<DoubleArray> logPhiPartition = new Partition<DoubleArray>(termID, new DoubleArray(logPhi, 0, numOfTopics));
	    	          logPhiTable.addPartition(logPhiPartition);
	    	        }

	    	        int termCounts = doc.occurrenceList.get(t);
	    	        
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

	    output.likelihood = documentLogLikelihood;
	    
	    double digammaSumGamma = Gamma.digamma(sumGamma);
	    
	    for (int i = 0; i < numOfTopics; i++) {
	   		alphaSufficientStatistics[i] = Gamma.digamma(tempGamma[i]) - digammaSumGamma;
	   	}
	    
		return output;
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
