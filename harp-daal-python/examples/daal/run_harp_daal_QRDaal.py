from harp.daal.applications import QRDaalApplication

my_app = QRDaalApplication('My QRDaal with Harp')

my_app.args('/daal_qr /qr-work 5120 1 4')

my_app.run()
