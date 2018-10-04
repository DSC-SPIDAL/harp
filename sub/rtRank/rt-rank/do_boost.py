import shlex, subprocess,sys,time
from math import sqrt
from operator import itemgetter as iget
from evtools import evaluate, rel2ranks

INPUTDATA=sys.argv[1]
TESTDATA=sys.argv[2]
NUM_FEATURES=int(sys.argv[3])
DEPTH=int(sys.argv[4])
ITER=int(sys.argv[5])
STEPSIZE=float(sys.argv[6])
PROCESSORS=int(sys.argv[7])

INITPRED=''
if len(sys.argv)>8:
    INITPRED = sys.argv[8]

cmdline = './cart/tree %s %s /dev/null -f %i -r %i -d %i -z -p %i' %  (INPUTDATA,TESTDATA,NUM_FEATURES,ITER,DEPTH,PROCESSORS)
args = shlex.split(cmdline);

# start loading the data
p = subprocess.Popen(args, stdin=subprocess.PIPE,stdout=subprocess.PIPE)

# read in targets
def readtargets(filename):
	p1=subprocess.Popen(shlex.split('cut -f 1,2 -d\  %s' % filename),stdout=subprocess.PIPE);
	output=[a.split(' ',1) for a in p1.stdout.read().split('\n')  if len(a)>0];
	ys=[float(a[0]) for a in output];
	qs=[int(a[1].split(':')[1]) for a in output];	
	return(ys,qs)
    
[traintargets,trainqueries]=readtargets(INPUTDATA)
[testtargets,testqueries]=readtargets(TESTDATA)
targets=traintargets+testtargets

ntra=len(traintargets)
ntst=len(testtargets)
nall=ntra+ntst

preds = [0]*nall
# Read initial cumulative probabilities if available
if INITPRED!='':
    preds = [float(l.split(' ', 1)[0]) for l in open(INITPRED)]

# Run boosting        
for itr in range(ITER):        
    [TRrmse,TRerr,TRndcg]=evaluate(preds[0:ntra],trainqueries,traintargets)
    [TErmse,TEerr,TEndcg]=evaluate(preds[ntra:nall],testqueries,testtargets)
    print >>sys.stderr,"%i,%2.5f,%2.5f,%2.5f,%2.5f,%2.5f,%2.5f" % (itr,TRrmse,TRerr,TRndcg,TErmse,TEerr,TEndcg) 
    sys.stdout.flush()
    
    # write target
    for i in range(ntra):
        p.stdin.write('%2.4f\n' % (traintargets[i]-preds[i]))
    p.stdin.flush()
        
    # read prediction
    for i in range(nall):
        l=p.stdout.readline()
        preds[i] += STEPSIZE*float(l.split(' ',1)[0])

# print predictions to stdout
print '\n'.join(map(str,preds))
