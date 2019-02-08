#!/usr/bin/python
# -*- coding: utf-8 -*-

import logging
import numpy as np
from optparse import OptionParser
import sys, os
from time import time
import datetime
 

def load_option():
    op = OptionParser()
    op.add_option("--input",
                  action="store", type=str, default="", 
                  help="define the dataset file name.")
    op.add_option("--output",
                  action="store", type=str, default="", 
                  help="define the output file name.")
    op.add_option("--debug",
                  action="store_true", 
                  help="Show debug info.")
    op.add_option("--h",
                  action="store_true", dest="print_help",
                  help="Show help info.")
    
    (opts, args) = op.parse_args()
   
    #set default
    if opts.print_help:
        print(__doc__)
        op.print_help()
        print()
        sys.exit(0)

    return opts

def run(infile, outfile):
    logger.info('Start loading data....')

    lineid = 0
    outf = open(outfile, 'w')
    with open(infile) as inf:
        for line in inf:
            #libsvm format
            items = line.split()
            if (lineid == 0):
                #write the whole line of the first
                outf.write(line)
            else:
                #write only the label and first feature
                outf.write('%s %s\n'%(items[0], items[1]))

            lineid += 1

            if (lineid % 100000) == 0:
                print '.',

    outf.close()

if __name__=="__main__":
    program = os.path.basename(sys.argv[0])
    logger = logging.getLogger(program)

    # logging configure
    import logging.config
    logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s')
    logging.root.setLevel(level=logging.DEBUG)
    logger.info("running %s" % ' '.join(sys.argv))

    opt = load_option()

    run(opt.input, opt.output)


