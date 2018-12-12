from harp.HarpClient import HarpClient
import cloudpickle as cp
import numpy as np
import sys
import time

ctx = HarpClient()
session = ctx.newSession("kmeans_test")

kmeans = session.ml.cluster.KMeans(n_clusters=10, max_iter=100, n_jobs=20)

a = np.fromfile('/tmp/kmeans/d', dtype=int, sep=",")
print(a[2])
print(np.split(a, 6))

t1 = time.time()
x = np.full((100, 100), 1)
t2 = time.time()

print(sys.getsizeof(x) / 1000 / 1000)

t3 = time.time()
dump = cp.dumps(x)
t4 = time.time()

t5 = time.time()
session.byteTest(dump)
t6 = time.time()

print("For generation : " + str(t2 - t1))
print("For dumping : " + str(t4 - t3))
print("For sending : " + str(t6 - t5))
print(x[15])
