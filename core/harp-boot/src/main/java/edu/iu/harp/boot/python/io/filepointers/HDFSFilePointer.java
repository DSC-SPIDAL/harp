package edu.iu.harp.boot.python.io.filepointers;

import org.apache.hadoop.fs.Path;

public class HDFSFilePointer extends AbstractFilePointer {

    public HDFSFilePointer(String path) {
        super(
                new Path("hdfs://" + path),
                false
        );
    }
}
