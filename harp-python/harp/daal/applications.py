from harp.applications import HarpApplication


class HarpDaalApplication(HarpApplication):
    def __init__(self, name):
        super(HarpDaalApplication, self).__init__(name)
        self.daal_flag = False  # By default, Intel Daal library is not used.


class ALSDaalApplication(HarpDaalApplication):
    def __init__(self, name):
        super(ALSDaalApplication, self).__init__(name)
        self.class_name = 'edu.iu.daal_als.ALSDaalLauncher'


class COVDaalApplication(HarpDaalApplication):
    def __init__(self, name):
        super(COVDaalApplication, self).__init__(name)
        self.class_name = 'edu.iu.daal_cov.COVDaalLauncher'


class KMeansDaalApplication(HarpDaalApplication):
    def __init__(self, name):
        super(KMeansDaalApplication, self).__init__(name)
        self.class_name = 'edu.iu.daal_kmeans.regroupallgather.KMeansDaalLauncher'


class LinRegDaalApplication(HarpDaalApplication):
    def __init__(self, name):
        super(LinRegDaalApplication, self).__init__(name)
        self.class_name = 'edu.iu.daal_linreg.LinRegDaalLauncher'


class MOMDaalApplication(HarpDaalApplication):
    def __init__(self, name):
        super(MOMDaalApplication, self).__init__(name)
        self.class_name = 'edu.iu.daal_mom.MOMDaalLauncher'


class NaiveDaalApplication(HarpDaalApplication):
    def __init__(self, name):
        super(NaiveDaalApplication, self).__init__(name)
        self.class_name = 'edu.iu.daal_naive.NaiveDaalLauncher'


class NNDaalApplication(HarpDaalApplication):
    def __init__(self, name):
        super(NNDaalApplication, self).__init__(name)
        self.class_name = 'edu.iu.daal_nn.NNDaalLauncher'


class PCADaalApplication(HarpDaalApplication):
    def __init__(self, name):
        super(PCADaalApplication, self).__init__(name)
        self.class_name = 'edu.iu.daal_pca.PCADaalLauncher'


class QRDaalApplication(HarpDaalApplication):
    def __init__(self, name):
        super(QRDaalApplication, self).__init__(name)
        self.class_name = 'edu.iu.daal_qr.QRDaalLauncher'


class RidgeRegDaalApplication(HarpDaalApplication):
    def __init__(self, name):
        super(RidgeRegDaalApplication, self).__init__(name)
        self.class_name = 'edu.iu.daal_ridgereg.RidgeRegDaalLauncher'


class SGDDaalApplication(HarpDaalApplication):
    def __init__(self, name):
        super(SGDDaalApplication, self).__init__(name)
        self.class_name = 'edu.iu.daal_sgd.SGDDaalLauncher'


class SVDDaalApplication(HarpDaalApplication):
    def __init__(self, name):
        super(SVDDaalApplication, self).__init__(name)
        self.class_name = 'edu.iu.daal_svd.SVDDaalLauncher'
