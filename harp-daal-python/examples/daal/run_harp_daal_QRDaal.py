from harp.daal.applications import QRDaalApplication

my_app = QRDaalApplication('My QRDaal with Harp')

my_app.args('/daal_qr /qr-work 10240 2 16')

my_app.run()
