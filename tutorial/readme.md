tutorial
=================

## Environment & Dependencies

Package | version
-----  | ------------
python |    2.7.12  | 
numpy  |    1.13.1  | Library for large, multi-dimensional arrays and matrices
scipy             |        0.19.1   | Python Scientific Computing Library
scikit-learn      |        0.19.0   | Machine Learning in Python
matplotlib        |        2.0.2    | Matplotlib: Python plotting
pillow            |        3.2.0    | Python Imaging Library
scikit-surprise   |        1.0.4    | A Python scikit for recommender systems

All these packages should be first installed properly. [Anaconda](https://anaconda.org/) is recommended distribution easy to use.


## Installation

```sh
git clone git@github.iu.edu:pengb/scdemo.git
```

## Run Applications

```sh
cd scdemo/test
../bin/run_kmeans.sh
```

This script run image clustering application on the 15scene(Fifteen Scene Categories) dataset.

There are two output files:

+ draw plot of clusters on 2D dimension, save as kmeans_2d.png
+ draw samples of the result clusters, save as random.pdf with each line a cluster



