from enum import Enum


class Type(Enum):
    BYTE = 1
    SHORT = 2
    INT = 3
    FLOAT = 4
    LONG = 5
    DOUBLE = 6


class PartitioningMode(Enum):
    HETEROGENEOUS = 1
    HOMOGENEOUS = 2
