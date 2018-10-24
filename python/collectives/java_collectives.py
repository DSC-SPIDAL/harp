from collectives.collectives import Collectives
from context.harp_context import HarpContext
from harp_constants import Type, PartitioningMode
from numpy import ndarray


class JavaCollectives(Collectives):

    def __init__(self, harp_ctx: HarpContext):
        self.hctx = harp_ctx

    def barrier(self, ctx_name: str, op_name: str) -> bool:
        pass

    def broadcast(self, ctx_name: str, op_name: str, data: ndarray, data_type: Type,
                  partition_mode: PartitioningMode,
                  bcast_worker_id: int, use_mst_bcast: bool) -> bool:
        self.hctx.broadcast(ctx_name, op_name, data, bcast_worker_id, use_mst_bcast)
