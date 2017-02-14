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

package edu.iu.harp.schdynamic;

import java.util.concurrent.Semaphore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/*******************************************************
 * Some utils including acquiring semaphora and joining threads
 ******************************************************/
public class ComputeUtil {

    protected static final Log LOG = LogFactory.getLog(ComputeUtil.class);

    /**
     * Acquires the given number of permits from this semaphore
     * 
     * @param sem
     *            the Semaphore
     * @param count
     *            the number of permits to acquire
     */
    public static void acquire(Semaphore sem, int count) {
	boolean isFailed = false;
	do {
	    try {
		sem.acquire(count);
		isFailed = false;
	    } catch (Exception e) {
		isFailed = true;
		LOG.error("Error when acquiring semaphore", e);
	    }
	} while (isFailed);
    }

    /**
     * Acquires a permit from this semaphore
     * 
     * @param sem
     *            the Semaphore
     */
    public static void acquire(Semaphore sem) {
	boolean isFailed = false;
	do {
	    try {
		sem.acquire();
		isFailed = false;
	    } catch (Exception e) {
		isFailed = true;
		LOG.error("Error when acquiring semaphore", e);
	    }
	} while (isFailed);
    }

    /**
     * Join the thread
     * 
     * @param thread
     *            the thread to be joined
     */
    public static void joinThread(Thread thread) {
	boolean isFailed = false;
	do {
	    isFailed = false;
	    try {
		thread.join();
	    } catch (Exception e) {
		LOG.error("Error when joining thread.", e);
		isFailed = true;
	    }
	} while (isFailed);
    }
}
