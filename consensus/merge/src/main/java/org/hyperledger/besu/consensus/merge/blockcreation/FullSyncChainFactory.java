package org.hyperledger.besu.consensus.merge.blockcreation;

import org.hyperledger.besu.ethereum.ProtocolContext;
import org.hyperledger.besu.ethereum.eth.manager.EthContext;
import org.hyperledger.besu.ethereum.eth.sync.ChainDownloader;
import org.hyperledger.besu.ethereum.eth.sync.SynchronizerConfiguration;
import org.hyperledger.besu.ethereum.eth.sync.fullsync.FullSyncChainDownloader;
import org.hyperledger.besu.ethereum.eth.sync.fullsync.SyncTerminationCondition;
import org.hyperledger.besu.ethereum.eth.sync.state.SyncState;
import org.hyperledger.besu.ethereum.mainnet.ProtocolSchedule;
import org.hyperledger.besu.plugin.services.MetricsSystem;

public class FullSyncChainFactory {
  private final ProtocolSchedule protocolSchedule;
  private final ProtocolContext protocolContext;
  private final EthContext ethContext;
  private final SyncState syncState;
  private final MetricsSystem metricsSystem;

  public FullSyncChainFactory(
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final EthContext ethContext,
      final SyncState syncState,
      final MetricsSystem metricsSystem) {
    this.protocolSchedule = protocolSchedule;
    this.protocolContext = protocolContext;
    this.ethContext = ethContext;
    this.syncState = syncState;
    this.metricsSystem = metricsSystem;
  }

  public ChainDownloader until(final SyncTerminationCondition terminalCondition) {
    return FullSyncChainDownloader.create(
        SynchronizerConfiguration.builder().build(),
        protocolSchedule,
        protocolContext,
        ethContext,
        syncState,
        metricsSystem,
        terminalCondition);
  }
}
