# SubGraph2Vec on NVIDIA GPU

This is a GPU implementation of the SubGraph2Vec  from the paper  
https://arxiv.org/abs/1903.04395

## Compiling:

run `compile-gpu-gnu.sh` to compile:

```bash
./compile-gpu-gnu.sh
```

This will generate `sc-gpu-gnu.bin` file.

## Running:

```bash
/sc-gpu-gnu.bin <graph-path> <template-path> <iteraration-to-run> <threads> <read-binary> <write-binary> <prune> <use-SPMM>
```

An example:

```bash
/sc-gpu-gnu.bin /share/project/FG474/TrainingData/subgraph/biodata/EcoliGO-BP.graph /share/project/FG474/TrainingData/subgraph/biodata/Tree_8_1.fascia 10 24 0 0 1 1
```


