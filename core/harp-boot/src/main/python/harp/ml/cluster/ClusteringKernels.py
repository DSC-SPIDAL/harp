class ClusteringKernels:

    def __init__(self, java_ckernels):
        self.__java_ckernels = java_ckernels

    def KMeans(self, n_clusters=8, init='k-means++', n_init=10, max_iter=300, tol=0.0001, precompute_distances='auto',
               verbose=0, random_state=None, copy_x=True, n_jobs=1, algorithm='auto'):
        return self.__java_ckernels.kmeans().setClusters(n_clusters).setMappers(n_jobs).setIterations(max_iter)
