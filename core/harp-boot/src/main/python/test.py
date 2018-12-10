from harp.HarpClient import HarpClient

ctx = HarpClient()
session = ctx.newSession("kmeans_test")

kmeans = session.ml.cluster.KMeans(n_clusters=10, max_iter=100, n_jobs=20)

print(kmeans)
