from harp.applications import CCDApplication
import numpy

my_app = CCDApplication('My CCD with Harp')

my_app.args("/netflix/netflix-train 100 0.1 10 4 16 2 /cdd-work /netflix/netflix-test")

my_app.run()

# my_app.print_result('/kmeans/centroids/out/output')

# arr = my_app.result_to_array('/kmeans/centroids/out/output')

# print(arr)

# sorted_arr = numpy.sort(arr)

# print(sorted_arr)
