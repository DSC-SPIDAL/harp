from harp.daal.applications import NaiveDaalApplication

my_app = NaiveDaalApplication('My NaiveDaal with Harp')

my_app.args('1 16 110000 1 /daal_naive/train /naive-work 20 20 20 /daal_naive/test /daal_naive/groundTruth true 10000 10 200 /tmp/naive')

my_app.run()
