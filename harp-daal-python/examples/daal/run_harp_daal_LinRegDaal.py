from harp.daal.applications import LinRegDaalApplication

my_app = LinRegDaalApplication('My LinRegDaal with Harp')

my_app.args('/daal_reg/train /daal_reg/test /daal_reg/groundTruth /linreg-work 10240 50 2 16')

my_app.run()
