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

package org.apache.hadoop.mapreduce.v2.app;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.MRJobConfig;
import org.apache.hadoop.mapreduce.v2.app.client.ClientService;
import org.apache.hadoop.mapreduce.v2.app.launcher.ContainerLauncher;
import org.apache.hadoop.mapreduce.v2.app.launcher.ContainerLauncherEvent;
import org.apache.hadoop.mapreduce.v2.app.launcher.MapCollectiveContainerLauncherImpl;
import org.apache.hadoop.mapreduce.v2.app.rm.ContainerAllocator;
import org.apache.hadoop.mapreduce.v2.app.rm.ContainerAllocatorEvent;
import org.apache.hadoop.mapreduce.v2.app.rm.MapCollectiveContainerAllocator;
import org.apache.hadoop.mapreduce.v2.app.rm.RMCommunicator;
import org.apache.hadoop.mapreduce.v2.app.rm.RMHeartbeatHandler;
import org.apache.hadoop.mapreduce.v2.util.MRWebAppUtil;
import org.apache.hadoop.service.AbstractService;
import org.apache.hadoop.service.Service;
import org.apache.hadoop.service.ServiceOperations;
import org.apache.hadoop.util.ExitUtil;
import org.apache.hadoop.util.ShutdownHookManager;
import org.apache.hadoop.yarn.YarnUncaughtExceptionHandler;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.ApplicationConstants.Environment;
import org.apache.hadoop.yarn.api.records.ApplicationAttemptId;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.util.ConverterUtils;

import java.io.IOException;

/*******************************************************
 * The Map-Reduce Application Master. The state
 * machine is encapsulated in the implementation
 * of Job interface. All state changes happens via
 * Job interface. Each event results in a Finite
 * State Transition in Job.
 * 
 * MR AppMaster is the composition of loosely
 * coupled services. The services interact with
 * each other via events. The components resembles
 * the Actors model. The component acts on
 * received event and send out the events to other
 * components. This keeps it highly concurrent
 * with no or minimal synchronization needs.
 * 
 * The events are dispatched by a central Dispatch
 * mechanism. All components register to the
 * Dispatcher.
 * 
 * The information is shared across different
 * components using AppContext.
 ******************************************************/

public class MapCollectiveAppMaster
  extends MRAppMaster {

  private static final Log LOG = LogFactory
    .getLog(MapCollectiveAppMaster.class);

  public MapCollectiveAppMaster(
    ApplicationAttemptId applicationAttemptId,
    ContainerId containerId, String nmHost,
    int nmPort, int nmHttpPort,
    long appSubmitTime) {
    super(applicationAttemptId, containerId,
      nmHost, nmPort, nmHttpPort, appSubmitTime);
  }

  protected void serviceInit(
    final Configuration conf) throws Exception {
    // conf will be written as the config of this
    // AppMaster and the job
    conf.setBoolean(
      MRJobConfig.JOB_UBERTASK_ENABLE, false);
    conf.setBoolean(MRJobConfig.MAP_SPECULATIVE,
      false);
    conf.getBoolean(
      MRJobConfig.REDUCE_SPECULATIVE, false);
    super.serviceInit(conf);
  }

  protected ContainerAllocator
    createContainerAllocator(
      final ClientService clientService,
      final AppContext context) {
    return new ContainerAllocatorRouter(
      clientService, context);
  }

  protected ContainerLauncher
    createContainerLauncher(
      final AppContext context) {
    return new ContainerLauncherRouter(context);
  }

  /**
   * This ContainerAllocatorRouter doesn't do
   * local resource allocation for job uber mode
   */
  private final class ContainerAllocatorRouter
    extends AbstractService implements
    ContainerAllocator, RMHeartbeatHandler {
    private final ClientService clientService;
    private final AppContext context;
    private ContainerAllocator containerAllocator;

    ContainerAllocatorRouter(
      ClientService clientService,
      AppContext context) {
      super(
        ContainerAllocatorRouter.class.getName());
      this.clientService = clientService;
      this.context = context;
    }

    @Override
    protected void serviceStart()
      throws Exception {
      this.containerAllocator =
        new MapCollectiveContainerAllocator(
          this.clientService, this.context);
      ((Service) this.containerAllocator)
        .init(getConfig());
      ((Service) this.containerAllocator).start();
      super.serviceStart();
    }

    @Override
    protected void serviceStop()
      throws Exception {
      ServiceOperations
        .stop((Service) this.containerAllocator);
      super.serviceStop();
    }

    @Override
    public void
      handle(ContainerAllocatorEvent event) {
      this.containerAllocator.handle(event);
    }

    public void
      setSignalled(boolean isSignalled) {
      ((RMCommunicator) this.containerAllocator)
        .setSignalled(isSignalled);
    }

    public void setShouldUnregister(
      boolean shouldUnregister) {
      ((RMCommunicator) this.containerAllocator)
        .setShouldUnregister(shouldUnregister);
    }

    @Override
    public long getLastHeartbeatTime() {
      return ((RMCommunicator) this.containerAllocator)
        .getLastHeartbeatTime();
    }

    @Override
    public void
      runOnNextHeartbeat(Runnable callback) {
      ((RMCommunicator) this.containerAllocator)
        .runOnNextHeartbeat(callback);
    }
  }

  /**
   * By the time life-cycle of this router starts,
   * job-init would have already happened.
   */
  private final class ContainerLauncherRouter
    extends AbstractService
    implements ContainerLauncher {
    private final AppContext context;
    private ContainerLauncher containerLauncher;

    ContainerLauncherRouter(AppContext context) {
      super(
        ContainerLauncherRouter.class.getName());
      this.context = context;
    }

    @Override
    protected void serviceStart()
      throws Exception {
      this.containerLauncher =
        new MapCollectiveContainerLauncherImpl(
          context);
      ((Service) this.containerLauncher)
        .init(getConfig());
      ((Service) this.containerLauncher).start();
      super.serviceStart();
    }

    @Override
    public void
      handle(ContainerLauncherEvent event) {
      this.containerLauncher.handle(event);
    }

    @Override
    protected void serviceStop()
      throws Exception {
      ServiceOperations
        .stop((Service) this.containerLauncher);
      super.serviceStop();
    }
  }

  private static void
    validateInputParam(String value, String param)
      throws IOException {
    if (value == null) {
      String msg = param + " is null";
      LOG.error(msg);
      throw new IOException(msg);
    }
  }

  public static void main(String[] args) {
    // Log that a modified MRAppMaster starts
    LOG.info(
      "MapCollectiveAppMaster (MRAppMaster) starts.");
    try {
      Thread.setDefaultUncaughtExceptionHandler(
        new YarnUncaughtExceptionHandler());
      String containerIdStr = System
        .getenv(Environment.CONTAINER_ID.name());
      String nodeHostString =
        System.getenv(Environment.NM_HOST.name());
      String nodePortString =
        System.getenv(Environment.NM_PORT.name());
      String nodeHttpPortString = System
        .getenv(Environment.NM_HTTP_PORT.name());
      String appSubmitTimeStr = System.getenv(
        ApplicationConstants.APP_SUBMIT_TIME_ENV);

      validateInputParam(containerIdStr,
        Environment.CONTAINER_ID.name());
      validateInputParam(nodeHostString,
        Environment.NM_HOST.name());
      validateInputParam(nodePortString,
        Environment.NM_PORT.name());
      validateInputParam(nodeHttpPortString,
        Environment.NM_HTTP_PORT.name());
      validateInputParam(appSubmitTimeStr,
        ApplicationConstants.APP_SUBMIT_TIME_ENV);

      ContainerId containerId = ConverterUtils
        .toContainerId(containerIdStr);
      ApplicationAttemptId applicationAttemptId =
        containerId.getApplicationAttemptId();
      long appSubmitTime =
        Long.parseLong(appSubmitTimeStr);

      MRAppMaster appMaster =
        new MapCollectiveAppMaster(
          applicationAttemptId, containerId,
          nodeHostString,
          Integer.parseInt(nodePortString),
          Integer.parseInt(nodeHttpPortString),
          appSubmitTime);
      ShutdownHookManager.get().addShutdownHook(
        new MRAppMasterShutdownHook(appMaster),
        SHUTDOWN_HOOK_PRIORITY);
      JobConf conf =
        new JobConf(new YarnConfiguration());
      conf.addResource(
        new Path(MRJobConfig.JOB_CONF_FILE));

      MRWebAppUtil.initialize(conf);
      String jobUserName = System.getenv(
        ApplicationConstants.Environment.USER
          .name());
      conf.set(MRJobConfig.USER_NAME,
        jobUserName);
      // Do not automatically close FileSystem
      // objects so that in case of
      // SIGTERM I have a chance to write out the
      // job history. I'll be closing
      // the objects myself.
      conf.setBoolean("fs.automatic.close",
        false);
      initAndStartAppMaster(appMaster, conf,
        jobUserName);
    } catch (Throwable t) {
      LOG.fatal("Error starting MRAppMaster", t);
      ExitUtil.terminate(1, t);
    }
  }
}
