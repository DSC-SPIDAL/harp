
---
INSTALLATION
---

To compile, run 'make' in the cart directory.  If the default setup does not work, we've included others that has worked for us
'make linux' for ubuntu
'make mac' for mac os x
'make cloud' for debian

GCC and the Boost Threading Libraries are need to compile.

---
USAGE
---

After compiling, the Python scripts are ready to run.  They take a training file, testing file, various parameters, and print the predictions of the data in the both sets to stdout (training predictions first).  The training and test files need to be in the svm-light format (see http://www.cs.cornell.edu/people/tj/svm_light/svm_rank.html).  The paramaters are as follows:

NF (int) - number of features in the data sets.
DEPTH (float) - for gradient boosting each tree is typically limited to a certain depth to create weak learners.
ITERATIONS (int) - number of trees for the ensemble.
STEPSIZE (float) - for gradient boosting, only a fraction of the output is added to the running sum.
K (int) - for random forests, each tree only considers K features to split on.
PROC (int) - how many processors/threads to use.
initial_preds.txt (string) - optional paramater for boosting, initializes the predictions of each tuple in the data set (if not given, default value is 0).

Gradient Boosting
run 'do_boost.py training_data testing_data NF DEPTH ITERATIONS STEPSIZE PROC [inital_preds.txt] > preds.txt'

Random Forests
run 'do_forest.py training_data testing_data NF K ITERATIONS PROC > preds.txt'

IGBRT
run 'do_forest.py training_data testing_data NF K ITERATIONS PROC > bpreds.txt'
then boost with the output of random forests as the starting point
run 'do_boost.py training_data testing_data NF DEPTH ITERATIONS STEPSIZE PROC bpreds.txt > igbrt-preds.txt'

To run classification (as described the Burges et. al., see http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.143.6630&rep=rep1&type=pdf), run the appropriate '...-class.py' script with the same parameters.  Running IGBRT with classification is a bit different, see the demo file for an example.

---
OUTPUT
---

During the computations of each algorithm, statistics are printed out to stderr.
Each boosting iteration, 7 numbers are printed: iteration count, RMSE,ERR, and NDCG of the training set, testing set.
For forests, the current tree each processor has finished making.  Upon completion, the 7 numbers as with boosting.

When the algorithms complete, the predictions for the training and test sets are concatenated and printed to stdout.  Typically this is redirected to a file, and head/tail is used to extract the predictions for a particular data set.
