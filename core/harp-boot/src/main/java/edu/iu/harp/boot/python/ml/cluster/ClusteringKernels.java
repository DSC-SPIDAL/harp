package edu.iu.harp.boot.python.ml.cluster;

import edu.iu.harp.boot.python.HarpSession;

public class ClusteringKernels {

    private final HarpSession harpSession;

    public ClusteringKernels(HarpSession harpSession) {
        this.harpSession = harpSession;
    }


    public KMeans kmeans() {
        return new KMeans(this.harpSession);
    }

    /**
     * Scikit learn compatible Kmeans function
     *
     * @return
     */
    public KMeans kmeans(int clusters, int iterations, int mappers) {
        return kmeans().setClusters(clusters)
                .setIterations(iterations)
                .setMappers(mappers);
    }
}
