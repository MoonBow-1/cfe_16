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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.teragrep.cfe_16.Acknowledgements;
import com.teragrep.cfe_16.bo.Ack;
import com.teragrep.cfe_16.bo.Session;
import com.teragrep.cfe_16.config.Configuration;
import com.teragrep.cfe_16.exceptionhandling.ServerIsBusyException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import tools.jackson.databind.exc.InvalidFormatException;

/*
 * Tests the functionality of Acknowledgements
 */
final class AcknowledgementsIT {

    @Test
    void getCurrentAckValueTest() {
        final Configuration configuration = new Configuration(
                "localhost",
                1234,
                1000000,
                20000,
                30000,
                1000000,
                1000000,
                true
        );
        final Acknowledgements acknowledgements = new Acknowledgements(configuration);

        final String authToken1 = "AUTH_TOKEN_11111";
        final String authToken2 = "AUTH_TOKEN_22222";
        final String channel1 = "CHANNEL_11111";
        final String channel2 = "CHANNEL_22222";

        int currentAckValue;
        currentAckValue = acknowledgements.getCurrentAckValue(authToken1, channel1);
        acknowledgements.incrementAckValue(authToken1, channel1);

        currentAckValue = acknowledgements.getCurrentAckValue(authToken1, channel1);
        acknowledgements.incrementAckValue(authToken1, channel1);

        currentAckValue = acknowledgements.getCurrentAckValue(authToken1, channel1);
        acknowledgements.incrementAckValue(authToken1, channel1);

        currentAckValue = acknowledgements.getCurrentAckValue(authToken1, channel1);
        acknowledgements.incrementAckValue(authToken1, channel1);

        Assertions.assertEquals(3, currentAckValue, "channel1 current ack value should be 3");

        currentAckValue = acknowledgements.getCurrentAckValue(authToken1, channel1);
        acknowledgements.incrementAckValue(authToken1, channel1);
        Assertions.assertEquals(4, currentAckValue, "channel1 current ack value should be 4");

        currentAckValue = acknowledgements.getCurrentAckValue(authToken2, channel2);
        Assertions.assertEquals(0, currentAckValue, "channel2 current ack value should be 0");
    }

    /**
     * First we acknowledge the Ack with an id of 0 in channel 1, then we test that it is indeed acknowledged. All the
     * other Acks should not be acknowledged. If Ack id is not used at all, isAckAcknowledged should return false.
     */
    @Test
    void acknowledgeTest() {
        final Configuration configuration = new Configuration(
                "localhost",
                1234,
                1000000,
                20000,
                30000,
                1000000,
                1000000,
                true
        );
        final Acknowledgements acknowledgements = new Acknowledgements(configuration);

        final String authToken1 = "AUTH_TOKEN_11111";
        final String authToken2 = "AUTH_TOKEN_22222";
        final String channel1 = "CHANNEL_11111";
        final String channel2 = "CHANNEL_22222";

        acknowledgements.initializeContext(authToken1, channel1);
        acknowledgements.addAck(authToken1, channel1, new Ack(0, false));
        acknowledgements.acknowledge(authToken1, channel1, 0);
        Assertions
                .assertTrue(

                        acknowledgements.isAckAcknowledged(authToken1, channel1, 0), "ackId 0 should be acknowledged for channel 1"
                );
        Assertions
                .assertFalse(

                        acknowledgements.isAckAcknowledged(authToken1, channel1, 1), "ackId 1 should not be acknowledged for channel 1"
                );
        Assertions
                .assertFalse(

                        acknowledgements.isAckAcknowledged(authToken1, channel1, 10), "ackId 10 is not used yet for channel 1, so isAckAcknowledged should return false"
                );
        acknowledgements.incrementAckValue(authToken1, channel1);
        acknowledgements.initializeContext(authToken2, channel2);
        Assertions
                .assertFalse(

                        acknowledgements.isAckAcknowledged(authToken2, channel2, 0), "ackId 0 is not used yet for channel 2 so isAckAcknowledged should return false"
                );
    }

    /**
     * Tests getting the Ack statuses from Acknowledgements. First we create the request bodies and the supposed
     * responses as strings. Then we create the nodes for the requests and read the strings into the node object. After
     * that Acknowledgements' getRequestedAckStatuses() is called with the JsonNode requests and the response is
     * compared to the supposed responses.
     */
    @Test
    void getRequestedAckStatusesTest() {
        final Configuration configuration = new Configuration(
                "localhost",
                1234,
                1000000,
                20000,
                30000,
                1000000,
                1000000,
                true
        );
        final Acknowledgements acknowledgements = new Acknowledgements(configuration);

        final String authToken1 = "AUTH_TOKEN_11111";
        final String authToken2 = "AUTH_TOKEN_22222";
        final String channel1 = "CHANNEL_11111";
        final String channel2 = "CHANNEL_22222";

        final String requestAsString = "{\"acks\": [1,3,4]}";
        final String notIntRequestAsString = "{\"acks\": [\"a\",\"b\",\"c\"]}";
        final String faultyRequestAsString = "{\"test\": [1,3,4]}";
        final String supposedResponseAsStringOneTrue = "{\"1\":true,\"3\":false,\"4\":false}";
        final String supposedResponseAsStringAllFalse = "{\"1\":false,\"3\":false,\"4\":false}";
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode emptyJsonNode = mapper.createObjectNode();
        final JsonNode queryNode;
        final JsonNode faultyNode;
        final JsonNode notIntNode;
        acknowledgements.initializeContext(authToken1, channel1);
        Assertions.assertTrue(acknowledgements.addAck(authToken1, channel1, new Ack(1, false)));
        Assertions.assertTrue(acknowledgements.acknowledge(authToken1, channel1, 1));

        queryNode = Assertions.assertDoesNotThrow(() -> mapper.readTree(requestAsString));
        faultyNode = Assertions.assertDoesNotThrow(() -> mapper.readTree(faultyRequestAsString));
        notIntNode = Assertions.assertDoesNotThrow(() -> mapper.readTree(notIntRequestAsString));

        Assertions
                .assertEquals(emptyJsonNode, acknowledgements.getRequestedAckStatuses(authToken1, "", null), "getRequestedAckStatuses should return null, when providing a null value as a parameter");

        Assertions
                .assertEquals(
                        supposedResponseAsStringOneTrue, acknowledgements
                                .getRequestedAckStatuses(authToken1, channel1, queryNode)
                                .toString(),
                        "ackId 1 status should be true on channel1, others should be false."
                );

        Assertions
                .assertEquals(

                        supposedResponseAsStringAllFalse, acknowledgements
                                .getRequestedAckStatuses(authToken1, channel1, queryNode)
                                .toString(),
                        "ackId 1 status should be false on channel1 after requesting it's status once. All others should be false as well"
                );

        acknowledgements.initializeContext(authToken2, channel2);
        Assertions
                .assertEquals(
                        supposedResponseAsStringAllFalse, acknowledgements
                                .getRequestedAckStatuses(authToken2, channel2, queryNode)
                                .toString(),
                        "All ack statuses should be false for channel2"
                );

        Assertions
                .assertEquals(
                        emptyJsonNode, acknowledgements.getRequestedAckStatuses(authToken1, channel1, emptyJsonNode), "An empty JsonNode should be returned when querying with an empty JsonNode"
                );

        Assertions
                .assertEquals(

                        emptyJsonNode, acknowledgements.getRequestedAckStatuses(authToken1, channel1, faultyNode), "An empty JsonNode should be returned when querying with a JsonNode that has no \"acks\" field in it."
                );

        Assertions
                .assertThrowsExactly(
                        InvalidFormatException.class,
                        () -> acknowledgements.getRequestedAckStatuses(authToken1, channel1, notIntNode)
                );
    }

    @Test
    void getCurrentAckValueAndIncrementTest() {
        final Configuration configuration = new Configuration(
                "localhost",
                1234,
                1000000,
                20000,
                30000,
                1000000,
                1000000,
                true
        );
        final String authToken1 = "AUTH_TOKEN_11111";

        final Acknowledgements acknowledgements1 = new Acknowledgements(configuration);
        final Acknowledgements acknowledgements2 = new Acknowledgements(configuration);

        Assertions
                .assertEquals(0, acknowledgements1.getCurrentAckValue(authToken1, Session.DEFAULT_CHANNEL), "Acknowledgements 1 should return 0");
        acknowledgements1.incrementAckValue(authToken1, Session.DEFAULT_CHANNEL);
        Assertions
                .assertEquals(1, acknowledgements1.getCurrentAckValue(authToken1, Session.DEFAULT_CHANNEL), "Acknowledgements 1 should return 1");

        Assertions
                .assertEquals(0, acknowledgements2.getCurrentAckValue(authToken1, Session.DEFAULT_CHANNEL), "Acknowledgements 2 should return 0");
        acknowledgements2.incrementAckValue(authToken1, Session.DEFAULT_CHANNEL);
        Assertions
                .assertEquals(1, acknowledgements2.getCurrentAckValue(authToken1, Session.DEFAULT_CHANNEL), "Acknowledgements 2 should return 1");
    }

    /**
     * Tests deleting the Ack from Acknowledgements. First we get the list that is currently in the Acknowledgements and
     * save it to list1 variable. After that we delete an Ack from the Acknowledgements's list, then we get the list
     * from Acknowledgements again and save it to list2 variable. These 2 lists are then compared to each other.
     */
    @Test
    void deleteAckTest() {
        final String authToken1 = "AUTH_TOKEN_11111";
        final String authToken2 = "AUTH_TOKEN_22222";
        final String channel1 = "CHANNEL_11111";
        final String channel2 = "CHANNEL_22222";

        final Acknowledgements acknowledgements1 = new Acknowledgements(new Configuration());
        final Acknowledgements acknowledgements2 = new Acknowledgements(new Configuration());

        acknowledgements1.initializeContext(authToken1, channel1);
        Assertions.assertTrue(acknowledgements1.addAck(authToken1, channel1, new Ack(0, false)));
        Assertions.assertTrue(acknowledgements1.addAck(authToken1, channel1, new Ack(1, false)));

        Map<Integer, Ack> list1 = acknowledgements1.getAckList(authToken1, channel1);
        int list1Size = acknowledgements1.getAckListSize(authToken1, channel1);
        Assertions.assertEquals(2, list1Size, "Ack list 1 size should be 2.");

        Ack deletedAck = list1.values().iterator().next();

        acknowledgements2.initializeContext(authToken2, channel2);
        Assertions.assertTrue(acknowledgements2.addAck(authToken2, channel2, new Ack(0, false)));

        acknowledgements2.initializeContext(authToken2, channel2);
        Assertions.assertTrue(acknowledgements2.addAck(authToken2, channel2, new Ack(1, false)));

        acknowledgements2.deleteAckFromList(authToken2, channel2, deletedAck);
        Map<Integer, Ack> list2 = acknowledgements2.getAckList(authToken2, channel2);
        int list2Size = list2.size();

        Assertions.assertNotSame(list1.toString(), list2.toString(), "Ack lists should not be same");
        Assertions.assertEquals(list1Size - 1, list2Size, "list2 should be shorter by one index");
        Assertions.assertFalse(list2.containsKey(deletedAck.getId()), "list2 should not contain the deleted ack");
    }

    /**
     * Max Ack value is set to 2, so the Ack list should be full after getCurrentAckValue() is called 3 times.
     * getCurrentAckValue is called 4 times here, so ServerIsBusyException is expected to happen.
     */
    @Test
    void maxAckValueTest() {
        final String authToken1 = "AUTH_TOKEN_11111";
        final String channel1 = "CHANNEL_11111";

        final Configuration configuration = new Configuration(
                "localhost",
                1234,
                2, // maxAckValue
                20000,
                30000,
                1000000,
                1000000,
                true
        );
        final Acknowledgements acknowledgements1 = new Acknowledgements(configuration);

        final int ackId1 = acknowledgements1.getCurrentAckValue(authToken1, channel1);
        Assertions.assertEquals(0, ackId1);
        acknowledgements1.incrementAckValue(authToken1, channel1);
        acknowledgements1.addAck(authToken1, channel1, new Ack(ackId1, false));

        final int ackId2 = acknowledgements1.getCurrentAckValue(authToken1, channel1);
        Assertions.assertEquals(1, ackId2);
        acknowledgements1.incrementAckValue(authToken1, channel1);
        acknowledgements1.addAck(authToken1, channel1, new Ack(ackId2, false));

        final int ackId3 = acknowledgements1.getCurrentAckValue(authToken1, channel1);
        Assertions.assertEquals(2, ackId3);
        acknowledgements1.incrementAckValue(authToken1, channel1);
        acknowledgements1.addAck(authToken1, channel1, new Ack(ackId3, false));

        final int ackId4 = acknowledgements1.getCurrentAckValue(authToken1, channel1);
        Assertions.assertEquals(0, ackId4);

        Assertions
                .assertThrows(
                        ServerIsBusyException.class, () -> acknowledgements1.incrementAckValue(authToken1, channel1)
                );
    }
}
