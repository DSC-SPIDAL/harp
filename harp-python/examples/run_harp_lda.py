from harp.applications import LDAApplication
import numpy

my_app = LDAApplication('My LDA with Harp')

my_app.config_hadoop_bin('/Users/bfeng/hadoop-2.6.0/bin/hadoop')

my_app.config_harp_jar('/Users/bfeng/hadoop-2.6.0/harp-app-1.0-SNAPSHOT.jar')

my_app.args("/nytimes 1000 0.05 0.1 300 100 100 2 16 1.0 2000 /lda-work true")

my_app.run()

# my_app.print_result('/kmeans/centroids/out/output')

# arr = my_app.result_to_array('/kmeans/centroids/out/output')

# print(arr)

# sorted_arr = numpy.sort(arr)

# print(sorted_arr)
