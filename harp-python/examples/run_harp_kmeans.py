from harp.applications import KMeansApplication
import numpy

my_kmeans = KMeansApplication('My Harp KMeans with Harp')

my_kmeans.config_hadoop_bin('/Users/bfeng/hadoop-2.6.0/bin/hadoop')

my_kmeans.config_harp_jar('/Users/bfeng/hadoop-2.6.0/harp-app-1.0-SNAPSHOT.jar')

my_kmeans.args("1000 10 100 5 2 2 10", "/kmeans", "/kmeans", "allreduce")

my_kmeans.run()

my_kmeans.print_result('/kmeans/centroids/out/output')

arr = my_kmeans.result_to_array('/kmeans/centroids/out/output')

print(arr)

sorted_arr = numpy.sort(arr)

print(sorted_arr)
