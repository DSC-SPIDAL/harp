/*
 * Copyright 2013-2016 Indiana University
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
 */

package edu.iu.daal_als;

import com.intel.daal.algorithms.implicit_als.training.*;
import com.intel.daal.data_management.data.CSRNumericTable;
import com.intel.daal.data_management.data.KeyValueDataCollection;
import com.intel.daal.data_management.data.NumericTable;
import com.intel.daal.services.DaalContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// packages from Daal

public class ALSTrainStep4 {

    private DistributedStep4Local algo;
    private long nFactor;
    private long numThreads;
    private double alpha;
    private double lambda_als;
    private static DaalContext daal_Context = new DaalContext();
    private ALSDaalCollectiveMapper collect_mapper;

    private KeyValueDataCollection step4_local_input;
    private CSRNumericTable train_input;
    private NumericTable step2_res;

    protected static final Log LOG = LogFactory.getLog(ALSTrainStep4.class);

    public ALSTrainStep4(long nFactor, long numThreads, double alpha, double lambda_als,
                         KeyValueDataCollection step4_local_input, 
                         CSRNumericTable train_input, 
                         NumericTable step2_res,
                         ALSDaalCollectiveMapper collect_mapper)
    {
        this.algo = new DistributedStep4Local(daal_Context, Double.class, TrainingMethod.fastCSR);
        this.nFactor = nFactor;
        this.numThreads = numThreads;
        this.alpha = alpha;
        this.lambda_als = lambda_als;
        this.step4_local_input = step4_local_input;
        this.train_input = train_input;
        this.step2_res = step2_res;
        this.collect_mapper = collect_mapper;

        this.algo.parameter.setNFactors(this.nFactor);

        LOG.info("Default Alpha: " + this.algo.parameter.getAlpha());
        this.algo.parameter.setAlpha(this.alpha);
        LOG.info("Current Alpha: " + this.algo.parameter.getAlpha());

        LOG.info("Default Lambda: " + this.algo.parameter.getLambda());
        LOG.info("Current Lambda: " + this.algo.parameter.getLambda());

        this.algo.input.set(Step4LocalPartialModelsInputId.partialModels,        this.step4_local_input);
        this.algo.input.set(Step4LocalNumericTableInputId.partialData,           this.train_input);
        this.algo.input.set(Step4LocalNumericTableInputId.inputOfStep4FromStep2, this.step2_res);

    }

    public DistributedPartialResultStep4 compute()
    {
        return this.algo.compute(); 
    }

    
}

