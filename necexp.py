#!/usr/bin/env python
import os
import subprocess
import re
from time import time

def do_exp():
    NTHREADS = [1,
               #2,
               #4,
                8,
               #16,
               #24,
               #48
               ]
    NEXP = 3
    graphs = ["web-Google","miami","graph500_scale20", "graph500_scale21"]
    templates = [
                 #"u3-1",
                 #"u5-1",
                 #"u5-2",
                 #"u7-1",
                 #"u7-2",
                 #"u10-1",
                 #"u10-2",
                 #"u12-2",
                 #"u13",
                 #"u14",
                 #"u15-1",
                 "u15-2",
                 #"u16-1"
                 ]
    for graph in graphs:
        for num_thread in NTHREADS:
            my_env = os.environ.copy()
            my_env["OMP_NUM_THREADS"] = str(num_thread)
            my_env["VE_LD_PRELOAD"] = "libveaccio.so.1"
            ntimesall = []
            for templ in templates:
                cmd = "/opt/nec/ve/bin/ve_exec -N 1 /tmp/sc-nec-ncc-withoutMorph-02.bin /dev/shm/%s.csc.data /tmp/%s.fascia 1 %s 1 0 1 1" % (graph, templ, num_thread)
                print (cmd)
                times = []
                for nexp in range(NEXP):
                    ret = subprocess.Popen(cmd.split(" "), stdout=subprocess.PIPE, env=my_env).communicate()[0].strip()
                    timeptstr = "(.*)Time for count per iter:(.+)seconds(.*)"
                    timept = re.compile(timeptstr, re.M|re.I|re.S)
                    try:
                        timestr = re.match(timept, ret).group(2).strip()
                    except:
                        timestr = "NA"
                    #print (timestr)
                    times.append(timestr)
                ntimesall.append(times)
            timesfname = "nec_%s_%s_times_u152.txt" % (graph, num_thread)
            with open(timesfname, "w") as timesf:
                for times in ntimesall:
                    timesf.write("%s\n" % ",".join(times))

if __name__ == "__main__":
    do_exp()

