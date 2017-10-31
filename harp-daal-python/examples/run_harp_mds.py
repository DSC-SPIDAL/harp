from harp.applications import MDSApplication
import numpy

my_app = MDSApplication('My MDS with Harp')

my_app.args("")

my_app.run()

# my_app.print_result('/kmeans/centroids/out/output')

# arr = my_app.result_to_array('/kmeans/centroids/out/output')

# print(arr)

# sorted_arr = numpy.sort(arr)

# print(sorted_arr)
