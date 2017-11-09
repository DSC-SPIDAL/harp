"""
===============================================================
HarpDaal K-Means compatible with scikit-learn KMeans Interface
===============================================================
"""
from time import time
import numpy as np
import sys

from harp.daal.applications import NaiveDaalApplication


class DAALNavieBayes():
    """
    interface to daal-navie call
    """
    def __init__(self, n_node = 1, n_thread = 8, n_mem = 8000, workdir = "/daal-naive/"
            ):
        """
            max_iter    ; set maximum iteration number
            n_node      ; set mapper number
            n_thread    ; set thread number in each mapper
        """
        self.coef_ = None
        #daal init parameters
        self.n_node = n_node
        self.n_thread = n_thread
        self.n_mem = n_mem
        self.workdir = workdir

    #fit and predict in single call
    def fit_predict(self, x, y, x_test, y_test):
        """
        """
        #init the daal interface
        harp_naivebayes = NaiveDaalApplication("naivebayes with Harp-Daal")
        harp_naivebayes.set_workdir(self.workdir)

        #use dense matrix version
        x = x.toarray()
        x_test = x_test.toarray()
        y = y.reshape((y.shape[0], 1))
        y_test = y_test.reshape((y_test.shape[0], 1))
        nClasses = len(np.unique(y))
        print("dim: %s, %s, %s, %s, classes: %s"%(x.shape, y.shape, x_test.shape, y_test.shape, nClasses))
        #prepare the input data
        data = np.hstack((x,y))
        self.coef_ = np.ones((1, x.shape[1]))
        harp_naivebayes.load_matrix("train/train.csv", data)
        harp_naivebayes.load_matrix("test/test.csv", x_test)
        harp_naivebayes.load_matrix("groundTruth/groundTruth.csv", y_test)

        # call run harpdaal naivebayes
        #my_app.args('/daal_naive/train /daal_naive/test /daal_naive/groundTruth /naive-work 10240 50 2 24')
        harp_naivebayes.args(self.workdir + '/train', self.workdir + '/test', self.workdir + '/groundTruth', self.workdir + '/work',
                self.n_mem, 50, self.n_node, self.n_thread,
                nClasses, x.shape[1], x.shape[0], x_test.shape[0])

        harp_naivebayes.run()

        #    pred    ; prediction for x_test
        #    mode    ; coef_
        #get result
        pred = harp_naivebayes.result_to_array(self.workdir + "/out/pred")
        #self.coef_ = harp_naivebayes.result_to_array(self.workdir + "/out/coef")
        return pred

