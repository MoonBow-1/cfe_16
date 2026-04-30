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
package com.teragrep.cfe_16.rest;

import com.teragrep.cfe_16.Acknowledgements;
import com.teragrep.cfe_16.SessionManager;
import com.teragrep.cfe_16.TokenManager;
import com.teragrep.cfe_16.config.Configuration;
import com.teragrep.cfe_16.connection.RelpConnection;
import com.teragrep.cfe_16.response.AcknowledgedJsonResponse;
import com.teragrep.cfe_16.response.JsonResponse;
import com.teragrep.cfe_16.server.TestServer;
import com.teragrep.cfe_16.server.TestServerFactory;
import com.teragrep.cfe_16.service.HECService;
import com.teragrep.cfe_16.service.HECServiceImpl;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import tools.jackson.databind.JsonNode;

final class HECRestControllerTest {

    @Test
    @DisplayName("test JSON sendEvents endpoint with channel present")
    void testJsonSendEventsEndpointWithChannelPresent() {
        final int serverPort = 1248;
        final TestServerFactory serverFactory = new TestServerFactory();
        final ConcurrentLinkedDeque<byte[]> messageList = new ConcurrentLinkedDeque<>();
        final AtomicLong openCount = new AtomicLong();
        final AtomicLong closeCount = new AtomicLong();

        final TestServer server = Assertions
                .assertDoesNotThrow(() -> serverFactory.create(serverPort, messageList, openCount, closeCount));

        server.run();

        final Configuration configuration = new Configuration();
        final RelpConnection relpConnection = new RelpConnection("localhost", serverPort);
        Assertions
                .assertTimeout(Duration.of(5, ChronoUnit.SECONDS), relpConnection::connect, "RelpConnection did not connect in 5 seconds");
        final HECService service = new HECServiceImpl(
                new Acknowledgements(configuration),
                new SessionManager(configuration),
                new TokenManager(),
                relpConnection
        );

        final HECRestController hecRestController = new HECRestController(service, configuration);

        final MockHttpServletRequest request1 = new MockHttpServletRequest();
        request1.addHeader("Authorization", "AUTH_TOKEN_11111");
        final String channel1 = "CHANNEL_11111";
        final String eventInJson = "{\"sourcetype\":\"access\", \"source\":\"/var/log/access.log\", "
                + "\"event\": {\"message\":\"Access log test message 1\"}} "
                + "{\"sourcetype\":\"access\", \"source\":\"/var/log/access.log\", \"event\": "
                + "{\"message\":\"Access log test message 2\"}}";

        final ResponseEntity<JsonNode> responseEntity = Assertions
                .assertDoesNotThrow(() -> hecRestController.sendEvents(request1, eventInJson, channel1));
        final AcknowledgedJsonResponse expectedResponse = new AcknowledgedJsonResponse("Success", 0);
        final ResponseEntity<JsonNode> expectedResponseEntity = expectedResponse.asJsonNodeResponseEntity();

        Assertions.assertEquals(expectedResponseEntity, responseEntity);

        Assertions.assertDoesNotThrow(relpConnection::close);
        Assertions.assertDoesNotThrow(server::close);
        Assertions.assertEquals(1, openCount.intValue());
        Assertions.assertEquals(1, closeCount.intValue());
    }

    @Test
    @DisplayName("test JSON sendEvents endpoint without channel present")
    void testJsonSendEventsEndpointWithoutChannelPresent() {
        final int serverPort = 1248;
        final TestServerFactory serverFactory = new TestServerFactory();
        final ConcurrentLinkedDeque<byte[]> messageList = new ConcurrentLinkedDeque<>();
        final AtomicLong openCount = new AtomicLong();
        final AtomicLong closeCount = new AtomicLong();

        final TestServer server = Assertions
                .assertDoesNotThrow(() -> serverFactory.create(serverPort, messageList, openCount, closeCount));

        server.run();

        final Configuration configuration = new Configuration();
        final RelpConnection relpConnection = new RelpConnection("localhost", serverPort);
        Assertions
                .assertTimeout(Duration.of(5, ChronoUnit.SECONDS), relpConnection::connect, "RelpConnection did not connect in 5 seconds");
        final HECService service = new HECServiceImpl(
                new Acknowledgements(configuration),
                new SessionManager(configuration),
                new TokenManager(),
                relpConnection
        );

        final HECRestController hecRestController = new HECRestController(service, configuration);

        final MockHttpServletRequest request1 = new MockHttpServletRequest();
        request1.addHeader("Authorization", "AUTH_TOKEN_11111");
        final String eventInJson = "{\"sourcetype\":\"access\", \"source\":\"/var/log/access.log\", "
                + "\"event\": {\"message\":\"Access log test message 1\"}} "
                + "{\"sourcetype\":\"access\", \"source\":\"/var/log/access.log\", \"event\": "
                + "{\"message\":\"Access log test message 2\"}}";

        final ResponseEntity<JsonNode> responseEntity = Assertions
                .assertDoesNotThrow(() -> hecRestController.sendEvents(request1, eventInJson, null));
        final JsonResponse expectedResponse = new JsonResponse("Success");
        final ResponseEntity<JsonNode> expectedResponseEntity = expectedResponse.asJsonNodeResponseEntity();

        Assertions.assertEquals(expectedResponseEntity, responseEntity);

        Assertions.assertDoesNotThrow(relpConnection::close);
        Assertions.assertDoesNotThrow(server::close);
        Assertions.assertEquals(1, openCount.intValue());
        Assertions.assertEquals(1, closeCount.intValue());
    }

    @Test
    @DisplayName("test multiValueMap sendEvents endpoint with channel present")
    void testMultiValueMapSendEventsEndpointWithChannelPresent() {
        final int serverPort = 1248;
        final TestServerFactory serverFactory = new TestServerFactory();
        final ConcurrentLinkedDeque<byte[]> messageList = new ConcurrentLinkedDeque<>();
        final AtomicLong openCount = new AtomicLong();
        final AtomicLong closeCount = new AtomicLong();

        final TestServer server = Assertions
                .assertDoesNotThrow(() -> serverFactory.create(serverPort, messageList, openCount, closeCount));

        server.run();

        final Configuration configuration = new Configuration();
        final RelpConnection relpConnection = new RelpConnection("localhost", serverPort);
        Assertions
                .assertTimeout(Duration.of(5, ChronoUnit.SECONDS), relpConnection::connect, "RelpConnection did not connect in 5 seconds");
        final HECService service = new HECServiceImpl(
                new Acknowledgements(configuration),
                new SessionManager(configuration),
                new TokenManager(),
                relpConnection
        );

        final HECRestController hecRestController = new HECRestController(service, configuration);

        final MockHttpServletRequest request1 = new MockHttpServletRequest();
        request1.addHeader("Authorization", "AUTH_TOKEN_11111");
        final String channel1 = "CHANNEL_11111";
        // Send JSON without the outer object brackets
        final String eventInJson = "\"sourcetype\":\"access\", \"source\":\"/var/log/access.log\", "
                + "\"event\": {\"message\":\"Access log test message 1\"}} "
                + "{\"sourcetype\":\"access\", \"source\":\"/var/log/access.log\", \"event\": "
                + "{\"message\":\"Access log test message 2\"}";
        final MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>();
        multiValueMap.add("channel", channel1);
        multiValueMap.add(eventInJson, null);

        final ResponseEntity<JsonNode> responseEntity = Assertions
                .assertDoesNotThrow(() -> hecRestController.sendEvents(request1, multiValueMap, channel1));
        final AcknowledgedJsonResponse expectedResponse = new AcknowledgedJsonResponse("Success", 0);
        final ResponseEntity<JsonNode> expectedResponseEntity = expectedResponse.asJsonNodeResponseEntity();

        Assertions.assertEquals(expectedResponseEntity, responseEntity);

        Assertions.assertDoesNotThrow(relpConnection::close);
        Assertions.assertDoesNotThrow(server::close);
        Assertions.assertEquals(1, openCount.intValue());
        Assertions.assertEquals(1, closeCount.intValue());
    }

    @Test
    @DisplayName("test multiValueMap sendEvents endpoint without channel present")
    void testMultiValueMapSendEventsEndpointWithoutChannelPresent() {
        final int serverPort = 1248;
        final TestServerFactory serverFactory = new TestServerFactory();
        final ConcurrentLinkedDeque<byte[]> messageList = new ConcurrentLinkedDeque<>();
        final AtomicLong openCount = new AtomicLong();
        final AtomicLong closeCount = new AtomicLong();

        final TestServer server = Assertions
                .assertDoesNotThrow(() -> serverFactory.create(serverPort, messageList, openCount, closeCount));

        server.run();

        final Configuration configuration = new Configuration();
        final RelpConnection relpConnection = new RelpConnection("localhost", serverPort);
        Assertions
                .assertTimeout(Duration.of(5, ChronoUnit.SECONDS), relpConnection::connect, "RelpConnection did not connect in 5 seconds");
        final HECService service = new HECServiceImpl(
                new Acknowledgements(configuration),
                new SessionManager(configuration),
                new TokenManager(),
                relpConnection
        );

        final HECRestController hecRestController = new HECRestController(service, configuration);

        final MockHttpServletRequest request1 = new MockHttpServletRequest();
        request1.addHeader("Authorization", "AUTH_TOKEN_11111");
        // Send JSON without the outer object brackets
        final String eventInJson = "\"sourcetype\":\"access\", \"source\":\"/var/log/access.log\", "
                + "\"event\": {\"message\":\"Access log test message 1\"}} "
                + "{\"sourcetype\":\"access\", \"source\":\"/var/log/access.log\", \"event\": "
                + "{\"message\":\"Access log test message 2\"}";
        final MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>();
        multiValueMap.add(eventInJson, null);

        final ResponseEntity<JsonNode> responseEntity = Assertions
                .assertDoesNotThrow(() -> hecRestController.sendEvents(request1, multiValueMap, null));
        final JsonResponse expectedResponse = new JsonResponse("Success");
        final ResponseEntity<JsonNode> expectedResponseEntity = expectedResponse.asJsonNodeResponseEntity();

        Assertions.assertEquals(expectedResponseEntity, responseEntity);

        Assertions.assertDoesNotThrow(relpConnection::close);
        Assertions.assertDoesNotThrow(server::close);
        Assertions.assertEquals(1, openCount.intValue());
        Assertions.assertEquals(1, closeCount.intValue());
    }

    @Test
    @DisplayName("Test healthCheck endpoint with empty request")
    void testHealthCheckEndpointWithEmptyRequest() {
        final MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        final ResponseEntity<String> responseEntity = Assertions
                .assertDoesNotThrow(() -> this.hecRestController.getHealth(mockHttpServletRequest));

        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertEquals("HEC is available and accepting input", responseEntity.getBody());
    }

    @Test
    @DisplayName("Test healthCheck endpoint with a token in the request")
    void testHealthCheckEndpointWithATokenInTheRequest() {
        final MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.addHeader("Authorization", "AUTH_TOKEN_11111");
        final ResponseEntity<String> responseEntity = Assertions
                .assertDoesNotThrow(() -> this.hecRestController.getHealth(mockHttpServletRequest));

        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertEquals("HEC is available and accepting input", responseEntity.getBody());
    }

    @Test
    @DisplayName("Test event consumer with mediaType ALL")
    void testEventConsumerWithMediaTypeAll() {
        final MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
                .post("/services/collector/event")
                .contentType(MediaType.ALL)
                .header("Authorization", "AUTH_TOKEN_11111")
                .content(
                        "\"sourcetype\":\"access\", \"source\":\"/var/log/access.log\", "
                                + "\"event\": {\"message\":\"Access log test message 1\"}} "
                                + "{\"sourcetype\":\"access\", \"source\":\"/var/log/access.log\", \"event\": "
                                + "{\"message\":\"Access log test message 2\"}"
                );
        // Send a request with MediaType.ALL using the mockMVC object
        final ResultActions resultActions = Assertions
                .assertDoesNotThrow(() -> mockMvc.perform(mockHttpServletRequestBuilder));
        final MvcResult mvcResult = resultActions.andReturn();
        final MockHttpServletResponse response = mvcResult.getResponse();

        Assertions.assertEquals(200, response.getStatus());
        final String responseContentAsString = Assertions.assertDoesNotThrow(() -> response.getContentAsString());
        Assertions.assertEquals("{\"message\":\"Success\"}", responseContentAsString);
    }
}
