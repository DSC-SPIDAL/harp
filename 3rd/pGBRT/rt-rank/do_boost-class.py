import shlex, subprocess, sys,time
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

CUMPORBS=''
if len(sys.argv)>8:
    CUMPORBS = sys.argv[8]

cmdline = './cart/tree %s %s /dev/null -f %i -p %d -r %i -d %i -z' %  (INPUTDATA,TESTDATA,NUM_FEATURES,PROCESSORS,ITER*4,DEPTH)
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
    
def expectedvalue(l):
    s=0
    for i in range(len(l)):
        s+=i*l[i]
    return s
    
def cumtoclass(cum_prob):
	classprobs = [0]*5
	for c in range(5):
		if (c==0):
			classprobs[c]=cum_prob[0];
		elif (c==4):
			classprobs[c]=1-cum_prob[3];
		else:
			classprobs[c]=cum_prob[c]-cum_prob[c-1];
	return classprobs
    
[traintargets,trainqueries]=readtargets(INPUTDATA)
[testtargets,testqueries]=readtargets(TESTDATA)
targets=traintargets+testtargets

ntra=len(traintargets)
ntst=len(testtargets)
nall=ntra+ntst
    
targetcumprobs = [None]*nall
cumprobs = [None]*nall
for i in range(nall):
    targetcumprobs[i] = [0]*4
    cumprobs[i] = [0]*4
    for c in range(4):
        targetcumprobs[i][c] = int(targets[i] <= c)

# Read initial cumulative probabilities if available
if CUMPORBS!='':
    for c in range(4):
        idx=0
        once=1
        for l in open(CUMPORBS+str(c)):
            #if once==1:
            #    once=0;
            #    continue
            cumprobs[idx][c] = float(l.split(' ', 1)[0])
            cumprobs[idx][c] = max(0, cumprobs[idx][c])
            cumprobs[idx][c] = min(1, cumprobs[idx][c])
            idx+=1

start=time.time()
        
# Run boosting        
for itr in range(ITER):
    # get cumulative probabilites
    preds = [0]*nall
    for i in range(nall): 
        preds[i] = expectedvalue(cumtoclass(cumprobs[i]))
    
    [TRrmse,TRerr,TRndcg]=evaluate(preds[0:ntra],trainqueries,traintargets)
    [TErmse,TEerr,TEndcg]=evaluate(preds[ntra:nall],testqueries,testtargets)
    print>>sys.stderr, "%i,%2.5f,%2.5f,%2.5f,%2.5f,%2.5f,%2.5f" % (itr,TRrmse,TRerr,TRndcg,TErmse,TEerr,TEndcg) 
    sys.stdout.flush()
    
    for c in range(4):
        # write target
        for i in range(ntra):
            p.stdin.write('%f\n' % (targetcumprobs[i][c]-cumprobs[i][c]))

        p.stdin.flush()
        
        # read prediction
        for i in range(nall):
            l=p.stdout.readline()
            cumprobs[i][c] += STEPSIZE*float(l.split(' ',1)[0])
            cumprobs[i][c] = max(0, cumprobs[i][c])
            cumprobs[i][c] = min(1, cumprobs[i][c])

print '\n'.join(map(str,preds))
