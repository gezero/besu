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

import org.hyperledger.bela.components.SearchForBlockPanel;
import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueStorageProviderBuilder;
import org.hyperledger.besu.metrics.noop.NoOpMetricsSystem;
import org.hyperledger.besu.plugin.services.storage.rocksdb.RocksDBKeyValueStorageFactory;
import org.hyperledger.besu.plugin.services.storage.rocksdb.RocksDBMetricsFactory;
import org.hyperledger.besu.plugin.services.storage.rocksdb.configuration.RocksDBCLIOptions;
import org.hyperledger.besu.plugin.services.storage.rocksdb.configuration.RocksDBFactoryConfiguration;
import org.hyperledger.besu.services.BesuConfigurationImpl;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.DefaultWindowManager;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

public class Bela {
  public static void main(final String[] args) throws Exception {
    final Path dataDir = Paths.get(args[0]);
    System.out.println("We are loading : " + dataDir);
    final StorageProvider provider =
        createKeyValueStorageProvider(dataDir, dataDir.resolve("database"));

    BlockChainBrowser browser = BlockChainBrowser.fromProvider(provider);

    try (Terminal terminal = new DefaultTerminalFactory().createTerminal()) {
      Screen screen = new TerminalScreen(terminal);
      screen.startScreen();
      SearchForBlockPanel searchPanel = new SearchForBlockPanel();

      // Create window to hold the panel
      BasicWindow window = new BasicWindow();
      window.setComponent(searchPanel.createComponent());

      searchPanel.onChange(
          blockNumber -> window.setComponent(browser.findBlock(blockNumber).createComponent()));

      // Create gui and start gui
      MultiWindowTextGUI gui =
          new MultiWindowTextGUI(
              screen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.BLUE));
      gui.addWindowAndWait(window);
    }

    // Create panel to hold components

  }

  private static StorageProvider createKeyValueStorageProvider(
      final Path dataDir, final Path dbDir) {
    return new KeyValueStorageProviderBuilder()
        .withStorageFactory(
            new RocksDBKeyValueStorageFactory(
                () ->
                    new RocksDBFactoryConfiguration(
                        RocksDBCLIOptions.DEFAULT_MAX_OPEN_FILES,
                        RocksDBCLIOptions.DEFAULT_MAX_BACKGROUND_COMPACTIONS,
                        RocksDBCLIOptions.DEFAULT_BACKGROUND_THREAD_COUNT,
                        RocksDBCLIOptions.DEFAULT_CACHE_CAPACITY),
                Arrays.asList(KeyValueSegmentIdentifier.values()),
                RocksDBMetricsFactory.PUBLIC_ROCKS_DB_METRICS))
        .withCommonConfiguration(new BesuConfigurationImpl(dataDir, dbDir))
        .withMetricsSystem(new NoOpMetricsSystem())
        .build();
  }
}
