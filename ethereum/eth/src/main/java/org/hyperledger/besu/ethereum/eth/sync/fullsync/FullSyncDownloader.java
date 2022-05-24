/*
 * Copyright ConsenSys AG.
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
package org.hyperledger.besu.ethereum.eth.sync.fullsync;

import org.hyperledger.besu.ethereum.ProtocolContext;
import org.hyperledger.besu.ethereum.eth.manager.EthContext;
import org.hyperledger.besu.ethereum.eth.sync.ChainDownloader;
import org.hyperledger.besu.ethereum.eth.sync.SynchronizerConfiguration;
import org.hyperledger.besu.ethereum.eth.sync.TrailingPeerRequirements;
import org.hyperledger.besu.ethereum.eth.sync.state.SyncState;
import org.hyperledger.besu.ethereum.mainnet.ProtocolSchedule;
import org.hyperledger.besu.plugin.services.MetricsSystem;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FullSyncDownloader implements SyncDownloader {

  private static final Logger LOG = LoggerFactory.getLogger(FullSyncDownloader.class);
  private final ChainDownloader chainDownloader;
  private final SynchronizerConfiguration syncConfig;
  private final ProtocolContext protocolContext;
  private final SyncState syncState;
  private final SyncTerminationCondition terminationCondition;

  public FullSyncDownloader(
      final SynchronizerConfiguration syncConfig,
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final EthContext ethContext,
      final SyncState syncState,
      final MetricsSystem metricsSystem,
      final SyncTerminationCondition terminationCondition) {
    this.syncConfig = syncConfig;
    this.protocolContext = protocolContext;
    this.syncState = syncState;
    this.terminationCondition = terminationCondition;

    this.chainDownloader =
        FullSyncChainDownloader.create(
            syncConfig,
            protocolSchedule,
            protocolContext,
            ethContext,
            syncState,
            metricsSystem,
            terminationCondition);
  }

  @Override
  public CompletableFuture<Void> start() {
    LOG.info("Starting full sync.");
    if (terminationCondition.shouldContinueDownload()) {
      syncState.markInitialSyncPhaseAsDone();
      return chainDownloader
          .start()
          .thenRun(
              () -> {
                if (terminationCondition.shouldStopDownload()) {
                  syncState.setReachedTerminalDifficulty(true);
                }
              });
    }
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void stop() {
    chainDownloader.cancel();
  }

  @Override
  public Optional<TrailingPeerRequirements> calculateTrailingPeerRequirements() {
    if (syncState.isInSync()) return Optional.of(TrailingPeerRequirements.UNRESTRICTED);
    return Optional.of(
        new TrailingPeerRequirements(
            protocolContext.getBlockchain().getChainHeadBlockNumber(),
            syncConfig.getMaxTrailingPeers()));
  }
}
