from harp.HarpClient import HarpClient

ctx = HarpClient()
session = ctx.newSession("kmeans_test")

print(session)