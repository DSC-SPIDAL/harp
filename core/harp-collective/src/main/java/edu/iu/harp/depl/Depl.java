/*
 * Copyright 2013-2017 Indiana University
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

package edu.iu.harp.depl;

import edu.iu.harp.worker.Nodes;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*******************************************************
 * Some tools including command execution, and
 * getting rack information
 ******************************************************/
public class Depl {

  private static final Logger LOG =
      Logger.getLogger(Depl.class);

  final static String java_home = getJavaHome();
  final static String prjt_home =
      getProjectHome();
  final static String bin_directory = "scripts/";
  public final static String nodes_file =
      prjt_home + bin_directory + "nodes";

  /**
   * Execute the command and get the output
   *
   * @param cmd the command
   * @return the output from the command execution
   */
  private static Output executeCMDandReturn(String[] cmd) {
    Output cmdOutput = new Output();
    List<String> output =
        cmdOutput.getExeOutput();
    try {
      Process q = Runtime.getRuntime().exec(cmd);
      q.waitFor();
      InputStream is = q.getInputStream();
      InputStreamReader isr =
          new InputStreamReader(is);
      BufferedReader br = new BufferedReader(isr);

      String line;
      while ((line = br.readLine()) != null) {
        output.add(line);
      }
      br.close();
      if (q.exitValue() != 0) {
        cmdOutput.setExeStatus(false);
        output.clear();
      }
    } catch (Exception e) {
      LOG.error(
          "Errors happen in executing " + cmd);
      cmdOutput.setExeStatus(false);
      output.clear();
    }
    return cmdOutput;
  }

  /**
   * Just do execution. The output is not
   * returned, it shows on the screen directly
   *
   * @param cmd the command
   */
  public static Output executeCMDandForward(String[] cmd) {
    Output cmdOutput = new Output();
    try {
      Process q = Runtime.getRuntime().exec(cmd);
      q.waitFor();
      if (q.exitValue() != 0) {
        cmdOutput.setExeStatus(false);
      }
    } catch (Exception e) {
      LOG.error("Errors in executing " + cmd);
      cmdOutput.setExeStatus(false);
    }
    return cmdOutput;
  }

  /**
   * Execute the command and doesn't wait for
   * outputs
   *
   * @param cmd the command
   * @return the Output
   */
  public static Output executeCMDandNoWait(String[] cmd) {
    Output cmdOutput = new Output();
    try {
      Runtime.getRuntime().exec(cmd);
      cmdOutput.setExeStatus(true);
    } catch (Exception e) {
      LOG.error("Errors in executing " + cmd, e);
      cmdOutput.setExeStatus(false);
    }
    return cmdOutput;
  }

  /**
   * Get the JAVA home directory from the system
   *
   * @return the JAVA home directory
   */
  private static String getJavaHome() {
    // It seems the only way to execute echo
    // command
    String cmdstr[] =
        {"bash", "-c", "echo $JAVA_HOME"};
    Output cmdOutput =
        Depl.executeCMDandReturn(cmdstr);
    String java_home = null;
    if (cmdOutput.getExeStatus()) {
      // Home directory is returned with "/" at
      // the end
      java_home =
          cmdOutput.getExeOutput().get(0) + "/";
      java_home = java_home.replace("//", "/");
      java_home = java_home.replace(" ", "\\ ");
    }
    return java_home;
  }

  /**
   * Get the project home directory from the
   * system
   *
   * @return the project home directory
   */
  private static String getProjectHome() {
    // It seems the only way to execute echo
    // command
    String cmdstr[] =
        {"bash", "-c", "echo $HARP_HOME"};
    Output cmdOutput =
        Depl.executeCMDandReturn(cmdstr);
    String harp_home = null;
    if (cmdOutput.getExeStatus()) {
      // Home directory is returned with "/" at
      // the end
      harp_home =
          cmdOutput.getExeOutput().get(0) + "/";
      harp_home = harp_home.replace("//", "/");
      harp_home = harp_home.replace(" ", "\\ ");
    }
    return harp_home;
  }

  /**
   * Return the java home path
   *
   * @return the java home path
   */
  public static String getJavaHomePath() {
    return java_home;
  }

  /**
   * Return the project home path
   *
   * @return the project home path
   */
  public static String getProjectHomePath() {
    return prjt_home;
  }

  /**
   * Return the bin directory
   *
   * @return the bin directory
   */
  public static String getBinDirectory() {
    return bin_directory;
  }

  /**
   * Write a list of strings to lines
   *
   * @param filePath the file to be written into
   * @param contents the contents to write
   */
  static boolean writeToFile(String filePath,
                             List<String> contents) {
    boolean status = true;
    // delete the original file
    File file = new File(filePath);
    if (file.exists()) {
      file.delete();
    }
    try {
      BufferedWriter writer = new BufferedWriter(
          new FileWriter(filePath));
      for (int i = 0; i < contents.size(); i++) {
        writer.write(contents.get(i));
        // if it is not the last line, enter
        if (i < (contents.size() - 1)) {
          writer.newLine();
        }
      }
      writer.flush();
      writer.close();
    } catch (Exception e) {
      System.err.println(
          "Errors happen in writing " + filePath);
      status = false;
    }
    return status;
  }

  /**
   * Check if this string is a tag of rack or not
   *
   * @param line the string
   * @return true if this string is a tag of rack
   */
  public static boolean isRack(String line) {
    Pattern p = Pattern.compile("#[0-9]*");
    Matcher m = p.matcher(line);
    return m.matches();
  }

  /**
   * Get the rack ID from the string
   *
   * @param line the string
   * @return the rack ID
   */
  public static int getRackID(String line) {
    return Integer.parseInt(line.substring(1));
  }

  public static void main(String[] args) {
    boolean invalidJavaHome = false;
    boolean invalidProjectHome = false;
    boolean invalidNodes = false;
    boolean configureStatus = true;
    // Java home detection
    // Assume that java is installed on every
    // node.
    // Here only pick the current node to check.
    if (Depl.getJavaHomePath() == null) {
      invalidJavaHome = true;
    } else {
      File javaHomeFile =
          new File(Depl.getJavaHomePath());
      if (!javaHomeFile.exists()) {
        invalidJavaHome = true;
      }
    }
    if (invalidJavaHome) {
      LOG
          .info("Java Home is not set properly...");
      System.exit(-1);
    } else {
      LOG.info(
          "Java Home: " + Depl.getJavaHomePath());
    }
    // Project home detection
    if (Depl.getProjectHomePath() == null) {
      invalidProjectHome = true;
    } else {
      File homeFile =
          new File(Depl.getProjectHomePath());
      if (!homeFile.exists()) {
        invalidProjectHome = true;
      }
    }
    if (invalidProjectHome) {
      LOG
          .info("Harp Home is not set properly...");
      System.exit(-1);
    } else {
      LOG.info("Harp Home: "
          + Depl.getProjectHomePath());
    }
    // Check nodes information
    Nodes nodes = null;
    try {
      nodes = new Nodes();
    } catch (Exception e) {
      e.printStackTrace();
    }
    if (nodes == null
        || nodes.getNumPhysicalNodes() == 0) {
      invalidNodes = true;
    }
    if (invalidNodes) {
      LOG.error(
          "No or wrong nodes file format is provided.");
      System.exit(-1);
    }
    // Check Project location.
    // The code of checking could be wrong, always
    // set to true on HPC and supercomputer
    // Start deployment
    // Start to deploy nodes
    LOG.info("Start deploying nodes.");
    nodes.sortRacks();
    configureStatus = writeToFile(Depl.nodes_file,
        nodes.printToNodesFile());
    if (!configureStatus) {
      LOG
          .error(" Errors when configuring nodes.");
      System.exit(-1);
    } else {
      LOG.info("Deployment is done.");
      System.exit(0);
    }
  }
}