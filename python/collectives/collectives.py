from abc import ABC, abstractmethod
from numpy import ndarray

from harp_constants import PartitioningMode, Type


class Collectives(ABC):

    @abstractmethod
    def barrier(self, ctx_name: str, op_name: str) -> bool:
        pass

    def broadcast(self, ctx_name: str, op_name: str, data: ndarray, data_type: Type,
                  partition_mode: PartitioningMode,
                  bcast_worker_id: int, use_mst_bcast: bool) -> bool:
        pass
