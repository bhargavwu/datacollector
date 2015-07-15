/**
 * (c) 2015 StreamSets, Inc. All rights reserved. May not
 * be copied, modified, or distributed in whole or part without
 * written consent of StreamSets, Inc.
 */
package com.streamsets.dc.execution.manager.slave;

import com.streamsets.dc.execution.Manager;
import com.streamsets.dc.execution.PipelineState;
import com.streamsets.dc.execution.PipelineStateStore;
import com.streamsets.dc.execution.PipelineStatus;
import com.streamsets.dc.execution.Previewer;
import com.streamsets.dc.execution.Runner;
import com.streamsets.dc.execution.manager.RunnerProvider;
import com.streamsets.pipeline.api.impl.Utils;
import com.streamsets.pipeline.lib.executor.SafeScheduledExecutorService;
import com.streamsets.pipeline.main.RuntimeInfo;
import com.streamsets.pipeline.store.PipelineStoreException;
import com.streamsets.pipeline.task.AbstractTask;
import com.streamsets.pipeline.util.Configuration;
import dagger.ObjectGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class SlavePipelineManager extends AbstractTask implements Manager {
  private static final Logger LOG = LoggerFactory.getLogger(SlavePipelineManager.class);
  private static final String SLAVE_MANAGER = "SlaveManager";

  private final ObjectGraph objectGraph;
  @Inject RuntimeInfo runtimeInfo;
  @Inject Configuration configuration;
  @Inject PipelineStateStore pipelineStateStore;
  @Inject @Named("runnerExecutor") SafeScheduledExecutorService runnerExecutor;
  @Inject
  RunnerProvider runnerProvider;
  private Runner runner;

  public SlavePipelineManager(ObjectGraph objectGraph) {
    super(SLAVE_MANAGER);
    this.objectGraph = objectGraph;
    this.objectGraph.inject(this);
  }

  @Override
  public Previewer createPreviewer(String user, String name, String rev) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Previewer getPreview(String previewerId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Runner getRunner(String user, String name, String rev) throws PipelineStoreException {
    if (runner != null && runner.getName().equals(name) && runner.getRev().equals(rev)) {
      return runner;
    }
    if (runner != null) {
      throw new IllegalStateException(Utils.format("Cannot create runner for '{}::{}', only one "
        + "runner allowed in a slave SDC", name, rev));
    }
    runner = runnerProvider.createRunner(user, name, rev, null, objectGraph);
    // Set the initial state
    pipelineStateStore.saveState(user, name, rev, PipelineStatus.EDITED, null, null, null);
    return runner;
  }

  @Override
  public List<PipelineState> getPipelines() throws PipelineStoreException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isPipelineActive(String name, String rev) throws PipelineStoreException {
    return (runner == null) ? false : runner.getStatus().getStatus().isActive();
  }

  @Override
  public void stopTask() {
    if (runner != null) {
      try {
        runner.onDataCollectorStop();
      } catch (Exception ex) {
        LOG.error(
          Utils.format("Cannot stop runner for pipeline '{}::{}' due to '{}'", runner.getName(), runner.getRev(), ex),
          ex);
      }
    }
  }

}