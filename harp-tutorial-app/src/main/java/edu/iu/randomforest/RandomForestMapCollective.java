package edu.iu.randomforest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.ArrayList;
import java.io.File;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.util.Random;

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

    Random rand;

    public static void main(String[] argv) throws Exception {
        System.out.println("Random-Forest with Categorical support");
        int res = ToolRunner.run(new Configuration(), new RandomForestMapCollective(), argv);
        System.exit(res);
    }


    @Override
    public int run(String[] args) throws Exception {

        rand = new Random(System.currentTimeMillis());

        //keep this unchanged.
        if (args.length < 6) {
            System.err.println("Usage: RandomForestMapCollective <trainFolder> <trainNameFormat> <testFile> <doBootstrapSampling> <workDir> <localDir>");
            ToolRunner.printGenericCommandUsage(System.err);
                return -1;
        }

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

            ArrayList<String> trainData = new ArrayList();
            ArrayList<String> testData = new ArrayList();
            

            //placeholder variables
            int i;
            ArrayList<String> files = new ArrayList();

            // Load the test data to write to each sample file
            files.add(testFile);            
            loadData(testData,files);
            System.out.println("Test data size: " + testData.size());
            files.clear();

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

            trainData = null;   
            files = null;
            testData = null;        

            // Create the local directory as a path object
            Path localPath = new Path(localDirStr);
            // Copy from the generated data from the locally written files to the
            // distributed file system
            fs.copyFromLocalFile(localPath, dataDir);



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

    /*
    * This function takes a String list, and loads all the files as data.
    */
    private void loadData(ArrayList<String> data, ArrayList<String> fileNames) throws IOException{

    // This line is used to more through each data file
    String sCurrentLine;

      // Process each file
      for (String filename: fileNames) {        
        BufferedReader br = null;
        try {          
             br = new BufferedReader(new FileReader(filename));
            // Read through the file until all lines have been seen
            while ((sCurrentLine = br.readLine()) != null) {
                data.add(sCurrentLine);
            }
        
        } finally {
          // Close the input stream
          br.close();
        }
      }
    }


    public ArrayList<Integer> DoBootStrapSampling(int dataSize) {

        int sizeToSample = (int) (0.7 * dataSize), i, index;
        ArrayList<Integer> dPpos = new ArrayList();

        // This array list represents the data points that have not yet been sampled
        ArrayList<Integer> available = new ArrayList<Integer>();
        // Populate available with the indices of data points in allData
        for (i=0; i < sizeToSample; i++) { available.add(i); }

        for(i = 0; i < sizeToSample; i ++) {
            // Randomly selet a data point by randomly selecting an index from available
            index = rand.nextInt(available.size());
            // Add the data point referred to at index of available
            dPpos.add(index);
            // Remove the value in avaible that points to the data point in allData
            // referenced at index in available
            available.remove(index);
        }

        available = null;
        return dPpos;
    }

    public void createMapperFiles(ArrayList<String> trainData, ArrayList<String> testData, FileSystem fs,  String localDirStr, int i, ArrayList<Integer> positions) throws IOException {      

        //Write data to localDir
        try
        {
            String filename =Integer.toString(i);
            // Create the file
            File file = new File(localDirStr + File.separator + "data_" + filename);
            // Create the objects that will be used to write the data to the file
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);

            if(positions == null) {
                //write Train file
                for(String dataPoint: trainData) {
                    bw.write(dataPoint+"\n");
                }
            } else {
                for(Integer pos: positions) {
                    bw.write(trainData.get(pos)+"\n");
                }
            }                

            //empty line separating train and test data
            bw.newLine();

            //write the test file
            for(String dataPoint: testData) {
                bw.write(dataPoint+"\n");
            }

            // Close the writer
            bw.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }       
    }
}
