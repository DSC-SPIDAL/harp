from harp.daal.applications import ALSDaalApplication

my_app = ALSDaalApplication('My ALSDaal with Harp')

my_app.args('/netflix/netflix-train 100 0.05 0.002 4 false 2 16 10240 /als-work /netflix/netflix-test')

my_app.run()
