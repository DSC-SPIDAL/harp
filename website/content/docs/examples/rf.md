---
title: Harp Random Forests
---


<img src="/img/4-5-1.png" width="60%"  >

Random forests are an ensemble learning method for classification, regression and other tasks that operate by constructing a multitude of decision trees at training time and outputting the class that is the mode of the classes (classification) or mean prediction (regression) of the individual trees. Random decision forests correct for decision trees' habit of overfitting to their training set.

The training algorithm for random forests applies the general technique of bootstrap aggregating, or bagging, to tree learners. Given a training set `X` with responses `Y`, bagging repeatedly selects a random sample with replacement of the training set. After training, predictions for unseen samples can be made by averaging the predictions from all the individual regression trees or by taking the majority vote in the case of decision trees. Random forests can also do another bagging -- feature bagging, which is a random subset of the features. The reason for doing this is the correlation of the trees in an ordinary bootstrap sample: if one or a few features are very strong predictors for the response variable (target output), these features will be selected in many of the B trees, causing them to become correlated.
 

## DEFINITION

* `N` is the number of data points
* `M` is the number of selected features
* `K` is the number of trees

## METHOD

The following is the procedure of Harp Random Forests training:

1. Randomly select `N` samples with replacement for `K` trees (totally `K` times).

2. Select `M` features randomly which will be used in decision tree.

3. Build decision tree based on these `M` features

4. Repeat Step 2 and 3 `K` times.

The predicting part will use these decision trees to predict `K` results. For regression, the result is the average of each tree's result and for classification, the majority vote will be the output.

## Step 0 --- Data preprocessing

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

## Step 1 ---Train forests
```Java
// Create the Random Forest classifier that uses 5 neighbors to make decisions
Classifier rf = new RandomForest(numTrees, false, numAttributes, new Random());
// Learn the forest
rf.buildClassifier(trainDataPoints);
```

## Step 2 --- Synchronize majority vote
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
