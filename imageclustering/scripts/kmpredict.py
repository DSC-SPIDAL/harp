import sys, os, math,re
import numpy as np
import logging
import tarfile
import random
from sklearn.externals import joblib
from sklearn.decomposition import PCA

logger = logging.getLogger(__name__)

class KMeans():
    """
    interface to daal-kmeans call
    """
    def __init__(self, n_clusters=10, max_iter=10, init = 'random', n_init = 1,
            n_node = 1, n_thread = 8, n_mem = 6000, workdir = "/kmeans-work"
            ):
        """
            n_clusters  ; set number of clusters
            max_iter    ; set maximum iteration number
            n_node      ; set mapper number
            n_thread    ; set thread number in each mapper
            init        ; set the centroid initialization method, 'random' by default
            n_init      ; set the number of runs to select the best model, 1 by default
        """
        self.labels_ = None
        self.centroids_ = None
        self.inertia_ = 0.
        self.max_iter = max_iter
        self.n_clusters = n_clusters
        self.n_init = n_init

        #daal init parameters
        self.n_node = n_node
        self.n_thread = n_thread
        self.n_mem = n_mem
        self.workdir = workdir

    def run(self, datafile, modelfile):

        data = np.loadtxt(datafile, delimiter=',')
        self.centroids_= np.loadtxt(modelfile)
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



if __name__ == '__main__':
    program = os.path.basename(sys.argv[0])
    logger = logging.getLogger(program)

    # logging configure
    import logging.config
    logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s')
    logging.root.setLevel(level=logging.DEBUG)
    logger.info("running %s" % ' '.join(sys.argv))

    # check and process input arguments
    if len(sys.argv) < 2:
        print(globals()['__doc__'] % locals())
        sys.exit(1)


    datafile = sys.argv[1]
    modelfile = sys.argv[2]

    #load data
    kmeans = KMeans()
    kmeans.run(datafile, modelfile)

    dataname = datafile.split("\/")[-1]
    np.savetxt(dataname + '.cluster', kmeans.labels_)
    logger.info('Model shape: %s', kmeans.centroids_.shape)

    #predict

