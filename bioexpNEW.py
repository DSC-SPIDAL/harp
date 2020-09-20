#!/usr/bin/env python
import subprocess
import re
from time import time
import os

def do_exp():
    NEXP = 10
    my_env = os.environ.copy()
    my_env["OMP_NUM_THREADS"] = str(24)

    for graph in ["STRING10_HiConf_PPI",
                  ]:
    #for graph in ["EcoliGO-BP"]:
        for nvert in [9]:
        #for nvert in [8]:
            time0 = time()
            ntempl = {}
            ntempl[9] = 47
            ntimesall = []
            ncountsall = []
            for ncnt in range(ntempl[nvert]):
                cmd = "/N/u/fuwang/harp/sc-hsw-icc-no.bin /share/project/FG474/TrainingData/subgraph/biodata/%s.graph /share/project/FG474/TrainingData/subgraph/biodata/Tree_%s_%s.fascia 10 24 0 0 1 1 1" % (graph, nvert, ncnt+1)
                print (cmd)
                times = []
                counts = []
                for nexp in range(NEXP):
                    ret = subprocess.Popen(cmd.split(" "), stdout=subprocess.PIPE, env=my_env).communicate()[0].strip()
                    timeptstr = "(.*)Time for count per iter:(.+)seconds(.*)"
                    timept = re.compile(timeptstr, re.M|re.I|re.S)
                    timestr = re.match(timept, ret).group(2).strip()
                    print (timestr)
                    times.append(timestr)
                    countptstr = "(.*)Final count is(.+)"
                    countpt = re.compile(countptstr, re.M|re.I|re.S)
                    countstr = re.match(countpt, ret).group(2).strip()
                    print (countstr)
                    counts.append(countstr)
                ntimesall.append(times)
                ncountsall.append(counts)
            time1 = time()
            print ("%s - %s - %s" % (graph, nvert, time1-time0))
            timesfname = "%s_%s_times.txt" % (graph, nvert)
            countsfname = "%s_%s_counts.txt" % (graph, nvert)
            with open(timesfname, "w") as timesf:
                for times in ntimesall:
                    timesf.write("%s\n" % ",".join(times))
            with open(countsfname, "w") as countsf:
                for counts in ncountsall:
                    countsf.write("%s\n" % ",".join(counts))
 
if __name__ == "__main__":
    #prep_graph()
    #gen_templates()
    do_exp()

