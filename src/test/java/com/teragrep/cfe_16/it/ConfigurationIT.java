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
package com.teragrep.cfe_16.it;

import com.teragrep.cfe_16.config.Configuration;
import com.teragrep.cfe_16.server.TestServer;
import com.teragrep.cfe_16.server.TestServerFactory;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class ConfigurationIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationIT.class);
    private static final String hostname = "localhost";
    private static final Integer port = 1235;
    private static final ConcurrentLinkedDeque<byte[]> messageList = new ConcurrentLinkedDeque<>();
    private static final AtomicLong openCount = new AtomicLong();
    private static final AtomicLong closeCount = new AtomicLong();
    private static TestServer server;
    @Autowired
    private Configuration configuration;

    @BeforeAll
    public static void init() {
        TestServerFactory serverFactory = new TestServerFactory();
        try {
            server = serverFactory.create(port, messageList, openCount, closeCount);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        server.run();
    }

    @AfterAll
    public static void close() {
        try {
            server.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    public void clear() {
        openCount.set(0);
        closeCount.set(0);
        messageList.clear();
    }

    @Test
    public void instantiateConfigurationTest() {
        String expected = "Configuration [sysLogHost=127.0.0.1, sysLogProtocol=relp, sysLogPort=1235, maxAckValue=1000000, maxAckAge=20000, maxSessionAge=30000, "
                + "maxChannels=1000000, pollTime=1000000, printTimes=true]";
        LOGGER.debug(configuration.toString());

        assertEquals(expected, configuration.toString());
        assertEquals(0, messageList.size());
        assertEquals(1, openCount.get());
        assertEquals(0, closeCount.get()); // FIXME: does not close
    }
}
