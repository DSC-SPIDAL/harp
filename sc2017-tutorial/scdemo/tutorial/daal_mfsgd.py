"""
===============================================================
HarpDaal K-Means compatible with scikit-learn KMeans Interface
===============================================================
"""
from time import time
import numpy as np
import sys

from harp.daal.applications import SGDDaalApplication

class DAALMFSGD():
    """
    interface to daal-navie call
    """
    def __init__(self, n_node = 1, n_thread = 8, 
            n_factors = 100, n_epochs = 20, lr_all = 0.005, reg_all = 0.02,
            n_mem = 6000, workdir = "/daal-sgd"
            ):
        """
            max_iter    ; set maximum iteration number
            n_node      ; set mapper number
            n_thread    ; set thread number in each mapper
        """
        self.rmse = None
        self.lr_all = lr_all
        self.reg_all = reg_all
        self.n_factors = n_factors
        self.n_epochs = n_epochs

        #daal init parameters
        self.n_node = n_node
        self.n_thread = n_thread
        self.n_mem = n_mem
        self.workdir = workdir

    #fit and predict in single call
    def train_test(self, trainfile, testfile):
        """
        """
        #init the daal interface
        app = SGDDaalApplication('MFSGD with Harp-DAAL')
        app.set_workdir(self.workdir)


        #prepare the input data
        app.put_file(trainfile, app.get_workdir() + "/train/train.mm")
        app.put_file(testfile, app.get_workdir() + "/test/test.mm")

        # call run harpdaal naivebayes
        #my_app.args('/movielens/movielens-train 500 0.05 0.002 5 false 1 4 6000 /sgd-work /movielens/movielens-test')
        app.args(self.workdir + '/train/train.mm', self.n_factors, self.lr_all, self.reg_all, self.n_epochs, 
                False, self.n_node, self.n_thread, self.n_mem, self.workdir + "/work", 
                self.workdir + '/test/test.mm')

        app.run()

        #get result
        rmse = app.result_to_array(self.workdir + "/work/model/out/rmse")
        return rmse

