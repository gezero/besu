/*
 * Copyright contributors to Hyperledger Besu
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.ethereum.eth.sync.snapsync;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import org.hyperledger.besu.ethereum.eth.manager.EthScheduler;
import org.hyperledger.besu.ethereum.eth.sync.worldstate.TaskQueueIterator;
import org.hyperledger.besu.ethereum.eth.sync.worldstate.WorldStateDownloadProcess;
import org.hyperledger.besu.metrics.BesuMetricCategory;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.metrics.Counter;
import org.hyperledger.besu.plugin.services.metrics.LabelledMetric;
import org.hyperledger.besu.services.pipeline.Pipe;
import org.hyperledger.besu.services.pipeline.Pipeline;
import org.hyperledger.besu.services.pipeline.PipelineBuilder;
import org.hyperledger.besu.services.pipeline.WritePipe;
import org.hyperledger.besu.services.tasks.Task;
import org.hyperledger.besu.util.ExceptionUtils;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hyperledger.besu.services.pipeline.PipelineBuilder.createPipelineFrom;
import static org.slf4j.LoggerFactory.getLogger;

public class SnapWorldStateDownloadProcess implements WorldStateDownloadProcess {
  private static final Logger LOG = getLogger(SnapWorldStateDownloadProcess.class);


  private final Pipeline<Task<SnapDataRequest>> fetchDataPipeline;
  private final Pipeline<Task<SnapDataRequest>> completionPipeline;

  private final WritePipe<Task<SnapDataRequest>> requestsToComplete;
  private final Pipeline<Task<SnapDataRequest>> fetchCodePipeline;
  private final Pipeline<Task<SnapDataRequest>> fetchTrieNodesPipeline;

  private SnapWorldStateDownloadProcess(
          final Pipeline<Task<SnapDataRequest>> fetchDataPipeline,
          final Pipeline<Task<SnapDataRequest>> completionPipeline,
      final WritePipe<Task<SnapDataRequest>> requestsToComplete,
      final Pipeline<Task<SnapDataRequest>> fetchCodePipeline,
      final Pipeline<Task<SnapDataRequest>> fetchTrieNodesPipeline) {
    this.fetchDataPipeline = fetchDataPipeline;
    this.completionPipeline = completionPipeline;
    this.requestsToComplete = requestsToComplete;
    this.fetchCodePipeline = fetchCodePipeline;
    this.fetchTrieNodesPipeline = fetchTrieNodesPipeline;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public CompletableFuture<Void> start(final EthScheduler ethScheduler) {
    final CompletableFuture<Void> fetchDataFuture = ethScheduler.startPipeline(fetchDataPipeline);
    final CompletableFuture<Void> fetchCodeFuture = ethScheduler.startPipeline(fetchCodePipeline);
    final CompletableFuture<Void> fetchTrieFuture =
        ethScheduler.startPipeline(fetchTrieNodesPipeline);
    final CompletableFuture<Void> completionFuture = ethScheduler.startPipeline(completionPipeline);

    fetchDataFuture
        .thenCombine(fetchCodeFuture, (unused, unused2) -> null)
        .thenCombine(fetchTrieFuture, (unused, unused2) -> null)
        .whenComplete(
            (result, error) -> {
              if (error != null) {
                if (!(ExceptionUtils.rootCause(error) instanceof CancellationException)) {
                  LOG.error("Pipeline failed", error);
                }
                completionPipeline.abort();
              } else {
                // No more data to fetch, so propagate the pipe closure onto the completion pipe.
                requestsToComplete.close();
              }
            });

    completionFuture.exceptionally(
        error -> {
          if (!(ExceptionUtils.rootCause(error) instanceof CancellationException)) {
            LOG.error("Pipeline failed", error);
          }
          fetchDataPipeline.abort();
          return null;
        });
    return completionFuture;
  }

  @Override
  public void abort() {
    fetchDataPipeline.abort();
    completionPipeline.abort();
  }

  public static class Builder {

    private int hashCountPerRequest;
    private int maxOutstandingRequests;
    private SnapWorldDownloadState downloadState;
    private MetricsSystem metricsSystem;
    private LoadLocalDataStep loadLocalDataStep;
    private RequestDataStep requestDataStep;
    private SnapSyncState snapSyncState;
    private PersistDataStep persistDataStep;
    private CompleteTaskStep completeTaskStep;

    public Builder hashCountPerRequest(final int hashCountPerRequest) {
      this.hashCountPerRequest = hashCountPerRequest;
      return this;
    }

    public Builder maxOutstandingRequests(final int maxOutstandingRequests) {
      this.maxOutstandingRequests = maxOutstandingRequests;
      return this;
    }

    public Builder loadLocalDataStep(final LoadLocalDataStep loadLocalDataStep) {
      this.loadLocalDataStep = loadLocalDataStep;
      return this;
    }

    public Builder requestDataStep(final RequestDataStep requestDataStep) {
      this.requestDataStep = requestDataStep;
      return this;
    }

    public Builder persistDataStep(final PersistDataStep persistDataStep) {
      this.persistDataStep = persistDataStep;
      return this;
    }

    public Builder completeTaskStep(final CompleteTaskStep completeTaskStep) {
      this.completeTaskStep = completeTaskStep;
      return this;
    }

    public Builder downloadState(final SnapWorldDownloadState downloadState) {
      this.downloadState = downloadState;
      return this;
    }

    public Builder fastSyncState(final SnapSyncState fastSyncState) {
      this.snapSyncState = fastSyncState;
      return this;
    }

    public Builder metricsSystem(final MetricsSystem metricsSystem) {
      this.metricsSystem = metricsSystem;
      return this;
    }

    public SnapWorldStateDownloadProcess build() {
      checkNotNull(loadLocalDataStep);
      checkNotNull(requestDataStep);
      checkNotNull(persistDataStep);
      checkNotNull(completeTaskStep);
      checkNotNull(downloadState);
      checkNotNull(snapSyncState);
      checkNotNull(metricsSystem);
      checkNotNull(loadLocalDataStep);

      // Room for the requests we expect to do in parallel plus some buffer but not unlimited.
      final int bufferCapacity = hashCountPerRequest * 2;
      final LabelledMetric<Counter> outputCounter =
          metricsSystem.createLabelledCounter(
              BesuMetricCategory.SYNCHRONIZER,
              "snap_world_state_pipeline_processed_total",
              "Number of entries processed by each world state download pipeline stage",
              "step",
              "action");

      final Pipeline<Task<SnapDataRequest>> completionPipeline =
          PipelineBuilder.<Task<SnapDataRequest>>createPipeline(
                  "requestDataAvailable", bufferCapacity, outputCounter, true, "node_data_request")
              .andFinishWith(
                  "requestCompleteTask",
                  task ->
                      completeTaskStep.markAsCompleteOrFailed(snapSyncState, downloadState, task));

      final Pipe<Task<SnapDataRequest>> requestsToComplete = completionPipeline.getInputPipe();
      final Pipeline<Task<SnapDataRequest>> fetchDataPipeline =
          createPipelineFrom(
                  "requestDequeued",
                  new TaskQueueIterator<>(downloadState, downloadState::dequeueRequestBlocking),
                  bufferCapacity,
                  outputCounter,
                  true,
                  "world_state_download")
              .thenProcessAsync(
                  "batchDownloadData",
                  requestTask ->
                      requestDataStep.requestData(requestTask, snapSyncState, downloadState),
                  maxOutstandingRequests)
              .thenProcess(
                  "batchPersistData", task -> persistDataStep.persistAndCommit(task, downloadState))
              .thenProcess(
                  "checkNewPivotBlock",
                  task -> {
                    downloadState.checkNewPivotBlock(snapSyncState);
                    return task;
                  })
              .andFinishWith("batchDataDownloaded", requestsToComplete::put);

      final Pipeline<Task<SnapDataRequest>> fetchCodePipeline =
          createPipelineFrom(
                  "requestDequeued",
                  new TaskQueueIterator<>(downloadState, downloadState::dequeueCodeRequestBlocking),
                  bufferCapacity,
                  outputCounter,
                  true,
                  "code_blocks_download_pipeline")
              .thenProcessAsync(
                  "batchDownloadCodeBlocksData",
                  requestTask ->
                      requestDataStep.requestData(requestTask, snapSyncState, downloadState),
                  maxOutstandingRequests)
              .thenProcess(
                  "batchPersistData", task -> persistDataStep.persistAndCommit(task, downloadState))
              .andFinishWith("batchDataDownloaded", requestsToComplete::put);

      final Pipeline<Task<SnapDataRequest>> fetchTrieNodesPipeline =
          createPipelineFrom(
                  "requestTrieNodesDequeued",
                  new TaskQueueIterator<>(downloadState, downloadState::dequeueTrieRequestBlocking),
                  bufferCapacity,
                  outputCounter,
                  true,
                  "trie_blocks_heal_download_pipeline")
              .thenFlatMapInParallel(
                  "requestLoadLocalData",
                  task -> loadLocalDataStep.loadLocalData(task, requestsToComplete),
                  3,
                  bufferCapacity)
              .inBatches(hashCountPerRequest)
              .thenProcessAsync(
                  "batchDownloadTrieBlocksData",
                  requestTasks ->
                      requestDataStep.requestTrieNodeByPath(requestTasks, snapSyncState),
                  maxOutstandingRequests)
              .thenProcess(
                  "batchPersistData", task -> persistDataStep.persistAndCommit(task, downloadState))
              .andFinishWith(
                  "batchDataDownloaded", tasks -> tasks.forEach(requestsToComplete::put));

      return new SnapWorldStateDownloadProcess(
          fetchDataPipeline,
          completionPipeline,
          requestsToComplete,
          fetchCodePipeline,
          fetchTrieNodesPipeline);
    }
  }
}
