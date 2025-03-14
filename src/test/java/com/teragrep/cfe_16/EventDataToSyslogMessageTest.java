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

import com.cloudbees.syslog.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teragrep.cfe_16.bo.HeaderInfo;
import com.teragrep.cfe_16.bo.XForwardedForStub;
import com.teragrep.cfe_16.bo.XForwardedHostStub;
import com.teragrep.cfe_16.bo.XForwardedProtoStub;
import com.teragrep.cfe_16.bo.HECRecordImpl;
import com.teragrep.cfe_16.event.EventMessageImpl;
import com.teragrep.cfe_16.event.time.HECTimeImpl;
import java.time.Instant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Tests the functionality of the eventData.toSyslogMessage method
 */
public class EventDataToSyslogMessageTest {

    @Test
    @DisplayName("fields match for the first HECRecord")
    void fieldsMatchForTheFirstHecRecord() {
        // Timestamp used as fallback value for HECRecord time
        Facility expectedFacility = Facility.USER;
        Severity expectedSeverity = Severity.INFORMATIONAL;
        SDElement expectedMetadataSDE1 = new SDElement("cfe_16-metadata@48577");

        HECRecordImpl hecRecord1 = new HECRecordImpl(
                "CHANNEL_11111",
                new EventMessageImpl("Event 1"),
                "AUTH_TOKEN_11111",
                0,
                new HECTimeImpl(Assertions.assertDoesNotThrow(() -> new ObjectMapper().readTree("1433188255253"))),
                new HeaderInfo(new XForwardedForStub(), new XForwardedHostStub(), new XForwardedProtoStub())
        );

        expectedMetadataSDE1.addSDParam("authentication_token", hecRecord1.authenticationToken());
        expectedMetadataSDE1.addSDParam("channel", hecRecord1.channel());
        expectedMetadataSDE1.addSDParam("ack_id", String.valueOf(hecRecord1.ackID()));
        expectedMetadataSDE1.addSDParam("time_source", hecRecord1.time().source());
        expectedMetadataSDE1.addSDParam("time_parsed", "true");
        expectedMetadataSDE1.addSDParam("time", "1433188255253");
        expectedMetadataSDE1.addSDParam("generated", "false");

        SyslogMessage expectedSyslogMessage1 = new SyslogMessage()
                .withTimestamp(1433188255253L)
                .withSeverity(expectedSeverity)
                .withAppName("capsulated")
                .withHostname("cfe-16")
                .withFacility(expectedFacility)
                .withSDElement(expectedMetadataSDE1)
                .withMsg(hecRecord1.event().asString());

        SyslogMessage returnedMessage1 = hecRecord1.toSyslogMessage();

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
    @DisplayName("SDElements match for the first HECRecord")
    void sdElementsMatchForTheFirstHecRecord() {
        Facility expectedFacility = Facility.USER;
        Severity expectedSeverity = Severity.INFORMATIONAL;
        SDElement expectedMetadataSDE1 = new SDElement("cfe_16-metadata@48577");

        HECRecordImpl hecRecord1 = new HECRecordImpl(
                "CHANNEL_11111",
                new EventMessageImpl("Event 1"),
                "AUTH_TOKEN_11111",
                0,
                new HECTimeImpl(Assertions.assertDoesNotThrow(() -> new ObjectMapper().readTree("1433188255253"))),
                new HeaderInfo(new XForwardedForStub(), new XForwardedHostStub(), new XForwardedProtoStub())
        );

        expectedMetadataSDE1.addSDParam("authentication_token", hecRecord1.authenticationToken());
        expectedMetadataSDE1.addSDParam("channel", hecRecord1.channel());
        expectedMetadataSDE1.addSDParam("ack_id", String.valueOf(hecRecord1.ackID()));
        expectedMetadataSDE1.addSDParam("time_source", hecRecord1.time().source());
        expectedMetadataSDE1.addSDParam("time_parsed", "true");
        expectedMetadataSDE1.addSDParam("time", "1433188255253");
        expectedMetadataSDE1.addSDParam("generated", "false");

        SyslogMessage expectedSyslogMessage1 = new SyslogMessage()
                .withTimestamp(1433188255253L)
                .withSeverity(expectedSeverity)
                .withAppName("capsulated")
                .withHostname("cfe-16")
                .withFacility(expectedFacility)
                .withSDElement(expectedMetadataSDE1)
                .withMsg(hecRecord1.event().asString());

        SyslogMessage returnedMessage1 = hecRecord1.toSyslogMessage();
        Set<SDElement> returnedSDElements1 = returnedMessage1.getSDElements();
        Set<SDElement> expectedSDElements1 = expectedSyslogMessage1.getSDElements();

        List<SDParam> supposedSDParams = new ArrayList<>();
        List<SDParam> returnedSDParams = new ArrayList<>();

        // Gets the SDParams from the SDEs from the first SyslogMessage returned from
        // Converter and saves them in a List
        for (SDElement sdElement : returnedSDElements1) {
            returnedSDParams.addAll(sdElement.getSdParams());
        }

        // Gets the SDParams from the SDEs from the first SyslogMessage created in
        // initialize() and saves them in a List
        for (SDElement sdElement : expectedSDElements1) {
            supposedSDParams.addAll(sdElement.getSdParams());
        }

        // Goes through all the returned SDParams and checks that they are all found in
        // supposed SDParams
        final int expectedReturnedSDParamsAssertions = 7; // See lines 135 - 141
        int loopedReturnedSDParamsAssertions = 0;
        for (SDParam returnedSDParam : returnedSDParams) {
            loopedReturnedSDParamsAssertions++;
            Assertions
                    .assertTrue(supposedSDParams.contains(returnedSDParam), "SDParam '" + returnedSDParam + "' should not be in returned SDElement.");
        }
        Assertions
                .assertEquals(
                        expectedReturnedSDParamsAssertions, loopedReturnedSDParamsAssertions,
                        "All returnedSDParams were looped through"
                );
    }

    @Test
    @DisplayName("fields match for the second HECRecord")
    void fieldsMatchForTheSecondHecRecord() {
        // Timestamp used as fallback value for HECRecord time
        final long currentEpoch = Instant.now().toEpochMilli();

        Facility expectedFacility = Facility.USER;
        Severity expectedSeverity = Severity.INFORMATIONAL;
        SDElement expectedMetadataSDE2 = new SDElement("cfe_16-metadata@48577");

        HECRecordImpl hecRecord2 = new HECRecordImpl(
                "CHANNEL_22222",
                new EventMessageImpl("Event 2"),
                "AUTH_TOKEN_22222",
                1,
                new HECTimeImpl(Assertions.assertDoesNotThrow(() -> new ObjectMapper().readTree("null"))),
                new HeaderInfo(new XForwardedForStub(), new XForwardedHostStub(), new XForwardedProtoStub())
        );
        expectedMetadataSDE2.addSDParam("authentication_token", hecRecord2.authenticationToken());
        expectedMetadataSDE2.addSDParam("channel", hecRecord2.channel());
        expectedMetadataSDE2.addSDParam("ack_id", String.valueOf(hecRecord2.ackID()));
        expectedMetadataSDE2.addSDParam("time_source", hecRecord2.time().source());
        expectedMetadataSDE2.addSDParam("time_parsed", "false");
        expectedMetadataSDE2.addSDParam("time", String.valueOf(currentEpoch));
        expectedMetadataSDE2.addSDParam("generated", "true");

        SyslogMessage expectedSyslogMessage2 = new SyslogMessage()
                .withSeverity(expectedSeverity)
                .withAppName("capsulated")
                .withHostname("cfe-16")
                .withFacility(expectedFacility)
                .withSDElement(expectedMetadataSDE2)
                .withMsg(hecRecord2.event().asString());
        SyslogMessage returnedMessage2 = hecRecord2.toSyslogMessage();

        Assertions
                .assertEquals(
                        expectedSyslogMessage2.getSeverity(), returnedMessage2.getSeverity(),
                        "Severity should be INFORMATIONAL"
                );
        Assertions
                .assertEquals(
                        expectedSyslogMessage2.getFacility(), returnedMessage2.getFacility(), "Facility should be USER"
                );

        Assertions.assertNull(expectedSyslogMessage2.getTimestamp(), "Timestamp should be null");
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
    @DisplayName("SDElements match for the second HECRecord")
    void sdElementsMatchForTheSecondHecRecord() {
        // Timestamp used as fallback value for HECRecord time
        final long currentEpoch = Instant.now().toEpochMilli();

        Facility expectedFacility = Facility.USER;
        Severity expectedSeverity = Severity.INFORMATIONAL;
        SDElement expectedMetadataSDE2 = new SDElement("cfe_16-metadata@48577");

        HECRecordImpl hecRecord2 = new HECRecordImpl(
                "CHANNEL_22222",
                new EventMessageImpl("Event 2"),
                "AUTH_TOKEN_22222",
                1,
                new HECTimeImpl(Assertions.assertDoesNotThrow(() -> new ObjectMapper().readTree("null"))),
                new HeaderInfo(new XForwardedForStub(), new XForwardedHostStub(), new XForwardedProtoStub())
        );
        expectedMetadataSDE2.addSDParam("authentication_token", hecRecord2.authenticationToken());
        expectedMetadataSDE2.addSDParam("channel", hecRecord2.channel());
        expectedMetadataSDE2.addSDParam("ack_id", String.valueOf(hecRecord2.ackID()));
        expectedMetadataSDE2.addSDParam("time_source", hecRecord2.time().source());
        expectedMetadataSDE2.addSDParam("time_parsed", "false");
        expectedMetadataSDE2.addSDParam("time", String.valueOf(currentEpoch));
        expectedMetadataSDE2.addSDParam("generated", "true");

        SyslogMessage expectedSyslogMessage2 = new SyslogMessage()
                .withSeverity(expectedSeverity)
                .withAppName("capsulated")
                .withHostname("cfe-16")
                .withFacility(expectedFacility)
                .withSDElement(expectedMetadataSDE2)
                .withMsg(hecRecord2.event().asString());
        SyslogMessage returnedMessage2 = hecRecord2.toSyslogMessage(currentEpoch);

        List<SDParam> expectedSDParams = new ArrayList<>();
        List<SDParam> returnedSDParams = new ArrayList<>();

        // Gets the SDParams from the SDElements from the second SyslogMessage
        for (SDElement sdElement : returnedMessage2.getSDElements()) {
            returnedSDParams.addAll(sdElement.getSdParams());
        }

        // Gets the SDParams from the SDEs from the second SyslogMessage created in initialize()
        for (SDElement sdElement : expectedSyslogMessage2.getSDElements()) {
            expectedSDParams.addAll(sdElement.getSdParams());
        }

        // Goes through all the returned SDParams and checks that they are all found in
        // supposed SDParams
        final int expectedReturnedSDParamsAssertions1 = 7; // See lines 143 - 147
        int loopedReturnedSDParamsAssertions1 = 0;
        for (SDParam returnedSDParam : returnedSDParams) {
            loopedReturnedSDParamsAssertions1++;
            Assertions
                    .assertTrue(expectedSDParams.contains(returnedSDParam), "SDParam '" + returnedSDParam + "' should not be in returned SDElement.");
        }
        Assertions
                .assertEquals(
                        expectedReturnedSDParamsAssertions1, loopedReturnedSDParamsAssertions1,
                        "All returnedSDParams were NOT looped through"
                );
    }

    @Test
    @DisplayName("fields match for the third HECRecord")
    void fieldsMatchForTheThirdHecRecord() {
        // Timestamp used as fallback value for HECRecord time
        final long currentEpoch = Instant.now().toEpochMilli();

        SDElement expectedMetadataSDE3 = new SDElement("cfe_16-metadata@48577");
        Severity expectedSeverity = Severity.INFORMATIONAL;
        Facility expectedFacility = Facility.USER;
        HECRecordImpl hecRecord3 = new HECRecordImpl(
                "defaultchannel",
                new EventMessageImpl("Event 3"),
                "AUTH_TOKEN_33333",
                null,
                new HECTimeImpl(Assertions.assertDoesNotThrow(() -> new ObjectMapper().readTree("null"))),
                new HeaderInfo(new XForwardedForStub(), new XForwardedHostStub(), new XForwardedProtoStub())
        );
        expectedMetadataSDE3.addSDParam("authentication_token", hecRecord3.authenticationToken());
        expectedMetadataSDE3.addSDParam("channel", hecRecord3.channel());
        expectedMetadataSDE3.addSDParam("time_source", hecRecord3.time().source());
        expectedMetadataSDE3.addSDParam("time_parsed", "false");
        expectedMetadataSDE3.addSDParam("time", String.valueOf(currentEpoch));
        expectedMetadataSDE3.addSDParam("generated", "true");

        SyslogMessage expectedSyslogMessage3 = new SyslogMessage()
                .withSeverity(expectedSeverity)
                .withAppName("capsulated")
                .withHostname("cfe-16")
                .withFacility(expectedFacility)
                .withSDElement(expectedMetadataSDE3)
                .withMsg(hecRecord3.event().asString());

        SyslogMessage returnedMessage3 = hecRecord3.toSyslogMessage(currentEpoch);

        Assertions
                .assertEquals(
                        expectedSyslogMessage3.getSeverity(), returnedMessage3.getSeverity(),
                        "Severity should be INFORMATIONAL"
                );
        Assertions.assertNull(expectedSyslogMessage3.getTimestamp(), "Timestamp should be null");
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
    @DisplayName("SDElements match for the third HECRecord")
    void sdElementsMatchForTheThirdHecRecord() {
        // Timestamp used as fallback value for HECRecord time
        final long currentEpoch = Instant.now().toEpochMilli();

        Severity expectedSeverity = Severity.INFORMATIONAL;
        Facility expectedFacility = Facility.USER;
        HECRecordImpl hecRecord3 = new HECRecordImpl(
                "defaultchannel",
                new EventMessageImpl("Event 3"),
                "AUTH_TOKEN_33333",
                null,
                new HECTimeImpl(Assertions.assertDoesNotThrow(() -> new ObjectMapper().readTree("null"))),
                new HeaderInfo(new XForwardedForStub(), new XForwardedHostStub(), new XForwardedProtoStub())
        );
        SDElement expectedMetadataSDE3 = new SDElement("cfe_16-metadata@48577");
        expectedMetadataSDE3.addSDParam("authentication_token", hecRecord3.authenticationToken());
        expectedMetadataSDE3.addSDParam("channel", hecRecord3.channel());
        expectedMetadataSDE3.addSDParam("time_source", hecRecord3.time().source());
        expectedMetadataSDE3.addSDParam("time_parsed", "false");
        expectedMetadataSDE3.addSDParam("time", String.valueOf(currentEpoch));
        expectedMetadataSDE3.addSDParam("generated", "true");

        SDElement expectedHeaderMetadata = new SDElement("cfe_16-origin@48577");

        SyslogMessage expectedSyslogMessage3 = new SyslogMessage()
                .withSeverity(expectedSeverity)
                .withAppName("capsulated")
                .withHostname("cfe-16")
                .withFacility(expectedFacility)
                .withSDElement(expectedMetadataSDE3)
                .withSDElement(expectedHeaderMetadata)
                .withMsg(hecRecord3.event().asString());

        List<SDParam> supposedSDParams = new ArrayList<>();
        List<SDParam> returnedSDParams = new ArrayList<>();

        // Gets the SDParams from the SDEs from the third SyslogMessage returned from
        // Converter and saves them in a List
        for (SDElement sdElement : hecRecord3.toSyslogMessage(currentEpoch).getSDElements()) {
            returnedSDParams.addAll(sdElement.getSdParams());
        }

        // Gets the SDParams from the SDEs from the third SyslogMessage created in
        // initialize() and saves them in a List
        for (SDElement sdElement : expectedSyslogMessage3.getSDElements()) {
            supposedSDParams.addAll(sdElement.getSdParams());
        }

        // Goes through all the returned SDParams and checks that they are all found in
        // supposed SDParams
        final int expectedReturnedSDParamsAssertions2 = 6;
        int loopedReturnedSDParamsAssertions2 = 0;
        for (SDParam returnedSDParam : returnedSDParams) {
            loopedReturnedSDParamsAssertions2++;
            Assertions
                    .assertTrue(supposedSDParams.contains(returnedSDParam), "SDParam '" + returnedSDParam + "' should not be in returned SDElement.");
        }
        Assertions
                .assertEquals(
                        expectedReturnedSDParamsAssertions2, loopedReturnedSDParamsAssertions2,
                        "All returnedSDParams were NOT looped through"
                );
    }
}
