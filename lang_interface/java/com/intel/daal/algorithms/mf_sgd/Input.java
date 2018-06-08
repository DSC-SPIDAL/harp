/* file: Input.java */
/*******************************************************************************
* Copyright 2014-2016 Intel Corporation
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/

package com.intel.daal.algorithms.mf_sgd;

import java.lang.System.*;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import java.lang.Long;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import com.intel.daal.algorithms.ComputeMode;
import com.intel.daal.algorithms.Precision;
import com.intel.daal.data_management.data.HomogenNumericTable;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.data_management.data.SerializableBase;
import com.intel.daal.services.DaalContext;

/**
 * <a name="DAAL-CLASS-ALGORITHMS__MF_SGD__INPUT"></a>
 * @brief Input objects for the mf_sgd algorithm in the batch and distributed mode and for the  
 */
public final class Input extends com.intel.daal.algorithms.Input {
    /** @private */
    static {
        System.loadLibrary("JavaAPI");
    }

    public Input(DaalContext context, long cObject) {
        super(context, cObject);
    }

    /**
     * Sets input object for the mf_sgd algorithm
     * @param id    Identifier of the %input object for the mf_sgd algorithm
     * @param val   Value of the input object
     */
    public void set(InputId id, SerializableBase val) {
        cSetInputTable(cObject, id.getValue(), val.getCObject());
    }

    /**
     * Returns input object for the mf_sgd algorithm
     * @param id Identifier of the input object
     * @return   Input object that corresponds to the given identifier
     */
    public SerializableBase get(InputId id) {
        // return new HomogenNumericTable(getContext(), cGetInputTable(cObject, id.getValue()));
        return new SerializableBase(getContext(), cGetInputTable(cObject, id.getValue()));
    }

    /**
     * @brief generate training points, used in debugging 
     *
     * @param points_Train
     * @param num_Train
     * @param points_Test
     * @param num_Test
     * @param row_num_w
     * @param col_num_h
     *
     * @return 
     */
    public void generate_points(VPoint[] points_Train, int num_Train, VPoint[] points_Test, int num_Test, int row_num_w, int col_num_h)
    {//{{{

        Random randomGenerator = new Random();

        int counts_train = 0;
        int counts_test = 0;
        float scale = 10;

        int i, j;

        for(i=0;i<row_num_w;i++)
        {
            for (j=0;j<col_num_h;j++) 
            {
                if (i == j)
                {
                    // put diagonal item into train dataset
                    if (counts_train < num_Train)
                    {
                        points_Train[counts_train]._wPos = i;
                        points_Train[counts_train]._hPos = j;

                        points_Train[counts_train]._val = scale*randomGenerator.nextFloat();
                        counts_train++;
                    }
                }
                else
                {
                    if ( randomGenerator.nextFloat() > 0.2)
                    {
                        // put item into train dataset
                        if (counts_train < num_Train)
                        {
                            points_Train[counts_train]._wPos = i;
                            points_Train[counts_train]._hPos = j;

                            points_Train[counts_train]._val = scale*randomGenerator.nextFloat();
                            counts_train++;
                        }
                        else if (counts_test < num_Test)
                        {
                            points_Test[counts_test]._wPos = i;
                            points_Test[counts_test]._hPos = j;

                            points_Test[counts_test]._val = scale*randomGenerator.nextFloat();
                            counts_test++;
                        }
                    }
                    else
                    {
                        // put item into test dataset
                        if (counts_test < num_Test)
                        {
                            points_Test[counts_test]._wPos = i;
                            points_Test[counts_test]._hPos = j;

                            points_Test[counts_test]._val = scale*randomGenerator.nextFloat();
                            counts_test++;
                        }
                        else if (counts_train < num_Train)
                        {
                            points_Train[counts_train]._wPos = i;
                            points_Train[counts_train]._hPos = j;

                            points_Train[counts_train]._val = scale*randomGenerator.nextFloat();
                            counts_train++;
                        }
                    }
                }
            }
        }


    }//}}}

    /**
     * @brief load data from input CSV files 
     *
     * @param DataFile
     * @param table
     *
     * @return 
     */
	public int loadData(String DataFile, ArrayList<VPoint> table)
	{//{{{

		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = " ";

		int num_vpoint = 0;

		try
		{
			br = new BufferedReader(new FileReader(DataFile));

			while ((line = br.readLine()) != null) {

				String[] vpoints = line.split(cvsSplitBy);
				int wPos = Integer.parseInt(vpoints[0]);
				int hPos = Integer.parseInt(vpoints[1]);
				float val = Float.parseFloat(vpoints[2]);

				VPoint elem = new VPoint(wPos, hPos, val);
				table.add(elem);
				num_vpoint++;

				// if (map.containsKey(new Long(wPos)) == false)
				// {
				// 	//not found, load element
				// 	ArrayList<VPoint> array_elem = new ArrayList<>();
				// 	array_elem.add(elem);
                //
				// 	map.put(new Long(wPos), array_elem);
                //
				// }
				// else
				// {
				// 	//find the row_id
				// 	map.get(new Long(wPos)).add(elem);
				// }

			}


		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return num_vpoint;

	}//}}}

    /**
     * @brief convert imported csv data into format used in mf_sgd, 
     * used in distributed mode, no test data
     *
     * @param points_Train
     * @param num_Train
     * @param table_train
     *
     * @return 
     */
	public int[] convert_format_distri(VPoint[] points_Train, int num_Train, ArrayList<VPoint> table_train)
	{//{{{

		HashMap<Long, Long> vMap_row_w = new HashMap<Long, Long>();;
		HashMap<Long, Long> vMap_col_h = new HashMap<Long, Long>();;

		int row_pos_itr = 0;
		int col_pos_itr = 0;

		int row_pos = 0;
		int col_pos = 0;
		int entry_itr = 0;

		int[] row_col_num = new int[2];

		//loop over table_train 
		for(int j=0;j<table_train.size();j++)
		{

			Long wPos = new Long(table_train.get(j)._wPos);	
			Long hPos = new Long(table_train.get(j)._hPos);	
			float value = table_train.get(j)._val;	

			if (vMap_row_w.containsKey(wPos) == false)
			{

				vMap_row_w.put(wPos, new Long(row_pos_itr));
				row_pos = row_pos_itr;
				row_pos_itr++;

			}
			else
				row_pos = vMap_row_w.get(wPos).intValue();

			if (vMap_col_h.containsKey(hPos) == false)
			{

				vMap_col_h.put(hPos, new Long(col_pos_itr));
				col_pos = col_pos_itr;
				col_pos_itr++;

			}
			else
				col_pos = vMap_col_h.get(hPos).intValue();

			points_Train[entry_itr]._wPos = row_pos;
			points_Train[entry_itr]._hPos = col_pos;
			points_Train[entry_itr]._val = value;

			entry_itr++;

		}

		row_col_num[0] = row_pos_itr;
		row_col_num[1] = col_pos_itr;

		return row_col_num;

	}//}}}

    private native void cSetInputTable(long cInput, int id, long ntAddr);
    private native long cGetInputTable(long cInput, int id);
}
