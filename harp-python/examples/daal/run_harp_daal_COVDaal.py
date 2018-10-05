from harp.daal.applications import COVDaalApplication

my_app = COVDaalApplication('My COVDaal with Harp')

my_app.args('1 16 110000 1 /daal_cov /cov-work 10 10')

my_app.run()
