from harp.daal.applications import KMeansDaalApplication
import numpy

my_kmeans = KMeansDaalApplication('My Harp KMeans with Harp-Daal')

my_kmeans.args("5000 100 100 5 2 4 10 10240 /daal-kmeans-work /tmp/kmeans true")

my_kmeans.run()

# my_kmeans.print_result('/kmeans/centroids/out/output')

# arr = my_kmeans.result_to_array('/kmeans/centroids/out/output')

# print(arr)

# sorted_arr = numpy.sort(arr)

# print(sorted_arr)
