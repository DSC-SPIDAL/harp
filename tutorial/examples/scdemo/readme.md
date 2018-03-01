scdemo2017
============

(go to subdirectory "test", and run test on these datasets)

### datasets for scdemo

dataset     |   m   |   n   | class     |   type    |   content   | size
--------    | ----- | ----- | --------  | --------  | ------------| ---------
15scene     |  4485 | 1000  |  15       | dense int  |  image     |  20MB
lfw         |  5985 | 1850  |  400      | dense int  |  image     |  85MB
mnist8m     |  100,000   | 784   |  10       | sparse float   | image  | 230MB
20news      | 18,846 | 101,631 | 20    | sparse float | text  |  40MB
rcv1        | 804,414 | 47236 | 4/103     | sparse float | text | 1.5GB
movielens   | 6040 | 3883 |  | sparse int | ratings | 13MB/1M ratings


### file format

format          |  ext  | notes
----            |  ---- | --------
numpy ndarray   |  .npz | numpy uncompressed [.npz format](https://docs.scipy.org/doc/numpy-1.13.0/reference/generated/numpy.savez.html), see [example](http://scikit-learn.org/stable/modules/generated/sklearn.datasets.fetch_20newsgroups_vectorized.html#sklearn.datasets.fetch_20newsgroups_vectorized)
svmlight        |  .svm | [svmlight format](https://stackoverflow.com/questions/18339547/file-format-for-classification-using-svm-light)
matrix csv file |  .csv | comma seprated csv file, sparse matrix in [csr format](https://github.com/DSC-SPIDAL/harp/blob/04d55e5ab1e8e0241fee65872f40717df69f1af9/examples/java/com/intel/daal/examples/naive_bayes/MnNaiveBayesCSRDistr.java) @line 103 Service.createSparseTable
matrix market format | .mm | COO sparse matrix generally used for recommender system. [mm](http://math.nist.gov/MatrixMarket/formats.html)

### dataset descriptions

**15scene**

[Fifteen Scene Categories.](http://www-cvr.ai.uiuc.edu/ponce_grp/data/)

A dataset used in the scene categorization tasks. This is a dataset of fifteen natural scene categories that expands on the thirteen category dataset released by Fei-Fei Li. The two new categories are industrial and store. 
Extract sbow features with the imagenet feature extraction tool: ILSVRC2010_devkit-1.0.


**lfw**

[Labeled Faces in the Wild(LFW) people dataset.](http://scikit-learn.org/stable/modules/generated/sklearn.datasets.fetch_lfw_people.html#sklearn.datasets.fetch_lfw_people)

Each picture is centered on a single face. Each pixel of each channel (color in RGB) is encoded by a float in range 0.0 - 1.0.
original dataset.data : numpy array of shape (13233, 2914)
Each row corresponds to a ravelled face image of original size 62 x 47 pixels.


**mnist8m**

A subset of the [infinite MNIST dataset](http://leon.bottou.org/projects/infimnist).
This code produces an infinite supply of digit images derived from the well known MNIST dataset using pseudo-random deformations and translations. 


**20news**

The [20 newsgroups dataset](http://scikit-learn.org/stable/datasets/index.html#the-20-newsgroups-text-dataset) comprises around 18000 newsgroups posts on 20 topics split in two subsets: one for training (or development) and the other one for testing (or for performance evaluation). The split between the train and test set is based upon a messages posted before and after a specific date.

**rcv1**

RCV1 [Reuters Corpus Volume 1](http://www.ai.mit.edu/projects/jmlr/papers/volume5/lewis04a/lyrl2004_rcv1v2_README.htm)
A corpus of newswire stories made available by Reuters, Ltd. There are 103 categories and 804,414 documents.


**movielens**

GroupLens Research has collected and made available rating data sets from the [MovieLens web site](http://movielens.org). The data sets were collected over various periods of time, depending on the size of the set.

ml-1m is one of the stable benchmark datasets, with 1 million ratings from 6000 users on 4000 movies. Released 2/2003.

