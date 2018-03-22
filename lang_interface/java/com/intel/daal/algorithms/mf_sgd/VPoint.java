/* file: VPoint.java */
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
import java.io.Serializable;


/**
 * @brief A class to store points in Train/Test dataset
 */
public class VPoint implements Serializable
{
	public int _wPos;   /* row id of points in model W */
	public int _hPos;   /* col id of points in model H */
	public float _val;  /* value of points */

	public VPoint(int wPos, int hPos, float val)
	{
		_wPos = wPos;
		_hPos = hPos;
		_val = val; 
	}


}
