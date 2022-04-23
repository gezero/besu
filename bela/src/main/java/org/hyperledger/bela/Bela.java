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

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

public class Bela {
  public static void main(final String[] args) throws Exception {
    DefaultTerminalFactory defaultTerminalFactory = new DefaultTerminalFactory();
    Terminal terminal = null;
    try {
      terminal = defaultTerminalFactory.createTerminal();
      terminal.putCharacter('H');
      terminal.putCharacter('e');
      terminal.putCharacter('l');
      terminal.putCharacter('l');
      terminal.putCharacter('o');
      terminal.putCharacter('\n');
      terminal.flush();
      Thread.sleep(2000);

      TerminalPosition startPosition = terminal.getCursorPosition();
      terminal.setCursorPosition(startPosition.withRelativeColumn(3).withRelativeRow(2));
      terminal.flush();
      Thread.sleep(2000);
      terminal.setBackgroundColor(TextColor.ANSI.BLUE);
      terminal.setForegroundColor(TextColor.ANSI.YELLOW);

      terminal.putCharacter('Y');
      terminal.putCharacter('e');
      terminal.putCharacter('l');
      terminal.putCharacter('l');
      terminal.putCharacter('o');
      terminal.putCharacter('w');
      terminal.putCharacter(' ');
      terminal.putCharacter('o');
      terminal.putCharacter('n');
      terminal.putCharacter(' ');
      terminal.putCharacter('b');
      terminal.putCharacter('l');
      terminal.putCharacter('u');
      terminal.putCharacter('e');
      terminal.flush();
      Thread.sleep(2000);

      terminal.setCursorPosition(startPosition.withRelativeColumn(3).withRelativeRow(3));
      terminal.flush();
      Thread.sleep(2000);
      terminal.enableSGR(SGR.BOLD);
      terminal.putCharacter('Y');
      terminal.putCharacter('e');
      terminal.putCharacter('l');
      terminal.putCharacter('l');
      terminal.putCharacter('o');
      terminal.putCharacter('w');
      terminal.putCharacter(' ');
      terminal.putCharacter('o');
      terminal.putCharacter('n');
      terminal.putCharacter(' ');
      terminal.putCharacter('b');
      terminal.putCharacter('l');
      terminal.putCharacter('u');
      terminal.putCharacter('e');
      terminal.flush();
      Thread.sleep(2000);

      terminal.resetColorAndSGR();
      terminal.setCursorPosition(terminal.getCursorPosition().withColumn(0).withRelativeRow(1));
      terminal.putCharacter('D');
      terminal.putCharacter('o');
      terminal.putCharacter('n');
      terminal.putCharacter('e');
      terminal.putCharacter('\n');
      terminal.flush();

      Thread.sleep(2000);

      terminal.bell();
      terminal.flush();
      Thread.sleep(200);
    } finally {
      if (terminal != null) {
        terminal.close();
      }
    }
  }
}
