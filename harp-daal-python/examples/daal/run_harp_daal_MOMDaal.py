from harp.daal.applications import MOMDaalApplication

my_app = MOMDaalApplication('My MOMDaal with Harp')

my_app.args('/daal_mom /mom-work 10240 1 16')

my_app.run()
