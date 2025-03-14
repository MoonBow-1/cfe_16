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

import java.io.IOException;

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
public class ServiceAndHECBatchIT {

    private static final Integer port = 1603;
    private static final ConcurrentLinkedDeque<byte[]> messageList = new ConcurrentLinkedDeque<>();
    private static final AtomicLong openCount = new AtomicLong();
    private static final AtomicLong closeCount = new AtomicLong();
    private static TestServer server;
    @Autowired
    private HECService service;
    private MockHttpServletRequest request1;
    private MockHttpServletRequest request2;
    private MockHttpServletRequest request3;
    private MockHttpServletRequest request4;
    private MockHttpServletRequest request5;
    private String eventInJson;
    private String channel1;
    private String channel2;
    private String channel3;
    private String authToken1;
    private String authToken2;
    private String authToken3;
    private String authToken4;
    private String ackRequest;
    private ObjectMapper objectMapper;
    private JsonNode ackRequestNode;

    @BeforeAll
    public static void init() {
        TestServerFactory serverFactory = new TestServerFactory();
        try {
            server = serverFactory.create(port, messageList, openCount, closeCount);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        server.run();
    }

    @AfterAll
    public static void close() {
        try {
            server.close();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    public void clear() {
        openCount.set(0);
        closeCount.set(0);
        messageList.clear();
    }

    /*
     * Opens a ServerSocket so that the sending an event won't produce an error. 4
     * Mock requests are created. request2 will not have authorization token
     * assigned to it. eventInJson variable is the body of the request as a string
     * when sending an event and ackRequest is the body of the request when
     * requesting Ack statuses.
     */
    @BeforeEach
    public void initialize() {
        objectMapper = new ObjectMapper();
        request1 = new MockHttpServletRequest();
        request2 = new MockHttpServletRequest();
        request3 = new MockHttpServletRequest();
        request4 = new MockHttpServletRequest();
        request5 = new MockHttpServletRequest();

        eventInJson = "{\"sourcetype\": \"mysourcetype\", \"event\": \"Hello, world!\", \"host\": \"localhost\", \"source\": \"mysource\", \"index\": \"myindex\"}";

        channel1 = "CHANNEL_11111";
        channel2 = "CHANNEL_22222";
        channel3 = "CHANNEL_33333";

        authToken1 = "AUTH_TOKEN_12223";
        authToken2 = "AUTH_TOKEN_16664";
        authToken3 = "AUTH_TOKEN_23667";
        authToken4 = "AUTH_TOKEN_23249";

        request1.addHeader("Authorization", authToken1);
        request3.addHeader("Authorization", authToken2);
        request4.addHeader("Authorization", authToken3);
        request5.addHeader("Authorization", authToken4);

        ackRequest = "{\"acks\": [1,3,4]}";

        ackRequestNode = objectMapper.createObjectNode();
        try {
            ackRequestNode = objectMapper.readTree(ackRequest);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * Tests the sendEvents() and getAcks() method of the service.
     */
    @Test
    public void sendEventsAndGetAcksTest() {
        String supposedResponse;

        supposedResponse = "{\"text\":\"Success\",\"code\":0,\"ackID\":0}";
        assertEquals(
                "Service should return JSON object with fields 'text', 'code' and 'ackID' (ackID should be 0)",
                supposedResponse, service.sendEvents(request1, channel3, eventInJson).toString()
        );

        supposedResponse = "{\"text\":\"Success\",\"code\":0,\"ackID\":1}";
        assertEquals(
                "Service should return JSON object with fields 'text', 'code' and 'ackID' (ackID should be 1)",
                supposedResponse, service.sendEvents(request1, channel3, eventInJson).toString()
        );

        supposedResponse = "{\"text\":\"Success\",\"code\":0,\"ackID\":0}";
        assertEquals(
                "Service should return JSON object with fields 'text', 'code' and 'ackID' (ackID should be 0)",
                supposedResponse, service.sendEvents(request1, channel2, eventInJson).toString()
        );

        assertEquals(
                "Service should return JSON object with fields 'text', 'code' and 'ackID' (ackID should be 0)",
                supposedResponse, service.sendEvents(request3, channel3, eventInJson).toString()
        );

        supposedResponse = "{\"acks\":{\"1\":true,\"3\":false,\"4\":false}}";
        assertEquals(
                "JSON object should be returned with ack statuses.", supposedResponse,
                service.getAcks(request1, channel3, ackRequestNode).toString()
        );
    }

    /*
     * Tests sending a request with no authentication token. In this case
     * AuthenticationTokenMissingException is expected to happen.
     */
    @Test
    public void sendEventsWithoutAuthTokenTest() {
        Assertions.assertThrows(AuthenticationTokenMissingException.class, () -> {
            service.sendEvents(request2, eventInJson, channel1);
        });
    }

    /*
     * Tests sending a request with no channel provided. In this case no Ack id is
     * returned.
     */
    @Test
    public void sendEventsWithoutChannelTest() {
        String supposedResponse = "{\"text\":\"Success\",\"code\":0}";
        String response = service.sendEvents(request1, null, eventInJson).toString();
        assertEquals("Service should return JSON object with fields 'text' and 'code'", supposedResponse, response);
    }

    /*
     * Tests getting the Ack statuses without providing channel in the request. In
     * this case ChannelNotProvidedException is expected to happen.
     */
    @Test
    public void getAcksWithoutChannel() {
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
    public void getAcksWithoutAuthTokenTest() {
        Assertions.assertThrows(AuthenticationTokenMissingException.class, () -> {
            service.getAcks(request2, channel1, ackRequestNode);
        });
    }

    /*
     * Tests trying to get Ack statuses with an authentication that is not used to
     * send events. In this case SessionNotFoundException is expected to happen.
     */
    @Test
    public void getAcksWithUnusedAuthToken() {
        Assertions.assertThrows(SessionNotFoundException.class, () -> {
            service.getAcks(request4, channel1, ackRequestNode);
        });
    }

    /*
     * Tests trying to get Ack statuses with a channel that is does not exist in the
     * session. In this case ChannelNotFoundException is expected to happen.
     */
    @Test
    public void getAcksWithUnusedChannel() {
        Assertions.assertThrows(ChannelNotFoundException.class, () -> {
            service.sendEvents(request5, channel1, eventInJson);
            service.getAcks(request5, channel2, ackRequestNode);
        });
    }

    /*
     * Testing using EventManager's convertData() method by sending multiple events
     * at once.
     */
    @Test
    public void sendingMultipleEventsTest() {
        String allEventsInJson = "{\"event\": \"Pony 1 has left the barn\", \"sourcetype\": \"mysourcetype\", \"time\": 1426279439}{\"event\": \"Pony 2 has left the barn\"}{\"event\": \"Pony 3 has left the barn\", \"sourcetype\": \"newsourcetype\"}{\"event\": \"Pony 4 has left the barn\"}";
        String supposedResponse = "{\"text\":\"Success\",\"code\":0,\"ackID\":0}";
        assertEquals(
                "Should get a JSON with fields text, code and ackID", supposedResponse,
                service.sendEvents(request1, channel1, allEventsInJson).toString()
        );

    }

    /*
     * Testing using EventManager's convertDataWithDefaultChannel() method by
     * sending multiple events at once.
     */
    @Test
    public void sendingMultipleEventsWithDefaultChannelTest() {
        String allEventsInJson = "{\"event\": \"Pony 1 has left the barn\", \"sourcetype\": \"mysourcetype\", \"time\": 1426279439}{\"event\": \"Pony 2 has left the barn\"}{\"event\": \"Pony 3 has left the barn\", \"sourcetype\": \"newsourcetype\"}{\"event\": \"Pony 4 has left the barn\"}";
        String supposedResponse = "{\"text\":\"Success\",\"code\":0}";
        assertEquals(
                "Should get a JSON with fields text, code and ackID", supposedResponse,
                service.sendEvents(request1, null, allEventsInJson).toString()
        );
    }
}
