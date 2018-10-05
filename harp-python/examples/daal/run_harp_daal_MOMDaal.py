from harp.daal.applications import MOMDaalApplication

my_app = MOMDaalApplication('My MOMDaal with Harp')

my_app.args('1 16 110000 1 /daal_mom /mom-work 10 10')

my_app.run()
