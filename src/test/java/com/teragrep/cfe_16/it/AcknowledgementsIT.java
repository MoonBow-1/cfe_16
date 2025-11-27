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
import com.teragrep.cfe_16.bo.Ack;
import com.teragrep.cfe_16.bo.Session;
import com.teragrep.cfe_16.config.Configuration;
import com.teragrep.cfe_16.exceptionhandling.ServerIsBusyException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.*;

/*
 * Tests the functionality of Acknowledgements
 */
@SpringBootTest
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "syslog.server.host=127.0.0.1",
        "syslog.server.port=1234",
        "syslog.server.protocol=TCP",
        "max.channels=1000000",
        "max.ack.value=1000000",
        "max.ack.age=20000",
        "max.session.age=30000",
        "poll.time=30000",
        "server.print.times=true"
})
final class AcknowledgementsIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcknowledgementsIT.class);
    private String authToken1;
    private String authToken2;

    private String channel1;
    private String channel2;

    @Autowired
    private Configuration configuration;
    @Autowired
    private Acknowledgements acknowledgements;

    /*
     * Initializes 2 channels. getCurrentAckValue in channel 1 is called 3 times
     * which means that the next Ack id given to a sent event should be 3.
     */
    @BeforeEach
    public void initialize() {

        authToken1 = "AUTH_TOKEN_11111";
        authToken2 = "AUTH_TOKEN_22222";

        channel1 = "CHANNEL_11111";
        channel2 = "CHANNEL_22222";

    }

    /*
     * In initialize() we call channel 1's getCurrentAckValue 3 times, so the next
     * time we call it, the currentAckValue() should return 3 and increase the ack
     * value by 1, so the next time it will be 4. We have not called
     * getCurrentAckValue() in channel2 yet, so it should be 0.
     */
    @Test
    void getCurrentAckValueTest() {
        int currentAckValue;
        currentAckValue = acknowledgements.getCurrentAckValue(this.authToken1, this.channel1);
        acknowledgements.incrementAckValue(this.authToken1, this.channel1);

        currentAckValue = acknowledgements.getCurrentAckValue(this.authToken1, this.channel1);
        acknowledgements.incrementAckValue(this.authToken1, this.channel1);

        currentAckValue = acknowledgements.getCurrentAckValue(this.authToken1, this.channel1);
        acknowledgements.incrementAckValue(this.authToken1, this.channel1);

        currentAckValue = acknowledgements.getCurrentAckValue(this.authToken1, this.channel1);
        acknowledgements.incrementAckValue(this.authToken1, this.channel1);

        assertEquals("channel1 current ack value should be 3", 3, currentAckValue);

        currentAckValue = acknowledgements.getCurrentAckValue(this.authToken1, this.channel1);
        acknowledgements.incrementAckValue(this.authToken1, this.channel1);
        assertEquals("channel1 current ack value should be 4", 4, currentAckValue);

        currentAckValue = acknowledgements.getCurrentAckValue(this.authToken2, this.channel2);
        assertEquals("channel2 current ack value should be 0", 0, currentAckValue);
    }

    /*
     * First we acknowledge the Ack with an id of 0 in channel 1, then we test that
     * it is indeed acknowledged. All the other Acks should not be acknowledged. If
     * Ack id is not used at all, isAckAcknowledged should return false.
     */
    @Test
    void acknowledgeTest() {
        acknowledgements.initializeContext(this.authToken1, this.channel1);
        acknowledgements.addAck(this.authToken1, this.channel1, new Ack(0, false));
        acknowledgements.acknowledge(this.authToken1, this.channel1, 0);
        assertTrue(
                "ackId 0 should be acknowledged for channel 1",
                acknowledgements.isAckAcknowledged(this.authToken1, this.channel1, 0)
        );
        assertFalse(
                "ackId 1 should not be acknowledged for channel 1",
                acknowledgements.isAckAcknowledged(this.authToken1, this.channel1, 1)
        );
        assertFalse(
                "ackId 10 is not used yet for channel 1, so isAckAcknowledged should return false",
                acknowledgements.isAckAcknowledged(this.authToken1, this.channel1, 10)
        );
        acknowledgements.incrementAckValue(this.authToken1, this.channel1);
        acknowledgements.initializeContext(this.authToken2, this.channel2);
        assertFalse(
                "ackId 0 is not used yet for channel 2 so isAckAcknowledged should return false",
                acknowledgements.isAckAcknowledged(this.authToken2, this.channel2, 0)
        );
    }

    /*
     * Tests getting the Ack statuses from Acknowledgements. First we create the request
     * bodies and the supposed responses as strings. Then we create the nodes for
     * the requests and read the strings into the node object. After that
     * Acknowledgements' getRequestedAckStatuses() is called with the JsonNode requests
     * and the response is compared to the supposed responses.
     */
    @Test
    void getRequestedAckStatusesTest() {
        String requestAsString = "{\"acks\": [1,3,4]}";
        String notIntRequestAsString = "{\"acks\": [\"a\",\"b\",\"c\"]}";
        String faultyRequestAsString = "{\"test\": [1,3,4]}";
        String supposedResponseAsStringOneTrue = "{\"1\":true,\"3\":false,\"4\":false}";
        String supposedResponseAsStringAllFalse = "{\"1\":false,\"3\":false,\"4\":false}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode emptyJsonNode = mapper.createObjectNode();
        JsonNode queryNode = mapper.createObjectNode();
        JsonNode faultyNode = mapper.createObjectNode();
        JsonNode notIntNode = mapper.createObjectNode();
        acknowledgements.initializeContext(this.authToken1, this.channel1);
        assertTrue(acknowledgements.addAck(this.authToken1, this.channel1, new Ack(1, false)));
        assertTrue(acknowledgements.acknowledge(this.authToken1, this.channel1, 1));
        try {
            queryNode = mapper.readTree(requestAsString);
            faultyNode = mapper.readTree(faultyRequestAsString);
            notIntNode = mapper.readTree(notIntRequestAsString);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        assertEquals(
                "getRequestedAckStatuses should return null, when providing a null value as a parameter", emptyJsonNode,
                acknowledgements.getRequestedAckStatuses(this.authToken1, "", null)
        );

        try {
            assertEquals(
                    "ackId 1 status should be true on channel1, others should be false.",
                    supposedResponseAsStringOneTrue,
                    acknowledgements.getRequestedAckStatuses(this.authToken1, this.channel1, queryNode).toString()
            );
        }
        catch (Throwable e1) {
            // TODO Auto-generated catch block
            LOGGER.warn("Failed to handle ack request: ", e1);
        }

        assertEquals(
                "ackId 1 status should be false on channel1 after requesting it's status once. All others should be false as well",
                supposedResponseAsStringAllFalse,
                acknowledgements.getRequestedAckStatuses(this.authToken1, this.channel1, queryNode).toString()
        );

        acknowledgements.initializeContext(this.authToken2, this.channel2);
        assertEquals(
                "All ack statuses should be false for channel2", supposedResponseAsStringAllFalse,
                acknowledgements.getRequestedAckStatuses(this.authToken2, this.channel2, queryNode).toString()
        );

        assertEquals(
                "An empty JsonNode should be returned when querying with an empty JsonNode", emptyJsonNode,
                acknowledgements.getRequestedAckStatuses(this.authToken1, this.channel1, emptyJsonNode)
        );

        assertEquals(
                "An empty JsonNode should be returned when querying with a JsonNode that has no \"acks\" field in it.",
                emptyJsonNode, acknowledgements.getRequestedAckStatuses(this.authToken1, this.channel1, faultyNode)
        );
        try {
            assertEquals(
                    "An empty JsonNode should be returned when querying with a JsonNode that has \"acks\" field in it, but it has something else than integers as it's values",
                    emptyJsonNode, acknowledgements.getRequestedAckStatuses(this.authToken1, this.channel1, notIntNode)
            );
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
        }
    }

    void getCurrentAckValueAndIncrementTest() {
        Acknowledgements acknowledgements1 = new Acknowledgements();
        Acknowledgements acknowledgements2 = new Acknowledgements();

        assertEquals(
                "Acknowledgements 1 should return 0", 0,
                acknowledgements1.getCurrentAckValue(this.authToken1, Session.DEFAULT_CHANNEL)
        );
        acknowledgements1.incrementAckValue(this.authToken1, Session.DEFAULT_CHANNEL);
        assertEquals(
                "Acknowledgements 1 should return 1", 1,
                acknowledgements1.getCurrentAckValue(this.authToken1, Session.DEFAULT_CHANNEL)
        );

        assertEquals(
                "Acknowledgements 2 should return 0", 0,
                acknowledgements2.getCurrentAckValue(this.authToken1, Session.DEFAULT_CHANNEL)
        );
        acknowledgements2.incrementAckValue(this.authToken1, Session.DEFAULT_CHANNEL);
        assertEquals(
                "Acknowledgements 2 should return 1", 1,
                acknowledgements2.getCurrentAckValue(this.authToken1, Session.DEFAULT_CHANNEL)
        );

    }

    /*
     * Tests deleting the Ack from Acknowledgements. First we get the list that is
     * currently in the Acknowledgements and save it to list1 variable. After that we
     * delete an Ack from the Acknowledgements's list, then we get the list from
     * Acknowledgements again and save it to list2 variable. These 2 lists are then
     * compared to each other.
     */
    @Test
    void deleteAckTest() {
        Acknowledgements acknowledgements1 = new Acknowledgements();
        Acknowledgements acknowledgements2 = new Acknowledgements();

        acknowledgements1.initializeContext(this.authToken1, this.channel1);
        assertTrue(acknowledgements1.addAck(this.authToken1, this.channel1, new Ack(0, false)));
        assertTrue(acknowledgements1.addAck(this.authToken1, this.channel1, new Ack(1, false)));

        Map<Integer, Ack> list1 = acknowledgements1.getAckList(this.authToken1, this.channel1);
        int list1Size = acknowledgements1.getAckListSize(this.authToken1, this.channel1);
        assertEquals("Ack list 1 size should be 2.", 2, list1Size);

        Ack deletedAck = list1.values().iterator().next();

        acknowledgements2.initializeContext(this.authToken2, this.channel2);
        assertTrue(acknowledgements2.addAck(this.authToken2, this.channel2, new Ack(0, false)));

        acknowledgements2.initializeContext(this.authToken2, this.channel2);
        assertTrue(acknowledgements2.addAck(this.authToken2, this.channel2, new Ack(1, false)));

        acknowledgements2.deleteAckFromList(this.authToken2, this.channel2, deletedAck);
        Map<Integer, Ack> list2 = acknowledgements2.getAckList(this.authToken2, this.channel2);
        int list2Size = list2.size();

        assertNotSame("Ack lists should not be same", list1.toString(), list2.toString());
        assertEquals("list2 should be shorter by one index", list1Size - 1, list2Size);
        assertFalse("list2 should not contain the deleted ack", list2.containsKey(deletedAck.getId()));
    }

    /*
     * Max Ack value is set to 2, so the Ack list should be full after
     * getCurrentAckValue() is called 3 times. getCurrentAckValue is called 4 times
     * here, so ServerIsBusyException is expected to happen.
     */
    @Test
    void maxAckValueTest() {
        Assertions.assertThrows(ServerIsBusyException.class, () -> {
            this.configuration.setMaxAckValue(2);
            int ackId;

            ackId = acknowledgements.getCurrentAckValue(this.authToken1, this.channel1);
            acknowledgements.incrementAckValue(this.authToken1, this.channel1);
            acknowledgements.addAck(this.authToken1, this.channel1, new Ack(ackId, false));

            ackId = acknowledgements.getCurrentAckValue(this.authToken1, this.channel1);
            acknowledgements.incrementAckValue(this.authToken1, this.channel1);
            acknowledgements.addAck(this.authToken1, this.channel1, new Ack(ackId, false));

            ackId = acknowledgements.getCurrentAckValue(this.authToken1, this.channel1);
            acknowledgements.incrementAckValue(this.authToken1, this.channel1);
            acknowledgements.addAck(this.authToken1, this.channel1, new Ack(ackId, false));

            ackId = acknowledgements.getCurrentAckValue(this.authToken1, this.channel1);
            acknowledgements.incrementAckValue(this.authToken1, this.channel1);
            acknowledgements.addAck(this.authToken1, this.channel1, new Ack(ackId, false));
        });
    }
}
