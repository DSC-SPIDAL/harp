class PythonListener(object):

    def __init__(self, gateway):
        self.gateway = gateway

    def mapCollective(self, msg, harp_context):
        harp_context.p(msg+1)
        print(msg)

    class Java:
        implements = ["edu.iu.harp.boot.python.MapCollectiveReceiver"]