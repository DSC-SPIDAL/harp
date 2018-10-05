from harp.daal.applications import QRDaalApplication

my_app = QRDaalApplication('My QRDaal with Harp')

my_app.args('1 16 110000 1 /daal_qr /qr-work 18 18')

my_app.run()
