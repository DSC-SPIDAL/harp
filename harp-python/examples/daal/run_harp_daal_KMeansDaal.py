from harp.daal.applications import KMeansDaalApplication
import numpy

my_app = KMeansDaalApplication('My KMeansDaal with Harp')

my_app.args('1 4 110000 100 /daal-kmeans-data /daal-kmeans-work 100 100 10 true 100000 5 /tmp/kmeans')

my_app.run()

my_app.print_result('/daal-kmeans-work/evaluation')

arr = my_app.result_to_array('/daal-kmeans-work/centroids/out/output')

print(arr)

sorted_arr = numpy.sort(arr)

print(sorted_arr)
