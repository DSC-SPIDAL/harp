from py4j.java_gateway import JavaGateway
from context.harp_java_context import JavaHarpContext
from collectives.java_collectives import JavaCollectives


class HarpSession:

    def __init__(self, name):
        gateway = JavaGateway()
        self.__java_harp_session = gateway.entry_point
        self.ctx = JavaHarpContext(self.__java_harp_session.newSession(name))
        self.collective_com = JavaCollectives(self.ctx)

    @property
    def name(self):
        return self.ctx.name

    @property
    def com(self) -> JavaCollectives:
        return self.collective_com
