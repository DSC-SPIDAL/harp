from harp.applications import SGDApplication
import numpy

my_app = SGDApplication('My SGD with Harp')

my_app.args("/netflix/netflix-train 1000 0.01 0.004 200 100 4 16 2.0 80000 /sgd-work /netflix/netflix-test")

my_app.run()

# my_app.print_result('/kmeans/centroids/out/output')

# arr = my_app.result_to_array('/kmeans/centroids/out/output')

# print(arr)

# sorted_arr = numpy.sort(arr)

# print(sorted_arr)
