/*
 * Copyright 2013-2016 Indiana University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.iu.data_aux;

import edu.iu.fileformat.MultiFileInputFormat;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;


public class Initialize {
  private Configuration conf;
  private String[] args;
  private int sys_args_num;
  private int num_mapper;
  private int num_thread;
  private int mem;
  private int iterations;
  private String inputDir;
  private String workDir;
  private Path inputPath;
  private Path workPath;
  private Path outputPath;

  public Initialize(Configuration conf, String[] args) {
    this.conf = conf;
    this.args = args;
    // num_mappers,
    // num_threads per mapper,
    // memory per node,
    // iterations
    // inputDir
    // workDir
    this.sys_args_num = 6;
  }


  public void loadDistributedLibs() throws Exception {
    DistributedCache.createSymlink(this.conf);
    DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libJavaAPI.so#libJavaAPI.so"), this.conf);
    DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libtbb.so.2#libtbb.so.2"), this.conf);
    DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libtbb.so#libtbb.so"), this.conf);
    DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libtbbmalloc.so.2#libtbbmalloc.so.2"), this.conf);
    DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libtbbmalloc.so#libtbbmalloc.so"), this.conf);
  }

  public void loadDistributedLibsExp() throws Exception {
    DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libiomp5.so#libiomp5.so"), conf);
    DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libhdfs.so#libhdfs.so"), conf);
    DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libhdfs.so.0.0.0#libhdfs.so.0.0.0"), conf);
    DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libautohbw.so#libautohbw.so"), conf);
    DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libmemkind.so#libmemkind.so"), conf);
    DistributedCache.addCacheFile(new URI("/Hadoop/Libraries/libmemkind.so.0#libmemkind.so.0"), conf);
  }

  public int getSysArgNum() {
    return this.sys_args_num;
  }

  public Path getInputPath() {
    return this.inputPath;
  }

  public Path getWorkPath() {
    return this.workPath;
  }

  public boolean loadSysArgs() throws IOException, URISyntaxException, InterruptedException,
      ExecutionException, ClassNotFoundException {//{{{
    if (this.args.length < this.sys_args_num) {
      System.err.println("Wrong Command line args number");
      ToolRunner.printGenericCommandUsage(System.err);
      return false;
    }

    // init system args
    this.num_mapper = Integer.parseInt(args[0]);
    this.num_thread = Integer.parseInt(args[1]);
    this.mem = Integer.parseInt(args[2]);
    this.iterations = Integer.parseInt(args[3]);
    this.inputDir = args[4];
    this.workDir = args[5];

    //create HDFS directories
    FileSystem fs = FileSystem.get(this.conf);
    this.inputPath = new Path(this.inputDir);
    this.workPath = new Path(this.workDir);
    if (fs.exists(workPath)) {
      fs.delete(workPath, true);
      fs.mkdirs(workPath);
    }
    this.outputPath = new Path(this.workDir, "output");

    //config Constants value
    this.conf.setInt(HarpDAALConstants.NUM_MAPPERS, this.num_mapper);
    this.conf.setInt(HarpDAALConstants.NUM_THREADS, this.num_thread);
    this.conf.setInt(HarpDAALConstants.NUM_ITERATIONS, this.iterations);

    return true;
  }//}}}


  public Job createJob(String job_name, java.lang.Class<?> launcherCls,
                       java.lang.Class<? extends org.apache.hadoop.mapreduce.Mapper> mapperCls)
      throws IOException, URISyntaxException {//{{{
    Job thisjob = Job.getInstance(this.conf, job_name);
    JobConf thisjobConf = (JobConf) thisjob.getConfiguration();

    //override mapred.xml content
    thisjobConf.set("mapreduce.framework.name", "map-collective");
    thisjobConf.setInt("mapreduce.job.max.split.locations", 10000);
    thisjobConf.setInt("mapreduce.map.collective.memory.mb", this.mem);
    thisjobConf.setInt("mapreduce.task.timeout", 60000000);
    int xmx = (int) Math.ceil((mem - 2000) * 0.5);
    int xmn = (int) Math.ceil(0.25 * xmx);
    thisjobConf.set("mapreduce.map.collective.java.opts",
        "-Xmx" + xmx + "m -Xms" + xmx + "m"
            + " -Xmn" + xmn + "m");

    // set mapper number and reducer number
    thisjobConf.setNumMapTasks(this.num_mapper);
    thisjob.setNumReduceTasks(0);

    // set input and output Path
    FileInputFormat.setInputPaths(thisjob, this.inputPath);
    FileOutputFormat.setOutputPath(thisjob, this.outputPath);
    // set the input format
    thisjob.setInputFormatClass(MultiFileInputFormat.class);

    thisjob.setJarByClass(launcherCls);
    thisjob.setMapperClass(mapperCls);

    return thisjob;

  }//}}}


} 
