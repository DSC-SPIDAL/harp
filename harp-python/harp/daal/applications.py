import os
import tempfile
import numpy
from harp.applications import HarpApplication


class HarpDaalApplication(HarpApplication):
    def __init__(self, name):
        super(HarpDaalApplication, self).__init__(name)
        self.daal_flag = False  # By default, Intel Daal library is not used.
        self.__load_daal_env()
        self.__load_harp_daal_env()

    def __load_daal_env(self):
        try:
            daal_root = os.environ['DAALROOT']
            self.config_daal_root(daal_root)
        except KeyError:
            pass

    def __load_harp_daal_env(self):
        try:
            jar_path = os.environ['HARP_DAAL_JAR']
            self.config_harp_daal_jar(jar_path)
        except KeyError:
            pass

    def config_daal_root(self, daal_root):
        self.daal_root = daal_root
        self.libjars = "-libjars {0}/lib/daal.jar".format(self.daal_root)

    def config_harp_daal_jar(self, jar_path):
        self.harp_daal_jar = jar_path
        self.daal_flag = True

    def args(self, *args, **kwargs):
        self.cmd = ''
        for arg in args:
            self.cmd = self.cmd + str(arg) + ' '
        self.cli = "{0} {1} {2} {3} {4}".format(self.hadoop_cmd, self.harp_daal_jar, self.class_name, self.libjars, self.cmd)
        self.log("Command: " + self.cli)


class ALSDaalApplication(HarpDaalApplication):
    def __init__(self, name):
        super(ALSDaalApplication, self).__init__(name)
        self.class_name = 'edu.iu.daal_als.ALSDaalLauncher'


class COVDaalApplication(HarpDaalApplication):
    def __init__(self, name):
        super(COVDaalApplication, self).__init__(name)
        self.class_name = 'edu.iu.daal_cov.densedistri.COVDaalLauncher'


class KMeansDaalApplication(HarpDaalApplication):
    def __init__(self, name):
        super(KMeansDaalApplication, self).__init__(name)
        self.class_name = 'edu.iu.daal_kmeans.regroupallgather.KMeansDaalLauncher'

    def init_centroids(self, data):
        file_path = self.get_workdir() + '/centroids/init_centroids'
        with tempfile.NamedTemporaryFile(delete=False) as tf:
            numpy.savetxt(tf, data, fmt="%f")
        self.put_file(tf.name, file_path)


class LinRegDaalApplication(HarpDaalApplication):
    def __init__(self, name):
        super(LinRegDaalApplication, self).__init__(name)
        self.class_name = 'edu.iu.daal_linreg.normaleq.LinRegDaalLauncher'


class MOMDaalApplication(HarpDaalApplication):
    def __init__(self, name):
        super(MOMDaalApplication, self).__init__(name)
        self.class_name = 'edu.iu.daal_mom.densedistri.MOMDaalLauncher'


class NaiveDaalApplication(HarpDaalApplication):
    def __init__(self, name):
        super(NaiveDaalApplication, self).__init__(name)
        self.class_name = 'edu.iu.daal_naive.densedistri.NaiveDaalLauncher'


class NNDaalApplication(HarpDaalApplication):
    def __init__(self, name):
        super(NNDaalApplication, self).__init__(name)
        self.class_name = 'edu.iu.daal_nn.NNDaalLauncher'


class PCADaalApplication(HarpDaalApplication):
    def __init__(self, name):
        super(PCADaalApplication, self).__init__(name)
        self.class_name = 'edu.iu.daal_pca.svddensedistr.PCADaalLauncher'


class QRDaalApplication(HarpDaalApplication):
    def __init__(self, name):
        super(QRDaalApplication, self).__init__(name)
        self.class_name = 'edu.iu.daal_qr.QRDaalLauncher'


class RidgeRegDaalApplication(HarpDaalApplication):
    def __init__(self, name):
        super(RidgeRegDaalApplication, self).__init__(name)
        self.class_name = 'edu.iu.daal_ridgereg.RidgeRegDaalLauncher'

class SVDDaalApplication(HarpDaalApplication):
    def __init__(self, name):
        super(SVDDaalApplication, self).__init__(name)
        self.class_name = 'edu.iu.daal_svd.SVDDaalLauncher'
