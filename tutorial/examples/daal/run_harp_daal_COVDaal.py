from harp.daal.applications import COVDaalApplication

my_app = COVDaalApplication('My COVDaal with Harp')

my_app.args('/daal_cov /cov-work 6000 1 4')

my_app.run()
