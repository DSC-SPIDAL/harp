from harp.daal.applications import NaiveDaalApplication

my_app = NaiveDaalApplication('My NaiveDaal with Harp')

my_app.args('/daal_naive/train /daal_naive/test /daal_naive/groundTruth /naive-work 6000 50 1 4')

my_app.run()
