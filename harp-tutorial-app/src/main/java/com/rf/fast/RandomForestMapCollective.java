package com.rf.fast;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.ArrayList;
import java.io.File;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;


import edu.iu.fileformat.MultiFileInputFormat;

public class RandomForestMapCollective  extends Configured implements Tool {

    public static void main(String[] argv) throws Exception {
        System.out.println("Random-Forest with Categorical support");
        int res = ToolRunner.run(new Configuration(), new RandomForestMapCollective(), argv);
        System.exit(res);
    }


    @Override
    public int run(String[] args) throws Exception {
        //keep this unchanged.
        if (args.length < 6) {
            System.err.println("Usage: RandomForestMapCollective <trainFolder> <trainNameFormat> <testFile> <doBootstrapSampling> <workDir> <localDir> <numTrees>");
            ToolRunner.printGenericCommandUsage(System.err);
                return -1;
        }

        /*
         * Generate data randomly - if needed
         * Configure jobs
         *   **for inputFormatClass: use job.setInputFormatClass(MultiFileInputFormat.class)
         * Launch jobs
         */

        String trainFileFolder = args[0];
        String trainNameFormat = args[1];
        String testFile = args[2];
        boolean doBootstrapSampling = false;
        if(Integer.parseInt(args[3]) == 1) {
            doBootstrapSampling = true;
        }

        // This is where the data is written in the distributed file system
        String workDir = args[4];
        // This is where the data is written locally before being transferred to the
        // distributed file system
        String localDir = args[5];

        //Get number of trees from path
        int numTrees = Integer.parseInt(args[6]);

        launch(trainFileFolder,trainNameFormat,testFile,doBootstrapSampling,workDir,localDir, numTrees);

        System.out.println("Harp RandomForest Completed");
        return 0;
    }

    void launch(String trainFileFolder, String trainNameFormat, String testFile, boolean doBootstrapSampling, String workDir,String localDirStr, int numTrees)
            throws IOException, URISyntaxException, InterruptedException, ExecutionException, ClassNotFoundException {

            Configuration configuration = getConf();

            //create a DescribeTrees object
            DescribeTrees DT = new DescribeTrees();
            System.out.println("Creating the data format representation...");
            ArrayList<Character> DataLayout = DT.CreateFinalLayout(RandomForestConstants.DATA_CONFIG);

            ArrayList<ArrayList<String>> trainSampleFile = null;
            int i;

            // Load the test data to write to each sample file
            System.out.println("Loading the test data...");
            ArrayList<ArrayList<String>> testData = DT.CreateInputCateg(RandomForestConstants.DATA_CONFIG, testFile);   
            System.out.println("Test data size: " + testData.size());

            //Create folders for data and out in workDir
            Path workDirPath = new Path(workDir);
            FileSystem fs = FileSystem.get(configuration);
            Path dataDir = new Path(workDirPath, "data");
            Path outDir = new Path(workDirPath, "out");
            if (fs.exists(outDir)) {
                fs.delete(outDir, true);
            }
            fs.mkdirs(outDir);

            System.out.println("Generate data.");
            //Main thread appends the test data to the all the input file (whether bootstrap sample true or false)
            // Check whether or not the data directory exists, so that main can
            if (fs.exists(dataDir)) {
                // It does, so delete
                // Want to start with new data, so need to clear out the data that is
                // already in the distributed file system
                fs.delete(dataDir, true);
            }
            // Check local directory
            File localDir = new File(localDirStr);
            // If existed, regenerate data. Generating new data, so need to remove the
            // data that is already associated with that location on the local machine
            if (localDir.exists() && localDir.isDirectory()) {
             for (File file : localDir.listFiles()) {
                 file.delete();
             }
             localDir.delete();
            }
            // Create a fresh version of the directory on the local machine
            boolean success = localDir.mkdir();
            if (success) {
             System.out.println("Directory: " + localDirStr + " created");
            }

            // In the case of merging the data from different location, main process needs to create bootstrap samples.
            // So here, the main process loads all the data and creates the bootstrap samples.
            if(doBootstrapSampling) {                                     

                //Load each of the training files under train folder
                String[] filePaths = new String[RandomForestConstants.NUM_MAPPERS];                
                for(i = 0; i < RandomForestConstants.NUM_MAPPERS; i++) {                    
                    filePaths[i] = trainFileFolder + File.separator + trainNameFormat + i + ".csv";   
                }
                System.out.println("Loading the training data...");
                ArrayList<ArrayList<String>> AllData = DT.CreateInputCateg(RandomForestConstants.DATA_CONFIG,filePaths);
                System.out.println("Data size: " + AllData.size());

                //Create the bootstrapped samples and write to local disk
                for(i = 0; i < RandomForestConstants.NUM_MAPPERS; i ++) {
                    trainSampleFile = DT.DoBootStrapSampling(AllData);
                    System.out.println("Sampled data for: "+ i +"; size: " + trainSampleFile.size());
                    DT.createMapperFiles(trainSampleFile,testData,fs,localDirStr,i);
                    trainSampleFile = null;
                }

                AllData = null;
            } else {
                //Load each of the training files under train folder                
                for(i = 0; i < RandomForestConstants.NUM_MAPPERS; i ++) {
                    System.out.println("Loading the training data for " + i + "...");
                    trainSampleFile = DT.CreateInputCateg(RandomForestConstants.DATA_CONFIG,trainFileFolder+File.separator+trainNameFormat+i+".csv");
                    System.out.println("Unsampled data for: "+ i +"; size: " + trainSampleFile.size());
                    DT.createMapperFiles(trainSampleFile,testData,fs,localDirStr,i);
                    trainSampleFile = null;
                }
            }                    

            // Create the local directory as a path object
            Path localPath = new Path(localDirStr);
            // Copy from the generated data from the locally written files to the
            // distributed file system
            fs.copyFromLocalFile(localPath, dataDir);

            trainSampleFile = null;

            // Start tracking the time so that the performance can be analzed
            long startTime = System.currentTimeMillis();

            runRandomForestAllReduce(configuration, workDirPath, dataDir, outDir, localDirStr, numTrees);

            long endTime = System.currentTimeMillis();
            System.out.println("Total Harp RandomForest Execution Time: "+ (endTime - startTime));
    }

    private void runRandomForestAllReduce(Configuration configuration,
            Path workDirPath, Path dataDir, Path outDir, String localDir, int numTrees) throws IOException,URISyntaxException, InterruptedException,ClassNotFoundException {

            System.out.println("Starting Job");
            boolean jobSuccess = true;
            int jobRetryCount = 0;

            do {
                // ----------------------------------------------------------------------
                Job randomForestJob = configureRandomForestJob(configuration, workDirPath, dataDir, outDir, numTrees);

                jobSuccess = randomForestJob.waitForCompletion(true);

                if (!jobSuccess) {
                    System.out.println("RandomForest Job failed. ");
                    jobRetryCount++;
                    if (jobRetryCount == 3) {
                        break;
                    }
                }else{
                    break;
                }
            } while (true);

            // Copy output from hdfs back to local file system
            // Create the local directory as a path object
            Path localPath = new Path(localDir);
            // Path hdfsPath = new Path(outDir+File.separator+RandomForestConstants.OUTPUT_FILE);
            Path hdfsPath = outDir;
            FileSystem.get(configuration).copyToLocalFile(hdfsPath, localPath);

    }

    private Job configureRandomForestJob(Configuration configuration,Path workDirPath, Path dataDir,
            Path outDir, int numTrees) throws IOException, URISyntaxException, InterruptedException, ClassNotFoundException {


            Job job = Job.getInstance(configuration, "randomForest_job");
            Configuration jobConfig = job.getConfiguration();
            Path jobOutDir = new Path(outDir, "randomForest_out");
            FileSystem fs = FileSystem.get(configuration);
            if (fs.exists(jobOutDir)) {
                fs.delete(jobOutDir, true);
            }
            FileInputFormat.setInputPaths(job, dataDir);
            FileOutputFormat.setOutputPath(job, jobOutDir);
            // FileOutputFormat.setOutputPath(job, outDir);            
            job.setInputFormatClass(MultiFileInputFormat.class);
            job.setJarByClass(RandomForestMapCollective.class);
            job.setMapperClass(RandomForestMapper.class);
            JobConf jobConf = (JobConf) job.getConfiguration();
            jobConf.set("mapreduce.framework.name", "map-collective");
            jobConf.setNumMapTasks(RandomForestConstants.NUM_MAPPERS);
            jobConf.setInt("mapreduce.job.max.split.locations", 10000);
            jobConf.setInt(RandomForestConstants.NUM_TREES,numTrees);
            job.setNumReduceTasks(0);                               
            return job;
    }
}
