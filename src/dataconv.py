#!/usr/bin/python
# -*- coding: utf-8 -*-

"""
Data Format Converter

File Format type: 
    svm ;   format
    npz ;   numpy ndarray format
    csv ; dense matrix
    csr ; sparse matrix
    
Usage:
    dataconv.py --from <input type> --to <output type> <input> <output>

"""

import sys
from scipy.io import loadmat
import numpy as np
import os
from glob import glob
import logging
from optparse import OptionParser
from sklearn.datasets import load_svmlight_file, dump_svmlight_file
import scipy.sparse as sparse

logger = logging.getLogger(__name__)

# global constants
oneBased = 1
zeroBased = 0


def load_csr_file(fname, indexing = oneBased):
    """
    daal csr format, comma separate and oneBased

    """
    inf = open(fname, 'r') 
    # the last comma ',' create an null item, remove it
    rowoffset = inf.readline().strip().split(',')[:-1]
    colindex = inf.readline().strip().split(',')[:-1]
    values = inf.readline().strip().split(',')[:-1]
    
    print('rowoffset:%s, colindex:%s, values=%s'%(len(rowoffset), len(colindex), len(values)))
    print('rowoffset:%s, colindex:%s, values=%s'%(rowoffset[:10], colindex[:10], values[:10]))


    rowoffset = [ int(x) - indexing for x in rowoffset]
    colindex = [ int(x) - indexing for x in colindex]
    values = [ float(x) for x in values]

    x = sparse.csr_matrix((values, colindex, rowoffset))

    return x

def dump_csr_file(x, y, fname, indexing = oneBased):
    """
    x, y    ; sparse matrix in csr format

    output:
        _train_csr_id.csv
        _train_labels_id.csv
    """

    #check format
    if not sparse.isspmatrix_csr(x):
        print('Error: csr sparse matrix not found in the inputs')
        return False
    
    trainfile = fname + '_train_csr.csv'
    labelfile = fname + '_labels.csv'

    #write rowoffset
    rowoffset = [ str(item + indexing) for item in x.indptr]
    colIndex = [ str(item + indexing) for item in x.indices ] 
    values = [ str(item) for item in x.data ]

    with open(trainfile, 'wb') as outf:
        outf.write(','.join(rowoffset))
        outf.write(',\n')
        outf.write(','.join(colIndex))
        outf.write(',\n')
        outf.write(','.join(values))
        outf.write(',\n')

    with open(labelfile, 'wb') as outf:
        labels = '\n'.join([ str(item) for item in y ])
        outf.write(labels)
        outf.write('\n')
 
def load_option():
    op = OptionParser()
    op.add_option("--from",
                  action="store", type=str, default="svm", dest="fromtype",
                  help="define the input file type, svm format by default.")
    op.add_option("--to",
                  action="store", type=str, default="csr", dest="totype",
                  help="define the output file type, csr format by default.")
    op.add_option("--labelfile",
                  action="store", type=str, default="",
                  help="define the label file for csr format input.")
    op.add_option("--h",
                  action="store_true", dest="print_help",
                  help="Show help info.")
    
    (opts, args) = op.parse_args()
   
    if opts.print_help or len(args) != 2 or \
        not os.path.exists(args[0]):
        print(__doc__)
        op.print_help()
        print('\n')
        sys.exit(0)

    return opts, args

def load_data(infile, ftype, labelfile = None):
    """
    inputs:
    1. svm file
    2. npz file (data, target, images)
    3. npz file (features, labels, rids)
    4. csr
    5. csv  
    """

    if ftype =='svm':
        x, y = load_svmlight_file(infile)
        return x, y
    elif ftype == 'npz':
        data = np.load(infile)
        if 'data' in data:
            return data['data'], data['target']
        elif 'features' in data:
            return data['features'], data['labels']
    elif ftype == 'csr':
        x = load_csr_file(infile)
        if labelfile:
            y = np.loadtxt(labelfile)
        else:
            y = np.zeros(x.shape[0])
            logger.error('ERROR: labelfile not provided')
        return x, y

    return None, None

def convert2csv(infile, output, fromtype, labelfile, split = 0.2):
    """
    convert 2 dense matrix csv file, the last column is label
    """
    datafile = output +'-train.csv'
    testfile = output +'-test.csv'
    truthfile = output +'-truth.csv'

    # check the path
    if not os.path.exists(datafile):
        logger.info('load image data....')

        x, y = load_data(infile, fromtype, labelfile)

        logger.info('done!, x.shape= %s, y.shape=%s', x.shape, y.shape)
        if fromtype == 'svm':
            logger.info('sample x[0]=%s', x[0].toarray())

        reccnt = x.shape[0]
        split = int(reccnt * split)
        if split > 0 and split < 1:
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
                    if fromtype == 'svm':
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
                    if fromtype == 'svm':
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

def convert(infile, output, fromtype, totype, labelfile):
    """
    """
    x, y = load_data(infile, fromtype, labelfile)

    logger.info('done!, x.shape= %s, y.shape=%s', x.shape, y.shape)
 
    if totype == 'csr':
        dump_csr_file(x, y, output)
    elif totype == 'svm':
        dump_svmlight_file(x, y, output)
    else:
        print('Error: %s not support yet'%totype)

if __name__ == "__main__":
    program = os.path.basename(sys.argv[0])
    logger = logging.getLogger(program)

    opts, args = load_option()

    # logging configure
    import logging.config
    logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s')
    logging.root.setLevel(level=logging.DEBUG)
    logger.info("running %s" % ' '.join(sys.argv))

    infile = args[0]
    outfile = args[1]

    if opts.totype == 'csv':
        convert2csv(infile, outfile, opts.fromtype, opts.labelfile)
    else:
        convert(infile, outfile, opts.fromtype, opts.totype, opts.labelfile)

