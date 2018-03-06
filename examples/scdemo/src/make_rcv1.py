"""
Reuters Corpus Volume I (RCV1) is an archive of over 800,000 manually categorized newswire stories made available by Reuters, Ltd. for research purposes. The dataset is extensively described in:
Lewis, D. D., Yang, Y., Rose, T. G., & Li, F. (2004). RCV1: A new benchmark collection for text categorization research. The Journal of Machine Learning Research, 5, 361-397.

The feature matrix is a scipy CSR sparse matrix, with 804414 samples and 47236 features. Non-zero values contains cosine-normalized, log TF-IDF vectors.

Ref: http://scikit-learn.org/stable/datasets/index.html#rcv1-dataset

"""
from __future__ import print_function

import sys
import numpy as np
from time import time
import logging

from sklearn.datasets import fetch_rcv1, dump_svmlight_file

print(__doc__)

# Display progress logs on stdout
logging.basicConfig(level=logging.INFO, format='%(asctime)s %(message)s')

datahome = '.'
if len(sys.argv) > 1:
    datahome = sys.argv[1] 

# #############################################################################
# Download the data, if not already on disk and load it as numpy arrays
dname = 'rcv1'
data = fetch_rcv1(subset='all',  data_home='.')


n_samples, n_features = data.data.shape
x = data.data
y = data.target
target_names = data.target_names
n_classes = target_names.shape[0]

print("Total dataset size:")
print("n_samples: %d" % n_samples)
print("n_features: %d" % n_features)
print("n_classes: %d" % n_classes)

#save to file
#np.savez(dname, data= data.data, target=data.target, target_names=data.target_names)

#
# downgrade to top level classes
# CCAT, ECAT, GCAT, and MCAT
#
categories=[u'CCAT', u'ECAT', u'GCAT',u'MCAT']
indexes= [0,0,0,0]
for idx, cate in enumerate(categories):
    indexes[idx] = (np.argwhere(data.target_names == cate)[0][0])
    print('%s : %d'%(cate, indexes[idx]))

topy = np.zeros((n_samples))
for rowid, row in enumerate(data.target):
    targets = row.toarray()[0]
    
    for idx, cate in enumerate(categories):
        if targets[indexes[idx]] == 1:
            #match
            topy[rowid] = idx + 1
            break

if np.sum(topy == 0) > 0:
    #something wrong there
    print('Error: %d records have no top categories'%(np.sum(topy==0)))


dump_svmlight_file(x, topy, dname + '.svm')

