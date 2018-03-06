from harp.daal.applications import LinRegDaalApplication

my_app = LinRegDaalApplication('My LinRegDaal with Harp')

my_app.args('/daal_reg/train /daal_reg/test /daal_reg/groundTruth /linreg-work 6000 50 1 4')

my_app.run()
