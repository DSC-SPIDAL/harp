from harp.daal.applications import RidgeRegDaalApplication

my_app = RidgeRegDaalApplication('My RidgeRegDaal with Harp')

my_app.args('/daal_reg/train /daal_reg/test /daal_reg/groundTruth /ridgereg-work 6000 50 1 4')

my_app.run()
