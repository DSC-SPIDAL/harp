---
title: Harp Support Vector Machine
---

In machine learning, support vector machines (SVM) are supervised learning models with associated learning algorithms that analyze data used for classification and regression analysis. Given a set of training examples, each marked as belonging to one or the other of two categories, an SVM training algorithm builds a model that assigns new examples to one category or the other, making it a non-probabilistic binary linear classifier. An SVM model is a representation of the examples as points in space, mapped so that the examples of the separate categories are divided by a clear gap that is as wide as possible. New examples are then mapped into that same space and predicted to belong to a category based on which side of the gap they fall.

In addition to performing linear classification, SVMs can efficiently perform a non-linear classification using what is called the kernel trick, implicitly mapping their inputs into high-dimensional feature spaces.

![SVM-1](/img/4-4-1.png)

In this project, Harp won't touch the core code base of computing SVM. It will use LibSVM which is an open source library and do parallel around LibSVM. LibSVM is a popular open source machine learning library, which is developed at the National Taiwan University and written in C++ though with other programming languages' APIs. LibSVM implements the SMO algorithm for kernelized support vector machines (SVMs), supporting classification and regression.

## METHOD

The Harp based SVM algorithm works as follows:

1. The training set of the algorithm is split into subsets.

2. Each node trains sub dataset locally via LibSVM's API.

3. Allgather support vectors to global.

4. Each node combines its training data and the global support vectors.

5. Repeat Step 2 to 4 until support vectors don't change any more.

![SVM-2](/img/4-4-2.png)


# RESULT
We can see from the figure that with larger number of mappers, the number of support vectors has greater  gradient decent. Less iterations are needed to reach the final result which however remains the same.

![result](/img/svm/result.png)


# COMPARISON
The speed up time both has an linear acceleration along with the number of mappers. Harp performance is better than Hadoop since it reduces I/Os of communication.

![comparison](/img/svm/comparison.png)


# REFERENCE
[1] Suykens, Johan AK, and Joos Vandewalle. “Least squares support vector machine classifiers.” Neural processing letters 9.3 (1999): 293-300.

[2] Zhang, Bingjing, Yang Ruan, and Judy Qiu. “Harp: Collective communication on hadoop.” Cloud Engineering (IC2E), 2015 IEEE International Conference on. IEEE, 2015.

[3] Çatak, Ferhat Özgür, and Mehmet Erdal Balaban. “A MapReduce-based distributed SVM algorithm for binary classification.” Turkish Journal of Electrical Engineering & Computer Sciences 24.3 (2016): 863-873.

[4] LeCun, Yann, Corinna Cortes, and Christopher JC Burges. “The MNIST database of handwritten digits.” (1998).

[5] docs.opencv.org/doc/tutorials/ml/introduction_to_svm/introduction_to_svm.html

