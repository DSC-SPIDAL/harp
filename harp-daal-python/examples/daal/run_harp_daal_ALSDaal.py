from harp.daal.applications import ALSDaalApplication

my_app = ALSDaalApplication('My ALSDaal with Harp')

my_app.args('/movielens/movielens-train 100 0.05 0.002 4 false 1 8 10240 /als-work /movielens/movielens-test')

my_app.run()
