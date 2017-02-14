#50 trees
HADOOP_CLIENT_OPTS="-XX:-UseGCOverheadLimit -Xmx29496m" hadoop jar harp3-app-hadoop-2.6.0.jar com.rf.fast.RandomForestMapCollective data loc_ data/test.csv 1 /randomForest /scratch/rakkumar/tmp/randomForestGlobal50 50 2>&1 1>outputs/output-global50 
#hadoop jar harp3-app-hadoop-2.6.0.jar com.rf.fast.RandomForestMapCollective data loc_ data/test.csv 0 /randomForest /scratch/rakkumar/tmp/randomForestLabelDist50 50 2>&1 1>outputs/output-labelDist50

#100 trees
#hadoop jar harp3-app-hadoop-2.6.0.jar com.rf.fast.RandomForestMapCollective data loc_ data/test.csv 1 /randomForest /scratch/rakkumar/tmp/randomForestGlobal100 100 2>&1 1>outputs/output-global100
#hadoop jar harp3-app-hadoop-2.6.0.jar com.rf.fast.RandomForestMapCollective data loc_ data/test.csv 0 /randomForest /scratch/rakkumar/tmp/randomForestLabelDist100 100 2>&1 1>outputs/output-labelDist100

#200 trees
#hadoop jar harp3-app-hadoop-2.6.0.jar com.rf.fast.RandomForestMapCollective data loc_ data/test.csv 1 /randomForest /scratch/rakkumar/tmp/randomForestGlobal200 200 2>&1 1>outputs/output-global200
#hadoop jar harp3-app-hadoop-2.6.0.jar com.rf.fast.RandomForestMapCollective data loc_ data/test.csv 0 /randomForest /scratch/rakkumar/tmp/randomForestLabelDist200 200 2>&1 1>outputs/output-labelDist200 
