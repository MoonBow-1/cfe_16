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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import static org.junit.Assert.*;

/*
 * Tests the functionality of HECServiceImpl
 */

@SpringBootTest
@TestPropertySource(properties = {
        "syslog.server.host=127.0.0.1",
        "syslog.server.port=1603",
        "syslog.server.protocol=RELP",
        "max.channels=1000000",
        "max.ack.value=1000000",
        "max.ack.age=20000",
        "max.session.age=30000",
        "poll.time=30000",
        "server.print.times=true"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
final class ServiceAndHECBatchIT {

    private static final Integer port = 1603;
    private static final ConcurrentLinkedDeque<byte[]> messageList = new ConcurrentLinkedDeque<>();
    private static final AtomicLong openCount = new AtomicLong();
    private static final AtomicLong closeCount = new AtomicLong();
    private static TestServer server;
    @Autowired
    private HECService service;

    @BeforeAll
    static void init() {
        final TestServerFactory serverFactory = new TestServerFactory();
        server = Assertions.assertDoesNotThrow(() -> serverFactory.create(port, messageList, openCount, closeCount));
        server.run();
    }

    @AfterAll
    static void close() {
        Assertions.assertDoesNotThrow(() -> server.close());
    }

    @AfterEach
    void clear() {
        openCount.set(0);
        closeCount.set(0);
        messageList.clear();
    }

    /*
     * Tests the sendEvents() and getAcks() method of the service.
     */
    @Test
    void sendEventsAndGetAcksTest() {
        final String eventInJson = "{\"sourcetype\": \"mysourcetype\", \"event\": \"Hello, world!\", \"host\": \"localhost\", \"source\": \"mysource\", \"index\": \"myindex\"}";

        final MockHttpServletRequest request1 = new MockHttpServletRequest();
        request1.addHeader("Authorization", "AUTH_TOKEN_12223");
        Response supposedResponse;

        supposedResponse = new AcknowledgedJsonResponse("Success", 0);
        Assertions
                .assertEquals(supposedResponse, service.sendEvents(request1, "CHANNEL_33333", eventInJson), "Service should return JSON object with fields 'text', 'code' and 'ackID' (ackID should be 0)");

        supposedResponse = new AcknowledgedJsonResponse("Success", 1);
        Assertions
                .assertEquals(supposedResponse, service.sendEvents(request1, "CHANNEL_33333", eventInJson), "Service should return JSON object with fields 'text', 'code' and 'ackID' (ackID should be 1)");

        supposedResponse = new AcknowledgedJsonResponse("Success", 0);
        Assertions
                .assertEquals(supposedResponse, service.sendEvents(request1, "CHANNEL_22222", eventInJson), "Service should return JSON object with fields 'text', 'code' and 'ackID' (ackID should be 0)");

        final MockHttpServletRequest request3 = new MockHttpServletRequest();
        request3.addHeader("Authorization", "AUTH_TOKEN_16664");

        Assertions
                .assertEquals(supposedResponse, service.sendEvents(request3, "CHANNEL_33333", eventInJson), "Service should return JSON object with fields 'text', 'code' and 'ackID' (ackID should be 0)");

        final ObjectMapper objectMapper = new ObjectMapper();
        final String ackRequest = "{\"acks\": [1,3,4]}";

        final JsonNode ackRequestNode = Assertions.assertDoesNotThrow(() -> objectMapper.readTree(ackRequest));
        final String supposedAckResponse = "{\"acks\":{\"1\":true,\"3\":false,\"4\":false}}";
        Assertions
                .assertEquals(supposedAckResponse, service.getAcks(request1, "CHANNEL_33333", ackRequestNode).toString(), "JSON object should be returned with ack statuses.");
    }

    /*
     * Tests sending a request with no authentication token. In this case
     * AuthenticationTokenMissingException is expected to happen.
     */
    @Test
    void sendEventsWithoutAuthTokenTest() {
        final String eventInJson = "{\"sourcetype\": \"mysourcetype\", \"event\": \"Hello, world!\", \"host\": \"localhost\", \"source\": \"mysource\", \"index\": \"myindex\"}";
        final MockHttpServletRequest request2 = new MockHttpServletRequest();

        Assertions.assertThrows(AuthenticationTokenMissingException.class, () -> {
            service.sendEvents(request2, eventInJson, "CHANNEL_11111");
        });
    }

    /*
     * Tests sending a request with no channel provided. In this case no Ack id is
     * returned.
     */
    @Test
    void sendEventsWithoutChannelTest() {
        final String eventInJson = "{\"sourcetype\": \"mysourcetype\", \"event\": \"Hello, world!\", \"host\": \"localhost\", \"source\": \"mysource\", \"index\": \"myindex\"}";
        final MockHttpServletRequest request1 = new MockHttpServletRequest();
        request1.addHeader("Authorization", "AUTH_TOKEN_12223");

        final Response supposedResponse = new JsonResponse("Success");
        final Response response = service.sendEvents(request1, null, eventInJson);
        Assertions
                .assertEquals(
                        supposedResponse, response, "Service should return JSON object with fields 'text' and 'code'"
                );
    }

    /*
     * Tests getting the Ack statuses without providing channel in the request. In
     * this case ChannelNotProvidedException is expected to happen.
     */
    @Test
    void getAcksWithoutChannel() {
        final ObjectMapper objectMapper = new ObjectMapper();
        final String ackRequest = "{\"acks\": [1,3,4]}";

        final JsonNode ackRequestNode = Assertions.assertDoesNotThrow(() -> objectMapper.readTree(ackRequest));

        final MockHttpServletRequest request1 = new MockHttpServletRequest();
        request1.addHeader("Authorization", "AUTH_TOKEN_12223");

        Assertions.assertThrows(ChannelNotProvidedException.class, () -> {
            service.getAcks(request1, null, ackRequestNode);
        });
    }

    /*
     * Tests getting the Ack statuses without providing an authentication token in
     * the request. In this case AuthenticationTokenMissingException is expected to
     * happen.
     */
    @Test
    void getAcksWithoutAuthTokenTest() {
        final ObjectMapper objectMapper = new ObjectMapper();
        final String ackRequest = "{\"acks\": [1,3,4]}";

        final JsonNode ackRequestNode = Assertions.assertDoesNotThrow(() -> objectMapper.readTree(ackRequest));

        final MockHttpServletRequest request2 = new MockHttpServletRequest();

        Assertions.assertThrows(AuthenticationTokenMissingException.class, () -> {
            service.getAcks(request2, "CHANNEL_11111", ackRequestNode);
        });
    }

    /*
     * Tests trying to get Ack statuses with an authentication that is not used to
     * send events. In this case SessionNotFoundException is expected to happen.
     */
    @Test
    void getAcksWithUnusedAuthToken() {
        final ObjectMapper objectMapper = new ObjectMapper();
        final String ackRequest = "{\"acks\": [1,3,4]}";

        final JsonNode ackRequestNode = Assertions.assertDoesNotThrow(() -> objectMapper.readTree(ackRequest));

        final MockHttpServletRequest request4 = new MockHttpServletRequest();
        request4.addHeader("Authorization", "AUTH_TOKEN_23667");

        Assertions.assertThrows(SessionNotFoundException.class, () -> {
            service.getAcks(request4, "CHANNEL_11111", ackRequestNode);
        });
    }

    /*
     * Tests trying to get Ack statuses with a channel that is does not exist in the
     * session. In this case ChannelNotFoundException is expected to happen.
     */
    @Test
    void getAcksWithUnusedChannel() {
        final MockHttpServletRequest request5 = new MockHttpServletRequest();
        final ObjectMapper objectMapper = new ObjectMapper();
        final String eventInJson = "{\"sourcetype\": \"mysourcetype\", \"event\": \"Hello, world!\", \"host\": \"localhost\", \"source\": \"mysource\", \"index\": \"myindex\"}";
        final String ackRequest = "{\"acks\": [1,3,4]}";

        final JsonNode ackRequestNode = Assertions.assertDoesNotThrow(() -> objectMapper.readTree(ackRequest));

        request5.addHeader("Authorization", "AUTH_TOKEN_23249");

        Assertions.assertThrows(ChannelNotFoundException.class, () -> {
            service.sendEvents(request5, "CHANNEL_11111", eventInJson);
            service.getAcks(request5, "CHANNEL_22222", ackRequestNode);
        });
    }

    /*
     * Testing using EventManager's convertData() method by sending multiple events
     * at once.
     */
    @Test
    void sendingMultipleEventsTest() {
        final MockHttpServletRequest request1 = new MockHttpServletRequest();
        request1.addHeader("Authorization", "AUTH_TOKEN_12223");

        final String allEventsInJson = "{\"event\": \"Pony 1 has left the barn\", \"sourcetype\": \"mysourcetype\", \"time\": 1426279439}{\"event\": \"Pony 2 has left the barn\"}{\"event\": \"Pony 3 has left the barn\", \"sourcetype\": \"newsourcetype\"}{\"event\": \"Pony 4 has left the barn\"}";
        final Response supposedResponse = new AcknowledgedJsonResponse("Success", 0);
        final Response response = service.sendEvents(request1, "CHANNEL_11111", allEventsInJson);
        Assertions.assertEquals(supposedResponse, response, "Should get a JSON with fields text, code and ackID");

    }

    /*
     * Testing using EventManager's convertDataWithDefaultChannel() method by
     * sending multiple events at once.
     */
    @Test
    void sendingMultipleEventsWithDefaultChannelTest() {
        final MockHttpServletRequest request1 = new MockHttpServletRequest();
        request1.addHeader("Authorization", "AUTH_TOKEN_12223");

        final String allEventsInJson = "{\"event\": \"Pony 1 has left the barn\", \"sourcetype\": \"mysourcetype\", \"time\": 1426279439}{\"event\": \"Pony 2 has left the barn\"}{\"event\": \"Pony 3 has left the barn\", \"sourcetype\": \"newsourcetype\"}{\"event\": \"Pony 4 has left the barn\"}";
        final Response supposedResponse = new JsonResponse("Success");
        final Response response = service.sendEvents(request1, null, allEventsInJson);

        Assertions.assertEquals(supposedResponse, response, "Should get a JSON with fields text, code and ackID");
    }
}
