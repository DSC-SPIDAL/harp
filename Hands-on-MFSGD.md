# Demo Application : Recommender System

## Prerequisites

### Download Docker Image and launch Container Instance

Download the image and launch the container
```bash
# Download an image
sudo docker pull dscspidal/harp-daal
# Start a container
sudo docker run -it dscspidal/harp-daal /etc/bootstrap.sh -bash
```

After executing the last command you will be logged on to the docker image and use the 
following commands to build the latest version of harp-daal

```bash
cd harp/
git pull origin master
mvn clean package
```

The container takes up to 20GB disk space. If the machine has more than 50GB disk space, there shall be no problem to 
launch the container instance. Otherwise, users could use the following commands to clean up the docker space 

```bash
# Remove useless docker images
sudo docker image rm <useless-docker-image>
# Remove exited containers
sudo docker rm $(sudo docker ps -a -f status=exited -q)
# Clean up all dangling cache
sudo docker system prune
```
Find the docker container ID

```bash
sudo docker ps
```
and log into the docker 

```bash
sudo docker exec -it <container_id> bash
```

### Dependencies and Environment Variables

The hands-on codes have the dependencies as follows,

* Python 2.7+
* Python module Numpy 
* Hadoop 2.6.0/Hadoop 2.6.5
* DAAL 2018+ 

The following section describes where the important components of the Tutorial are

1. Harp Source Code - /harp                                                                   
2. Hadoop Installation - /usr/local/hadoop                                                        
3. K-Means tutorial code - /harp/harp-daal-app/src/main/java/edu/iu/daal_tutorial/daal_kmeans      
4. Python Code - /harp/harp-daal-python/examples/daal/                                   

The docker image already includes them and other tools, the image has the following machine learning algorithms

```bash
# List of HarpDaal applications (in harp-daal-<version>.jar)
edu.iu.daal_als.ALSDaalLauncher
edu.iu.daal_cov.COVDaalLauncher
edu.iu.daal_kmeans.regroupallgather.KMeansDaalLauncher
edu.iu.daal_linreg.LinRegDaalLauncher
edu.iu.daal_mom.MOMDaalLauncher
edu.iu.daal_naive.NaiveDaalLauncher
edu.iu.daal_nn.NNDaalLauncher
edu.iu.daal_pca.PCADaalLauncher
edu.iu.daal_qr.QRDaalLauncher
edu.iu.daal_ridgereg.RidgeRegDaalLauncher
edu.iu.daal_sgd.SGDDaalLauncher
edu.iu.daal_svd.SVDDaalLauncher

# List of HarpDaal examples in Python (/harp/harp-daal-python/examples/daal/)
run_harp_daal_ALSDaal.py
run_harp_daal_COVDaal.py
run_harp_daal_KMeansDaal.py
run_harp_daal_LinRegDaal.py
run_harp_daal_MOMDaal.py
run_harp_daal_NNDaal.py
run_harp_daal_NaiveDaal.py
run_harp_daal_PCADaal.py
run_harp_daal_QRDaal.py
run_harp_daal_RidgeRegDaal.py
run_harp_daal_SGDDaal.py
run_harp_daal_SVDDaal.py

# Image clustering example
/harp/harp-daal-python/examples/scdemo/tutorial/
```

The *bootstrap* script shall launch a Hadoop Cluster and set up all the environment variables. 
To verify the Hadoop status
```bash
## To check the status of HDFS
${HADOOP_HOME}/bin/hdfs dfsadmin -report
## To check the status of Yarn 
${HADOOP_HOME}/bin/yarn node -list
```

Check out the environment variables to figure out the locations of the important files.
```bash
echo $HADOOP_HOME
/usr/local/hadoop

echo $HARP_JAR
/usr/local/hadoop/harp-app-1.0-SNAPSHOT.jar

echo $HARP_DAAL_JAR
/usr/local/hadoop/harp-daal-app-1.0-SNAPSHOT.jar

echo $DAALROOT
/harp/harp-daal-app/__release__lnx/daal

echo $PYTHONPATH
/harp/harp-daal-python
```

If the script fails to complete these steps, users could manually set them 
```bash
export HADOOP_HOME=<path to hadoop home folder>
export HARP_JAR=<path to>/harp-app-1.0-SNAPSHOT.jar
export HARP_DAAL_JAR=<path to>/harp-daal-app-1.0-SNAPSHOT.jar
export DAALROOT=<path to your compiled daal folder>
export PYTHONPATH=<path to>/harp-daal-python
```

In a situation where you would like to stop and restart Hadoop, use the following commands

```bash
## stop all services
${HADOOP_HOME}/sbin/stop-all.sh
## launch HDFS service
${HADOOP_HOME}/sbin/start-dfs.sh
## launch yarn daemons
${HADOOP_HOME}/sbin/start-yarn.sh
```
## Invoke Harp-DAAL MatrixFactorization via Python Interface 

Besides, the Java programming API, Harp-DAAL currently provides another Python API, which interfaces 
Harp-DAAL with other python written applications. By just adding several lines of python code, you 
are able to deploy the original python application on Hadoop Cluster and boost the performance by 
leveraging DAAL kernels. 

The python codes for demo application is located at the path

```bash
${PYTHONPATH}/examples/scdemo/tutorial
```

### Step.1 Run MatrixFactorization(MFSGD) on movielens-1m Dataset with Python Scikit-Learn Suprise 

Run the pipeline from data loading, training, evaluation and finally check results.

```python
cd ${PYTHONPATH}/examples/scdemo/test
../tutorial/run_mf.sh
```
![screen shot of results](https://raw.githubusercontent.com/DSC-SPIDAL/harp/master/harp-daal-python/examples/scdemo/tutorial/mf_runlocal.png)


### Step.2 Modify to invokes Harp-DAAL

*demo_mf_local.py* is the original python codes of matrix factorization without Harp-DAAL. 
```python
# ############################################################################
# call SVD module 
# ############################################################################
SVD(n_factors = 100, n_epochs = 20, lr_all = 0.005, reg_all = 0.02)
```
*demo_mf_daal.py* replaces the above SVD module by a Harp-DAAL invocation

```python
# ############################################################################
# call Harp-DAAL MatrixFactorization module 
# ############################################################################
DAALMFSGD(n_node = 1, n_thread = 8, n_factors = 100, n_epochs = 20, lr_all = 0.005, reg_all = 0.02)
```

View all the modifications by
```bash
diff ../tutorial/demo_mf_local.py ../tutorial/demo_mf_daal.py
```

### Step.3 Invokes Harp-DAAL

```bash
../tutorial/run_mf.sh daal
```

