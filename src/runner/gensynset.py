# pylint: skip-file
import sys, argparse
import xgboost as xgb
import numpy as np
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
                    if not np.isnan(vec[j]):
                        ss += ' %d:%.8f'%(j, vec[j])
                recstr = '%s\n'%ss
                outf.write(recstr)

        else:
            for i in range(X.shape[0]):
                rec = ["%.8f"%(x) for x in X[i]]
                recstr = ','.join(rec) + ',%s'%Y[i] + ',\n'
                outf.write(recstr)


def generate_data(args):
    print("Generating dataset: {} rows * {} columns".format(args.rows, args.columns))
    print("{}/{} test/train split".format(args.test_size, 1.0 - args.test_size))

    tmp = time.time()
    X, y = make_classification(args.rows, n_features=args.columns, n_redundant=0, n_informative=args.columns, n_repeated=0, random_state=7)

    if args.sparsity < 1.0:
       X = np.array([[np.nan if rng.uniform(0, 1) < args.sparsity else x for x in x_row] for x_row in X])

    if args.test_size > 0:
        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=args.test_size, random_state=7)
    else:
        X_train = X
        y_train = y
        X_test = None
        y_test = None
    print ("Generate Time: %s seconds" % (str(time.time() - tmp)))

    #save to .csv file
    if args.sparsity == 1.0:
        np.savetxt('synset_train_%dx%d.csv'%(args.rows, args.columns), np.concatenate((X_train, y_train.reshape((y_train.shape[0],1))), axis=1), fmt='%.8f', delimiter=',')

    savedata('synset_train_%dx%d_%f.libsvm'%(args.rows, args.columns,args.sparsity), X_train, y_train)

    if (X_test is not None):
        np.savetxt('synset_test_%dx%d.csv'%(args.rows, args.columns), np.concatenate((X_test, y_test.reshape((y_test.shape[0],1))), axis=1), fmt='%.8f',delimiter=',')
        savedata('synset_test_%dx%d.libsvm'%(args.rows, args.columns), X_test, y_test)


if __name__=="__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('--sparsity', type=float, default=0.0)
    parser.add_argument('--rows', type=int, default=1000000)
    parser.add_argument('--columns', type=int, default=50)
    parser.add_argument('--test_size', type=float, default=0.)
    args = parser.parse_args()

    generate_data(args)
