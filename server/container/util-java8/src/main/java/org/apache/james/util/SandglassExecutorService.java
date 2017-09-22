/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/
package org.apache.james.util;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * The goal of this ExecutorService is to execute tasks in a dedicated time (sandglass).
 * 
 * <p>It's running the tasks in 10 threads, hard coded yet.
 * 
 * <p>The usage is simple, just submit tasks to it;
 * then call the {@link SandglassExecutorService#waitThenCheck} method.
 * This method ensure that all tasks are done during the sandglass time.
 * If one of them isn't finished at the end of the duration, the method should throw an {@link IllegalStateException}.
 * If all tasks are finished, then the method will return directly without waiting for the end of the duration.
 *
 */
public class SandglassExecutorService implements ExecutorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SandglassExecutorService.class);
    private static final int MAX_PARALLEL_THREADS = 10;

    private final Duration sandglass;
    private final ExecutorService executorService;

    public SandglassExecutorService(Duration sandglass) {
        Preconditions.checkNotNull(sandglass);
        this.sandglass = sandglass;
        this.executorService = Executors.newFixedThreadPool(MAX_PARALLEL_THREADS);
    }

    public void waitThenCheck() {
        try {
            shutdown();
            awaitTermination(sandglass.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOGGER.error("Error while waiting for processors initialization");
        } finally {
            if (!isTerminated()) {
                LOGGER.warn("Killing uninitialized processors");
                shutdownNow();
                throw new IllegalStateException("Duration elapse but still processes");
            }
        }
    }

    @Override
    public void execute(Runnable command) {
        executorService.execute(command);
    }

    @Override
    public void shutdown() {
        executorService.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return executorService.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return executorService.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executorService.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executorService.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return executorService.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return executorService.submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return executorService.submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return executorService.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return executorService.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return executorService.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return executorService.invokeAny(tasks, timeout, unit);
    }

}
