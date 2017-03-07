---
title: Harp Computation Models
---

![Inter-node Computation Model](/img/2-4-1.png)

## Computation Model A (Locking, use the synchronized algorithm and the latest model parameters)
This “Locking”-based computation model guarantees each worker exclusive access to model parameters. Once a worker trains a data item, it locks the related model parameters and prevents other worker from accessing them. When the related model parameters are updated, the worker unlocks the parameters. Thus the model parameters used in local computation is always the latest. This computation model can be implemented through Harp event-driven APIs.

## Computation Model B (Rotation, use the synchronized algorithm and the latest model parameters)
This “Rotation”-based computation model rotates model parameters between workers. Each worker first takes a part of the shared model and performs training. Afterwards, the model is shifted between workers. Through model rotation, each model parameters are updated by one worker at a time so that the model is consistent. This computation model can be implemented with Harp “rotate” operation.

## Computation Model C (Allreduce, use the synchronized algorithm and the stale model parameters)
In this computation model, each process first fetches all the model parameters required by local computation. When the local computation is completed, modifications of the local model from all processes are gathered to update the model. This computation model can be implemented through the “allreduce” operation for small models, the “regroup+allgather” operation or “psuh&pull” with big models.

## Computation Model D (No-sync, use the asynchronous algorithm and the stale model parameters)
In this computation model, each process independently fetches related model parameters, performs local computation, and returns model modifications. Unlike “Locking”-based computation model, workers are allowed to fetch or update the same model parameters in parallel. In contrast to “Rotation” or “Allreduce” computation models, there is no synchronization barrier. This computation model can be implemented through Harp event-driven APIs.


