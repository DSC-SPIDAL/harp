from harp.daal.applications import RidgeRegDaalApplication

my_app = RidgeRegDaalApplication('My RidgeRegDaal with Harp')

my_app.args('/daal_reg/train /daal_reg/test /daal_reg/groundTruth /ridgereg-work 10240 50 2 16')

my_app.run()
