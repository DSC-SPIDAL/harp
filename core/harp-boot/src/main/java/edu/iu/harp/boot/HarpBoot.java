package edu.iu.harp.boot;

import edu.iu.harp.boot.python.PythonBridge;
import py4j.GatewayServer;

public class HarpBoot {

    public static void main(String[] args) {
        GatewayServer gatewayServer = new GatewayServer(new PythonBridge());
        gatewayServer.start();
    }
}
