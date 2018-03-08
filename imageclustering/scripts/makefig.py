#!/usr/bin/python
# -*- coding: utf-8 -*-

"""
make the cluster of image files under sub-directories

make_result <dataroot> <resultfile> <sample file> <samplenumber>
    dataroot    ; source image files
    resultfile  ; .cluster file with cluster id in each line
    samplenum   ; how many sample files will deployed

"""

import sys, os
import numpy as np
from PIL import Image
import matplotlib
# Force matplotlib to not use any Xwindows backend.
matplotlib.use('Agg')

import matplotlib.pyplot as plt
import matplotlib.image as mpimg


def drawfig(rownum, samplenum, rids, samples, dataroot, retfile):
    #draw the result sample on a pdf file
    plt.figure(1)
    plt.gray()
    #plt.rcParams.update({'figure.figsize':(6*6, 5*6)})
    plt.rcParams.update({'figure.figsize':(6*8, 5*8)})
    
    hf, ha = plt.subplots(rownum, samplenum)
    
    hashf = open('hash.ids','w')

    for id in range(rownum):
        idx = samples[id,1]
        print('draw on %s'%idx)
        #cluster id
        sub_rids = rids[result[:] == idx]
    
        #perm = np.arange(sub_rids.shape[0])
        perm = np.random.permutation(sub_rids.shape[0])
    
        #sort the names
        #s_rids = sorted(sub_rids[perm[:samplenum]]) 
        #s_rids = sorted(sub_rids[perm]) 
        s_rids = sub_rids[perm]
    
        okcnt = 0
        #for sid in range(samplenum):
        for sid in range(len(s_rids)):
            #dst = '%d'%id + '/' + sub_rids[perm[sid]] + '.jpg'
            #src = dataroot + '/' + sub_rids[perm[sid]] + '.jpg'
            #print("ln -s " + src + " " + dst + '\n')
            #os.system("ln -s " + src + " " + dst)
    
            # each draw a line of image
            src = dataroot + '/' + s_rids[sid] + '.jpg'
            hashf.write('%s\n'%src)
            if os.path.exists(src):
                #img = Image.open(src).convert("L")
                img = Image.open(src)
                img.thumbnail((300, 250), Image.ANTIALIAS) # resizes image in-place
                ha[id, okcnt].axis('off')
                ha[id, okcnt].imshow(img)
                #print('%s'%sub_rids[perm[sid]].split('_')[0])
                #ha[id, sid].set_title('%s'%sub_rids[perm[sid]].split('_')[0])
                #ha[id, sid].set_title('%s'%s_rids[sid].split('_')[0])
                #ha[id, sid].set_title('%s'%id)
                okcnt += 1
                if okcnt >= samplenum:
                    break
    
    plt.tight_layout()
    plt.savefig(retfile)
    hashf.close()

if __name__ == "__main__":
    if len(sys.argv) != 5:
        print(__doc__)
        sys.exit(-1)
    
    
    dataroot=sys.argv[1]
    retfile = sys.argv[2]
    samplefile = sys.argv[3]
    samplenum = int(sys.argv[4])
        
    result = np.loadtxt(retfile).astype(int)
    rids = np.loadtxt('top10_hash.txt', dtype=np.dtype('|S64'))
    
    print('rids: %s'%rids[0])
    
    
    n_digits = len(np.unique(result))
    print('n_digits: %d, rids:%d'%(n_digits, rids.shape[0]))
    
    
    #load samples ids
    samples = np.loadtxt(samplefile).astype(int)
    print('samples: %d'%samples.shape[0])
    rownum = samples.shape[0]
    
    retfile = 'top10.pdf'
    drawfig(rownum, samplenum, rids, samples, dataroot, retfile)
