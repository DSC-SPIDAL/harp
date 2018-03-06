from harp.daal.applications import MOMDaalApplication

my_app = MOMDaalApplication('My MOMDaal with Harp')

my_app.args('/daal_mom /mom-work 6000 1 4')

my_app.run()
