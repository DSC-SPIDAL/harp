#!/usr/bin/python
# -*- coding: utf-8 -*-

"""
run pca on yfcc100m dataset

vgg input vector file format:
id \t vgg \t 4096 \t vector


Usage:
    pca2yfcc.py <-train|-predict> <batch list file> 
        -train      ; <sample number> <component number> <output model>
        -predict    ; 

"""

import sys, os, math,re
import numpy as np
import logging
import tarfile
import random
from sklearn.externals import joblib
from sklearn.decomposition import PCA

logger = logging.getLogger(__name__)


def load_vggfile(inputfile):
    #load file
    logger.info('loading %s...', inputfile)
    fs = []
    with open(inputfile, 'r') as inf:
        for l in inf:
            its = l.strip().split('\t')
            vals = [float(x) for x in its[3].split()]
            fs.append(vals)
    fsarr = np.array(fs)
    return fsarr

def read_vggs(samples):
    data = None
    for l in samples:
        tmp = load_vggfile(l)
        if data is None:
            data = tmp
        else:
            #concanate
            data = np.concatenate((data,tmp), axis=0)
    return data

def load_batchlist(inputfile):
    #load file
    fs = []
    with open(inputfile, 'r') as inf:
        for l in inf:
            vals = l.strip()
            fs.append(vals)
    return fs

def get_sample(box, num):
    """
        get random num samples from the box
    """
    ids = np.arange(len(box))
    perm = np.random.permutation(len(box))

    samples = [box[x] for x in perm[:num]]
    return samples

def transform_vggs(model, box, boxname='yfcc128', block=100, outdir='pca128'):
    
    reccnt = len(box)
    for blkid in range(reccnt/block):
        sample = box[blkid*block: (blkid+1)*block]
        data = read_vggs(sample)
        data = model.transform(data)
        outname = outdir + '/%s-%04d'%(boxname,blkid)
        #np.save(outname, data)
        np.savetxt(outname, data, fmt="%f", delimiter=',')

    if (reccnt % block) != 0:
        blkid = int(reccnt/block)
        sample = box[blkid*block:]
        data = read_vggs(sample)
        data = model.transform(data)
        outname = outdir + '/%s-%04d'%(boxname,blkid)
        #np.save(outname, data)
        np.savetxt(outname, data, fmt="%f", delimiter=',')

if __name__ == '__main__':
    program = os.path.basename(sys.argv[0])
    logger = logging.getLogger(program)

    # logging configure
    import logging.config
    logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s')
    logging.root.setLevel(level=logging.DEBUG)
    logger.info("running %s" % ' '.join(sys.argv))

    # check and process input arguments
    if len(sys.argv) < 2:
        print(globals()['__doc__'] % locals())
        sys.exit(1)

    listfile = sys.argv[2]
    modelname = sys.argv[3]

    if sys.argv[1] == '-train':
        box = load_batchlist(listfile)
        samples = get_sample(box, 100)
        data = read_vggs(samples)
        pca = PCA(n_components=128, whiten=True, svd_solver='full')
        model = pca.fit(data)
        
        joblib.dump(model, modelname)

    else:
        box = load_batchlist(listfile)
        model = joblib.load(modelname)
        #box = box[:100]
        boxname = listfile.split('/')[-1]
        #transform_vggs(model, box, boxname = boxname, block = 65536)
        transform_vggs(model, box, boxname = boxname)

