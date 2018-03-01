#!/usr/bin/python
# -*- coding: utf-8 -*-

"""
Convert .sbow.mat input dataset into .npz format.

Input: 
    root directory of the image dataset, 
    load all .sbow.mat files as input

Output:
    .npz    ; the dataset format in numpy ndarray
        ['features']    ;sbow features
        ['lbaels']      ;the class name/id
        ['rdis']        ;the uniq record id, which has the format of sid_seqid

Usage:
    make_dataset.py --input <rootdir> --output <output name>

"""

import sys
from scipy.io import loadmat
import numpy as np
import os
from glob import glob
import logging

from optparse import OptionParser
logger = logging.getLogger(__name__)



def pad_to(X, n):
    """Pad a 1-d array of size <= n with zeros to size n."""
    if X.ndim != 1:
        raise ValueError("Only 1-d arrays can be padded.")
    size = X.size
    if size == n:
        return X
    elif size < n:
        return np.hstack([X, np.zeros((n-size))])
    else:
        raise ValueError("Size of X must be smaller or equal to n.")

def cached_bow(files):
    features = []
    file_names = []
    wnids = []
    rids = []
    counts = []

    for bow_file in files:
        print("loading %s"%bow_file)
        bow_structs = loadmat(bow_file, struct_as_record=False)['image_sbow']
        file_names.extend([str(x[0]._fieldnames) for x in bow_structs])
        #get sbow
        bags_of_words = [pad_to(np.bincount(struct[0].sbow[0][0].word.ravel()), 1000) for struct in bow_structs]
        #get ID
        ids = [struct[0].ID[0] for struct in bow_structs]

        rids.extend(ids)
        features.extend(bags_of_words)

        # if we where interested in the actual words:
        #words = [struct[0][1][0][0][0] for struct in bow_structs]
        # there is other stuff in the struct but I don't care at the moment:
        #x = [struct[0][1][0][0][1] for struct in bow_structs]
        #y = [struct[0][1][0][0][2] for struct in bow_structs]
        #scale = [struct[0][1][0][0][3] for struct in bow_structs]
        #norm = [struct[0][1][0][0][4] for struct in bow_structs]
        wnid = os.path.basename(bow_file).split(".")[0]
        wnids.append(wnid)
        counts.append(len(bags_of_words))

    features = np.array(features)
    rids = np.array(rids)

    return features, wnids, rids, counts


class ImageNetData(object):
    """ 
    dataset directory structure:
        root/
            imagenetxx.csv  ;  <sid , name>
            sbow    ;   sid.mat
            img     ;   sid/sid_xxid.jpg
    """
    def __init__(self, root_path, subset_file):
        self.root_path = root_path
        self.bow_path = root_path
        #self.bow_path = root_path + '/sbow/'
        self.image_path = root_path + '/img/'

    def img_path_from_id(self, classidx, imgidx):
        wnid = self.wnids[classidx]
        return os.path.join(self.image_path, wnid, wnid+'_'+imgidx+".JPEG")

    def class_idx_from_string(self, search_string):
        """Get class index from string in class name."""
        indices = np.where([search_string in x[2][0] for x in self.synsets])[0]
        for i in indices:
            print(self.synsets[i])
        return indices

    def get_image_ids(self, theclass):
        wnid = self.wnids[theclass]
        files = glob(os.path.join(self.image_path,wnid,wnid+"*"))
        filenames = [os.path.basename(f)[:-5] for f in files]
        numbers = map(lambda f: f.split("_")[1], filenames)
        return numbers

    def load_bow(self, save=''):
        """Get bow representation of dataset ``dataset``.
        Legal values are ``train``, ``val`` and ``test``.

        Returns
        -------
        features : numpy array, shape [n_samples, n_features],
            containing bow representation of all images in given dataset

        labels : numpy array, shape [n_samples],
            containing classidx for image labels. (Not available for ``test``)
        """
        if not self.bow_path:
            raise ValueError("You have to specify the path to" 
                "the bow features in ``bow_path`` to be able"
                "to load them")

        paths = os.path.join(self.bow_path, "*.sbow.mat")
        logger.info('check the path for sbow files: %s', paths)
        files = glob(os.path.join(self.bow_path, "*.sbow.mat"))

        if len(files) == 0:
            raise ValueError("Could not find any bow files.")

        features, wnids, rids, counts = cached_bow(files)
        labels = [[wnid] * count for wnid, count in zip(wnids, counts)]
        labels = np.array(labels).ravel()

        labels = np.array([ rid.split('_')[0] for rid in rids])

        return features, labels, rids

def load_option():
    op = OptionParser()
    op.add_option("--input",
                  action="store", type=str, default="", 
                  help="define the root directory of input data.")
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


if __name__ == "__main__":
    program = os.path.basename(sys.argv[0])
    logger = logging.getLogger(program)

    opts = load_option()

    # logging configure
    import logging.config
    logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s')
    logging.root.setLevel(level=logging.DEBUG)
    logger.info("running %s" % ' '.join(sys.argv))


    datafile = opts.output +'.npz'

    # check the path
    if not os.path.exists(datafile):
        logger.info('load image data....')
        imnet = ImageNetData(opts.input, opts.output)
        features, labels, rids = imnet.load_bow()
        np.savez(opts.output, features=features, labels=labels, rids=rids)
    else:
        logger.info('load image data from cached datafile: %s', datafile)
        f = np.load(datafile)
        features = f['features']
        labels = f['labels']
        rids = f['rids']

    # go clustering
    logger.info('data shapes: features:%s, labels:%s, rids:%s', features.shape, labels.shape, rids.shape)

#logger.info('%s, %s, %s', features[0], labels[0], rids[0])
