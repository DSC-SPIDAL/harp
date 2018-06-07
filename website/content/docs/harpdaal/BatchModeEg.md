---
title: Code Walk Through for Batch Mode Algorithms 
---

We take Association Rule algorithm as an example to illustrate the programming steps for batch mode algorithms in Harp-DAAL.
The codebase consists of three Java files

## A Harp-DAAL Application Launcher Class

A *ARDaalLauncher.java* file is the entrance of Harp-DAAL execution, which loads command line arguments, setting Harp
environments and launch the application. 

```java
public class ARDaalLauncher extends Configured
  implements Tool {

   public static void main(String[] argv)
    throws Exception {
    int res =
      ToolRunner.run(new Configuration(),
        new ARDaalLauncher(), argv);
    System.exit(res);
  }

  @Override
  public int run(String[] args) throws Exception {

      // create the Initialize Module
      Configuration conf = this.getConf();
      Initialize init = new Initialize(conf, args);

      // load native kernels to distributed cache
      init.loadDistributedLibs();

      // load system-wide args
      init.loadSysArgs();

      //load application-wide args
      conf.setInt(HarpDAALConstants.FILE_DIM, Integer.parseInt(args[init.getSysArgNum()]));
      conf.setDouble(Constants.MIN_SUPPORT, Double.parseDouble(args[init.getSysArgNum()+1]));
      conf.setDouble(Constants.MIN_CONFIDENCE, Double.parseDouble(args[init.getSysArgNum()+2]));

      // launch job, specifiy the launcher class name and mapper class name
      Job arbatchJob = init.createJob("arbatchJob", ARDaalLauncher.class, ARDaalCollectiveMapper.class);

      // finish job
      boolean jobSuccess = arbatchJob.waitForCompletion(true);
      if (!jobSuccess) {
              arbatchJob.killJob();
              System.out.println("ArBatchJob Job failed");
      }

    return 0;
  }
}
```

## Application-Wide Configuration Constants (Optional)

A *Constants.java* file is to store application-wide command line arguments, which would be used by the mapper class 

```java
public class Constants {

  public static final String MIN_SUPPORT =
    "min_support";
  public static final String MIN_CONFIDENCE =
    "min_confidence";
}
```

## A Mapper class (Main Body) 

A *ARDaalCollectiveMapper.java* file is the main body to implement the algorithm itself for each Harp Mapper process. 

```java
public class ARDaalCollectiveMapper
    extends
    CollectiveMapper<String, String, Object, Object> {
        //cmd args
        private int num_mappers;
        private int numThreads;
        private int harpThreads;
        private int fileDim;
        private double minSupport;      
        private double minConfidence;   
        private List<String> inputFiles;
        private Configuration conf;
        private static HarpDAALDataSource datasource;
        private static DaalContext daal_Context = new DaalContext();
        private NumericTable input;

        @Override
        protected void setup(Context context)
        throws IOException, InterruptedException 
        {
            this.conf =context.getConfiguration();
            this.num_mappers = this.conf.getInt(HarpDAALConstants.NUM_MAPPERS, 10);
            this.numThreads = this.conf.getInt(HarpDAALConstants.NUM_THREADS, 10);
            this.harpThreads = Runtime.getRuntime().availableProcessors();
            this.fileDim = this.conf.getInt(HarpDAALConstants.FILE_DIM, 10);
            this.minSupport = this.conf.getDouble(Constants.MIN_SUPPORT, 0.001);
            this.minConfidence = this.conf.getDouble(Constants.MIN_CONFIDENCE, 0.7);

            //set thread number used in DAAL
            LOG.info("The default value of thread numbers in DAAL: " + Environment.getNumberOfThreads());
            Environment.setNumberOfThreads(numThreads);
            LOG.info("The current value of thread numbers in DAAL: " + Environment.getNumberOfThreads());
        }

        protected void mapCollective(KeyValReader reader, Context context) throws IOException, InterruptedException 
        {
            // read data file names from HDFS
            this.inputFiles = new LinkedList<String>();
            while (reader.nextKeyValue()) {
                String key = reader.getCurrentKey();
                String value = reader.getCurrentValue();
                LOG.info("Key: " + key + ", Value: "
                        + value);
                LOG.info("file name: " + value);
                this.inputFiles.add(value);
            }

            // set up data source module
            this.datasource = new HarpDAALDataSource(harpThreads, conf);

            // ----------------------- start the execution -----------------------
            runAR(context);
            this.freeMemory();
            this.freeConn();
            System.gc();
        }

        private void runAR(Context context) throws IOException
        {
                //  load data 
                this.input = this.datasource.createDenseNumericTableInput(this.inputFiles, this.fileDim, ",",  daal_Context);
                // Create an algorithm to mine association rules using the Apriori method 
                Batch alg = new Batch(daal_Context, Double.class, Method.apriori);
                // Set an input object for the algorithm 
                alg.input.set(InputId.data, input);
                // Set Apriori algorithm parameters 
                alg.parameter.setMinSupport(minSupport);
                alg.parameter.setMinConfidence(minConfidence);
                // Find large item sets and construct association rules 
                Result res = alg.compute();
                HomogenNumericTable largeItemsets = (HomogenNumericTable) res.get(ResultId.largeItemsets);
                HomogenNumericTable largeItemsetsSupport = (HomogenNumericTable) res.get(ResultId.largeItemsetsSupport);
                // Print the large item sets 
                Service.printAprioriItemsets(largeItemsets, largeItemsetsSupport);
                HomogenNumericTable antecedentItemsets = (HomogenNumericTable) res.get(ResultId.antecedentItemsets);
                HomogenNumericTable consequentItemsets = (HomogenNumericTable) res.get(ResultId.consequentItemsets);
                HomogenNumericTable confidence = (HomogenNumericTable) res.get(ResultId.confidence);
                // Print the association rules 
                Service.printAprioriRules(antecedentItemsets, consequentItemsets, confidence);
                daal_Context.dispose();
        }
  }
```



