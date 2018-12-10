from py4j.java_gateway import JavaGateway, GatewayParameters

from harp.HarpSession import HarpSession


class HarpClient:
    def __init__(self, port=25333):
        self.gateway = JavaGateway(gateway_parameters=GatewayParameters(port=port))

    def newSession(self, name):
        return HarpSession(self.gateway.entry_point.newSession(name))
