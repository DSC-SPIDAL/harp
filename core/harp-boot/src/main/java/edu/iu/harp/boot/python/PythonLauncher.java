package edu.iu.harp.boot.python;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class PythonLauncher extends Configured implements Tool {

  @Override
  public int run(String[] args) throws Exception {
    if (args.length < 1) {
      System.err.println("Specifying the job name is mandatory");
      ToolRunner.printGenericCommandUsage(System.err);
      return -1;
    }

    Job job = Job.getInstance(getConf(),
            "python_job_" + args[0]);
    return 0;
  }
}
