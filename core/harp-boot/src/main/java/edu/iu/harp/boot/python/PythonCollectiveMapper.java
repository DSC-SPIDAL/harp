package edu.iu.harp.boot.python;

import org.apache.hadoop.mapred.CollectiveMapper;

import java.io.IOException;

public class PythonCollectiveMapper extends CollectiveMapper {

  private HarpContext harpContext;

  public PythonCollectiveMapper(HarpContext harpContext) {
    this.harpContext = harpContext;
  }

  @Override
  protected void mapCollective(KeyValReader reader, Context context) throws IOException, InterruptedException {
    super.mapCollective(reader, context);
    this.harpContext.setKeyValueReader(reader);
  }
}
