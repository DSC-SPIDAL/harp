from harp.daal.applications import SGDDaalApplication

my_app = SGDDaalApplication('My SGDDaal with Harp')

my_app.args('/movielens/movielens-train 500 0.05 0.002 5 false 1 4 6000 /sgd-work /movielens/movielens-test')

my_app.run()
