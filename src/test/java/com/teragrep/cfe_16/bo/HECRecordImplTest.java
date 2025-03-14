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
package com.teragrep.cfe_16.bo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teragrep.cfe_16.event.EventMessage;
import com.teragrep.cfe_16.event.EventMessageImpl;
import com.teragrep.cfe_16.event.time.HECTimeImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HECRecordImplTest {

    @Test
    @DisplayName("TimeSource() returns \"generated\" if time field is null")
    void timeSourceReturnsGeneratedIfTimeFieldIsNull() {
        final HECRecordImpl hecRecord = new HECRecordImpl(
                "channel",
                new EventMessageImpl("event"),
                "authToken",
                1,
                new HECTimeImpl(new ObjectMapper().createObjectNode().nullNode()),
                new HeaderInfo(new XForwardedForStub(), new XForwardedHostStub(), new XForwardedProtoStub())
        );

        Assertions.assertEquals("generated", hecRecord.time().source());
    }

    @Test
    @DisplayName("TimeSource() returns \"reported\" even if time is less than 10 characters long")
    void timeSourceReturnsReportedEvenIfTimeIsLessThan10CharactersLong() {
        final HECRecordImpl hecRecord = new HECRecordImpl(
                "channel",
                new EventMessageImpl("event"),
                "authToken",
                1,
                new HECTimeImpl(new ObjectMapper().createObjectNode().textNode("123456")),
                new HeaderInfo(new XForwardedForStub(), new XForwardedHostStub(), new XForwardedProtoStub())
        );

        Assertions.assertEquals("reported", hecRecord.time().source());
    }

    @Test
    @DisplayName("TimeSource() returns \"reported\" even if time is more than 13 characters long")
    void timeSourceReturnsReportedEvenIfTimeIsMoreThan13CharactersLong() {
        final HECRecordImpl hecRecord = new HECRecordImpl(
                "channel",
                new EventMessageImpl("event"),
                "authToken",
                1,
                new HECTimeImpl(new ObjectMapper().createObjectNode().textNode("12345678901234")),
                new HeaderInfo(new XForwardedForStub(), new XForwardedHostStub(), new XForwardedProtoStub())
        );

        Assertions.assertEquals("reported", hecRecord.time().source());
    }

    @Test
    @DisplayName("TimeSource returns \"reported\" if time is between 10 and 13 characters")
    void timeSourceReturnsReportedIfTimeIsBetween10And13Characters() {
        final HECRecordImpl hecRecord = new HECRecordImpl(
                "channel",
                new EventMessageImpl("event"),
                "authToken",
                1,
                new HECTimeImpl(new ObjectMapper().createObjectNode().textNode("1234567890123")),
                new HeaderInfo(new XForwardedForStub(), new XForwardedHostStub(), new XForwardedProtoStub())
        );

        Assertions.assertEquals("reported", hecRecord.time().source());
    }

    @Test
    @DisplayName("Happy equals test")
    void happyEqualsTest() {
        final HECRecordImpl hecRecord1 = new HECRecordImpl(
                "channel",
                new EventMessageImpl("event"),
                "authToken",
                1,
                new HECTimeImpl(new ObjectMapper().createObjectNode().textNode("1234567890123")),
                new HeaderInfo(new XForwardedForStub(), new XForwardedHostStub(), new XForwardedProtoStub())
        );

        final HECRecordImpl hecRecord2 = new HECRecordImpl(
                "channel",
                new EventMessageImpl("event"),
                "authToken",
                1,
                new HECTimeImpl(new ObjectMapper().createObjectNode().textNode("1234567890123")),
                new HeaderInfo(new XForwardedForStub(), new XForwardedHostStub(), new XForwardedProtoStub())
        );
        Assertions.assertEquals(hecRecord1, hecRecord2);
    }

    @Test
    @DisplayName("Unhappy equals test")
    void unhappyEqualsTest() {
        final HECRecordImpl hecRecord1 = new HECRecordImpl(
                "channel",
                new EventMessageImpl("event"),
                "authToken",
                1,
                new HECTimeImpl(new ObjectMapper().createObjectNode().textNode("1234567890123")),
                new HeaderInfo(new XForwardedForStub(), new XForwardedHostStub(), new XForwardedProtoStub())
        );

        final HECRecordImpl hecRecord2 = new HECRecordImpl(
                "channel is not the same",
                new EventMessageImpl("event"),
                "authToken",
                1,
                new HECTimeImpl(new ObjectMapper().createObjectNode().textNode("1234567890123")),
                new HeaderInfo(new XForwardedForStub(), new XForwardedHostStub(), new XForwardedProtoStub())
        );

        Assertions.assertNotEquals(hecRecord1, hecRecord2);
    }

    @Test
    @DisplayName("Event returns event")
    void eventReturnsEvent() {
        final HECRecordImpl hecRecord = new HECRecordImpl(
                "channel",
                new EventMessageImpl("event"),
                "authToken",
                1,
                new HECTimeImpl(new ObjectMapper().createObjectNode().textNode("1234567890123")),
                new HeaderInfo(new XForwardedForStub(), new XForwardedHostStub(), new XForwardedProtoStub())
        );

        final EventMessage expectedResult = new EventMessageImpl("event");

        Assertions.assertEquals(expectedResult, hecRecord.event());
    }

    @Test
    @DisplayName("Channel returns channel")
    void channelReturnsChannel() {
        final HECRecordImpl hecRecord = new HECRecordImpl(
                "channel",
                new EventMessageImpl("event"),
                "authToken",
                1,
                new HECTimeImpl(new ObjectMapper().createObjectNode().textNode("1234567890123")),
                new HeaderInfo(new XForwardedForStub(), new XForwardedHostStub(), new XForwardedProtoStub())
        );

        final String expectedResult = "channel";

        Assertions.assertEquals(expectedResult, hecRecord.channel());
    }

    @Test
    @DisplayName("AuthenticationToken returns authentication token")
    void authenticationTokenReturnsAuthenticationToken() {
        final HECRecordImpl hecRecord = new HECRecordImpl(
                "channel",
                new EventMessageImpl("event"),
                "authToken",
                1,
                new HECTimeImpl(new ObjectMapper().createObjectNode().textNode("1234567890123")),
                new HeaderInfo(new XForwardedForStub(), new XForwardedHostStub(), new XForwardedProtoStub())
        );

        final String expectedResult = "authToken";

        Assertions.assertEquals(expectedResult, hecRecord.authenticationToken());
    }

    @Test
    @DisplayName("AckID returns ackID if not null")
    void ackIdReturnsAckIdIfNotNull() {
        final HECRecordImpl hecRecord = new HECRecordImpl(
                "channel",
                new EventMessageImpl("event"),
                "authToken",
                123,
                new HECTimeImpl(new ObjectMapper().createObjectNode().textNode("1234567890123")),
                new HeaderInfo(new XForwardedForStub(), new XForwardedHostStub(), new XForwardedProtoStub())
        );

        final Integer expectedResult = 123;

        Assertions.assertEquals(expectedResult, hecRecord.ackID());
    }

    @Test
    @DisplayName("AckID returns null if ackID is null")
    void ackIdReturnsNullIfAckIdIsNull() {
        final HECRecordImpl hecRecord = new HECRecordImpl(
                "channel",
                new EventMessageImpl("event"),
                "authToken",
                null,
                new HECTimeImpl(new ObjectMapper().createObjectNode().textNode("1234567890123")),
                new HeaderInfo(new XForwardedForStub(), new XForwardedHostStub(), new XForwardedProtoStub())
        );

        Assertions.assertNull(hecRecord.ackID());
    }
}
