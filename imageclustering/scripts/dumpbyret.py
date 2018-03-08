import sys

imgmap={}

print 'loading hash-url map...'
inf=open(sys.argv[1])
for l in inf:
    its = l.strip().split('\t')
    #print its[2],its[16]
    imgmap[its[2]] = its[16]
    #print("wget -O %d/%s.jpg %s"%(cnt%10,its[2],its[16]))
    #if cnt > 5:
    #    sys.exit(0)
    #cnt += 1
print 'done, %s records'%len(imgmap)

print 'loading retfile...'
cnt = 0
errcnt = 0
fsf=open(sys.argv[2])
outf=open('downloadbyret.sh','w')
for l in fsf:
    #../../yfccimg/image//0/b91364e7c087144f87659baf673f9cae.jpg
    hash = l.strip().split('/')[-1].split('.')[0]
    print l
    print hash
    if hash in imgmap:
        url = imgmap[hash]
        outf.write("wget -O %s %s\n"%(l.strip(),url))
        cnt += 1
    else:
        errcnt += 1
print 'done, errcnt %s, total %s records'%(errcnt, cnt)

outf.close()
inf.close()
fsf.close()


