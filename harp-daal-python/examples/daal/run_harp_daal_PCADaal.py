from harp.daal.applications import PCADaalApplication

my_app = PCADaalApplication('My PCADaal with Harp')

my_app.args('100 10 5 1 4 6000 /pca-work /tmp/PCA true')

my_app.run()
