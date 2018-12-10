from harp.ml.cluster.ClusteringKernels import ClusteringKernels


class MLKernels:

    def __init__(self, java_ml):
        self.cluster = ClusteringKernels(java_ml.cluster())
