---
title: Harp Random Forests
---


<img src="/img/4-5-1.png" width="60%"  >

&nbsp;&nbsp;&nbsp;&nbsp; Random forests are an example of ensemble learning method called Bagging (Bootstrap Aggregation). Bagging is an ensemble technique which works based on the idea of wisdom of crowds. In this technique, multiple individual classifiers are built and these individual predictions are combined to produce a final output. Even if the individual classifier are weak i.e., slightly better than random (accuracy > 50%), bagging classifiers work when the individual classifiers are independent of each other. In case of classification, Bagged classifier votes among the individual classifiers and chooses its prediction as the mode of the predictions (one which occurs most of the times) and for regression problem, the mean of all the predictions is usually taken. 

&nbsp;&nbsp;&nbsp;&nbsp;Random forest is one such implementation of bagging in which individual classifiers are decision trees. Decision trees are easily interpretable model with great prediction power. It works as a set of if-else decision rules. Since decision tree has inbuilt feature selection present in the algorithm which selects the best features online, it is a favourable first choice when the data set has large number of features. There are various implementation of bagging decision trees. The problem with bagging decision trees is that, there is a possibility that the individual decision trees can choose the same features while making split and hence they can loose their independence property and become correlated. To avoid this, Random forest model implementation randomly selects a subset of the features and chooses the best among those subset of features to make the split at that level. 

&nbsp;&nbsp;&nbsp;&nbsp; Since the model building part consists of building multiple decision trees, we could make use of parallel implementation architecture of Harp to improve upon the efficiency of the algorithm's runtime and resource utilization. The training algorithm for random forests applies the general technique of bootstrap aggregation (bagging) to tree learners. Given a training set `X` with responses `Y`, bagging repeatedly selects a random sample with replacement of the training set. This bootstrap training data is used to build the decision tree in every mapper (node). Each node builds multiple decision trees based on the number of trees to be built per node which is specified by the user. Since there is no dependency between individual trees (as they are independent), there is no need of communication required between each nodes or the model data. 

&nbsp;&nbsp;&nbsp;&nbsp; After building multiple decision trees in each mapper, the test data is broadcasted to all the nodes where each decision trees makes their own set of predictions. These predictions are combined using `allreduce` collective communication method. The final prediction is done by taking average of all the predictions in case of regression trees and by taking majority vote in case of classification trees. As discussed earlier, random forest performs by using feature bagging, where at each split of decision tree random subset of features are selected and the best among them is chosen as split point. This helps in reducing the correlation between the trees and provides independent classifiers.  

### Parameter Definition
1. `N` - Number of data points
2. `M` - Number of randomly selected features
3. `K` - Number of individual trees to build


### Method
The following is the procedure adapted for implementing Random forest algorithm using Harp 
1. Randomly select `N` data points with replacement for `K` trees. This has to be done `K` times to ensure that each sample is independent of each other
2. Select `M` features randomly at each level from which the best feature will be used to build the decision tree
3. Build decision tree using the subset of features. Trees can be stopped at particular depth to avoid overfitting
4. Repeat steps 2 and 3 `K` times. 

Since the entire process works on building the trees `K` times, this can be parallelized using Harp. Below code explains how Random forests have been implemented using Harp AllReduce communication pattern. The complete code is available in GitHub.

### Step 0 --- Data preprocessing

```Java
// In the case of merging the data from different location, main process needs to create bootstrap samples.
// So here, the main process loads all the data and creates the bootstrap samples.
if(doBootstrapSampling) {                                     
    //Load each of the training files under train folder                
    for(i = 0; i < RandomForestConstants.NUM_GLOBAL; i++) {
        files.add(trainFileFolder + File.separator + trainNameFormat + i + ".csv");
    }
    System.out.println("Loading the training data...");
    loadData(trainData,files);
    System.out.println("Data size: " + trainData.size());
    files.clear();
    ArrayList<Integer> positions;
    //Create the bootstrapped samples and write to local disk
    for(i = 0; i < RandomForestConstants.NUM_MAPPERS; i ++) {
        positions = DoBootStrapSampling(trainData.size());
        System.out.println("Sampled data for: "+ i +"; size: " + positions.size());
        createMapperFiles(trainData,testData,fs,localDirStr,i,positions);
        positions = null;
    }
} else {
    //Load each of the training files under train folder                
    for(i = 0; i < RandomForestConstants.NUM_MAPPERS; i ++){
        System.out.println("Loading the training data for " + i + "...");
        files.add(trainFileFolder+File.separator+trainNameFormat+i+".csv");
        loadData(trainData,files);
        System.out.println("Unsampled data for: "+ i +"; size: " + trainData.size());
        createMapperFiles(trainData,testData,fs,localDirStr,i,null);
        files.clear();
        trainData.clear();
    }
} 
```

### Step 1 ---Train forests
```Java
// Create the Random Forest classifier that uses 5 neighbors to make decisions
Classifier rf = new RandomForest(numTrees, false, numAttributes, new Random());
// Learn the forest
rf.buildClassifier(trainDataPoints);
```

### Step 2 --- Synchronize majority vote
```Java
for (Instance inst : testDataPoints) {
    // This will hold this random forest's class vote for this data point
    //IntArray votes = IntArray.create(C, false);
    IntArray votes = IntArray.create(RandomForestConstants.NUM_CLASSES, false);
    // Get the prediction class from this Random Forest
    Object predictedClassValue = rf.classify(inst);
    // Get the true value
    Object realClassValue = inst.classValue();
    //int predIndex = Integer.parseInt(preds.get(dp));
    // Check which class was predicted
    for (int i = 0; i < RandomForestConstants.NUM_CLASSES; i++) {
        // Check to see if this index matches the class that was predicted
        if (predictedClassValue.equals(Integer.toString(i))) {
            // log.info("i: " + i + "; predictedClassValue: " + predictedClassValue + "; condition: " + predictedClassValue.equals(Integer.toString(i)));
            votes.get()[i] = 1;
        } else {
            votes.get()[i] = 0;
        }
    }
    // Add the voting results to the partition
    Partition<IntArray> dpP = new Partition<IntArray>(dp, votes);
    predTable.addPartition(dpP);
    // Move onto the next data point
    dp++;
}
log.info("Done populating predTable\n");
// All Reduce from all Mappers
log.info("Before allreduce!!!!");
allreduce("main", "allreduce", predTable);
log.info("After allreduce!!!!");
```
