#!/usr/bin/python
# -*- coding: utf-8 -*-

"""
make the cluster of image files under sub-directories

make_result <dataroot> <resultfile> <samplenumber>
    dataroot    ; source image files
    resultfile  ; .cluster file with cluster id in each line
    samplenum   ; how many sample files will deployed

"""

from time import time
import sys, os
import numpy as np
from PIL import Image
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.image as mpimg
from matplotlib.artist import setp
from collections import defaultdict


if len(sys.argv) != 4:
    print(__doc__)
    sys.exit(-1)


dataroot=sys.argv[1]
retfile = sys.argv[2]
retname = retfile[:retfile.rfind('.')]
samplenum = int(sys.argv[3])

#check the arguments
def check_file(fname):
    if not os.path.exists(fname):
        print('Error: %s not found, quit'%fname)
        return False
    return True

if not check_file(dataroot) or not check_file(retfile) or not check_file('standard.rids'):
    sys.exit(-1)

result = np.loadtxt(retfile).astype(int)
rids = np.loadtxt('standard.rids', dtype=np.str)
n_digits = len(np.unique(result))
print('cluster number: %d, images number:%d'%(n_digits, rids.shape[0]))

# match if there is 



t1 = time()
#make directories
#os.system("mkdir -p result")
#os.chdir('result')
#for id in range(n_digits):
#    os.system("mkdir -p %d"%id)

#perm = np.random.permutation(rids.shape[0])
#for id in range(samplenum*n_digits):
#
#    dst = '%d'%result[perm[id]] + '/' + rids[perm[id]] + '.JPEG'
#    src = dataroot + '/' + rids[perm[id]] + '.JPEG'
#
#    print("ln -s " + src + " " + dst + '\n')
#    os.system("ln -s " + src + " " + dst)
#
#draw the result sample on a pdf file
plt.figure(1)
plt.gray()
#plt.rcParams.update({'figure.figsize':(6*6, 5*6)})
plt.rcParams.update({'figure.figsize':(6*8, 5*8)})

hf, ha = plt.subplots(n_digits, samplenum)
np.random.seed()

for id in range(n_digits):
    #cluster id
    sub_ret = result[result[:] == id]
    sub_rids = rids[result[:] == id]

    #get the majority
    totalcnt = sub_rids.shape[0]
    if totalcnt == 0:
        print('%s cluster is a Zero cluster?'%(id))
        continue


    clusternames = [ x.split('_')[0] for x in sub_rids]
    ccnt = defaultdict(int)
    for name in clusternames:
        ccnt[name] += 1
    name = sorted(ccnt, key=ccnt.get, reverse=True)[0]
    print('%d cluster majority images are: %s,\t weight=%d/%d=%.2f'%(id, name, ccnt[name], totalcnt, ccnt[name]*1.0/totalcnt))

    #get a sample
    perm = np.random.permutation(sub_rids.shape[0])

    #sort the names
    s_rids = sub_rids[perm[:samplenum]]
    s_rids = sorted(s_rids, key = lambda x:ccnt[x.split('_')[0]], reverse=True) 

    totalnum = min(samplenum, totalcnt)
    #for sid in range(samplenum):
    for sid in range(totalnum):
        #dst = '%d'%id + '/' + sub_rids[perm[sid]] + '.jpg'
        #src = dataroot + '/' + sub_rids[perm[sid]] + '.jpg'
        #print("ln -s " + src + " " + dst + '\n')
        #os.system("ln -s " + src + " " + dst)

        # each draw a line of image
        #src = dataroot + '/' + s_rids[sid] + '.jpg'

        #for 15scene special
        subdir = s_rids[sid].split('_')[0]
        if subdir:
            src = dataroot + '/' + subdir + '/' + s_rids[sid] + '.jpg'
        else:
            src = dataroot + '/' + s_rids[sid] + '.jpg'

        img = Image.open(src).convert("L")
        img.thumbnail((300, 250), Image.ANTIALIAS) # resizes image in-place
        #ha[id, sid].axis('off')
        ha[id, sid].set_xticks(())
        ha[id, sid].set_yticks(())

        #print('%s'%sub_rids[perm[sid]].split('_')[0])
        #ha[id, sid].set_title('%s'%sub_rids[perm[sid]].split('_')[0])
        title_obj = ha[id, sid].set_title('%s'%s_rids[sid].split('_')[0])

        #plt.getp(title_obj)                    #print out the properties of title
        #plt.getp(title_obj, 'text')            #print out the 'text' property for title
        # set color
        if subdir == name:
            setp(title_obj, color='r', weight='bold')         #set the color of title to red
        else:
            setp(title_obj, color='k', weight='light')         #set the color of title to red


        ha[id, sid].imshow(img)

plt.tight_layout()
plt.savefig(retname + '.pdf')

print('===>draw samples of clusters in %s.pdf with each line denotes a cluster, in %.2fs'%(retname, time() - t1))


