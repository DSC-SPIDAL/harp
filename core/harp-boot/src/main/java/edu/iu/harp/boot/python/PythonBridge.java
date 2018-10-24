package edu.iu.harp.boot.python;

public class PythonBridge {

  private MapCollectiveReceiver mapCollectiveReceiver;
  private HarpContext harpContext;


  public HarpContext getHarpContext() {
    return harpContext;
  }

  void arr(byte [] ints) {
    System.out.println("Ints" + ints);
  }
}
