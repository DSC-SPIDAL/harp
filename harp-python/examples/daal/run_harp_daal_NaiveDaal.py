from harp.daal.applications import NaiveDaalApplication

my_app = NaiveDaalApplication('My NaiveDaal with Harp')

my_app.args('/daal_naive/train /daal_naive/test /daal_naive/groundTruth /naive-work 10240 50 2 24')

my_app.run()
