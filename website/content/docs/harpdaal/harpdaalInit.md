---
title: Initialization Module API 
---

Harp-DAAL provides a class *Initialize* to facilitate the initialization stage of 
Harp-DAAL application from Hadoop environment. To use *Initialize*, load the package 
at the beginning of application's launcher class.

```java
import edu.iu.data_aux.*;
```

To create an *Initialize* instance from the *launcher* class

```java
// obtain the hadoop configuration
Configuration conf = this.getConf();
Initialize init = new Initialize(conf, args);
```

To load all the native libraries required by Harp-DAAL into distributed cache of Harp.
Make sure that the native libraries exist at HDFS path */Hadoop/Libraries* before 
adding this line.

```java
init.loadDistributedLibs();
```

Harp-DAAL divides the command line arguments into two parts: 1) Harp-DAAL system-wide arguments; 
2) Application-wide arguments. System-wide arguments must be supplied at the first six 
entries of command line *args*, including

* num_mapper: the number of mappers (processes) in Harp-DAAL.
* num_thread: the number of threads per mapper
* mem: the memory in MB allocated to each mapper
* iterations: the iteration number for iterative applications (1 for non-iterative applications)
* inputDir: the HDFS directory path for input data
* workDir: the HDFS directory path for intermediate data and results 

To load all the system-wide arguments

```java
init.loadSysArgs();
```

To load the application-wide arguments, users must set them either in the *HarpDAALConstants* class or 
a user-defined *Constants* class accesible to the *mapper* class. Here function *getSysArgNum()*
obtain the number of system-wide arguments in *args[]* array. 

```java
conf.setInt(HarpDAALConstants.FILE_DIM, Integer.parseInt(args[init.getSysArgNum()]));
conf.setDouble(Constants.MIN_SUPPORT, Double.parseDouble(args[init.getSysArgNum()+1]));
conf.setDouble(Constants.MIN_CONFIDENCE, Double.parseDouble(args[init.getSysArgNum()+2]));
```

To create a Harp-DAAL job, specify the name of application job, the name of launcher class, and 
the name of mapper class.

```java
Job arbatchJob = init.createJob("jabname", launcherName.class, MapperName.class);
```

To launch the job and wait for its completion.

```java
boolean jobSuccess = arbatchJob.waitForCompletion(true);
```

Finally, we post the whole *run* function of an Association Rule application launcher class

```java
@Override
public int run(String[] args) throws Exception {
// get the configuration handle
Configuration conf = this.getConf();
// create Initialize obj
Initialize init = new Initialize(conf, args);
// load native libs
init.loadDistributedLibs();
// load system-wide args
init.loadSysArgs();
// load application-wide args
conf.setInt(HarpDAALConstants.FILE_DIM, Integer.parseInt(args[init.getSysArgNum()]));
conf.setDouble(Constants.MIN_SUPPORT, Double.parseDouble(args[init.getSysArgNum()+1]));
conf.setDouble(Constants.MIN_CONFIDENCE, Double.parseDouble(args[init.getSysArgNum()+2]));
// launch job
System.out.println("Starting Job");
Job arbatchJob = init.createJob("arbatchJob", ARDaalLauncher.class, ARDaalCollectiveMapper.class);
// finish job
boolean jobSuccess = arbatchJob.waitForCompletion(true);
System.out.println("End Job#"  + " "+ new SimpleDateFormat("HH:mm:ss.SSS").format(Calendar.getInstance().getTime()));
if (!jobSuccess) {
arbatchJob.killJob();
System.out.println("ArBatchJob Job failed");
}
return 0;
}

```












