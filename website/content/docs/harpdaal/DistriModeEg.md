---
title: Code Walk Through for Distributed Mode Algorithms 
---

We take Covariance algorithm as an example to illustrate the programming steps for distributed mode algorithms in Harp-DAAL.
The codebase consists of three Java files:

## A Harp-DAAL Application Launcher Class

Similarly to batch mode algorithm, a *COVDaalLauncher.java* file is the entrance of Harp-DAAL execution, which loads command line arguments, setting Harp
environments and launch the application. 

```java
public class COVDaalLauncher extends Configured implements Tool 
{
  public static void main(String[] argv)
  throws Exception {
    int res =
    ToolRunner.run(new Configuration(),
      new COVDaalLauncher(), argv);
    System.exit(res);
  }
  
  @Override
  public int run(String[] args) throws Exception {
    Configuration conf = this.getConf();
    Initialize init = new Initialize(conf, args);
    init.loadDistributedLibs();
    // load args
    init.loadSysArgs();
    // create the job
    Job covJob = init.createJob("CovJob", COVDaalLauncher.class, COVDaalCollectiveMapper.class);
    // finish job
    boolean jobSuccess = covJob.waitForCompletion(true);

    if (!jobSuccess) {
      covJob.killJob();
      System.out.println(
        "COV Job failed");
    }
    return 0;
  }
}
```

## A Mapper class (Main Body) 

A *COVDaalCollectiveMapper.java* file is the main body to implement the algorithm itself for each Harp Mapper process. 

```java
public class COVDaalCollectiveMapper
extends CollectiveMapper<String, String, Object, Object>
{
  
  private int num_mappers;
  private int numThreads;
  private int harpThreads;
  private List<String> inputFiles;
  private Configuration conf;

  private static HarpDAALDataSource datasource;
  private static HarpDAALComm harpcomm;
  private static DaalContext daal_Context = new DaalContext();

  private PartialResult partialResult;
  private SerializableBase[] partialResult_comm;
  private Result result;

  @Override
  protected void setup(Context context)
  throws IOException, InterruptedException 
  {
    long startTime = System.currentTimeMillis();
    this.conf = context.getConfiguration();
    num_mappers = this.conf.getInt(HarpDAALConstants.NUM_MAPPERS, 10);
    numThreads = this.conf.getInt(HarpDAALConstants.NUM_THREADS, 10);
    //always use the maximum hardware threads to load in data and convert data
    harpThreads = Runtime.getRuntime().availableProcessors();
    //set thread number used in DAAL
    Environment.setNumberOfThreads(numThreads);
  }

  protected void mapCollective(KeyValReader reader, Context context) throws IOException, InterruptedException 
  {
      long startTime = System.currentTimeMillis();
      this.inputFiles = new LinkedList<String>();
      //splitting files between mapper
      while (reader.nextKeyValue()) {
        String key = reader.getCurrentKey();
        String value = reader.getCurrentValue();
        LOG.info("Key: " + key + ", Value: "
          + value);
        System.out.println("file name : " + value);
        this.inputFiles.add(value);
      }

      //init data source
      this.datasource = new HarpDAALDataSource(harpThreads, conf);
      // create communicator
      this.harpcomm= new HarpDAALComm(this.getSelfID(), this.getMasterID(), this.num_mappers, daal_Context, this);
      // run the application codes
      runCOV(context);
      this.freeMemory();
      this.freeConn();
      System.gc();
    }

    private void runCOV(Context context) throws IOException 
    {
        //read in csr files 
        NumericTable featureArray_daal = this.datasource.loadCSRNumericTable(this.inputFiles, ",", daal_Context);
        // compute on local nodes
        computeOnLocalNode(featureArray_daal);
        // compute on master node
        if(this.isMaster()){
            computeOnMasterNode();
            HomogenNumericTable covariance = (HomogenNumericTable) result.get(ResultId.covariance);
            HomogenNumericTable mean = (HomogenNumericTable) result.get(ResultId.mean);
            Service.printNumericTable("Covariance matrix:", covariance);
            Service.printNumericTable("Mean vector:", mean);
        }
        daal_Context.dispose();
    }

    private void computeOnLocalNode(NumericTable featureArray_daal) throws java.io.IOException 
    {
        DistributedStep1Local algorithm = new DistributedStep1Local(daal_Context, Double.class, Method.fastCSR);
        // Set input objects for the algorithm 
        algorithm.input.set(InputId.data, featureArray_daal);
        // Compute partial estimates on nodes 
        partialResult = algorithm.compute();
        // gather the partial result
        this.partialResult_comm = this.harpcomm.harpdaal_gather(partialResult, "Covariance", "local-reduce");
    }

    private void computeOnMasterNode()
    {
        // create algorithm instance at master node
        DistributedStep2Master algorithm = new DistributedStep2Master(daal_Context, Double.class, Method.fastCSR);
        // add input data
        for(int j=0;j<this.num_mappers; j++)
          algorithm.input.add(DistributedStep2MasterInputId.partialResults, (PartialResult)(partialResult_comm[j]));
        // compute 
        algorithm.compute();
        result = algorithm.finalizeCompute();
    }
}
```

