
package edu.iu.daal_subgraph;

import edu.iu.harp.example.IntArrPlus;
import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.IntArray;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.harp.resource.LongArray;
import edu.iu.harp.io.ConnPool;
import edu.iu.harp.resource.ResourcePool;

import org.apache.hadoop.mapreduce.Mapper.Context;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.System;
import com.intel.daal.algorithms.subgraph.*;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.data_management.data.HomogenNumericTable;
// import com.intel.daal.data_management.data.HomogenBMNumericTable;
import com.intel.daal.data_management.data.SOANumericTable;
import com.intel.daal.data_management.data_source.DataSource;
import com.intel.daal.data_management.data_source.FileDataSource;
import com.intel.daal.services.DaalContext;

public class RotateTaskUpdate implements Runnable {

    private int pipeline_send_id;
    private int pipeline_recv_id;
    private int pipeline_update_id;

    // private int local_mapper_id;
    // private int mapper_num;
    private int sub_id;
    private Distri scAlgorithm;
    private double update_counts_pip;
    private long time_comp;
    private long time_comp_pip;
    private boolean enter_pip;

    protected static final Log LOG = LogFactory.getLog(RotateTaskComm.class);
    private static DaalContext daal_Context = new DaalContext();

    //constructor
    RotateTaskUpdate(int sub_id, Distri scAlgorithm){

        this.pipeline_send_id = -1;
        this.pipeline_recv_id = -1;
        this.pipeline_update_id = -1;
        this.update_counts_pip = 0.0;
        this.time_comp = 0;
        this.time_comp_pip = 0;
        // this.local_mapper_id = local_mapper_id;
        // this.mapper_num = mapper_num;
        this.sub_id = sub_id;
        this.scAlgorithm = scAlgorithm;
        this.enter_pip = false;
        // this.comm_data_table = null;
        // this.mapper = mapper;
    }

    void setIDs(int send_id, int recv_id, int update_id)
    {
        this.pipeline_send_id = send_id;
        this.pipeline_recv_id = recv_id;
        this.pipeline_update_id = update_id;
    }

    int getSendID() {return this.pipeline_send_id;}
    int getRecvID() {return this.pipeline_recv_id;}
    int getUpdateID() {return this.pipeline_update_id;}
    double getUpdateCountsPip() {return this.update_counts_pip;}
    long getCompTime() {return this.time_comp;}
    long getCompTimePip() {return this.time_comp_pip;}
    void set_enter_pip(boolean flag) { this.enter_pip = flag; }

    @Override
    public void run() {

        long start_comp = System.currentTimeMillis();
        this.scAlgorithm.updateRemoteCountsPip(sub_id, this.pipeline_update_id);
        this.update_counts_pip = this.scAlgorithm.parameter.getUpdateCounts();
        this.time_comp += (System.currentTimeMillis() - start_comp);

        if (this.enter_pip)
            this.time_comp_pip += (System.currentTimeMillis() - start_comp);

    }
    
}
