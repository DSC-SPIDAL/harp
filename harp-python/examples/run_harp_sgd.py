from harp.applications import SGDApplication
import numpy

my_app = SGDApplication('My SGD with Harp')

my_app.config_hadoop_bin('/Users/bfeng/hadoop-2.6.0/bin/hadoop')

my_app.config_harp_jar('/Users/bfeng/hadoop-2.6.0/harp-app-1.0-SNAPSHOT.jar')

my_app.args("/netflix/netflix-train 1000 0.01 0.004 200 4 16 1000 2 true /sgd-work /netflix/netflix-test")

my_app.run()

# my_app.print_result('/kmeans/centroids/out/output')

# arr = my_app.result_to_array('/kmeans/centroids/out/output')

# print(arr)

# sorted_arr = numpy.sort(arr)

# print(sorted_arr)
