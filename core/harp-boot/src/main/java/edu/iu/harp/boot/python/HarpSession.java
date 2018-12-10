package edu.iu.harp.boot.python;

import edu.iu.harp.boot.python.io.filepointers.HDFSFilePointer;
import edu.iu.harp.boot.python.io.filepointers.LocalFilePointer;
import edu.iu.harp.boot.python.ml.MLKernels;

public class HarpSession {

    private String name;
    private MLKernels ml;

    public HarpSession(String name) {
        this.name = name;
        this.ml = new MLKernels(this);
    }

    public MLKernels ml() {
        return ml;
    }

    public String getName() {
        return name;
    }

    public LocalFilePointer fromLocalFile(String absoluteFilePath) {
        return new LocalFilePointer(absoluteFilePath);
    }

    public HDFSFilePointer fromHDFS(String absoluteFilePath) {
        return new HDFSFilePointer(absoluteFilePath);
    }

    public void close() {
        //do cleanup
    }
}
