# pylint: skip-file
import sys, argparse
import xgboost as xgb
import numpy as np
from pandas import read_csv
from sklearn import metrics
from sklearn.datasets import make_classification
from sklearn.model_selection import train_test_split
import time
import ast

rng = np.random.RandomState(1994)

def savedata(outfile, X, Y, fmt='libsvm'):
    with open(outfile, 'w') as outf:
        if fmt=='libsvm':
            for i in range(X.shape[0]):
                vec = X[i]
                ss = str(Y[i])   #label
                for j in range(vec.shape[0]):
                    #if vec[j] != 0.0:
                    ss += ' %d:%s'%(j, vec[j])
                recstr = '%s\n'%ss
                outf.write(recstr)

        else:
            for i in range(X.shape[0]):
                rec = ["%s"%(x) for x in X[i]]
                recstr = ','.join(rec) + ',%s'%Y[i] + ',\n'
                outf.write(recstr)

def loaddata(csvfile):
    # load data
    data = read_csv(csvfile, header=None)
    dataset = data.values
    # split data into X and y
    X = dataset[:,0:-1]
    Y = dataset[:,-1]

    return X, Y


x,y = loaddata('train.csv')
savedata('train.libsvm', x, y)

x,y = loaddata('test.csv')
savedata('test.libsvm', x, y)

