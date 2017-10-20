from harp.daal.applications import KMeansDaalApplication
import numpy

my_app = KMeansDaalApplication('My KMeansDaal with Harp')

my_app.args('5000 100 100 5 2 4 10 10240 /daal-kmeans-work /tmp/kmeans true')

my_app.run()

my_app.print_result('/daal-kmeans-work/centroids/out/output')

arr = my_app.result_to_array('/daal-kmeans-work/centroids/out/output')

print(arr)

sorted_arr = numpy.sort(arr)

print(sorted_arr)
