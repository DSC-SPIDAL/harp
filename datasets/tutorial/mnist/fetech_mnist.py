#!/usr/bin/env python
# -*- coding: utf-8 -*-
#

"""
Fetech mnist dataset from sklearn dataset.
Output:
    mnist_data.X ; 70000x784
    mnist_data.Y ; 70000x10

"""

import numpy as np
from sklearn.datasets import fetch_mldata


mnist = fetch_mldata('MNIST original', data_home='.')

print('data.shape=%s,%s'%mnist.data.shape)
print('target.shape=%s'%mnist.target.shape)
rows = mnist.target.shape[0]
labels = np.unique(mnist.target).shape[0]
print('unique labels=%s'%np.unique(mnist.target))
print('rows = %d, labelcnt = %d'%(rows, labels))


#scale
from sklearn import preprocessing
mnist.data = preprocessing.scale(mnist.data)

target = np.zeros((rows, labels))
for r in range(rows):
    target[r, mnist.target[r]] = 1.

#save
#np.savetxt("mnist_data.txt",mnist.data, fmt='%.8f')
#np.savetxt("mnist_target.txt", target, fmt='%.8f')

#need ' ' at the end of each line, stupid code
def savetxt(fname, x,fmt):
    wf = open(fname, 'w')
    for rowv in x:
        l = [fmt%elem for elem in rowv] 
        wf.write('%s \n'%' '.join(l))

savetxt("mnist_data.X",mnist.data, fmt='%.8f')
savetxt("mnist_data.Y", target, fmt='%.8f')




