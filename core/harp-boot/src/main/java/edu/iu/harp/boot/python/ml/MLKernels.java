package edu.iu.harp.boot.python.ml;

import edu.iu.harp.boot.python.HarpSession;
import edu.iu.harp.boot.python.ml.cluster.ClusteringKernels;

public class MLKernels {

    private ClusteringKernels clusteringKernels;

    public MLKernels(HarpSession harpSession) {
        this.clusteringKernels = new ClusteringKernels(harpSession);
    }

    public ClusteringKernels cluster() {
        return this.clusteringKernels;
    }
}
