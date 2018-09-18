import shlex,subprocess,sys,time
from math import sqrt
from operator import itemgetter as iget
from evtools import evaluate,rel2ranks

INPUTDATA=sys.argv[1]
TESTDATA=sys.argv[2]
NUM_FEATURES=int(sys.argv[3])
K=int(sys.argv[4])
ITER=int(sys.argv[5])
PROCESSORS=int(sys.argv[6])

PREDOUT=''
if len(sys.argv)>7:
	PREDOUT=sys.argv[7]

# read in targets
def readtargets(filename):
	p1=subprocess.Popen(shlex.split('cut -f 1,2 -d\  %s' % filename),stdout=subprocess.PIPE);
	output=[a.split(' ',1) for a in p1.stdout.read().split('\n')  if len(a)>0];
	ys=[float(a[0]) for a in output];
	qs=[int(a[1].split(':')[1]) for a in output];	
	return(ys,qs)
    
def get_biclassify_err(targets, preds):
    n = len(targets)
    c = len(targets[0])
    err=[0]*c
    assert(len(preds)==n)
    assert(len(preds[0])==c)
    
    for i in range(n):
        for k in range(c):
            err[k]+= abs(targets[i][k]-preds[i][k])
    
    return [float(err[k])/n for k in range(c)]
    
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

#cmdline = '/scratch/kilian/zheng/bin/tree %s %s /dev/null -F -f %i -k %i -p 8 -r 4 -t %i' %  (INPUTDATA,VALDATA,NUM_FEATURES,K,ITER)
cmdline = './cart/tree %s %s /dev/null -F -f %i -z -k %i -p %i -r 4 -t %i' %  (INPUTDATA,TESTDATA,NUM_FEATURES,K,PROCESSORS,ITER)
args = shlex.split(cmdline);

# start loading the data
p = subprocess.Popen(args, stdin=subprocess.PIPE,stdout=subprocess.PIPE)
    
[traintargets,trainqueries]=readtargets(INPUTDATA)
[testtargets,testqueries]=readtargets(TESTDATA)
targets=traintargets+testtargets;

ntra=len(traintargets)
ntes=len(testtargets)
nall=ntra+ntes
    
targetcumprobs = [None]*nall
cumprobs = [None]*nall
for i in range(nall):
    targetcumprobs[i] = [0]*4
    cumprobs[i] = [0]*4
    for c in range(4):
        if i < ntra:
            targetcumprobs[i][c] = int(traintargets[i] <= c)
	else:
	    targetcumprobs[i][c] = int(testtargets[i-(ntra)] <= c)

start=time.time()
   
# Run random forest
for c in range(4):
    print >> sys.stderr, "Class", c  
    # write target
    for i in range(ntra):
        p.stdin.write('%2.4f\n' % targetcumprobs[i][c])
    p.stdin.flush()
    # read prediction
    for i in range(nall):
        l=p.stdout.readline()
        cumprobs[i][c]=float(l.split(' ',1)[0])
    # write prediction
    if PREDOUT!='':
	    fall=open(PREDOUT+str(c),'w')
	    for i in range(nall):
		    fall.write(str(cumprobs[i][c])+'\n')

        
# get cumulative probabilites
preds = [0]*nall
for i in range(nall): 
    preds[i] = expectedvalue(cumtoclass(cumprobs[i]))

[TRrmse,TRerr,TRndcg]=evaluate(preds[0:ntra],trainqueries,traintargets)	    
[TErmse,TEerr,TEndcg]=evaluate(preds[ntra:],testqueries,testtargets)
print>>sys.stderr,"%2.5f,%2.5f,%2.5f,%2.5f,%2.5f,%2.5f" % (TRrmse,TRerr,TRndcg,TErmse,TEerr,TEndcg)

print '\n'.join(map(str,preds))


