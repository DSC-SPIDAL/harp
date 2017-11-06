from harp.daal.applications import SVDDaalApplication

my_app = SVDDaalApplication('My SVDDaal with Harp')

my_app.args('100 10 5 1 4 5120 /Svd-work /tmp/SVD true')

my_app.run()
