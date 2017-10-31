from harp.daal.applications import SGDDaalApplication

my_app = SGDDaalApplication('My SGDDaal with Harp')

my_app.args('/netflix/netflix-train 500 0.05 0.002 5 false 2 24 10240 /sgd-work /netflix/netflix-test')

my_app.run()
