/*
 * HTTP Event Capture to RFC5424 CFE_16
 * Copyright (C) 2025 Suomen Kanuuna Oy
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
import com.teragrep.cfe_16.AckManager;
import com.teragrep.cfe_16.EventManager;
import com.teragrep.cfe_16.bo.HeaderInfo;
import com.teragrep.cfe_16.bo.Session;
import com.teragrep.cfe_16.exceptionhandling.*;
import com.teragrep.cfe_16.service.HECService;
import com.teragrep.rlp_03.Server;
import com.teragrep.rlp_03.ServerFactory;
import com.teragrep.rlp_03.config.Config;
import com.teragrep.rlp_03.delegate.DefaultFrameDelegate;
import com.teragrep.rlp_03.delegate.FrameDelegate;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.function.Supplier;

import static org.junit.Assert.*;

/*
 * Tests the functionality of HECServiceImpl
 */

@SpringBootTest
@TestPropertySource(properties = { 
		"syslog.server.host=127.0.0.1", 
		"syslog.server.port=1601",
		"syslog.server.protocol=RELP", 
		"max.channels=1000000", 
		"max.ack.value=1000000", 
		"max.ack.age=20000", 
		"max.session.age=30000", 
		"poll.time=30000",
		"server.print.times=true" 
		})
public class ServiceAndEventManagerIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceAndEventManagerIT.class);
    private static final String hostname = "localhost";
    private static final ServerSocket serverSocket = getSocket();
    private static final Integer port = 1601;
    private static Server server;
    private final HeaderInfo headerInfo = new HeaderInfo();
    @Autowired
    private HECService service;
    @Autowired
    private AckManager ackManager;
    private MockHttpServletRequest request1;
    private MockHttpServletRequest request2;
    private MockHttpServletRequest request3;
    private MockHttpServletRequest request4;
    private MockHttpServletRequest request5;
    private String eventInJson;
    private String channel1;
    private String channel2;
    private String channel3;
    private String defaultChannel;
    private String authToken1;
    private String authToken2;
    private String authToken3;
    private String authToken4;
    private String ackRequest;
    private ObjectMapper objectMapper;
    private JsonNode ackRequestNode;
    @Autowired
    private EventManager eventManager;

    @BeforeAll
    public static void init_x() throws IOException, InterruptedException {
        Supplier<FrameDelegate> frameDelegateSupplier = () -> new DefaultFrameDelegate(
            (frame) -> LOGGER.debug(frame.relpFrame().payload().toString()));
        Config config = new Config(port, 1);
        ServerFactory serverFactory = new ServerFactory(config, frameDelegateSupplier);

        server = serverFactory.create();
        Thread serverThread = new Thread(server);
        serverThread.start();
        server.startup.waitForCompletion();
    }

    @AfterAll
    public static void cleanup() throws InterruptedException {
        server.stop();
    }

    private static ServerSocket getSocket() {
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(1234);
        } catch (IOException e) {
            LOGGER.warn("Could not get a server socket: ", e);
            throw new RuntimeException(e);
        }
        return socket;
    }

    @AfterAll
    public static void closeServerSocket() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            LOGGER.warn("Could not close server socket: ", e);
        }
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
        /*
         * try { serverSocket = new ServerSocket(props.getSyslogPort()); } catch
         * (IOException e) { e.printStackTrace(); }
         */

        objectMapper = new ObjectMapper();
        request1 = new MockHttpServletRequest();
        request2 = new MockHttpServletRequest();
        request3 = new MockHttpServletRequest();
        request4 = new MockHttpServletRequest();
        request5 = new MockHttpServletRequest();

        eventInJson = "{\"sourcetype\": \"mysourcetype\", \"event\": \"Hello, world!\", \"host\":"
            + " \"localhost\", \"source\": \"mysource\", \"index\": \"myindex\"}";

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

        defaultChannel = Session.DEFAULT_CHANNEL;

        try {
            ackRequestNode = objectMapper.readTree(ackRequest);
        } catch (IOException e) {
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
            "Service should return JSON object with fields 'text', 'code' and 'ackID' (ackID "
                + "should be 0)",
            supposedResponse, service.sendEvents(request1, channel3, eventInJson).toString());

        supposedResponse = "{\"text\":\"Success\",\"code\":0,\"ackID\":1}";
        assertEquals(
            "Service should return JSON object with fields 'text', 'code' and 'ackID' (ackID "
                + "should be 1)",
            supposedResponse, service.sendEvents(request1, channel3, eventInJson).toString());

        supposedResponse = "{\"text\":\"Success\",\"code\":0,\"ackID\":0}";
        assertEquals(
            "Service should return JSON object with fields 'text', 'code' and 'ackID' (ackID "
                + "should be 0)",
            supposedResponse, service.sendEvents(request1, channel2, eventInJson).toString());

        assertEquals(
            "Service should return JSON object with fields 'text', 'code' and 'ackID' (ackID "
                + "should be 0)",
            supposedResponse, service.sendEvents(request3, channel3, eventInJson).toString());

        supposedResponse = "{\"acks\":{\"1\":true,\"3\":false,\"4\":false}}";
        assertEquals("JSON object should be returned with ack statuses.", supposedResponse,
            service.getAcks(request1, channel3, ackRequestNode).toString());
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
        assertEquals("Service should return JSON object with fields 'text' and 'code'",
            supposedResponse,
            response);
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
     * Tests the EventManager's convertData() method. First we create a Json node as
     * string, that we give as a parameter for convertData(). We also create a
     * supposed response Json node as string. convertData() returns an ObjectNode
     * object, which we convert to string here, so we can easily compare it to our
     * supposed response.
     */
    @Test
    public void convertDataTest() {
        /*AckManager ackManager = new AckManager();*/
        String allEventsInJson = "{\"sourcetype\": \"mysourcetype\", \"event\": \"Hello, "
            + "world!\", \"host\": \"localhost\", \"source\": \"mysource\", \"index\": "
            + "\"myindex\"}";
        String supposedResponse = "{\"text\":\"Success\",\"code\":0,\"ackID\":2}";
        String response = eventManager.convertData(authToken1, channel1, allEventsInJson,
            headerInfo, ackManager).toString();
        assertEquals("Should get a JSON with fields text, code and ackID", supposedResponse,
            response);
    }

    /*
     * Tests the EventManager's convertDataWithDefaultChannel() method which is
     * called when a channel is not provided in a request. First we create a Json
     * node as string, that we give as a parameter for convertData(). We also create
     * a supposed response Json node as string. convertData() returns an ObjectNode
     * object, which we convert to string here, so we can easily compare it to our
     * supposed response.
     */
    @Test
    public void convertDataTestWithDefaultChannel() {
        /* AckManager ackManager = new AckManager(); */
        String allEventsInJson = "{\"sourcetype\": \"mysourcetype\", \"event\": \"Hello, "
            + "world!\", \"host\": \"localhost\", \"source\": \"mysource\", \"index\": "
            + "\"myindex\"}";
        String supposedResponse = "{\"text\":\"Success\",\"code\":0}";

        assertEquals("Should get a JSON with fields text and code.", supposedResponse, eventManager
            .convertData(authToken1, defaultChannel, allEventsInJson, headerInfo, ackManager)
            .toString());

    }

    /*
     * Tests attempting to send a request, which has no "event"-field in it's body.
     * When this is the case, EventFieldMissingException is expected to happen.
     */
    @Test
    public void noEventFieldInRequestTest() {
        Assertions.assertThrows(EventFieldMissingException.class, () -> {
            /*AckManager ackManager = new AckManager();*/
            String allEventsInJson = "{\"sourcetype\": \"mysourcetype\", \"host\": \"localhost\","
                + " \"source\": \"mysource\", \"index\": \"myindex\"}";
            eventManager.convertData(authToken1, channel1, allEventsInJson, headerInfo, ackManager);
        });
    }

    /*
     * Tests attempting to send a request, which has a blank "event"-field. When
     * this is the case, EventFieldBlankException is expected to happen.
     */
    @Test
    public void eventFieldBlankInRequestTest() {
        Assertions.assertThrows(EventFieldBlankException.class, () -> {
            /*AckManager ackManager = new AckManager();*/
            String allEventsInJson = "{\"sourcetype\": \"mysourcetype\", \"event\": \"\", "
                + "\"host\": \"localhost\", \"source\": \"mysource\", \"index\": \"myindex\"}";
            eventManager.convertData(authToken1, channel1, allEventsInJson, headerInfo, ackManager);
        });
    }

    /*
     * Testing using EventManager's convertData() method by sending multiple events
     * at once.
     */
    public void sendingMultipleEventsTest() {
        AckManager ackManager = new AckManager();
        String allEventsInJson = "{\"event\": \"Pony 1 has left the barn\", \"sourcetype\": "
            + "\"mysourcetype\", \"time\": 1426279439}{\"event\": \"Pony 2 has left the "
            + "barn\"}{\"event\": \"Pony 3 has left the barn\", \"sourcetype\": "
            + "\"newsourcetype\"}{\"event\": \"Pony 4 has left the barn\"}";
        String supposedResponse = "{\"text\":\"Success\",\"code\":0,\"ackID\":0}";
        assertEquals("Should get a JSON with fields text, code and ackID", supposedResponse,
            eventManager.convertData(authToken1, channel1, allEventsInJson, headerInfo, ackManager)
                .toString());

    }

    /*
     * Testing using EventManager's convertDataWithDefaultChannel() method by
     * sending multiple events at once.
     */
    public void sendingMultipleEventsWithDefaultChannelTest() {
        AckManager ackManager = new AckManager();
        String allEventsInJson = "{\"event\": \"Pony 1 has left the barn\", \"sourcetype\": "
            + "\"mysourcetype\", \"time\": 1426279439}{\"event\": \"Pony 2 has left the "
            + "barn\"}{\"event\": \"Pony 3 has left the barn\", \"sourcetype\": "
            + "\"newsourcetype\"}{\"event\": \"Pony 4 has left the barn\"}";
        String supposedResponse = "{\"text\":\"Success\",\"code\":0,\"ackID\":0}";
        assertEquals("Should get a JSON with fields text, code and ackID", supposedResponse,
            eventManager
                .convertData(authToken1, defaultChannel, allEventsInJson, headerInfo, ackManager)
                .toString());
    }
}
