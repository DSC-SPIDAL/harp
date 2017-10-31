from harp.daal.applications import SVDDaalApplication

my_app = SVDDaalApplication('My SVDDaal with Harp')

my_app.args('1000 100 5 2 16 10240 /Svd-P1000-D100-F5-N2 /tmp/SVD true')

my_app.run()
