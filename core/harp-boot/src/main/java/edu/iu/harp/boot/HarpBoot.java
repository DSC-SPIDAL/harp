package edu.iu.harp.boot;

import edu.iu.harp.boot.python.PythonBridge;
import edu.iu.harp.boot.python.PythonLauncher;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ToolRunner;
import py4j.GatewayServer;

public class HarpBoot {

  public static void main(String[] args) throws Exception {

    int res = ToolRunner.run(new Configuration(),
            new PythonLauncher(), args);
    GatewayServer gatewayServer = new GatewayServer(new PythonBridge());
    gatewayServer.start();
  }
}
