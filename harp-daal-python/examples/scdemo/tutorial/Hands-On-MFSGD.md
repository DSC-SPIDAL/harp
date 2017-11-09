# Demo Application : Recommender System

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
![screen shot of results](mf_runlocal.png)


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

