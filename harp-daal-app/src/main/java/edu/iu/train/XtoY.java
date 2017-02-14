package edu.iu.train;

import edu.iu.harp.resource.IntArray;
import edu.iu.harp.resource.Writable;

public abstract class XtoY extends Writable {

  protected int yid;
  protected IntArray xids;

  public XtoY() {
    yid = 0;
    xids = null;
  }

  public XtoY(int yid, IntArray xids) {
    this.yid = yid;
    this.xids = xids;
  }

  public int getYID() {
    return yid;
  }

  public void setYID(int yid) {
    this.yid = yid;
  }

  public IntArray getXIDs() {
    return xids;
  }

  public void setXIDs(IntArray xids) {
    this.xids = xids;
  }
}
