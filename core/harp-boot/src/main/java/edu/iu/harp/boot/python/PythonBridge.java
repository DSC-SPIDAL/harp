package edu.iu.harp.boot.python;

public class PythonBridge {

  public HarpSession newSession(String name) {
    return new HarpSession(name);
  }
}
