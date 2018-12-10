package edu.iu.harp.boot.python.io.filepointers;


import org.apache.hadoop.fs.Path;

public abstract class AbstractFilePointer {

    protected boolean local;
    protected Path path;

    public AbstractFilePointer(Path path, boolean local) {
        this.path = path;
        this.local = local;
    }

    public boolean isLocal() {
        return local;
    }

    public void setLocal(boolean local) {
        this.local = local;
    }

    public Path getPath() {
        return this.path;
    }

    @Override
    public String toString() {
        return this.getPath().toString();
    }
}
