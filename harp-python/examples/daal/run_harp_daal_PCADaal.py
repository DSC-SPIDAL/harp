from harp.daal.applications import PCADaalApplication

my_app = PCADaalApplication('My PCADaal with Harp')

my_app.args('1 16 110000 1 /daal_pca /pca-work 10 10 true 1000 10 /tmp/PCA')

my_app.run()
