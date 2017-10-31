#!/usr/bin/python
# -*- coding: utf-8 -*-

"""
Convert svm light input dataset into .csv files.

Input: svm format
Output:
    train.csv    ;<features, label>
    test.csv     ;<features>
    groundtruth.csv      ;<labels>
Usage:
    data2csv.py --split <splitnum> --input <input> --output <output name>
    

"""

import sys
from scipy.io import loadmat
import numpy as np
import os
from glob import glob
import logging

from optparse import OptionParser
logger = logging.getLogger(__name__)

from sklearn.datasets import load_svmlight_file


def load_option():
    op = OptionParser()
    op.add_option("--split",
                  action="store", type=float, default=0.8, 
                  help="define the train/test split ratio, 0.8 by default.")
    op.add_option("--input",
                  action="store", type=str, default="", 
                  help="define the root directory of input data.")
    op.add_option("--type",
                  action="store", type=str, default="svm", 
                  help="define the input file type, svm format by default.")
    op.add_option("--output",
                  action="store", type=str, default="dataset", 
                  help="define the output file prefix.")
    op.add_option("--h",
                  action="store_true", dest="print_help",
                  help="Show help info.")
    
    (opts, args) = op.parse_args()
   
    if opts.print_help or \
        not opts.input or not os.path.exists(opts.input):
        print(__doc__)
        op.print_help()
        print('\n')
        sys.exit(0)

    return opts

def load_data(input, ftype):
    """
    inputs:
    1. svm file
    2. npz file (data, target, images)
    3. npz file (features, labels, rids)
    """

    if ftype =='svm':
        x, y = load_svmlight_file(opts.input)
        return x, y
    else:
        data = np.load(input)
        if 'data' in data:
            return data['data'], data['target']
        elif 'features' in data:
            return data['features'], data['labels']

    return None, None


if __name__ == "__main__":
    program = os.path.basename(sys.argv[0])
    logger = logging.getLogger(program)

    opts = load_option()

    # logging configure
    import logging.config
    logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s')
    logging.root.setLevel(level=logging.DEBUG)
    logger.info("running %s" % ' '.join(sys.argv))


    datafile = opts.output +'-train.csv'
    testfile = opts.output +'-test.csv'
    truthfile = opts.output +'-truth.csv'

    # check the path
    if not os.path.exists(datafile):
        logger.info('load image data....')

        x, y = load_data(opts.input, opts.type)


        logger.info('done!, x.shape= %s, y.shape=%s', x.shape, y.shape)
        if opts.type == 'svm':
            logger.info('sample x[0]=%s', x[0].toarray())

        reccnt = x.shape[0]
        split = int(reccnt * opts.split)
        if opts.split > 0 and opts.split < 1:
            permute = np.random.permutation(reccnt)
        else:
            permute = np.arange(reccnt)
 
        intlabel = True
        try:
            yy = int(y[0])
        except:
            intlabel = False

        if split >0:
            with open(datafile, 'w') as outf:
                for id in range(split):
                    if opts.type == 'svm':
                        items = [str(d) for d in x[permute[id]].toarray()[0]]
                    else:
                        items = [str(d) for d in x[permute[id]]]
                    if intlabel:
                        outf.write('%s,%s\n'%(','.join(items), int(y[permute[id]])))
                    else:
                        outf.write('%s,%s\n'%(','.join(items), y[permute[id]]))

        if split < reccnt:
            with open(testfile, 'w') as outf:
                for id in range(split, reccnt):
                    if opts.type == 'svm':
                        items = [str(d) for d in x[permute[id]].toarray()[0]]
                    else:
                        items = [str(d) for d in x[permute[id]]]
                    outf.write('%s\n'%(','.join(items)))

            with open(truthfile, 'w') as outf:
                for id in range(split, reccnt):
                    if intlabel:
                        outf.write('%s\n'%(int(y[permute[id]])))
                    else:
                        outf.write('%s\n'%(y[permute[id]]))

    else:
        logger.info('%s.csv alreay exist, quit.', datafile)
