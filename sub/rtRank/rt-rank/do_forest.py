import shlex, subprocess,sys

from math import sqrt
from operator import itemgetter as iget
from evtools import evaluate,bestalpha,rel2ranks

INPUTDATA=sys.argv[1]
TESTDATA=sys.argv[2]
NUMFEATURES=int(sys.argv[3])
K=int(sys.argv[4])
TREES=int(sys.argv[5])
PROCESSORS=int(sys.argv[6])

ITER=1;
DEPTH=-1;

cmdline = './cart/tree %s %s /dev/null -F -f %d -t %d -k %i -p %i -r 1 -a 1 -z' %  (INPUTDATA,TESTDATA,NUMFEATURES,TREES,K,PROCESSORS)
args = shlex.split(cmdline);

# start loading the data
p = subprocess.Popen(args, stdin=subprocess.PIPE,stdout=subprocess.PIPE);

# read in targets
def readtargets(filename):
	p1=subprocess.Popen(shlex.split('cut -f 1,2 -d\  %s' % filename),stdout=subprocess.PIPE);
	output=[a.split(' ',1) for a in p1.stdout.read().split('\n')  if len(a)>0];
	ys=[float(a[0]) for a in output];
	qs=[int(a[1].split(':')[1]) for a in output];	
	return(ys,qs)

[traintargets,trainqueries]=readtargets(INPUTDATA)
[testtargets,testqueries]=readtargets(TESTDATA)
targets=traintargets+testtargets;

ntra=len(traintargets)
ntes=len(testtargets)
totalpreds=[0]*len(targets)

for itr in range(0,ITER):
    # write target
    for i in range(0,ntra):
	    p.stdin.write('%2.4f\n' % traintargets[i])

    # read prediction
    for i in range(0,len(totalpreds)):
	    l=p.stdout.readline()
	    try:
		    totalpreds[i] += float(l.split(' ',1)[0])
	    except:
		    print l
		    sys.exit(1)

    # get and store results
    preds = [float(i) / (itr+1) for i in totalpreds];
    [TRrmse,TRerr,TRndcg]=evaluate(preds[0:ntra],trainqueries,traintargets)	    
    [TErmse,TEerr,TEndcg]=evaluate(preds[ntra:],testqueries,testtargets)
    print>>sys.stderr, "%i,%2.5f,%2.5f,%2.5f,%2.5f,%2.5f,%2.5f" % (itr,TRrmse,TRerr,TRndcg,TErmse,TEerr,TEndcg)
    p.stdin.flush()    

# print preds to stdout
print '\n'.join([str(i) for i in preds])


