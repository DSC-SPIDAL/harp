"""
===============================================================
HarpDaal K-Means compatible with scikit-learn KMeans Interface
===============================================================
"""
from time import time
import numpy as np
import sys

from harp.daal.applications import KMeansDaalApplication

class DAALKMeans():
    """
    interface to daal-kmeans call
    """
    def __init__(self, n_clusters=10, n_iter=10, init = 'random',
            n_node = 1, n_thread = 8, n_mem = 10240, workdir = "/kmeans-work"
            ):
        self.labels_ = None
        self.centroids_ = None
        self.inertia_ = 0.
        self.n_iter = n_iter
        self.n_clusters = n_clusters

        #daal init parameters
        self.n_node = n_node
        self.n_thread = n_thread
        self.n_mem = n_mem
        self.workdir = workdir

    def fit(self, data):
        """
        """
        #init the daal interface
        harp_kmeans = KMeansDaalApplication("KMeans with Harp-Daal")
        harp_kmeans.set_workdir(self.workdir)

        #prepare the input data
        harp_kmeans.load_array("data/input.data", data)

        # random select centroids
        centroids = np.random.permutation(data.shape[0])[:self.n_clusters]
        centroids = data[centroids]
        harp_kmeans.init_centroids(centroids)

        # call run harpdaal kmeans
        # args: <$Pts $Ced $Dim $File $Node $Thd $ITR $Mem $workdir $tmpdir $gendata>
        harp_kmeans.args(data.shape[0], self.n_clusters, data.shape[1], 
                1, self.n_node, self.n_thread, self.n_iter, self.n_mem, 
                harp_kmeans.get_workdir(), "/tmp/" + self.workdir, "false")
        harp_kmeans.run()

        #get result
        self.centroids_ = harp_kmeans.result_to_array(harp_kmeans.get_workdir() + "/centroids/out/output")
        #todo: to get result from kmeans output directly
        self.predict(data)

    def predict(self, data):
        """
        # hardcoded: minimize euclidean distance to cluster center:
        # ||a - b||^2 = ||a||^2 + ||b||^2 -2 <a, b>
        """
        a = data
        b = self.centroids_
        a2 = np.sum(a * a, axis =1)
        b2 = np.sum(b * b, axis =1)
        distmat = a2.reshape((a.shape[0],1)) - 2*np.dot(a, b.T)
        distmat = b2.T.reshape((1,b.shape[0])) + distmat

        self.labels_ = np.argmin(distmat, axis=1)


