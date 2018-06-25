---
title: Optimization Solvers 
---

An optimization solver is an algorithm to solve an optimization problem, which is to find the maximum or minimum of an 
objective function in the presence of constraints on its variables. 

Harp-DAAL currently provides the following iterative optimization solvers

## Adaptive Subgradient Method 

The adaptive subgradient method (AdaGrad) is from [^fn1].

More algorithmic details from Intel DAAL documentation is [here](https://software.intel.com/en-us/daal-programming-guide-adaptive-subgradient-method).

## Limited-Memory Broyden-Fletcher-Goldfarb-Shanno Algorithm

The limited-memory Broyden-Fletcher-Goldfarb-Shanno (LBFGS) algorithm is from [^fn2].

More algorithmic details from Intel DAAL documentation is [here](https://software.intel.com/en-us/daal-programming-guide-limited-memory-broyden-fletcher-goldfarb-shanno-algorithm).

## Stochastic Gradient Descent Algorithm

The stochastic gradient descent (SGD) algorithm is a special case of an iterative solver. 
The following computation methods are available in Intel DAAL for the stochastic gradient descent algorithm:

### Mini-batch.

The mini-batch method (miniBatch) of the stochastic gradient descent algorithm is from [^fn3].

### Momentum

The momentum method (momentum) of the stochastic gradient descent algorithm is from [^fn4]. 

More algorithmic details from Intel DAAL documentation is [here](https://software.intel.com/en-us/daal-programming-guide-stochastic-gradient-descent-algorithm).

[^fn1]: Elad Hazan, John Duchi, and Yoram Singer. Adaptive subgradient methods for online learning and stochastic optimization. The Journal of Machine Learning Research, 12:21212159, 2011.

[^fn2]: R. H. Byrd, S. L. Hansen, Jorge Nocedal, Y. Singer. A Stochastic Quasi-Newton Method for Large-Scale Optimization, 2015. arXiv:1401.7020v2 [math.OC]. Available from http://arxiv.org/abs/1401.7020v2.

[^fn3]: Mu Li, Tong Zhang, Yuqiang Chen, Alexander J. Smola. Efficient Mini-batch Training for Stochastic Optimization, 2014. Available from https://www.cs.cmu.edu/~muli/file/minibatch_sgd.pdf.

[^fn4]: David E. Rumelhart, Geoffrey E. Hinton, Ronald J. Williams. Learning representations by back-propagating errors. Nature (323), pp. 533-536, 1986.
