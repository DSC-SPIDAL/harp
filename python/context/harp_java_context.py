from collectives.collectives import Collectives
from context.harp_context import HarpContext
from context.data_reader import DataReader
from collectives.java_collectives import JavaCollectives


class JavaHarpContext(HarpContext):

    def __init__(self, java_harp_context_instance):
        self.jhc = java_harp_context_instance
        self.java_data_reader = DataReader(java_harp_context_instance.getReader())
        self.java_collectives = JavaCollectives(java_harp_context_instance.getCollectiveMapper())

    @property
    def name(self) -> str:
        return self.jhc.getName()

    @property
    def reader(self):
        return self.java_data_reader

    @property
    def self_id(self):
        return self.jhc.getSelfId()

    @property
    def com(self) -> Collectives:
        return self.java_collectives
