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
package com.teragrep.cfe_16;

import com.teragrep.rlo_14.Facility;
import com.teragrep.rlo_14.SDElement;
import com.teragrep.rlo_14.SDParam;
import com.teragrep.rlo_14.Severity;
import com.teragrep.rlo_14.SyslogMessage;
import tools.jackson.databind.ObjectMapper;
import com.teragrep.cfe_16.bo.HeaderInfo;
import com.teragrep.cfe_16.bo.HECRecordImpl;
import com.teragrep.cfe_16.event.EventMessageImpl;
import com.teragrep.cfe_16.event.JsonEventImpl;
import com.teragrep.cfe_16.event.time.HECTimeImpl;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Tests the functionality of the eventData.toSyslogMessage method
 */
public class EventDataToSyslogMessageTest {

    @Test
    @DisplayName("test fields when time is provided in HECRecord")
    void testFieldsWhenTimeIsProvidedInHECRecord() {
        // Timestamp used as fallback value for HECRecord time
        final Facility expectedFacility = Facility.USER;
        final Severity expectedSeverity = Severity.INFORMATIONAL;
        final SDElement expectedMetadataSDE1 = new SDElement("cfe_16-metadata@48577");

        final HECRecordImpl hecRecord1 = new HECRecordImpl(
                "CHANNEL_11111",
                new EventMessageImpl("Event 1"),
                "AUTH_TOKEN_11111",
                0,
                new HECTimeImpl(new JsonEventImpl(new ObjectMapper().createObjectNode().put("time", "1433188255253"))),
                new HeaderInfo(new MockHttpServletRequest())
        );

        expectedMetadataSDE1.addSDParam("authentication_token", hecRecord1.authenticationToken());
        expectedMetadataSDE1.addSDParam("channel", hecRecord1.channel());
        expectedMetadataSDE1.addSDParam("ack_id", String.valueOf(hecRecord1.ackID()));
        expectedMetadataSDE1.addSDParam("time_source", hecRecord1.time().source());
        expectedMetadataSDE1.addSDParam("time_parsed", "true");
        expectedMetadataSDE1.addSDParam("time", "1433188255253");
        expectedMetadataSDE1.addSDParam("generated", "false");

        final SyslogMessage expectedSyslogMessage1 = new SyslogMessage()
                .withTimestamp(1433188255253L)
                .withSeverity(expectedSeverity)
                .withAppName("capsulated")
                .withHostname("cfe-16")
                .withFacility(expectedFacility)
                .withSDElement(expectedMetadataSDE1)
                .withMsg(hecRecord1.event().asString());

        final SyslogMessage returnedMessage1 = hecRecord1.toSyslogMessage();

        Assertions
                .assertEquals(
                        expectedSyslogMessage1.getSeverity(), returnedMessage1.getSeverity(),
                        "Severity should be INFORMATIONAL"
                );
        Assertions
                .assertEquals(
                        expectedSyslogMessage1.getFacility(), returnedMessage1.getFacility(), "Facility should be USER"
                );
        Assertions
                .assertEquals(
                        expectedSyslogMessage1.getAppName(), returnedMessage1.getAppName(),
                        "App name should be '" + expectedSyslogMessage1.getAppName() + "'"
                );

        Assertions
                .assertEquals(
                        expectedSyslogMessage1.getHostname(), returnedMessage1.getHostname(),
                        "Host name should be '" + expectedSyslogMessage1.getHostname() + "'"
                );
        Assertions
                .assertEquals(
                        expectedSyslogMessage1.getMsg().toString(), returnedMessage1.getMsg().toString(),
                        "Msg should be '" + expectedSyslogMessage1.getMsg().toString() + "'"
                );
        Assertions
                .assertEquals(
                        expectedSyslogMessage1.getTimestamp(), returnedMessage1.getTimestamp(),
                        "Timestamp should be: " + expectedSyslogMessage1.getTimestamp()
                );
    }

    @Test
    @DisplayName("test SDElements when time is provided in HECRecord")
    void testSdElementsWhenTimeIsProvidedInHECRecord() {
        final HECRecordImpl hecRecord1 = new HECRecordImpl(
                "CHANNEL_11111",
                new EventMessageImpl("Event 1"),
                "AUTH_TOKEN_11111",
                0,
                new HECTimeImpl(new JsonEventImpl(new ObjectMapper().createObjectNode().put("time", "1433188255253"))),
                new HeaderInfo(new MockHttpServletRequest())
        );

        final SyslogMessage returnedSyslogMessage = hecRecord1.toSyslogMessage();
        final Set<SDElement> sdElementSet = returnedSyslogMessage.getSDElements();
        final Map<String, Map<String, String>> sdElementMap = sdElementSet
                .stream()
                .collect(Collectors.toMap((SDElement::getSdID), (sdElem) -> sdElem.getSdParams().stream().collect(Collectors.toMap(SDParam::getParamName, SDParam::getParamValue))));

        Assertions.assertTrue(sdElementMap.containsKey("cfe_16-metadata@48577"));
        Assertions.assertTrue(sdElementMap.containsKey("cfe_16-origin@48577"));

        Assertions
                .assertEquals("AUTH_TOKEN_11111", sdElementMap.get("cfe_16-metadata@48577").get("authentication_token"));
        Assertions.assertEquals("CHANNEL_11111", sdElementMap.get("cfe_16-metadata@48577").get("channel"));
        Assertions.assertEquals("0", sdElementMap.get("cfe_16-metadata@48577").get("ack_id"));
        Assertions.assertEquals("reported", sdElementMap.get("cfe_16-metadata@48577").get("time_source"));
        Assertions.assertEquals("true", sdElementMap.get("cfe_16-metadata@48577").get("time_parsed"));
        Assertions.assertEquals(String.valueOf(1433188255253L), sdElementMap.get("cfe_16-metadata@48577").get("time"));
        Assertions.assertEquals("false", sdElementMap.get("cfe_16-metadata@48577").get("generated"));
    }

    @Test
    @DisplayName("test fields when time is not provided in HECRecord")
    void testFieldsWhenTimeIsNotProvidedInHECRecord() {
        // Timestamp used as fallback value for HECRecord time
        final long currentEpoch = Instant.now().toEpochMilli();

        final Facility expectedFacility = Facility.USER;
        final Severity expectedSeverity = Severity.INFORMATIONAL;
        final SDElement expectedMetadataSDE2 = new SDElement("cfe_16-metadata@48577");

        final HECRecordImpl hecRecord2 = new HECRecordImpl(
                "CHANNEL_22222",
                new EventMessageImpl("Event 2"),
                "AUTH_TOKEN_22222",
                1,
                new HECTimeImpl(new JsonEventImpl(new ObjectMapper().createObjectNode().put("time", "null"))),
                new HeaderInfo(new MockHttpServletRequest())
        );
        expectedMetadataSDE2.addSDParam("authentication_token", hecRecord2.authenticationToken());
        expectedMetadataSDE2.addSDParam("channel", hecRecord2.channel());
        expectedMetadataSDE2.addSDParam("ack_id", String.valueOf(hecRecord2.ackID()));
        expectedMetadataSDE2.addSDParam("time_source", hecRecord2.time().source());
        expectedMetadataSDE2.addSDParam("time_parsed", "false");
        expectedMetadataSDE2.addSDParam("time", String.valueOf(currentEpoch));
        expectedMetadataSDE2.addSDParam("generated", "true");

        final SyslogMessage expectedSyslogMessage2 = new SyslogMessage()
                .withSeverity(expectedSeverity)
                .withAppName("capsulated")
                .withHostname("cfe-16")
                .withFacility(expectedFacility)
                .withSDElement(expectedMetadataSDE2)
                .withMsg(hecRecord2.event().asString());
        final SyslogMessage returnedMessage2 = hecRecord2.toSyslogMessage();

        Assertions
                .assertEquals(
                        expectedSyslogMessage2.getSeverity(), returnedMessage2.getSeverity(),
                        "Severity should be INFORMATIONAL"
                );
        Assertions
                .assertEquals(
                        expectedSyslogMessage2.getFacility(), returnedMessage2.getFacility(), "Facility should be USER"
                );

        Assertions
                .assertEquals(SyslogMessage.NILVALUE, expectedSyslogMessage2.getTimestamp(), "Timestamp should be null");
        Assertions
                .assertEquals(
                        expectedSyslogMessage2.getMsg().toString(), returnedMessage2.getMsg().toString(),
                        "Msg should be '" + expectedSyslogMessage2.getMsg().toString() + "'"
                );
        Assertions
                .assertEquals(
                        expectedSyslogMessage2.getAppName(), returnedMessage2.getAppName(),
                        "App name should be '" + expectedSyslogMessage2.getAppName() + "'"
                );

        Assertions
                .assertEquals(
                        expectedSyslogMessage2.getHostname(), returnedMessage2.getHostname(),
                        "Host name should be '" + expectedSyslogMessage2.getHostname() + "'"
                );
    }

    @Test
    @DisplayName("test SDElements when time is not provided in HECRecord")
    void testSdElementsWhenTimeIsNotProvidedInHECRecord() {
        // Timestamp used as fallback value for HECRecord time
        final long currentEpoch = Instant.now().toEpochMilli();

        final HECRecordImpl hecRecord = new HECRecordImpl(
                "CHANNEL_22222",
                new EventMessageImpl("Event 2"),
                "AUTH_TOKEN_22222",
                1,
                new HECTimeImpl(new JsonEventImpl(new ObjectMapper().createObjectNode().put("time", "null"))),
                new HeaderInfo(new MockHttpServletRequest())
        );

        final SyslogMessage returnedSyslogMessage = hecRecord.toSyslogMessage(currentEpoch);

        final Set<SDElement> sdElementSet = returnedSyslogMessage.getSDElements();
        final Map<String, Map<String, String>> sdElementMap = sdElementSet
                .stream()
                .collect(Collectors.toMap((SDElement::getSdID), (sdElem) -> sdElem.getSdParams().stream().collect(Collectors.toMap(SDParam::getParamName, SDParam::getParamValue))));

        Assertions.assertTrue(sdElementMap.containsKey("cfe_16-metadata@48577"));
        Assertions.assertTrue(sdElementMap.containsKey("cfe_16-origin@48577"));

        Assertions
                .assertEquals("AUTH_TOKEN_22222", sdElementMap.get("cfe_16-metadata@48577").get("authentication_token"));
        Assertions.assertEquals("CHANNEL_22222", sdElementMap.get("cfe_16-metadata@48577").get("channel"));
        Assertions.assertEquals("1", sdElementMap.get("cfe_16-metadata@48577").get("ack_id"));
        Assertions.assertEquals("generated", sdElementMap.get("cfe_16-metadata@48577").get("time_source"));
        Assertions.assertEquals("false", sdElementMap.get("cfe_16-metadata@48577").get("time_parsed"));
        Assertions.assertEquals(String.valueOf(currentEpoch), sdElementMap.get("cfe_16-metadata@48577").get("time"));
        Assertions.assertEquals("true", sdElementMap.get("cfe_16-metadata@48577").get("generated"));
    }

    @Test
    @DisplayName("test fields when time is not provided and ackID is null in HECRecord")
    void testFieldsWhenTimeIsNotProvidedAndAckIdIsNullInHECRecord() {
        // Timestamp used as fallback value for HECRecord time
        final long currentEpoch = Instant.now().toEpochMilli();

        final SDElement expectedMetadataSDE3 = new SDElement("cfe_16-metadata@48577");
        final Severity expectedSeverity = Severity.INFORMATIONAL;
        final Facility expectedFacility = Facility.USER;
        final HECRecordImpl hecRecord3 = new HECRecordImpl(
                "defaultchannel",
                new EventMessageImpl("Event 3"),
                "AUTH_TOKEN_33333",
                null,
                new HECTimeImpl(new JsonEventImpl(new ObjectMapper().createObjectNode().put("time", "null"))),
                new HeaderInfo(new MockHttpServletRequest())
        );
        expectedMetadataSDE3.addSDParam("authentication_token", hecRecord3.authenticationToken());
        expectedMetadataSDE3.addSDParam("channel", hecRecord3.channel());
        expectedMetadataSDE3.addSDParam("time_source", hecRecord3.time().source());
        expectedMetadataSDE3.addSDParam("time_parsed", "false");
        expectedMetadataSDE3.addSDParam("time", String.valueOf(currentEpoch));
        expectedMetadataSDE3.addSDParam("generated", "true");

        final SyslogMessage expectedSyslogMessage3 = new SyslogMessage()
                .withSeverity(expectedSeverity)
                .withAppName("capsulated")
                .withHostname("cfe-16")
                .withFacility(expectedFacility)
                .withSDElement(expectedMetadataSDE3)
                .withMsg(hecRecord3.event().asString());

        final SyslogMessage returnedMessage3 = hecRecord3.toSyslogMessage(currentEpoch);

        Assertions
                .assertEquals(
                        expectedSyslogMessage3.getSeverity(), returnedMessage3.getSeverity(),
                        "Severity should be INFORMATIONAL"
                );
        Assertions
                .assertEquals(SyslogMessage.NILVALUE, expectedSyslogMessage3.getTimestamp(), "Timestamp should be null");
        Assertions
                .assertEquals(
                        expectedSyslogMessage3.getMsg().toString(), returnedMessage3.getMsg().toString(),
                        "Msg should be '" + expectedSyslogMessage3.getMsg().toString() + "'"
                );
        Assertions
                .assertEquals(
                        expectedSyslogMessage3.getFacility(), returnedMessage3.getFacility(), "Facility should be USER"
                );
        Assertions
                .assertEquals(
                        expectedSyslogMessage3.getAppName(), returnedMessage3.getAppName(),
                        "App name should be '" + expectedSyslogMessage3.getAppName() + "'"
                );
        Assertions
                .assertEquals(
                        expectedSyslogMessage3.getHostname(), returnedMessage3.getHostname(),
                        "Host name should be '" + expectedSyslogMessage3.getHostname() + "'"
                );
    }

    @Test
    @DisplayName("test SDElements when time is not provided and ackID is null in HECRecord")
    void testSdElementsWhenTimeIsNotProvidedAndAckIdIsNullInHECRecord() {
        // Timestamp used as fallback value for HECRecord time
        final long currentEpoch = Instant.now().toEpochMilli();

        final HECRecordImpl hecRecord = new HECRecordImpl(
                "defaultchannel",
                new EventMessageImpl("Event 3"),
                "AUTH_TOKEN_33333",
                null,
                new HECTimeImpl(new JsonEventImpl(new ObjectMapper().createObjectNode().put("time", "null"))),
                new HeaderInfo(new MockHttpServletRequest())
        );

        final SyslogMessage returnedSyslogMessage = hecRecord.toSyslogMessage(currentEpoch);

        final Set<SDElement> sdElementSet = returnedSyslogMessage.getSDElements();
        final Map<String, Map<String, String>> sdElementMap = sdElementSet
                .stream()
                .collect(Collectors.toMap((SDElement::getSdID), (sdElem) -> sdElem.getSdParams().stream().collect(Collectors.toMap(SDParam::getParamName, SDParam::getParamValue))));

        Assertions.assertTrue(sdElementMap.containsKey("cfe_16-metadata@48577"));
        Assertions.assertTrue(sdElementMap.containsKey("cfe_16-origin@48577"));

        Assertions
                .assertEquals("AUTH_TOKEN_33333", sdElementMap.get("cfe_16-metadata@48577").get("authentication_token"));
        Assertions.assertEquals("defaultchannel", sdElementMap.get("cfe_16-metadata@48577").get("channel"));
        Assertions
                .assertFalse(sdElementMap.get("cfe_16-metadata@48577").containsKey("ack_id"), "ack_id should not be in SDElements since it is null");
        Assertions.assertEquals("generated", sdElementMap.get("cfe_16-metadata@48577").get("time_source"));
        Assertions.assertEquals("false", sdElementMap.get("cfe_16-metadata@48577").get("time_parsed"));
        Assertions.assertEquals(String.valueOf(currentEpoch), sdElementMap.get("cfe_16-metadata@48577").get("time"));
        Assertions.assertEquals("true", sdElementMap.get("cfe_16-metadata@48577").get("generated"));
    }
}
