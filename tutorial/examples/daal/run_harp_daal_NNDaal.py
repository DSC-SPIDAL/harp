from harp.daal.applications import NNDaalApplication

my_app = NNDaalApplication('My NNDaal with Harp')

my_app.args('/daal_nn/train /daal_nn/test /daal_nn/groundTruth /nn-work 6000 50 1 4')

my_app.run()
