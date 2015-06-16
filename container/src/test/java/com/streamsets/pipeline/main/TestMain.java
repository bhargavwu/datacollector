/**
 * (c) 2014 StreamSets, Inc. All rights reserved. May not
 * be copied, modified, or distributed in whole or part without
 * written consent of StreamSets, Inc.
 */
package com.streamsets.pipeline.main;

import com.streamsets.pipeline.task.Task;
import com.streamsets.pipeline.task.TaskWrapper;
import com.streamsets.pipeline.util.Configuration;
import dagger.Module;
import dagger.Provides;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;

import javax.inject.Singleton;
import java.io.File;
import java.util.UUID;

public class TestMain {
  private static Runtime runtime = Mockito.mock(Runtime.class);
  private static LogConfigurator logConfigurator = Mockito.mock(LogConfigurator.class);
  private static BuildInfo buildInfo = Mockito.mock(BuildInfo.class);
  private static RuntimeInfo runtimeInfo = Mockito.mock(RuntimeInfo.class);
  private static Task task = Mockito.mock(Task.class);

  @Module(injects = {TaskWrapper.class, LogConfigurator.class, BuildInfo.class, RuntimeInfo.class, Configuration.class})
  public static class TPipelineAgentModule {

    @Provides
    public LogConfigurator provideLogConfigurator() {
      return logConfigurator;
    }

    @Provides
    public BuildInfo provideBuildInfo() {
      return buildInfo;
    }

    @Provides
    public RuntimeInfo provideRuntimeInfo() {
      return runtimeInfo;
    }

    @Provides
    public Task provideAgent() {
      return task;
    }

    @Provides
    @Singleton
    public Configuration provideConfiguration() {
      return new Configuration();
    }
  }

  public static class TMain extends DataCollectorMain {

    public TMain() {
      super(TPipelineAgentModule.class);
    }

    @Override
    Runtime getRuntime() {
      return runtime;
    }
  }

  @Before
  public void before() {
    File dir = new File("target", UUID.randomUUID().toString());
    Assert.assertTrue(dir.mkdirs());
    System.setProperty(RuntimeModule.SDC_PROPERTY_PREFIX + RuntimeInfo.DATA_DIR, dir.getAbsolutePath());
    Mockito.reset(runtime);
    Mockito.reset(logConfigurator);
    Mockito.reset(buildInfo);
    Mockito.reset(runtimeInfo);
    Mockito.reset(task);
  }

  @After
  public void after() {
    System.getProperties().remove(RuntimeModule.SDC_PROPERTY_PREFIX + RuntimeInfo.DATA_DIR);
  }
  @Test
  public void testMainClassGetRuntime() {
    DataCollectorMain main = new DataCollectorMain();
    Assert.assertEquals(Runtime.getRuntime(), main.getRuntime());
  }

  @Test
  public void testOKFullRun() {
    DataCollectorMain main = new TMain();
    Mockito.verifyZeroInteractions(runtime);
    Mockito.verifyZeroInteractions(logConfigurator);
    Mockito.verifyZeroInteractions(buildInfo);
    Mockito.verifyZeroInteractions(runtimeInfo);
    Mockito.verifyZeroInteractions(task);
    Assert.assertEquals(0, main.doMain());
    Mockito.verify(logConfigurator, Mockito.times(1)).configure();
    Mockito.verify(buildInfo, Mockito.times(1)).log(Mockito.any(Logger.class));
    Mockito.verify(runtimeInfo, Mockito.times(1)).log(Mockito.any(Logger.class));
    Mockito.verify(task, Mockito.times(1)).init();
    Mockito.verify(runtime, Mockito.times(1)).addShutdownHook(Mockito.any(Thread.class));
    Mockito.verify(task, Mockito.times(1)).run();
    Mockito.verify(runtime, Mockito.times(1)).removeShutdownHook(Mockito.any(Thread.class));
  }

  @Test
  public void testInitException() {
    Mockito.doThrow(new RuntimeException()).when(task).init();
    DataCollectorMain main = new TMain();
    Assert.assertEquals(1, main.doMain());
    Mockito.verify(logConfigurator, Mockito.times(1)).configure();
    Mockito.verify(buildInfo, Mockito.times(1)).log(Mockito.any(Logger.class));
    Mockito.verify(runtimeInfo, Mockito.times(1)).log(Mockito.any(Logger.class));
    Mockito.verify(task, Mockito.times(1)).init();
    Mockito.verify(runtime, Mockito.times(0)).addShutdownHook(Mockito.any(Thread.class));
    Mockito.verify(task, Mockito.times(0)).run();
    Mockito.verify(runtime, Mockito.times(0)).removeShutdownHook(Mockito.any(Thread.class));
  }

  @Test
  public void testRunException() {
    Mockito.doThrow(new RuntimeException()).when(task).run();
    DataCollectorMain main = new TMain();
    Assert.assertEquals(1, main.doMain());
    Mockito.verify(logConfigurator, Mockito.times(1)).configure();
    Mockito.verify(buildInfo, Mockito.times(1)).log(Mockito.any(Logger.class));
    Mockito.verify(runtimeInfo, Mockito.times(1)).log(Mockito.any(Logger.class));
    Mockito.verify(task, Mockito.times(1)).init();
    Mockito.verify(runtime, Mockito.times(1)).addShutdownHook(Mockito.any(Thread.class));
    Mockito.verify(task, Mockito.times(1)).run();
    Mockito.verify(runtime, Mockito.times(0)).removeShutdownHook(Mockito.any(Thread.class));
  }

  @Test
  public void testShutdownHook() {
    ArgumentCaptor<Thread> shutdownHookCaptor = ArgumentCaptor.forClass(Thread.class);
    DataCollectorMain main = new TMain();
    Assert.assertEquals(0, main.doMain());
    Mockito.verify(runtime, Mockito.times(1)).addShutdownHook(shutdownHookCaptor.capture());
    Mockito.reset(task);
    shutdownHookCaptor.getValue().run();
    Mockito.verify(task, Mockito.times(1)).stop();
  }

}
