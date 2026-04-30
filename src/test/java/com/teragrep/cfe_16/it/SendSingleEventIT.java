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

import com.teragrep.cfe_16.Acknowledgements;
import com.teragrep.cfe_16.SessionManager;
import com.teragrep.cfe_16.TokenManager;
import com.teragrep.cfe_16.config.Configuration;
import com.teragrep.cfe_16.connection.RelpConnection;
import com.teragrep.cfe_16.response.AcknowledgedJsonResponse;
import com.teragrep.cfe_16.response.Response;
import com.teragrep.cfe_16.server.TestServer;
import com.teragrep.cfe_16.server.TestServerFactory;
import com.teragrep.cfe_16.service.HECService;
import com.teragrep.cfe_16.service.HECServiceImpl;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

final class SendSingleEventIT {

    @Test
    void send1EventTest() {
        final int serverPort = 1238;
        final TestServerFactory serverFactory = new TestServerFactory();
        final ConcurrentLinkedDeque<byte[]> messageList = new ConcurrentLinkedDeque<>();
        final AtomicLong openCount = new AtomicLong();
        final AtomicLong closeCount = new AtomicLong();

        final TestServer server = Assertions
                .assertDoesNotThrow(() -> serverFactory.create(serverPort, messageList, openCount, closeCount));

        server.run();

        final Configuration configuration = new Configuration(
                "localhost",
                serverPort,
                1000000,
                20000,
                30000,
                1000000,
                1000000,
                true
        );
        final RelpConnection relpConnection = new RelpConnection("localhost", serverPort);
        Assertions
                .assertTimeout(Duration.of(5, ChronoUnit.SECONDS), relpConnection::connect, "RelpConnection did not connect in 5 seconds");
        final HECService service = new HECServiceImpl(
                new Acknowledgements(configuration),
                new SessionManager(configuration),
                new TokenManager(),
                relpConnection
        );
        Assertions.assertEquals(1, openCount.intValue());

        final MockHttpServletRequest request1 = new MockHttpServletRequest();
        request1.addHeader("Authorization", "AUTH_TOKEN_11111");
        final String channel1 = "CHANNEL_11111";
        final String eventInJson = "{\"sourcetype\":\"access\", \"source\":\"/var/log/access.log\", "
                + "\"event\": {\"message\":\"Access log test message 1\"}} "
                + "{\"sourcetype\":\"access\", \"source\":\"/var/log/access.log\", \"event\": "
                + "{\"message\":\"Access log test message 2\"}}";

        final Response supposedResponse = new AcknowledgedJsonResponse("Success", 0);
        Assertions
                .assertEquals(supposedResponse, service.sendEvents(request1, channel1, eventInJson), "Service should return JSON object with fields 'text', 'code' and 'ackID' (ackID " + "should be " + 0 + ")");

        Assertions
                .assertEquals(2, messageList.size(), "Number of events received should match the number of sent ones");

        Assertions.assertDoesNotThrow(relpConnection::close);
        Assertions.assertDoesNotThrow(server::close);
        Assertions.assertEquals(1, closeCount.intValue());
    }
}
