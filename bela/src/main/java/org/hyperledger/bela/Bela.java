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

import java.util.regex.Pattern;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.DefaultWindowManager;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

public class Bela {
  public static void main(final String[] args) throws Exception {
    try (Terminal terminal = new DefaultTerminalFactory().createTerminal()) {
      Screen screen = new TerminalScreen(terminal);
      screen.startScreen();
      Panel panel = new Panel();
      panel.setLayoutManager(new GridLayout(2));

      final Label blkLbl = new Label("");

      panel.addComponent(new Label("Block Number"));
      final TextBox blockNmr =
          new TextBox().setValidationPattern(Pattern.compile("[0-9]*")).addTo(panel);

      new Button(
              "Search!",
              new Runnable() {
                @Override
                public void run() {
                  int blockNumber = Integer.parseInt(blockNmr.getText());
                  blkLbl.setText(Integer.toString(blockNumber));
                }
              })
          .addTo(panel);

      panel.addComponent(new EmptySpace(new TerminalSize(0, 0)));
      panel.addComponent(blkLbl);

      // Create window to hold the panel
      BasicWindow window = new BasicWindow();
      window.setComponent(panel);

      // Create gui and start gui
      MultiWindowTextGUI gui =
          new MultiWindowTextGUI(
              screen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.BLUE));
      gui.addWindowAndWait(window);
    }

    // Create panel to hold components

  }
}
