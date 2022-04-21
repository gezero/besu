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

package org.hyperledger.besu.util;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.rlp.RLP;
import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueStorageProviderBuilder;
import org.hyperledger.besu.ethereum.trie.CompactEncoding;
import org.hyperledger.besu.ethereum.trie.MerklePatriciaTrie;
import org.hyperledger.besu.ethereum.trie.Node;
import org.hyperledger.besu.ethereum.trie.TrieNodeDecoder;
import org.hyperledger.besu.ethereum.worldstate.StateTrieAccountValue;
import org.hyperledger.besu.metrics.noop.NoOpMetricsSystem;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;
import org.hyperledger.besu.plugin.services.storage.rocksdb.RocksDBKeyValueStorageFactory;
import org.hyperledger.besu.plugin.services.storage.rocksdb.RocksDBMetricsFactory;
import org.hyperledger.besu.plugin.services.storage.rocksdb.configuration.RocksDBFactoryConfiguration;
import org.hyperledger.besu.services.BesuConfigurationImpl;

import static org.hyperledger.besu.plugin.services.storage.rocksdb.configuration.RocksDBCLIOptions.DEFAULT_BACKGROUND_THREAD_COUNT;
import static org.hyperledger.besu.plugin.services.storage.rocksdb.configuration.RocksDBCLIOptions.DEFAULT_CACHE_CAPACITY;
import static org.hyperledger.besu.plugin.services.storage.rocksdb.configuration.RocksDBCLIOptions.DEFAULT_MAX_BACKGROUND_COMPACTIONS;
import static org.hyperledger.besu.plugin.services.storage.rocksdb.configuration.RocksDBCLIOptions.DEFAULT_MAX_OPEN_FILES;

public class BonsaiTreeVerifier {

    public static void main(final String[] args) {
        final Path dataDir = Paths.get(args[0]);
        System.out.println("We are veryfying : "+dataDir);
        final StorageProvider provider = createKeyValueStorageProvider(dataDir, dataDir.resolve("database"));
        BonsaiTraversal tr = new BonsaiTraversal(provider);
        System.out.println();
        System.out.println("ޏ₍ ὸ.ό₎ރ");
        System.out.println("\uD83E\uDD1E\uD83E\uDD1E\uD83E\uDD1E\uD83E\uDD1E\uD83E\uDD1E\uD83E\uDD1E\uD83E\uDD1E");


        try {
            tr.traverse();
            System.out.println("ޏ₍ ὸ.ό₎ރ World state was verified... ޏ₍ ὸ.ό₎ރ");
            System.out.println("Verified root "+ tr.getRoot()+ " with "+tr.getVisited()+" nodes");
        } catch (Exception e) {
            System.out.println("There was a problem (╯°□°)╯︵ ┻━┻: "+ e.getMessage());
            e.printStackTrace();
        }


        System.out.println("AAAAAAAAAA!!!!!!!");

    }

    private static StorageProvider createKeyValueStorageProvider(final Path dataDir, final Path dbDir) {
        return new KeyValueStorageProviderBuilder().withStorageFactory(new RocksDBKeyValueStorageFactory(() -> new RocksDBFactoryConfiguration(DEFAULT_MAX_OPEN_FILES, DEFAULT_MAX_BACKGROUND_COMPACTIONS, DEFAULT_BACKGROUND_THREAD_COUNT, DEFAULT_CACHE_CAPACITY), Arrays.asList(KeyValueSegmentIdentifier.values()), RocksDBMetricsFactory.PUBLIC_ROCKS_DB_METRICS))
                .withCommonConfiguration(new BesuConfigurationImpl(dataDir, dbDir))
                .withMetricsSystem(new NoOpMetricsSystem()).build();
    }
}


class BonsaiTraversal {

    Set<Bytes> visited = new HashSet<>();

    //    private final KeyValueStorage accountStorage;
    private final KeyValueStorage codeStorage;
    //    private final KeyValueStorage storageStorage;
    private final KeyValueStorage trieBranchStorage;
    private Node<Bytes> root;
    //    private final KeyValueStorage trieLogStorage;
    //    private final Pair<KeyValueStorage, KeyValueStorage> snapTrieBranchBucketsStorage;

    public BonsaiTraversal(final StorageProvider provider) {
//        accountStorage =
//                provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.ACCOUNT_INFO_STATE);
        codeStorage = provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.CODE_STORAGE);
//        storageStorage =
//                provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.ACCOUNT_STORAGE_STORAGE);
        trieBranchStorage = provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.TRIE_BRANCH_STORAGE);
//        trieLogStorage =
//                provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.TRIE_LOG_STORAGE);

    }

    public void traverse() {

        final Hash x = trieBranchStorage.get(Bytes.EMPTY.toArrayUnsafe()).map(Bytes::wrap).map(Hash::hash)
                .orElseThrow();
        root = getAccountNodeValue(x, Bytes.EMPTY);
        traverseAccountTrie(root);
    }

    public void traverseAccountTrie(final Node<Bytes> parentNode) {
        if (parentNode == null) {
            return;
        }
        if (visited.contains(parentNode.getHash())) {
            throw new RuntimeException("We have been here already!!!");
        }
        visited.add(parentNode.getHash());
        printVisited();
        final List<Node<Bytes>> nodes = TrieNodeDecoder.decodeNodes(parentNode.getLocation()
                .orElseThrow(), parentNode.getRlp());
        nodes.forEach(node -> {
            if (nodeIsHashReferencedDescendant(parentNode, node)) {
                traverseAccountTrie(getAccountNodeValue(node.getHash(), node.getLocation().orElseThrow()));
            } else {
                if (node.getValue().isPresent()) {
                    final StateTrieAccountValue accountValue = StateTrieAccountValue.readFrom(RLP.input(node.getValue()
                            .orElseThrow()));
                    final Hash accountHash = Hash.wrap(Bytes32.wrap(CompactEncoding.pathToBytes(Bytes.concatenate(parentNode.getLocation()
                            .orElseThrow(), node.getPath()))));
                    // Add code, if appropriate
                    if (!accountValue.getCodeHash().equals(Hash.EMPTY)) {
                        // traverse code
                        final Optional<Bytes> code = codeStorage.get(accountHash.toArrayUnsafe()).map(Bytes::wrap);
                        if (code.isEmpty()) {
                            System.out.format("missing code hash %s for account %s", accountValue.getCodeHash(), accountHash);
                        } else {
                            final Hash foundCodeHash = Hash.hash(code.orElseThrow());
                            if (!foundCodeHash.equals(accountValue.getCodeHash())) {
                                System.out.format("invalid code for account %s (expected %s and found %s)", accountHash, accountValue.getCodeHash(), foundCodeHash);
                            }
                        }
                    }
                    // Add storage, if appropriate
                    if (!accountValue.getStorageRoot().equals(MerklePatriciaTrie.EMPTY_TRIE_NODE_HASH)) {
                        traverseStorageTrie(accountHash, getStorageNodeValue(accountValue.getStorageRoot(), accountHash, Bytes.EMPTY));
                    }
                }
            }
        });

    }

    void printVisited(){
        if (getVisited() % 1000 ==0){
            System.out.print(".");
        }
        if (getVisited() % 100000 ==0){
            System.out.println();
            System.out.println("So far processed "+getVisited()+" nodes");
        }
    }


    public void traverseStorageTrie(final Bytes32 accountHash, final Node<Bytes> parentNode) {
        if (parentNode == null) {
            return;
        }
        final Bytes nodeId = Bytes.concatenate(accountHash, parentNode.getHash());
        if (visited.contains(nodeId)) {
            throw new RuntimeException("We have been here already!!!");
        }
        visited.add(nodeId);
        final List<Node<Bytes>> nodes = TrieNodeDecoder.decodeNodes(parentNode.getLocation()
                .orElseThrow(), parentNode.getRlp());
        nodes.forEach(node -> {
            if (nodeIsHashReferencedDescendant(parentNode, node)) {
                traverseStorageTrie(accountHash, getStorageNodeValue(node.getHash(), accountHash, node.getLocation()
                        .orElseThrow()));
            }
        });
    }

    @Nullable
    private Node<Bytes> getAccountNodeValue(final Bytes32 hash, final Bytes location) {
        final Optional<Bytes> bytes = trieBranchStorage.get(location.toArrayUnsafe()).map(Bytes::wrap);
        if (bytes.isEmpty()) {
            System.out.format("missing account trie node for hash %s and location %s", hash, location);
            return null;
        }
        final Hash foundHashNode = Hash.hash(bytes.orElseThrow());
        if (!foundHashNode.equals(hash)) {
            System.out.format("invalid account trie node for hash %s and location %s (found %s)", hash, location, foundHashNode);
            return null;
        }
        return TrieNodeDecoder.decode(location, bytes.get());
    }

    private Node<Bytes> getStorageNodeValue(final Bytes32 hash, final Bytes32 accountHash, final Bytes location) {
        final Optional<Bytes> bytes = trieBranchStorage.get(Bytes.concatenate(accountHash, location)
                .toArrayUnsafe()).map(Bytes::wrap);
        if (bytes.isEmpty()) {
            System.out.format("missing storage trie node for hash %s and location %s", hash, location);
            return null;
        }
        final Hash foundHashNode = Hash.hash(bytes.orElseThrow());
        if (!foundHashNode.equals(hash)) {
            System.out.format("invalid storage trie node for hash %s and location %s (found %s)", hash, location, foundHashNode);
            return null;
        }
        return TrieNodeDecoder.decode(location, bytes.get());
    }


    private boolean nodeIsHashReferencedDescendant(final Node<Bytes> parentNode, final Node<Bytes> node) {
        return !Objects.equals(node.getHash(), parentNode.getHash()) && node.isReferencedByHash();
    }


    public int getVisited() {
        return visited.size();
    }

    public String getRoot() {
        return root.getHash().toHexString();
    }
}

