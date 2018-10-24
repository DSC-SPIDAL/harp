package edu.iu.harp.boot.python;

import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.resource.Simple;
import org.apache.hadoop.mapred.CollectiveMapper;

public class HarpContext {

  private int selfId;
  private CollectiveMapper.KeyValReader keyValReader;


  public void setKeyValueReader(CollectiveMapper.KeyValReader keyValReader) {
    this.keyValReader = keyValReader;
  }

  public CollectiveMapper.KeyValReader getKeyValReader() {
    return keyValReader;
  }

  public <P extends Simple> boolean broadcast(
          String contextName, String operationName,
          Table<P> table, int bcastWorkerID,
          boolean useMSTBcast) {
    System.out.println(contextName);
    System.out.println(operationName);
    System.out.println(table);
    System.out.println(bcastWorkerID);
    return false;
  }
}
