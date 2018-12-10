package edu.iu.harp.boot.python.io.filepointers;

import org.apache.hadoop.fs.Path;

public class LocalFilePointer extends AbstractFilePointer {

    public LocalFilePointer(String path) {
        super(new Path("file://" + path), true);
    }
}
