package edu.iu.harp.boot.python.ml;

import java.io.File;

public abstract class AbstractOperationResult {

    private File outputFile;

    public AbstractOperationResult(File outputFile) {
        this.outputFile = outputFile;
    }

    public File getOutputFile() {
        return outputFile;
    }
}
