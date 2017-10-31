# Python Interface to Harp and Harp-Daal

## Installation

Installation is as easy as checking out the latest code from the stable branch of `harp`.

```bash
git clone https://github.com/DSC-SPIDAL/harp.git
```

### Dependencies
Please have all dependencies installed.

* Python 2.7+
* Python module Numpy (Install with your `pip`)
* Hadoop 2.6.0/Hadoop 2.6.5
* DAAL (Please see instructions under `harp-daal-app`)

### Environment Variables
After the code have been checked out and dependencies installed, please `export` all the following environment variables.

```
# Hadoop
export HADOOP_HOME=<path to hadoop home folder>

# Harp
export HARP_JAR=<path to>/harp-app-1.0-SNAPSHOT.jar
export HARP_DAAL_JAR=<path to>/harp-daal-app-1.0-SNAPSHOT.jar
export DAALROOT=<path to your compiled daal folder>
export PYTHONPATH=<path to>/harp-daal-python
```

## Example Code
Example code can be found under folder `examples`. For instance, to run KMeans with Harp-Daal, please run the following command:

```
cd harp-daal-python
python examples/daal/run_harp_daal_KMeansDaal.py
```

## Maintainance
Please raise issues in GitHub if you have any problems. Pull requests are very welcome.
