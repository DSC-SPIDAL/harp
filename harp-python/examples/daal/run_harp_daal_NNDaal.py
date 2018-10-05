from harp.daal.applications import NNDaalApplication

my_app = NNDaalApplication('My NNDaal with Harp')

my_app.args('1 16 110000 1 /daal_nn/train /nn-work 21 20 50 /daal_nn/test /daal_nn/groundTruth')

my_app.run()
