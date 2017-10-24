from harp.daal.applications import PCADaalApplication

my_app = PCADaalApplication('My PCADaal with Harp')

my_app.args('10000 100 5 1 16 10240 /Pca-P10000-D100-F5-N1 /tmp/PCA true')

my_app.run()
