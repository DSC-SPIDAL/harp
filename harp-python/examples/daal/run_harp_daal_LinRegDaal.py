from harp.daal.applications import LinRegDaalApplication

my_app = LinRegDaalApplication('My LinRegDaal with Harp')

my_app.args('1 16 110000 1 /daal_reg/train /linreg-work 12 10 2 /daal_reg/test /daal_reg/groundTruth')

my_app.run()
