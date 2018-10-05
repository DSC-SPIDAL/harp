from harp.daal.applications import RidgeRegDaalApplication

my_app = RidgeRegDaalApplication('My RidgeRegDaal with Harp')

my_app.args('1 16 110000 1 /daal_reg/train /ridgereg-work 12 10 2 /daal_reg/test /daal_reg/groundTruth')

my_app.run()
