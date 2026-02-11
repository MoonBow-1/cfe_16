/*
 * HTTP Event Capture to RFC5424 CFE_16
 * Copyright (C) 2021-2025 Suomen Kanuuna Oy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *
 * Additional permission under GNU Affero General Public License version 3
 * section 7
 *
 * If you modify this Program, or any covered work, by linking or combining it
 * with other code, such other code is not for that reason alone subject to any
 * of the requirements of the GNU Affero GPL version 3 as long as this Program
 * is the same Program as licensed from Suomen Kanuuna Oy without any additional
 * modifications.
 *
 * Supplemented terms under GNU Affero General Public License version 3
 * section 7
 *
 * Origin of the software must be attributed to Suomen Kanuuna Oy. Any modified
 * versions must be marked as "Modified version of" The Program.
 *
 * Names of the licensors and authors may not be used for publicity purposes.
 *
 * No rights are granted for use of trade names, trademarks, or service marks
 * which are in The Program if any.
 *
 * Licensee must indemnify licensors and authors for any liability that these
 * contractual assumptions impose on licensors and authors.
 *
 * To the extent this program is licensed as part of the Commercial versions of
 * Teragrep, the applicable Commercial License may apply to this file if you as
 * a licensee so wish it.
 */
package com.teragrep.cfe_16.connection;

import com.teragrep.rlp_03.Server;
import com.teragrep.rlp_03.ServerFactory;
import com.teragrep.rlp_03.config.Config;
import com.teragrep.rlp_03.delegate.DefaultFrameDelegate;
import com.teragrep.rlp_03.delegate.FrameDelegate;
import java.io.IOException;
import java.util.function.Supplier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ConnectionFactoryTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionFactoryTest.class);

    @Test
    @DisplayName("test that createSender() throws an IOException if type is not RELP")
    void testThatCreateSenderThrowsAnIoExceptionIfTypeIsNotRelp() {
        final Exception exception = Assertions
                .assertThrowsExactly(
                        IOException.class, () -> ConnectionFactory.createSender("notRelp", "127.0.0.1", 1234)
                );

        Assertions.assertEquals("Invalid connection type: notRelp", exception.getMessage());
    }

    @Test
    @DisplayName("test that createSender returns RelpConnection if type is RELP")
    void testThatCreateSenderReturnsRelpConnectionIfTypeIsRelp() {
        // Create a server, because the RelpConnection will try to connect in its postConstruct
        final int SERVER_PORT = 1236;

        final Supplier<FrameDelegate> frameDelegateSupplier = () -> new DefaultFrameDelegate((frame) -> {
            LOGGER.debug(frame.relpFrame().payload().toString());
        });
        final Config config = new Config(SERVER_PORT, 1);
        final ServerFactory serverFactory = new ServerFactory(config, frameDelegateSupplier);

        final Server server = Assertions.assertDoesNotThrow(serverFactory::create);
        final Thread serverThread = new Thread(server);
        serverThread.start();
        Assertions.assertDoesNotThrow(server.startup::waitForCompletion);

        final AbstractConnection abstractConnection = Assertions
                .assertDoesNotThrow(() -> ConnectionFactory.createSender("RELP", "127.0.0.1", SERVER_PORT));

        Assertions.assertEquals(RelpConnection.class, abstractConnection.getClass());

        Assertions.assertDoesNotThrow(server::stop);
    }
}
