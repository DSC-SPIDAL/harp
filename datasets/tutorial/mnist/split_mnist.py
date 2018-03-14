#!/usr/bin/env python
# -*- coding: utf-8 -*-
#

"""
Split mnist dataset into N partitions.
Input:
    dataset.X, .Y
Ouput:
    dataset_1.X, _1.Y
    dataset_2.X, _2.Y
    ...,    ...

Usage: split_mnist.py <dataset> <n>

"""
import sys
import numpy as np


def loadtxt(fname):
    with open(fname, 'r') as inf:
        return inf.readlines()
    return []

def writetxt(data, idx, fname):
    with open(fname, 'w') as wf:
        for pt in idx:
            wf.write(data[pt])

if __name__ == '__main__':
    if len(sys.argv) != 3:
        print(globals()['__doc__'] % locals())
        sys.exit(1)

    dataset = sys.argv[1]
    N = int(sys.argv[2])

    X = loadtxt(dataset + ".X")
    Y = loadtxt(dataset + ".Y")
    
    totalcnt = len(X)
    
    seq = np.random.permutation(np.arange(totalcnt))

    partlen = totalcnt / N

    for i in range(N):
        s = i* partlen
        t = s + partlen if (s+partlen<totalcnt) else totalcnt
        writetxt(X, seq[s:t], '%s_%s.X'%(dataset, i+1))
        writetxt(Y, seq[s:t], '%s_%s.Y'%(dataset, i+1))


