/*
 * Copyright 2013-2017 Indiana University
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

package edu.iu.kmeans.sgxsimu;

public class Constants {

  public static final String ENCLAVE_TASK =
    "sgx data size for each task";
  public static final String ENCLAVE_TOTAL =
    "total enclave capacity";
  public static final String ENCLAVE_PER_THD =
    "effective enclave size per thread";
  public static final String ENABLE_SIMU="true";

  // for enclave overhead simulation
  // overhead is measured by x1000 cycles at 2.3 GHz SGX-enabled CPU
  public static final double Ecall=8.5;
  public static final double Ocall=9.0;
  public static final double cross_enclave_per_kb=1.4;
  public static final double creation_enclave_fix=221000.0;
  public static final double creation_enclave_kb=22.677;
  public static final double local_attestation=80.0;
  public static final double remote_attestation=27200.0;
  public static final double swap_page_penalty=40;
  //for 3.4GHz SGX-enabled CPU
  public static final double ms_per_kcycle=0.0002941;
  // public static final boolean enablesimu=false;

}
