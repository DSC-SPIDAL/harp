"""
Demo code for surprise library.

http://surpriselib.com/

A simple example showing how you can (down)load a dataset, split it for n-folds cross-validation, and compute the MAE and RMSE of the SVD algorithm.

"""
from collections import defaultdict
import  numpy as np
from surprise import SVD
from surprise import Dataset
from surprise import evaluate, print_perf, accuracy

# Load the movielens-100k dataset (download it if needed),
# and split it into 3 folds for cross-validation.
#data = Dataset.load_builtin('ml-100k')
data = Dataset.load_builtin('ml-1m')
data.split(n_folds=3)

# We'll use the famous SVD algorithm. (MF-SGD)
#algo = SVD()
# Evaluate performances of our algorithm on the dataset.
#perf = evaluate(algo, data, measures=['RMSE', 'MAE'])

#
# extract the source code of above process
#
performances = defaultdict(list)
algo = SVD(n_factors = 100, n_epochs = 20, lr_all = 0.005, reg_all = 0.02)
measures=['RMSE', 'MAE']
verbose = True

#save the data to coo
for fold_i, (trainset, testset) in enumerate(data.folds()):
    trainf = open('ml-1m_train_%d.mm'%fold_i,'wb')
    for uid, iid, rate in trainset.all_ratings():
        trainf.write('%s %s %s\n'%(uid, iid, rate))
    trainf.close()

    testf = open('ml-1m_test_%d.mm'%fold_i,'wb')
    for uid, iid, rate in testset:
        testf.write('%s %s %s\n'%(uid, iid, rate))
    testf.close()

    if verbose:
        print('-' * 12)
        print('Fold ' + str(fold_i + 1))


    #
    # TODO: change to harp-daal mf-sgd call
    #
    #==================================================================
    # train and test algorithm. Keep all rating predictions in a list
    algo.train(trainset)
    predictions = algo.test(testset)
    #==================================================================


    # compute needed performance statistics
    for measure in measures:
        f = getattr(accuracy, measure.lower())
        performances[measure].append(f(predictions, verbose=verbose))

    if verbose:
        print('-' * 12)
        print('-' * 12)
        for measure in measures:
            print('Mean {0:4s}: {1:1.4f}'.format(
                    measure.upper(), np.mean(performances[measure])))
        print('-' * 12)
        print('-' * 12)

#end
print_perf(performances)
