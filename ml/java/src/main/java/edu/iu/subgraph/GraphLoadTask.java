package edu.iu.subgraph;

import edu.iu.harp.partition.Partition;
import edu.iu.harp.resource.IntArray;
import edu.iu.harp.schdynamic.Task;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;


/**
 * @brief modified for conversion between sahad and fascia graph format
 */
public class GraphLoadTask implements Task<String, ArrayList<Partition<IntArray>>>{

    protected static final Log LOG = LogFactory.getLog(GraphLoadTask.class);
    private Configuration conf;

    public GraphLoadTask(Configuration conf){
        this.conf = conf;
    }

    private int vert_num_local = 0;
    private int adjacent_local_num = 0;
    private int max_v_id = 0;

    @Override
    public ArrayList<Partition<IntArray>> run(String input) throws Exception {
        // TODO Auto-generated method stub
        String fileName = (String) input;
        ArrayList<Partition<IntArray>> partialGraphDataList = new ArrayList<Partition<IntArray>>();
        Path pointFilePath = new Path(fileName);
        FileSystem fs =pointFilePath.getFileSystem(conf);
        FSDataInputStream in = fs.open(pointFilePath);
        BufferedReader br  = new BufferedReader(new InputStreamReader(in));
        try {
            String line ="";
            while((line=br.readLine())!=null){
                line = line.trim();
                String splits[] = line.split("\\s+");
                String keyText = splits[0];
                int key = Integer.parseInt(keyText);


                if( splits.length == 2){
                    String valueText = splits[1];

                    String[] itr = valueText.split(",");
                    int length = itr.length;
                    int[] intValues = new int[length];

                    max_v_id = key > max_v_id? key: max_v_id;
                    vert_num_local++;
                    adjacent_local_num += length;

                    for(int i=0; i< length; i++){
                        intValues[i]= Integer.parseInt(itr[i]);
                    }

                    Partition<IntArray> partialgraph = new Partition<IntArray>(key, new IntArray(intValues, 0, length));
                    partialGraphDataList.add(partialgraph);
                }
            }
        } finally {
            in.close();
        }
        return partialGraphDataList;
    }

    int get_vert_num_local() {
        return vert_num_local;
    }

    int get_adjacent_local_num() {
        return adjacent_local_num;
    }

    int get_max_v_id() {
        return max_v_id;
    }

}
