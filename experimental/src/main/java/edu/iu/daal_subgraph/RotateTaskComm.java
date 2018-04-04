
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
import com.intel.daal.data_management.data.HarpNumericTable;
import com.intel.daal.data_management.data.SOANumericTable;
import com.intel.daal.data_management.data_source.DataSource;
import com.intel.daal.data_management.data_source.FileDataSource;
import com.intel.daal.services.DaalContext;

public class RotateTaskComm implements Runnable {

    private int pipeline_send_id;
    private int pipeline_recv_id;
    private int pipeline_update_id;

    private int local_mapper_id;
    private int mapper_num;
    private int sub_id;
	private long start_time;
    private long time_sync;
    private long time_comm;
    private long time_comm_single;
    private long time_sync_pip;
    private long time_comm_pip;

    private double transfer_comm_data;
    private double transfer_comm_data_pipstep;
    private double peak_data_comm;
    private boolean enter_pip;

    private Distri scAlgorithm;

    private Table<SCSet> comm_data_table;
    private SCDaalCollectiveMapper mapper;

    protected static final Log LOG = LogFactory.getLog(RotateTaskComm.class);
    private static DaalContext daal_Context = new DaalContext();

    //constructor
    RotateTaskComm(int local_mapper_id, int mapper_num,
            int sub_id, Distri scAlgorithm, SCDaalCollectiveMapper mapper){

        this.pipeline_send_id = -1;
        this.pipeline_recv_id = -1;
        this.pipeline_update_id = -1;
		this.start_time = 0;
        this.time_sync = 0;
        this.time_comm = 0;
		this.time_comm_single=0;
        this.time_sync_pip = 0;
        this.time_comm_pip = 0;
        this.transfer_comm_data = 0.0;
		this.transfer_comm_data_pipstep = 0.0;
        this.peak_data_comm = 0.0;

        this.local_mapper_id = local_mapper_id;
        this.mapper_num = mapper_num;
        this.sub_id = sub_id;
        this.scAlgorithm = scAlgorithm;
        this.comm_data_table = null;
        this.mapper = mapper;
        this.enter_pip = false;
    }

    void setIDs(int send_id, int recv_id, int update_id)
    {
        this.pipeline_send_id = send_id;
        this.pipeline_recv_id = recv_id;
        this.pipeline_update_id = update_id;
    }

    void calcIDInit()
    {
        this.pipeline_send_id = (this.local_mapper_id + 1)%this.mapper_num;
    }

    void calcID()
    {
        this.pipeline_send_id++;
        this.pipeline_send_id = this.pipeline_send_id%this.mapper_num;
    }

    int getSendID() {return this.pipeline_send_id;}
    int getRecvID() {return this.pipeline_recv_id;}
    int getUpdateID() {return this.pipeline_update_id;}
    long getSyncTime() {return this.time_sync; }
    long getCommTime() {return this.time_comm; }

    long getSyncTimePip() {return this.time_sync_pip; }
    long getCommTimePip() {return this.time_comm_pip; }

    double getTransferData() {return this.transfer_comm_data;}
    double getTransferDataPipStep() {return this.transfer_comm_data_pipstep;}
    double getPeakDataComm() {return this.peak_data_comm; }

    void set_enter_pip(boolean flag) { this.enter_pip = flag; }

    @Override
    public void run() {

            this.pipeline_recv_id = regroup_comm_atomic();
            this.pipeline_update_id = this.pipeline_recv_id;
    }
    
    private int regroup_comm_atomic()
    {
        int update_id_pipeline = 0;
        LOG.info("Pipeline Start prepare comm for subtemplate: " + this.sub_id + "; send to mapper: " + this.pipeline_send_id);

        this.comm_data_table =  new Table<>(0, new SCSetCombiner());
        int send_parcel_num= this.scAlgorithm.input.sendCommParcelInit(this.sub_id, this.pipeline_send_id);
        LOG.info("send parcel num sub id: "+ this.sub_id+"; send_id: " + this.pipeline_send_id + "; parcel num: " + send_parcel_num);

        //record time
        long start_sync = System.currentTimeMillis();

        //record peak comm data
        double peak_data_comm_count = 0.0;

        for(int j=0;j<send_parcel_num;j++)
        {
            //comm_id (32 bits) consists of three parts: 1) send_id (12 bits); 2) local mapper_id (12 bits) 3) array_parcel id (8 bits)
            int comm_id =  ((this.pipeline_send_id << 20) | (this.local_mapper_id << 8) | j );
            //daal side compress and prep scset data
            this.scAlgorithm.input.sendCommParcelPrep(j);

            // get num_v and count_num
            int parcel_v_num = this.scAlgorithm.input.getCommParcelPrepVNum();
            int parcel_c_len = this.scAlgorithm.input.getCommParcelPrepCountLen();

            //debug
            LOG.info("Parcel_c_len value debug: "+ parcel_c_len);

            // add three numerictables
            int[] parcel_v_offset = new int[parcel_v_num+1];
            // HomogenNumericTable parcel_v_offset_table = new HomogenNumericTable(daal_Context, parcel_v_offset, 1, (parcel_v_num+1));
            HarpNumericTable parcel_v_offset_table = new HarpNumericTable(daal_Context, 1, (parcel_v_num+1));
			parcel_v_offset_table.setArray(parcel_v_offset, 0, 0);

            float[] parcel_v_data = new float[parcel_c_len];
            // HomogenNumericTable parcel_v_data_table = new HomogenNumericTable(daal_Context, parcel_v_data, 1, parcel_c_len);
            HarpNumericTable parcel_v_data_table = new HarpNumericTable(daal_Context, 1, parcel_c_len);
			parcel_v_data_table.setArray(parcel_v_data, 0, 0);

            int[] parcel_v_index = new int[parcel_c_len];
            // HomogenNumericTable parcel_v_index_table = new HomogenNumericTable(daal_Context, parcel_v_index, 1, parcel_c_len);
            HarpNumericTable parcel_v_index_table = new HarpNumericTable(daal_Context, 1, parcel_c_len);
			parcel_v_index_table.setArray(parcel_v_index, 0, 0);

            // set the table to daal side
            this.scAlgorithm.input.set(InputId.ParcelOffset, parcel_v_offset_table);
            this.scAlgorithm.input.set(InputId.ParcelData, parcel_v_data_table);
            this.scAlgorithm.input.set(InputId.ParcelIdx, parcel_v_index_table);

            peak_data_comm_count += ((double)(parcel_v_num+1 + parcel_c_len*2)*4/(1024*1024*1024));

            //upload data from daal side to harp side
            this.scAlgorithm.input.sendCommParcelLoad();
            
            //record transfer data in bytes 
			this.transfer_comm_data += ((parcel_v_num+1 + parcel_c_len*2)*4);
			this.transfer_comm_data_pipstep = ((parcel_v_num+1 + parcel_c_len*2)*4); 

            //convert parcel index data from int to short
            short[] parcel_v_index_short = new short[parcel_c_len]; 
            for(int i=0;i<parcel_c_len;i++)
                parcel_v_index_short[i] = (short)parcel_v_index[i];

            SCSet comm_data = new SCSet(parcel_v_num, parcel_c_len, parcel_v_offset, parcel_v_data, parcel_v_index_short);
            //retrieve elements for assembling SCSet comm_data from daal side
            this.comm_data_table.addPartition(new Partition<>(comm_id, comm_data));

            // release data 
            parcel_v_offset = null;
            parcel_v_data = null;
            parcel_v_index = null;
            parcel_v_index_short = null;

            parcel_v_offset_table.freeDataMemory();
            parcel_v_data_table.freeDataMemory();
            parcel_v_index_table.freeDataMemory();

            parcel_v_offset_table = null;
            parcel_v_data_table = null;
            parcel_v_index_table = null;

            comm_data = null;

        } // end for parcels of a sender id

		this.start_time = System.currentTimeMillis();
        LOG.info("Start rotate comm for subtemplate: " + this.sub_id + "; for pipeline send id: " + this.pipeline_send_id);

        this.mapper.regroup("sc", "regroup counts data", this.comm_data_table, new SCPartitioner2(this.mapper_num));
        this.mapper.barrier("sc", "all regroup sync");

        LOG.info("Finish rotate comm for subtemplate: " + this.sub_id + "; for pipeline send id: " + this.pipeline_send_id);
		this.time_comm_single = (System.currentTimeMillis() - this.start_time);

        //update local g counts by adj from each other mapper
        //move this to daal side
        for(int comm_id : this.comm_data_table.getPartitionIDs())
        {
            this.scAlgorithm.input.updateRecvParcelInit(comm_id);

            int update_id_tmp = ( comm_id & ( (1 << 20) -1 ) );
            int update_id =  (update_id_tmp >>> 8);
            // int chunk_id = ( update_id_tmp & ( (1 << 8) -1 ) );

            // update vert list accounts for the adj vert may be used to update local v
            SCSet scset = this.comm_data_table.getPartition(comm_id).get();
            int[] recv_v_offset = scset.get_v_offset();
            float[] recv_v_data = scset.get_counts_data();
            short[] recv_v_index = scset.get_counts_index();
            int[] recv_v_index_int = new int[recv_v_index.length];
            for(int p= 0; p<recv_v_index.length;p++)
                recv_v_index_int[p] = (int)recv_v_index[p];
            
            // HomogenNumericTable recv_v_offset_table = new HomogenNumericTable(daal_Context, recv_v_offset, 1, recv_v_offset.length);
            HarpNumericTable recv_v_offset_table = new HarpNumericTable(daal_Context, 1, recv_v_offset.length);
			recv_v_offset_table.setArray(recv_v_offset, 0, 0);
            this.scAlgorithm.input.set(InputId.ParcelOffset, recv_v_offset_table);

            // HomogenNumericTable recv_v_data_table = new HomogenNumericTable(daal_Context, recv_v_data, 1, recv_v_data.length);
            HarpNumericTable recv_v_data_table = new HarpNumericTable(daal_Context, 1, recv_v_data.length);
			recv_v_data_table.setArray(recv_v_data, 0, 0);
            this.scAlgorithm.input.set(InputId.ParcelData, recv_v_data_table);

            // HomogenNumericTable recv_v_index_table = new HomogenNumericTable(daal_Context, recv_v_index_int, 1, recv_v_index_int.length);
            HarpNumericTable recv_v_index_table = new HarpNumericTable(daal_Context, 1, recv_v_index_int.length);
			recv_v_index_table.setArray(recv_v_index_int, 0, 0);
            this.scAlgorithm.input.set(InputId.ParcelIdx, recv_v_index_table);

            peak_data_comm_count += ((double)(recv_v_offset.length + recv_v_data.length + recv_v_index_int.length)*4/(1024*1024*1024));

            //daal side update
            this.scAlgorithm.input.updateRecvParcel();

            //release java side  
            scset = null;
            recv_v_offset = null;
            recv_v_data = null;
            recv_v_index = null;
            recv_v_index_int = null;
        
            recv_v_offset_table.freeDataMemory();
            recv_v_data_table.freeDataMemory();
            recv_v_index_table.freeDataMemory();

            recv_v_offset_table = null;
            recv_v_data_table = null;
            recv_v_index_table = null;

            System.gc();

            // there shall be only one update_id sent from one mapper
            update_id_pipeline = update_id;
        }

        long cur_sync_time = (System.currentTimeMillis() - start_sync);
        this.time_sync += cur_sync_time;
        if (this.enter_pip)
            this.time_sync_pip += cur_sync_time;

        // all reduce to get the miminal com time from all the mappers, set that to the comm time
        Table<LongArray> comm_time_table = new Table<>(0, new LongArrMin());
        LongArray comm_time_array = LongArray.create(1, false);
        // comm_time_array.get()[0] = cur_sync_time;
        comm_time_array.get()[0] = this.time_comm_single;
        comm_time_table.addPartition(new Partition<>(0, comm_time_array));
        this.mapper.allreduce("sc", "get-global-comm-time", comm_time_table);
        this.time_comm += (comm_time_table.getPartition(0).get().get()[0]);

        if (this.enter_pip)
            this.time_comm_pip += (comm_time_table.getPartition(0).get().get()[0]);

        //record peak data mem usage
        this.peak_data_comm = (peak_data_comm_count > this.peak_data_comm) ? peak_data_comm_count : this.peak_data_comm;  
        
        comm_time_array = null;
        comm_time_table = null;
        this.comm_data_table.free();
        this.comm_data_table = null;

        ResourcePool.get().clean();
        ConnPool.get().clean();

        System.gc();

        return update_id_pipeline;
    }
}
