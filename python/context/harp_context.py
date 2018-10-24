from abc import abstractmethod, ABC
from collectives.collectives import Collectives

from context.data_reader import DataReader


class HarpContext(ABC):

    @abstractmethod
    @property
    def self_id(self):
        pass

    @abstractmethod
    @property
    def name(self):
        pass

    @abstractmethod
    @property
    def reader(self) -> DataReader:
        pass

    @abstractmethod
    @property
    def com(self) -> Collectives:
        pass
