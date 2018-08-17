package edu.iu.examples;

import org.apache.commons.cli.CommandLine;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.*;
import java.util.concurrent.ExecutionException;

public final class Utils {
  private Utils() {
  }

  /**
   * We need a dummy dataset to initialize the workers, each file will have one line of data
   * @param numOfTasks numberof tasks
   * @param fs file system
   * @param dataPath datapath
   * @throws IOException
   * @throws InterruptedException
   * @throws ExecutionException
   */
  public static void generateData(int numOfTasks,
                           FileSystem fs,
                           Path dataPath) throws IOException,
      InterruptedException, ExecutionException {
  }

  public static String getStringValue(String name, String def, CommandLine c) {
    if (c.hasOption(name)) {
      return c.getOptionValue(name);
    } else {
      return def;
    }
  }

  public static int getIntValue(String name, int def, CommandLine c) {
    if (c.hasOption(name)) {
      return Integer.parseInt(c.getOptionValue(name));
    } else {
      return def;
    }
  }
}
