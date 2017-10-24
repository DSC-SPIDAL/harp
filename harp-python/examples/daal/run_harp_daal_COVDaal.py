from harp.daal.applications import COVDaalApplication

my_app = COVDaalApplication('My COVDaal with Harp')

my_app.args('/daal_cov /cov-work 10240 2 16')

my_app.run()
