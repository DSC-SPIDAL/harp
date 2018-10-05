from harp.daal.applications import SVDDaalApplication

my_app = SVDDaalApplication('My SVDDaal with Harp')

my_app.args('1 16 110000 1 /daal_svd /svd-work 100 100 true 1000 10 /tmp/svd')

my_app.run()
