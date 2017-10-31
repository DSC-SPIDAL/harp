"""
The 20 newsgroups dataset comprises around 18000 newsgroups posts on 20 topics split in two subsets: one for training (or development) and the other one for testing (or for performance evaluation). The split between the train and test set is based upon a messages posted before and after a specific date.

Ref: http://scikit-learn.org/stable/datasets/index.html#the-20-newsgroups-text-dataset

"""
from __future__ import print_function

import sys
import numpy as np
from time import time
import logging

from sklearn.datasets import fetch_20newsgroups_vectorized, dump_svmlight_file

print(__doc__)

# Display progress logs on stdout
logging.basicConfig(level=logging.INFO, format='%(asctime)s %(message)s')

datahome = '.'
if len(sys.argv) > 1:
    datahome = sys.argv[1] 

# #############################################################################
# Download the data, if not already on disk and load it as numpy arrays
remove = ('headers', 'footers', 'quotes')
data = fetch_20newsgroups_vectorized(subset='all',  remove=remove, data_home='.')


n_samples, n_features = data.data.shape
x = data.data
y = data.target
target_names = data.target_names
n_classes = np.max(y)

print("Total dataset size:")
print("n_samples: %d" % n_samples)
print("n_features: %d" % n_features)
print("n_classes: %d" % n_classes)

#save to file
np.savez('20news', data= data.data, target=data.target, target_names=data.target_names)

dump_svmlight_file(x, y, '20news.svm')

