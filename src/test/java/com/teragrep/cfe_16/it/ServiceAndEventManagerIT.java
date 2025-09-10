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
import com.teragrep.cfe_16.Acknowledgements;
import com.teragrep.cfe_16.EventManager;
import com.teragrep.cfe_16.bo.HeaderInfo;
import com.teragrep.cfe_16.bo.HttpEventData;
import com.teragrep.cfe_16.bo.Session;
import com.teragrep.cfe_16.bo.XForwardedForStub;
import com.teragrep.cfe_16.bo.XForwardedHostStub;
import com.teragrep.cfe_16.bo.XForwardedProtoStub;
import com.teragrep.cfe_16.exceptionhandling.*;
import com.teragrep.cfe_16.response.AcknowledgedJsonResponse;
import com.teragrep.cfe_16.response.ExceptionEvent;
import com.teragrep.cfe_16.response.ExceptionEventContext;
import com.teragrep.cfe_16.response.ExceptionJsonResponse;
import com.teragrep.cfe_16.response.JsonResponse;
import com.teragrep.cfe_16.response.Response;
import com.teragrep.cfe_16.service.HECService;
import com.teragrep.rlp_03.Server;
import com.teragrep.rlp_03.ServerFactory;
import com.teragrep.rlp_03.config.Config;
import com.teragrep.rlp_03.delegate.DefaultFrameDelegate;
import com.teragrep.rlp_03.delegate.FrameDelegate;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
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
        "syslog.server.port=1610",
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
    private static Server server;
    private static final String hostname = "localhost";
    private static Integer port = 1610;

    @BeforeAll
    public static void init_x() throws IOException, InterruptedException {
        Supplier<FrameDelegate> frameDelegateSupplier = () -> new DefaultFrameDelegate(
                (frame) -> LOGGER.debug(frame.relpFrame().payload().toString())
        );
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

    @Autowired
    private HECService service;
    @Autowired
    private Acknowledgements acknowledgements;
    private static final ServerSocket serverSocket = getSocket();

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

    private final HeaderInfo headerInfo = new HeaderInfo(
            new XForwardedForStub(),
            new XForwardedHostStub(),
            new XForwardedProtoStub()
    );

    private static ServerSocket getSocket() {
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(1234);
        }
        catch (IOException e) {
            LOGGER.warn("Could not get a server socket: ", e);
            throw new RuntimeException(e);
        }
        return socket;
    }

    @AfterAll
    public static void closeServerSocket() {
        try {
            serverSocket.close();
        }
        catch (IOException e) {
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

        defaultChannel = Session.DEFAULT_CHANNEL;

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
        final Response expectedResponse = new AcknowledgedJsonResponse(HttpStatus.OK, "Success", 0);

        Assertions
                .assertEquals(expectedResponse, service.sendEvents(request1, channel3, eventInJson), "Service should return JSON object with fields 'text', 'code' and 'ackID' (ackID should be 0)");

        final Response expectedResponse1 = new AcknowledgedJsonResponse(HttpStatus.OK, "Success", 1);
        Assertions
                .assertEquals(expectedResponse1, service.sendEvents(request1, channel3, eventInJson), "Service should return JSON object with fields 'text', 'code' and 'ackID' (ackID should be 1)");

        final Response expectedResponse2 = new AcknowledgedJsonResponse(HttpStatus.OK, "Success", 0);
        Assertions
                .assertEquals(expectedResponse2, service.sendEvents(request1, channel2, eventInJson), "Service should return JSON object with fields 'text', 'code' and 'ackID' (ackID should be 0)");

        Assertions
                .assertEquals(expectedResponse2, service.sendEvents(request3, channel3, eventInJson), "Service should return JSON object with fields 'text', 'code' and 'ackID' (ackID should be 0)");

        final String expectedResponse3 = "{\"acks\":{\"1\":true,\"3\":false,\"4\":false}}";
        Assertions
                .assertEquals(expectedResponse3, service.getAcks(request1, channel3, ackRequestNode).toString(), "JSON object should be returned with ack statuses.");
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
        final Response expectedResponse = new JsonResponse(HttpStatus.OK, "Success");
        final Response response = service.sendEvents(request1, null, eventInJson);

        Assertions
                .assertEquals(
                        expectedResponse, response, "Service should return JSON object with fields 'text' and 'code'"
                );
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

    /**
     * Tests the EventManager's convertData() method. First we create a Json node as string, that we give as a parameter
     * for convertData(). We also create a supposed response Json node as string. convertData() returns a Response and
     * because the request is using the channel 1, Response should be {@link AcknowledgedJsonResponse}
     */
    @Test
    public void convertDataTest() {
        final String allEventsInJson = "{\"sourcetype\": \"mysourcetype\", \"event\": \"Hello, world!\", \"host\": \"localhost\", \"source\": \"mysource\", \"index\": \"myindex\"}";

        final Response expectedResponse = new AcknowledgedJsonResponse(HttpStatus.OK, "Success", 0);

        Assertions
                .assertEquals(
                        expectedResponse,
                        Assertions
                                .assertDoesNotThrow(
                                        () -> eventManager
                                                .convertData(
                                                        authToken1, channel1, allEventsInJson, headerInfo,
                                                        acknowledgements
                                                )
                                ),
                        "Should get a JSON with fields text, code and ackID"
                );
    }

    /**
     * Tests the EventManager's convertDataWithDefaultChannel() method which is called when a channel is not provided in
     * a request. First we create a Json node as string, that we give as a parameter for convertData(). We also create a
     * supposed response Json node as string. convertData() returns a Response, and because the request is using the
     * Default channel, Response should be {@link JsonResponse}
     */
    @Test
    public void convertDataTestWithDefaultChannel() {
        final String allEventsInJson = "{\"sourcetype\": \"mysourcetype\", \"event\": \"Hello, world!\", \"host\": \"localhost\", \"source\": \"mysource\", \"index\": \"myindex\"}";

        final Response expectedResponse = new JsonResponse(HttpStatus.OK, "Success");

        Assertions
                .assertEquals(
                        expectedResponse,
                        Assertions
                                .assertDoesNotThrow(
                                        () -> eventManager
                                                .convertData(
                                                        authToken1, defaultChannel, allEventsInJson, headerInfo,
                                                        acknowledgements
                                                )
                                ),
                        "Should get a JSON with fields text and code."
                );

    }

    /*
     * Tests attempting to send a request, which has no "event"-field in it's body.
     * When this is the case, UnsupportedOperationException is expected to happen.
     */
    @Test
    public void unsupportedOperationExceptionThrownIfEventIsMissing() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            final String allEventsInJson = "{\"sourcetype\": \"mysourcetype\", \"host\": \"localhost\", \"source\": \"mysource\", \"index\": \"myindex\"}";
            eventManager.convertData(authToken1, channel1, allEventsInJson, headerInfo, acknowledgements);
        });
    }

    /*
     * Tests attempting to send a request, which has a blank "event"-field. When
     * this is the case, UnsupportedOperationException is expected to happen.
     */
    @Test
    public void unsupportedOperationExceptionThrownIfEventIsEmpty() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            final String allEventsInJson = "{\"sourcetype\": \"mysourcetype\", \"event\": \"\", \"host\": \"localhost\", \"source\": \"mysource\", \"index\": \"myindex\"}";
            eventManager.convertData(authToken1, channel1, allEventsInJson, headerInfo, acknowledgements);
        });
    }

    /*
     * EventManager needs to handle the time stamp, if it is provided in the
     * request. This method tests the handling of time.
     */
    @Test
    public void handleTimeTest() {
        // Content strings are created with different kinds of "time"-fields amd they
        // are read into a JsonNode object.

        // content1: time is in epoch seconds (10 digits)
        String content1 = "{\"event\": \"Pony 1 has left the barn\", \"sourcetype\": \"mysourcetype\", \"time\": 1277464192}";
        // content2: "time"-field is not given
        String content2 = "{\"event\": \"Pony 1 has left the barn\", \"sourcetype\": \"mysourcetype\"}";
        // content3: time is given in epoch seconds and a decimal giving the epoch
        // milliseconds
        String content3 = "{\"event\": \"Pony 1 has left the barn\", \"sourcetype\": \"mysourcetype\", \"time\": 1433188255.253}";
        // content4: time is given inepoch milliseconds (13 digits)
        String content4 = "{\"event\": \"Pony 1 has left the barn\", \"sourcetype\": \"mysourcetype\", \"time\": 1433188255253}";
        // content5: time is given as a string
        String content5 = "{\"event\": \"Pony 1 has left the barn\", \"sourcetype\": \"mysourcetype\", \"time\": \"1433188255253\"}";
        // content6: time is given with too small amount of digits
        String content6 = "{\"event\": \"Pony 1 has left the barn\", \"sourcetype\": \"mysourcetype\", \"time\": 143318}";
        // content7: time is given in epoch centiseconds
        String content7 = "{\"event\": \"Pony 1 has left the barn\", \"sourcetype\": \"mysourcetype\", \"time\": 143318825525}";
        // content8: time is given with too many digits
        String content8 = "{\"event\": \"Pony 1 has left the barn\", \"sourcetype\": \"mysourcetype\", \"time\": 1433188255252321}";

        JsonNode node1 = null;
        JsonNode node2 = null;
        JsonNode node3 = null;
        JsonNode node4 = null;
        JsonNode node5 = null;
        JsonNode node6 = null;
        JsonNode node7 = null;
        JsonNode node8 = null;

        try {
            node1 = objectMapper.readTree(content1);
            node2 = objectMapper.readTree(content2);
            node3 = objectMapper.readTree(content3);
            node4 = objectMapper.readTree(content4);
            node5 = objectMapper.readTree(content5);
            node6 = objectMapper.readTree(content6);
            node7 = objectMapper.readTree(content7);
            node8 = objectMapper.readTree(content8);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        HttpEventData testData1 = new HttpEventData();
        HttpEventData testData2 = new HttpEventData();
        HttpEventData testData3 = new HttpEventData();
        HttpEventData testData4 = new HttpEventData();
        HttpEventData testData5 = new HttpEventData();
        HttpEventData testData6 = new HttpEventData();
        HttpEventData testData7 = new HttpEventData();
        HttpEventData testData8 = new HttpEventData();

        // Accessing a private method handleTime() in EventManager and using the created
        // nodes and empty HttpEventData objects as parameters
        try {
            testData1 = Whitebox.invokeMethod(eventManager, "handleTime", testData1, node1, null);
            testData2 = Whitebox.invokeMethod(eventManager, "handleTime", testData2, node2, null);
            testData3 = Whitebox.invokeMethod(eventManager, "handleTime", testData3, node3, null);
            testData4 = Whitebox.invokeMethod(eventManager, "handleTime", testData4, node4, null);
            testData5 = Whitebox.invokeMethod(eventManager, "handleTime", testData5, node5, null);
            testData6 = Whitebox.invokeMethod(eventManager, "handleTime", testData6, node6, null);
            testData7 = Whitebox.invokeMethod(eventManager, "handleTime", testData7, node7, null);
            testData8 = Whitebox.invokeMethod(eventManager, "handleTime", testData8, node8, null);
        }
        catch (Exception e) {
            LOGGER.warn("Could not invokeMethods properly: ", e);
        }

        /*
         * Testing the getTimeSource(), isTimeParsed() and getTimeAsLong() methods from
         * the HttpEventData objects that were returned from EventManager's handleTime()
         * method.
         */
        assertEquals(
                "Time source should be 'reported' when the time is specified in a request", "reported",
                testData1.getTimeSource()
        );
        assertTrue("timeParsed should be true when the time is specified in a request", testData1.isTimeParsed());
        assertEquals(
                "Time should have been converted to epoch milliseconds", 1277464192000L, testData1.getTimeAsLong()
        );

        assertEquals(
                "Time source should be 'generated' when it's not specified in a request", "generated",
                testData2.getTimeSource()
        );
        assertFalse("timeParsed should be false when time is not specified in a request", testData2.isTimeParsed());
        assertEquals("Time as long should be 0 when time is not specified in a request", 0, testData2.getTimeAsLong());

        assertEquals(
                "Time source should be 'reported' when the time is specified in a request", "reported",
                testData3.getTimeSource()
        );
        assertTrue("timeParsed should be true when time is specified in a request.", testData3.isTimeParsed());
        assertEquals(
                "Time should be converted to epoch milliseconds when it's provided in a request in epoch seconds with decimals.",
                1433188255253L, testData3.getTimeAsLong()
        );

        assertEquals(
                "Time source should be 'reported' when the time is specified in a request", "reported",
                testData4.getTimeSource()
        );
        assertTrue("timeParsed should be true when time is specified in a request.", testData4.isTimeParsed());
        assertEquals(
                "Time should be in epoch milliseconds when it is provided as epoch milliseconds in the request",
                1433188255253L, testData4.getTimeAsLong()
        );

        assertEquals(
                "Time source should be 'generated' when time is given as a string in a request", "generated",
                testData5.getTimeSource()
        );
        assertFalse("timeParsed should be false when time is given as a string in a request", testData5.isTimeParsed());
        assertEquals("Time should be 0 when time is given as a string in a request", 0, testData5.getTimeAsLong());

        assertEquals(
                "Time source should be 'generated' when time is given as an integer with less than 10 digits",
                "generated", testData6.getTimeSource()
        );
        assertFalse(
                "timeParsed should be false when time is given as an integer with less than 10 digits",
                testData6.isTimeParsed()
        );
        assertEquals("Time as long should be as provided in the request.", 143318, testData6.getTimeAsLong());

        assertEquals(
                "Time source should be 'reported' when the time is specified in a request with 10-13 digits",
                "reported", testData7.getTimeSource()
        );
        assertTrue(
                "timeParsed should be true when time is specified in a request with 10-13 digits",
                testData7.isTimeParsed()
        );
        assertEquals(
                "Time should be converted to epoch milliseconds when provided in a request with 10-13 digits",
                1433188255250L, testData7.getTimeAsLong()
        );

        assertEquals(
                "Time source should be 'generated' when time is given as an integer with more than 13 digits",
                "generated", testData8.getTimeSource()
        );
        assertFalse(
                "timeParsed should be false when time is given as an integer with more than 13 digits",
                testData8.isTimeParsed()
        );
        assertEquals("Time should be as it's provided in a request.", 1433188255252321L, testData8.getTimeAsLong());
    }

    /*
     * Testing using EventManager's convertData() method by sending multiple events
     * at once.
     */
    public void sendingMultipleEventsTest() {
        Acknowledgements acknowledgements = new Acknowledgements();
        String allEventsInJson = "{\"event\": \"Pony 1 has left the barn\", \"sourcetype\": \"mysourcetype\", \"time\": 1426279439}{\"event\": \"Pony 2 has left the barn\"}{\"event\": \"Pony 3 has left the barn\", \"sourcetype\": \"newsourcetype\"}{\"event\": \"Pony 4 has left the barn\"}";
        String supposedResponse = "{\"text\":\"Success\",\"code\":0,\"ackID\":0}";
        assertEquals(
                "Should get a JSON with fields text, code and ackID", supposedResponse,
                Assertions
                        .assertDoesNotThrow(
                                () -> eventManager
                                        .convertData(authToken1, channel1, allEventsInJson, headerInfo, acknowledgements)
                        )
                        .toString()
        );

    }

    /*
     * Testing using EventManager's convertDataWithDefaultChannel() method by
     * sending multiple events at once.
     */
    public void sendingMultipleEventsWithDefaultChannelTest() {
        Acknowledgements acknowledgements = new Acknowledgements();
        String allEventsInJson = "{\"event\": \"Pony 1 has left the barn\", \"sourcetype\": \"mysourcetype\", \"time\": 1426279439}{\"event\": \"Pony 2 has left the barn\"}{\"event\": \"Pony 3 has left the barn\", \"sourcetype\": \"newsourcetype\"}{\"event\": \"Pony 4 has left the barn\"}";
        String supposedResponse = "{\"text\":\"Success\",\"code\":0,\"ackID\":0}";
        assertEquals(
                "Should get a JSON with fields text, code and ackID", supposedResponse,
                Assertions
                        .assertDoesNotThrow(
                                () -> eventManager
                                        .convertData(
                                                authToken1, defaultChannel, allEventsInJson, headerInfo,
                                                acknowledgements
                                        )
                        )
                        .toString()
        );
    }

    @Test
    @DisplayName("convertData() method returns ExceptionJsonReport if event was not read")
    void convertDataMethodReturnsExceptionJsonReportIfEventWasNotRead() {
        final String allEventsInJson = "{\"sourcetype\": \"mysourcetype\", \"event\": {\"Hello, world!\", \"host\": \"localhost\", \"source\": \"mysource\", \"index\": \"myindex\"}";
        final ExceptionEventContext exceptionEventContext = new ExceptionEventContext(
                new HeaderInfo(new XForwardedForStub(), new XForwardedHostStub(), new XForwardedProtoStub()),
                "user-agent",
                "uriPath",
                "host"
        );
        final Response expectedResponse = new ExceptionJsonResponse(
                HttpStatus.BAD_REQUEST,
                new ExceptionEvent(exceptionEventContext, UUID.randomUUID(), new Throwable())
        );

        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("authorization", "AUTH_TOKEN_12223");
        final Response actualResponse = Assertions
                .assertDoesNotThrow(() -> service.sendEvents(request, Session.DEFAULT_CHANNEL, allEventsInJson));

        Assertions.assertEquals(expectedResponse.status(), actualResponse.status());

        Assertions.assertEquals(ExceptionJsonResponse.class, actualResponse.getClass());
    }
}
