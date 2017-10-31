from harp.daal.applications import NNDaalApplication

my_app = NNDaalApplication('My NNDaal with Harp')

my_app.args('/daal_nn/train /daal_nn/test /daal_nn/groundTruth /nn-work 10240 50 2 16')

my_app.run()
