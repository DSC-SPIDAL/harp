package edu.iu.harp.boot.python.ml;

import edu.iu.harp.boot.python.HarpSession;

public class MLKernels {

    private HarpSession harpSession;

    public MLKernels(HarpSession harpSession) {
        this.harpSession = harpSession;
    }

    public KMeans getkMeans() {
        return new KMeans(this.harpSession);
    }
}
