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
import com.teragrep.cfe_16.response.AcknowledgementResponse;
import com.teragrep.cfe_16.service.HECServiceImpl;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.teragrep.cfe_16.exceptionhandling.*;
import com.teragrep.cfe_16.response.AcknowledgedJsonResponse;
import com.teragrep.cfe_16.response.JsonResponse;
import com.teragrep.cfe_16.response.Response;
import com.teragrep.cfe_16.server.TestServer;
import com.teragrep.cfe_16.server.TestServerFactory;
import com.teragrep.cfe_16.service.HECService;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.*;

import org.springframework.mock.web.MockHttpServletRequest;
import tools.jackson.databind.node.ObjectNode;

/**
 * Tests the functionality of HECServiceImpl
 */
final class ServiceAndHECBatchIT {

    /*
     * Tests the sendEvents() and getAcks() method of the service.
     */
    @Test
    void sendEventsAndGetAcksTest() {
        final int serverPort = 1603;
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

        final String authToken1 = "AUTH_TOKEN_12223";
        final String authToken2 = "AUTH_TOKEN_16664";

        final ObjectMapper objectMapper = new ObjectMapper();
        final MockHttpServletRequest request1 = new MockHttpServletRequest();
        request1.addHeader("Authorization", authToken1);
        final MockHttpServletRequest request3 = new MockHttpServletRequest();
        request3.addHeader("Authorization", authToken2);

        final String eventInJson = "{\"sourcetype\": \"mysourcetype\", \"event\": \"Hello, world!\", \"host\": \"localhost\", \"source\": \"mysource\", \"index\": \"myindex\"}";

        final String channel2 = "CHANNEL_22222";
        final String channel3 = "CHANNEL_33333";

        final String ackRequest = "{\"acks\": [1,3,4]}";

        final JsonNode ackRequestNode = Assertions.assertDoesNotThrow(() -> objectMapper.readTree(ackRequest));

        final Response supposedResponse1 = new AcknowledgedJsonResponse("Success", 0);
        final Response returnedResponse1 = service.sendEvents(request1, channel3, eventInJson);
        Assertions
                .assertEquals(
                        supposedResponse1, returnedResponse1,
                        "Service should return JSON object with fields 'text', 'code' and 'ackID' (ackID should be 0)"
                );

        final Response supposedResponse2 = new AcknowledgedJsonResponse("Success", 1);
        final Response returnedResponse2 = service.sendEvents(request1, channel3, eventInJson);

        Assertions
                .assertEquals(
                        supposedResponse2, returnedResponse2,
                        "Service should return JSON object with fields 'text', 'code' and 'ackID' (ackID should be 1)"
                );

        final Response supposedResponse3 = new AcknowledgedJsonResponse("Success", 0);
        final Response returnedResponse3 = service.sendEvents(request1, channel2, eventInJson);

        Assertions
                .assertEquals(
                        supposedResponse3, returnedResponse3,
                        "Service should return JSON object with fields 'text', 'code' and 'ackID' (ackID should be 0)"
                );

        final Response supposedResponse4 = new AcknowledgedJsonResponse("Success", 0);
        final Response returnedResponse4 = service.sendEvents(request3, channel3, eventInJson);

        Assertions
                .assertEquals(
                        supposedResponse4, returnedResponse4,
                        "Service should return JSON object with fields 'text', 'code' and 'ackID' (ackID should be 0)"
                );

        final ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("1", true);
        objectNode.put("3", false);
        objectNode.put("4", false);
        final Response supposedResponse5 = new AcknowledgementResponse(objectNode);
        final Response returnedResponse5 = service.getAcks(request1, channel3, ackRequestNode);

        Assertions
                .assertEquals(supposedResponse5, returnedResponse5, "JSON object should be returned with ack statuses.");

        Assertions.assertDoesNotThrow(relpConnection::close);
        Assertions.assertDoesNotThrow(server::close);
        Assertions.assertEquals(1, openCount.intValue());
        Assertions.assertEquals(1, closeCount.intValue());
    }

    /*
     * Tests sending a request with no authentication token. In this case
     * AuthenticationTokenMissingException is expected to happen.
     */
    @Test
    void sendEventsWithoutAuthTokenTest() {
        final int serverPort = 1603;
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

        final MockHttpServletRequest request2 = new MockHttpServletRequest();
        final String eventInJson = "{\"sourcetype\": \"mysourcetype\", \"event\": \"Hello, world!\", \"host\": \"localhost\", \"source\": \"mysource\", \"index\": \"myindex\"}";
        final String channel1 = "CHANNEL_11111";

        Assertions.assertThrows(AuthenticationTokenMissingException.class, () -> {
            service.sendEvents(request2, eventInJson, channel1);
        });

        Assertions.assertDoesNotThrow(relpConnection::close);
        Assertions.assertDoesNotThrow(server::close);
        Assertions.assertEquals(1, openCount.intValue());
        Assertions.assertEquals(1, closeCount.intValue());
    }

    /*
     * Tests sending a request with no channel provided. In this case no Ack id is
     * returned.
     */
    @Test
    void sendEventsWithoutChannelTest() {
        final int serverPort = 1603;
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

        final MockHttpServletRequest request1 = new MockHttpServletRequest();

        final String eventInJson = "{\"sourcetype\": \"mysourcetype\", \"event\": \"Hello, world!\", \"host\": \"localhost\", \"source\": \"mysource\", \"index\": \"myindex\"}";
        final String authToken1 = "AUTH_TOKEN_12223";
        request1.addHeader("Authorization", authToken1);

        final Response supposedResponse = new JsonResponse("Success");
        final Response response = service.sendEvents(request1, null, eventInJson);
        Assertions
                .assertEquals(
                        supposedResponse, response, "Service should return JSON object with fields 'text' and 'code'"
                );

        Assertions.assertDoesNotThrow(relpConnection::close);
        Assertions.assertDoesNotThrow(server::close);
        Assertions.assertEquals(1, openCount.intValue());
        Assertions.assertEquals(1, closeCount.intValue());
    }

    /*
     * Tests getting the Ack statuses without providing channel in the request. In
     * this case ChannelNotProvidedException is expected to happen.
     */
    @Test
    void getAcksWithoutChannel() {
        final int serverPort = 1603;
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

        final ObjectMapper objectMapper = new ObjectMapper();
        final MockHttpServletRequest request1 = new MockHttpServletRequest();
        final String authToken1 = "AUTH_TOKEN_12223";
        request1.addHeader("Authorization", authToken1);
        final String ackRequest = "{\"acks\": [1,3,4]}";
        final JsonNode ackRequestNode = Assertions.assertDoesNotThrow(() -> objectMapper.readTree(ackRequest));

        Assertions.assertThrows(ChannelNotProvidedException.class, () -> {
            service.getAcks(request1, null, ackRequestNode);
        });

        Assertions.assertDoesNotThrow(relpConnection::close);
        Assertions.assertDoesNotThrow(server::close);
        Assertions.assertEquals(1, openCount.intValue());
        Assertions.assertEquals(1, closeCount.intValue());
    }

    /*
     * Tests getting the Ack statuses without providing an authentication token in
     * the request. In this case AuthenticationTokenMissingException is expected to
     * happen.
     */
    @Test
    void getAcksWithoutAuthTokenTest() {
        final int serverPort = 1603;
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

        final ObjectMapper objectMapper = new ObjectMapper();
        final MockHttpServletRequest request2 = new MockHttpServletRequest();
        final String channel1 = "CHANNEL_11111";
        final String ackRequest = "{\"acks\": [1,3,4]}";
        final JsonNode ackRequestNode = Assertions.assertDoesNotThrow(() -> objectMapper.readTree(ackRequest));

        Assertions.assertThrows(AuthenticationTokenMissingException.class, () -> {
            service.getAcks(request2, channel1, ackRequestNode);
        });

        Assertions.assertDoesNotThrow(relpConnection::close);
        Assertions.assertDoesNotThrow(server::close);
        Assertions.assertEquals(1, openCount.intValue());
        Assertions.assertEquals(1, closeCount.intValue());
    }

    /*
     * Tests trying to get Ack statuses with an authentication that is not used to
     * send events. In this case SessionNotFoundException is expected to happen.
     */
    @Test
    void getAcksWithUnusedAuthToken() {
        final int serverPort = 1603;
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

        final ObjectMapper objectMapper = new ObjectMapper();
        final MockHttpServletRequest request4 = new MockHttpServletRequest();
        final String channel1 = "CHANNEL_11111";
        final String authToken3 = "AUTH_TOKEN_23667";
        request4.addHeader("Authorization", authToken3);
        final String ackRequest = "{\"acks\": [1,3,4]}";
        final JsonNode ackRequestNode = Assertions.assertDoesNotThrow(() -> objectMapper.readTree(ackRequest));

        Assertions.assertThrows(SessionNotFoundException.class, () -> {
            service.getAcks(request4, channel1, ackRequestNode);
        });

        Assertions.assertDoesNotThrow(relpConnection::close);
        Assertions.assertDoesNotThrow(server::close);
        Assertions.assertEquals(1, openCount.intValue());
        Assertions.assertEquals(1, closeCount.intValue());
    }

    /*
     * Tests trying to get Ack statuses with a channel that is does not exist in the
     * session. In this case ChannelNotFoundException is expected to happen.
     */
    @Test
    void getAcksWithUnusedChannel() {
        final int serverPort = 1603;
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

        final ObjectMapper objectMapper = new ObjectMapper();
        final MockHttpServletRequest request5 = new MockHttpServletRequest();
        final String eventInJson = "{\"sourcetype\": \"mysourcetype\", \"event\": \"Hello, world!\", \"host\": \"localhost\", \"source\": \"mysource\", \"index\": \"myindex\"}";
        final String channel1 = "CHANNEL_11111";
        final String channel2 = "CHANNEL_22222";
        final String authToken4 = "AUTH_TOKEN_23249";
        request5.addHeader("Authorization", authToken4);
        final String ackRequest = "{\"acks\": [1,3,4]}";
        final JsonNode ackRequestNode = Assertions.assertDoesNotThrow(() -> objectMapper.readTree(ackRequest));

        Assertions.assertThrows(ChannelNotFoundException.class, () -> {
            service.sendEvents(request5, channel1, eventInJson);
            service.getAcks(request5, channel2, ackRequestNode);
        });

        Assertions.assertDoesNotThrow(relpConnection::close);
        Assertions.assertDoesNotThrow(server::close);
        Assertions.assertEquals(1, openCount.intValue());
        Assertions.assertEquals(1, closeCount.intValue());
    }

    /*
     * Testing using EventManager's convertData() method by sending multiple events
     * at once.
     */
    @Test
    void sendingMultipleEventsTest() {
        final int serverPort = 1603;
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

        final MockHttpServletRequest request1 = new MockHttpServletRequest();
        final String channel1 = "CHANNEL_11111";
        final String authToken1 = "AUTH_TOKEN_12223";
        request1.addHeader("Authorization", authToken1);

        final String allEventsInJson = "{\"event\": \"Pony 1 has left the barn\", \"sourcetype\": \"mysourcetype\", \"time\": 1426279439}{\"event\": \"Pony 2 has left the barn\"}{\"event\": \"Pony 3 has left the barn\", \"sourcetype\": \"newsourcetype\"}{\"event\": \"Pony 4 has left the barn\"}";
        final Response supposedResponse = new AcknowledgedJsonResponse("Success", 0);
        Assertions
                .assertEquals(supposedResponse, service.sendEvents(request1, channel1, allEventsInJson), "Should get a JSON with fields text, code and ackID");

        Assertions.assertDoesNotThrow(relpConnection::close);
        Assertions.assertDoesNotThrow(server::close);
        Assertions.assertEquals(1, openCount.intValue());
        Assertions.assertEquals(1, closeCount.intValue());
    }

    /*
     * Testing using EventManager's convertDataWithDefaultChannel() method by
     * sending multiple events at once.
     */
    @Test
    void sendingMultipleEventsWithDefaultChannelTest() {
        final int serverPort = 1603;
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

        final MockHttpServletRequest request1 = new MockHttpServletRequest();
        final String authToken1 = "AUTH_TOKEN_12223";
        request1.addHeader("Authorization", authToken1);

        final String allEventsInJson = "{\"event\": \"Pony 1 has left the barn\", \"sourcetype\": \"mysourcetype\", \"time\": 1426279439}{\"event\": \"Pony 2 has left the barn\"}{\"event\": \"Pony 3 has left the barn\", \"sourcetype\": \"newsourcetype\"}{\"event\": \"Pony 4 has left the barn\"}";
        final Response supposedResponse = new JsonResponse("Success");
        Assertions
                .assertEquals(supposedResponse, service.sendEvents(request1, null, allEventsInJson), "Should get a JSON with fields text, code and ackID");

        Assertions.assertDoesNotThrow(relpConnection::close);
        Assertions.assertDoesNotThrow(server::close);
        Assertions.assertEquals(1, openCount.intValue());
        Assertions.assertEquals(1, closeCount.intValue());
    }
}
