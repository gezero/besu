/*
 *
 *  * Copyright Hyperledger Besu Contributors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  * the License. You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  * specific language governing permissions and limitations under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.hyperledger.bela;

import org.hyperledger.bela.components.BlockPanelComponent;
import org.hyperledger.bela.components.LanternaComponent;
import org.hyperledger.bela.components.MessagePanel;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.mainnet.MainnetBlockHeaderFunctions;
import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueStoragePrefixedKeyBlockchainStorage;

import java.util.Optional;

import com.googlecode.lanterna.gui2.Component;

public class BlockChainBrowser {

  private final KeyValueStoragePrefixedKeyBlockchainStorage storage;

  public BlockChainBrowser(final KeyValueStoragePrefixedKeyBlockchainStorage storage) {
    this.storage = storage;
  }

  public static BlockChainBrowser fromProvider(final StorageProvider provider) {
    return new BlockChainBrowser(
        new KeyValueStoragePrefixedKeyBlockchainStorage(
            provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.BLOCKCHAIN),
            new MainnetBlockHeaderFunctions()));
  }

  public LanternaComponent<? extends Component> findBlock(final long blockNumber) {
    final Optional<Hash> blockHash = storage.getBlockHash(blockNumber);
    final Optional<BlockHeader> blockHeader = blockHash.flatMap(storage::getBlockHeader);
    if (blockHeader.isPresent()) {
      return new BlockPanelComponent(blockHeader.get());
    }
    return new MessagePanel("Not found block " + blockNumber);
  }
}
