#!/usr/bin/python
# -*- coding: utf-8 -*-
"""
Remove empty columns in the .libsvm input dataset

empty columns have no effects on the tree building,
but they will cause problems in input data and model data alignment.

this is a workaround to preprocess the input data and remove those
'problem' columns. 
better solution should improve the feature2workindex_ data structure
in the code to deal with them correctly.


"""

import logging
import numpy as np
from optparse import OptionParser
import sys, os
from time import time
import datetime
from sklearn.datasets import load_svmlight_file,dump_svmlight_file 
 

def savedata(outfile, X, Y, removezero=True, fmt='libsvm'):
    with open(outfile, 'w') as outf:
        if fmt=='libsvm':
            for i in range(X.shape[0]):
                vec = X[i]
                ss = str(Y[i])   #label
                for j in range(vec.shape[0]):
                    if vec[j] != 0.0:
                        ss += ' %d:%s'%(j, vec[j])
                    elif not removezero:
                        ss += ' %d:%s'%(j, vec[j])
                recstr = '%s\n'%ss
                outf.write(recstr)

        else:
            for i in range(X.shape[0]):
                rec = ["%s"%(x) for x in X[i]]
                recstr = ','.join(rec) + ',%s'%Y[i] + ',\n'
                outf.write(recstr)


def load_option():
    op = OptionParser()
    op.add_option("--input",
                  action="store", type=str, default="", 
                  help="define the dataset file name.")
    op.add_option("--output",
                  action="store", type=str, default="", 
                  help="define the output file name.")
    op.add_option("--remove",
                  action="store", type=str, default="", 
                  help="define the remove column list.")
    op.add_option("--debug",
                  action="store_true", 
                  help="Show debug info.")
    op.add_option("--h",
                  action="store_true", dest="print_help",
                  help="Show help info.")
    
    (opts, args) = op.parse_args()
   
    if opts.remove != "":
        items = [int(x) for x in opts.remove.split(',')]
        opts.remove = items
    else:
        opts.remove = []
   
    #set default
    if opts.print_help:
        print(__doc__)
        op.print_help()
        print()
        sys.exit(0)

    return opts

def run(infile, outfile, removelist=[]):

    # get column statistics
    # x = [0] , y =[1]
    logger.info('Start loading data....')
    data = load_svmlight_file(infile)
    x = data[0].toarray()
    y = data[1]

    savelist = []
    n, m = x.shape
    if len(removelist) == 0:
        logger.info('Find the remove columns....')
        for j in range(m):
            minval = np.min(x[:,j])
            maxval = np.max(x[:,j])
            if (minval == maxval):
                #remove this column
                removelist.append(j)
            else:
                savelist.append(j)
    else:
        for j in range(m):
            if j not in removelist:
                savelist.append(j)

    logger.info('removelist:%s', removelist)

    # do remove
    logger.info('Get the data')
    saveX = x[:,savelist]

    logger.info('Start save to file %s', outfile)
    dump_svmlight_file(saveX, y, outfile)
    #savedata(outfile, saveX, y, True)


if __name__=="__main__":
    program = os.path.basename(sys.argv[0])
    logger = logging.getLogger(program)

    # logging configure
    import logging.config
    logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s')
    logging.root.setLevel(level=logging.DEBUG)
    logger.info("running %s" % ' '.join(sys.argv))

    opt = load_option()

    run(opt.input, opt.output, opt.remove)


