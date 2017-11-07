"""
Classification of text documents using sparse features

This is an example showing how scikit-learn can be used to classify documents
by topics using a bag-of-words approach. This example uses a scipy.sparse
matrix to store the features and demonstrates various classifiers that can
efficiently handle sparse matrices.

The dataset used in this example is the 20 newsgroups dataset. It will be
automatically downloaded, then cached.

The bar plot indicates the accuracy, training time (normalized) and test time
(normalized) of each classifier.

"""

# Author: Peter Prettenhofer <peter.prettenhofer@gmail.com>
#         Olivier Grisel <olivier.grisel@ensta.org>
#         Mathieu Blondel <mathieu@mblondel.org>
#         Lars Buitinck
# License: BSD 3 clause

from __future__ import print_function

import logging
import numpy as np
from optparse import OptionParser
import sys
from time import time
import matplotlib
# Force matplotlib to not use any Xwindows backend.
matplotlib.use('Agg')

import matplotlib.pyplot as plt

from sklearn.datasets import fetch_20newsgroups
from sklearn.datasets import load_svmlight_file, dump_svmlight_file
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.feature_extraction.text import HashingVectorizer
from sklearn.feature_selection import SelectFromModel
from sklearn.feature_selection import SelectKBest, chi2
from sklearn.linear_model import RidgeClassifier
from sklearn.pipeline import Pipeline
from sklearn.svm import LinearSVC
from sklearn.linear_model import SGDClassifier
from sklearn.linear_model import Perceptron
from sklearn.linear_model import PassiveAggressiveClassifier
from sklearn.naive_bayes import BernoulliNB, MultinomialNB
from sklearn.neighbors import KNeighborsClassifier
from sklearn.neighbors import NearestCentroid
from sklearn.ensemble import RandomForestClassifier
from sklearn.utils.extmath import density
from sklearn import metrics

#*******code snippet to insert**************************
from daal_naivebayes import DAALNavieBayes
#*******************************************************
run_version = 'daal'

# Display progress logs on stdout
logging.basicConfig(level=logging.INFO,
                    format='%(asctime)s %(levelname)s %(message)s')


# parse commandline arguments
op = OptionParser()
op.add_option("--report",
              action="store_true", dest="print_report",
              help="Print a detailed classification report.")
op.add_option("--chi2_select",
              action="store", type="int", dest="select_chi2",
              help="Select some number of features using a chi-squared test")
op.add_option("--confusion_matrix",
              action="store_true", dest="print_cm",
              help="Print the confusion matrix.")
op.add_option("--top10",
              action="store_true", dest="print_top10",
              help="Print ten most discriminative terms per class"
                   " for every classifier.")
op.add_option("--all_categories",
              action="store_true", dest="all_categories",
              help="Whether to use all categories or not.")
op.add_option("--use_hashing",
              action="store_true",
              help="Use a hashing vectorizer.")
op.add_option("--n_features",
              action="store", type=int, default=2 ** 16,
              help="n_features when using the hashing vectorizer.")
op.add_option("--filtered",
              action="store_true",
              help="Remove newsgroup information that is easily overfit: "
                   "headers, signatures, and quoting.")
op.add_option("--intro", dest="help", action="store_true")

def is_interactive():
    return not hasattr(sys.modules['__main__'], '__file__')

# work-around for Jupyter notebook and IPython console
argv = [] if is_interactive() else sys.argv[1:]
(opts, args) = op.parse_args(argv)
#if len(args) > 0:
#    op.error("this script takes no arguments.")
#    sys.exit(1)

if opts.help:
    print(__doc__)
    op.print_help()
    print()


# #############################################################################
# Load some categories from the training set

def load_npz():
    datasetfile = args[0] if len(args) > 0 else '20news.npz'
    print('loading dataset file: %s'%(datasetfile))
    
    dataset = np.load(datasetfile)
    
    data = dataset['data']
    targets = dataset['target']
    target_names = dataset['target_names']
    
    print('data shape:%s, targets:%s'%(data.shape, targets.shape))
    print('categories: %s'%(','.join([str(x) for x in target_names])))
    
    
    
def load_svm():
    datasetfile = args[0] if len(args) > 0 else '20news.svm'
    print('loading dataset file: %s'%(datasetfile))
    
    data, targets = load_svmlight_file(datasetfile)
    
    print('data shape:%s, targets:%s'%(data.shape, targets.shape))
    
    #data_train = fetch_20newsgroups(subset='train', categories=categories,
    #                                shuffle=True, random_state=42,
    #                                remove=remove)
    #
    #data_test = fetch_20newsgroups(subset='test', categories=categories,
    #                               shuffle=True, random_state=42,
    #                               remove=remove)
    return data, targets

#start loading
data, targets = load_svm()
target_names = [ str(x) for x in range(int(np.max(targets))+1)]

reccnt = data.shape[0]
permute = np.random.permutation(reccnt)
traincnt = int(reccnt * 0.8)
X_train = data[permute[:traincnt]]
y_train = targets[permute[:traincnt]]
X_test = data[permute[traincnt:]]
y_test = targets[permute[traincnt:]]
print('trainset: %d, testset: %d'%( traincnt, reccnt - traincnt))
print('data loaded')
 
# order of labels in `target_names` can be different from `categories`
#target_names = data_train.target_names


# mapping from integer feature name to original token string
feature_names = None

if opts.select_chi2:
    print("Extracting %d best features by a chi-squared test" %
          opts.select_chi2)
    t0 = time()
    ch2 = SelectKBest(chi2, k=opts.select_chi2)
    X_train = ch2.fit_transform(X_train, y_train)
    X_test = ch2.transform(X_test)
    if feature_names:
        # keep selected feature names
        feature_names = [feature_names[i] for i
                         in ch2.get_support(indices=True)]
    print("done in %fs" % (time() - t0))
    print()

if feature_names:
    feature_names = np.asarray(feature_names)


def trim(s):
    """Trim string to fit on terminal (assuming 80-column display)"""
    return s if len(s) <= 80 else s[:77] + "..."


# #############################################################################
# Benchmark classifiers
def benchmark(clf):
    print('_' * 80)
    print("Training: ")
    print(clf)
    t0 = time()

    #*******code snippet to insert**************************
    #clf.fit(X_train, y_train)
    #train_time = time() - t0
    #print("train time: %0.3fs" % train_time)

    #t0 = time()
    #pred = clf.predict(X_test)
    #test_time = time() - t0
    #print("test time:  %0.3fs" % test_time)

    pred = clf.fit_predict(X_train, y_train,X_test, y_test)
    test_time = time() - t0
    train_time = test_time
    print("tain & test time:  %0.3fs" % test_time)
    #*******************************************************

    score = metrics.accuracy_score(y_test, pred)
    print("accuracy:   %0.3f" % score)

    if hasattr(clf, 'coef_'):
        print("dimensionality: %d" % clf.coef_.shape[1])
        print("density: %f" % density(clf.coef_))

        if opts.print_top10 and feature_names is not None:
            print("top 10 keywords per class:")
            for i, label in enumerate(target_names):
                top10 = np.argsort(clf.coef_[i])[-10:]
                print(trim("%s: %s" % (label, " ".join(feature_names[top10]))))
        print()

    if opts.print_report:
        print("classification report:")
        print(metrics.classification_report(y_test, pred,
                                            target_names=target_names))

    if opts.print_cm:
        print("confusion matrix:")
        print(metrics.confusion_matrix(y_test, pred))

    print()
    clf_descr = str(clf).split('(')[0]
    return clf_descr, score, train_time, test_time


results = []
#for clf, name in (
#        (RidgeClassifier(tol=1e-2, solver="lsqr"), "Ridge Classifier"),
#        (Perceptron(n_iter=50), "Perceptron"),
#        (PassiveAggressiveClassifier(n_iter=50), "Passive-Aggressive"),
#        (KNeighborsClassifier(n_neighbors=10), "kNN"),
#        (RandomForestClassifier(n_estimators=100), "Random forest")):
#    print('=' * 80)
#    print(name)
#    results.append(benchmark(clf))
#
#for penalty in ["l2", "l1"]:
#    print('=' * 80)
#    print("%s penalty" % penalty.upper())
#    # Train Liblinear model
#    results.append(benchmark(LinearSVC(penalty=penalty, dual=False,
#                                       tol=1e-3)))
#
#    # Train SGD model
#    results.append(benchmark(SGDClassifier(alpha=.0001, n_iter=50,
#                                           penalty=penalty)))
#
## Train SGD with Elastic Net penalty
#print('=' * 80)
#print("Elastic-Net penalty")
#results.append(benchmark(SGDClassifier(alpha=.0001, n_iter=50,
#                                       penalty="elasticnet")))
#
## Train NearestCentroid without threshold
#print('=' * 80)
#print("NearestCentroid (aka Rocchio classifier)")
#results.append(benchmark(NearestCentroid()))

# Train sparse Naive Bayes classifiers
#print('=' * 80)
print
print("%s Naive Bayes"%run_version)
results.append(benchmark(DAALNavieBayes(n_node = 1, n_thread = 8, workdir='/20news/')))

#print('=' * 80)
#print("LinearSVC with L1-based feature selection")
## The smaller C, the stronger the regularization.
## The more regularization, the more sparsity.
#results.append(benchmark(Pipeline([
#  ('feature_selection', SelectFromModel(LinearSVC(penalty="l1", dual=False,
#                                                  tol=1e-3))),
#  ('classification', LinearSVC(penalty="l2"))])))
#
# make some plots

indices = np.arange(len(results))

results = [[x[i] for x in results] for i in range(4)]

clf_names, score, training_time, test_time = results
training_time = np.array(training_time) / np.max(training_time)
test_time = np.array(test_time) / np.max(test_time)

plt.figure(figsize=(12, 8))
plt.title("Score")
plt.barh(indices, score, .2, label="score", color='navy')
plt.barh(indices + .3, training_time, .2, label="training time",
         color='c')
plt.barh(indices + .6, test_time, .2, label="test time", color='darkorange')
plt.yticks(())
plt.legend(loc='best')
plt.subplots_adjust(left=.25)
plt.subplots_adjust(top=.95)
plt.subplots_adjust(bottom=.05)

for i, c in zip(indices, clf_names):
    plt.text(-.3, i, c)

#plt.show()
plt.savefig('naivebayes_perf_%s.png'%run_version)
