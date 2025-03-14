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

import com.cloudbees.syslog.Facility;
import com.cloudbees.syslog.SDElement;
import com.cloudbees.syslog.Severity;
import com.cloudbees.syslog.SyslogMessage;
import com.teragrep.cfe_16.event.EventMessage;
import com.teragrep.cfe_16.event.time.HECTime;
import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HECRecordImpl implements HECRecord {

    private static final Logger LOGGER = LoggerFactory.getLogger(HECRecordImpl.class);
    private final String channel;
    private final EventMessage eventMessage;
    private final String authenticationToken;
    private final Integer ackID;
    private final HECTime hecTime;
    private final String hostName;
    private final Severity severity;
    private final Facility facility;
    private final HeaderInfo headerInfo;

    public HECRecordImpl(
            String channel,
            EventMessage eventMessage,
            String authenticationToken,
            Integer ackID,
            HECTime hecTime,
            String hostName,
            Severity severity,
            Facility facility,
            HeaderInfo headerInfo
    ) {
        this.channel = channel;
        this.eventMessage = eventMessage;
        this.authenticationToken = authenticationToken;
        this.ackID = ackID;
        this.hecTime = hecTime;
        this.hostName = hostName;
        this.severity = severity;
        this.facility = facility;
        this.headerInfo = headerInfo;
    }

    public HECRecordImpl(
            String channel,
            EventMessage eventMessage,
            String authenticationToken,
            Integer ackID,
            HECTime hecTime,
            HeaderInfo headerInfo
    ) {
        this(
                channel,
                eventMessage,
                authenticationToken,
                ackID,
                hecTime,
                "cfe-16",
                Severity.INFORMATIONAL,
                Facility.USER,
                headerInfo
        );
    }

    @Override
    public EventMessage event() {
        return this.eventMessage;
    }

    @Override
    public String channel() {
        return this.channel;
    }

    @Override
    public String authenticationToken() {
        return this.authenticationToken;
    }

    @Override
    public HECTime time() {
        return this.hecTime;
    }

    @Override
    public SyslogMessage toSyslogMessage() {
        final long currentEpochMillis = Instant.now().toEpochMilli();
        final SDElement structuredMetadata = structuredDataParams(currentEpochMillis);

        SyslogMessage syslogMessage;
        if (this.time().parsed()) {

            /*
             * Creates a Syslogmessage with a time stamp
             */
            LOGGER.debug("Creating new syslog message with timestamp");
            syslogMessage = new SyslogMessage()
                    .withTimestamp(this.time().instant(currentEpochMillis))
                    .withSeverity(this.severity)
                    .withAppName("capsulated")
                    .withHostname(this.hostName)
                    .withFacility(this.facility)
                    .withSDElement(structuredMetadata)
                    .withSDElement(this.headerInfo.asSDElement())
                    .withMsg(this.event().asString());

        }
        else {
            /*
             * Creates a Syslogmessage without timestamp, because the time is already given
             * in the request.
             */
            LOGGER.debug("Creating new syslog message without timestamp");
            syslogMessage = new SyslogMessage()
                    .withSeverity(this.severity)
                    .withAppName("capsulated")
                    .withHostname(this.hostName)
                    .withFacility(this.facility)
                    .withSDElement(structuredMetadata)
                    .withSDElement(this.headerInfo.asSDElement())
                    .withMsg(this.event().asString());
        }

        return syslogMessage;
    }

    @Override
    public SyslogMessage toSyslogMessage(long defaultValue) {
        final SDElement structuredMetadata = structuredDataParams(defaultValue);

        SyslogMessage syslogMessage;
        if (this.time().parsed()) {

            /*
             * Creates a Syslogmessage with a time stamp
             */
            LOGGER.debug("Creating new syslog message with timestamp");
            syslogMessage = new SyslogMessage()
                    .withTimestamp(this.time().instant(defaultValue))
                    .withSeverity(this.severity)
                    .withAppName("capsulated")
                    .withHostname(this.hostName)
                    .withFacility(this.facility)
                    .withSDElement(structuredMetadata)
                    .withSDElement(this.headerInfo.asSDElement())
                    .withMsg(this.event().asString());

        }
        else {
            /*
             * Creates a Syslogmessage without timestamp, because the time is already given
             * in the request.
             */
            LOGGER.debug("Creating new syslog message without timestamp");
            syslogMessage = new SyslogMessage()
                    .withSeverity(this.severity)
                    .withAppName("capsulated")
                    .withHostname(this.hostName)
                    .withFacility(this.facility)
                    .withSDElement(structuredMetadata)
                    .withSDElement(this.headerInfo.asSDElement())
                    .withMsg(this.event().asString());
        }

        return syslogMessage;
    }

    @Override
    public Integer ackID() {
        return this.ackID;
    }

    @Override
    public boolean isStub() {
        return false;
    }

    /*
     * Gets the data from the HTTP Event Data and adds it to SD Element as SD
     * Parameters.
     */
    private SDElement structuredDataParams(final long fallbackEpoch) {
        LOGGER.debug("Setting Structured Data params");
        final SDElement metadataSDE = new SDElement("cfe_16-metadata@48577");

        if (this.authenticationToken() != null) {
            LOGGER.debug("Setting authentication token");
            metadataSDE.addSDParam("authentication_token", this.authenticationToken());
        }

        if (this.channel() != null) {
            LOGGER.debug("Setting channel");
            metadataSDE.addSDParam("channel", this.channel());
        }

        if (this.ackID() != null) {
            LOGGER.debug("Setting ack id");
            metadataSDE.addSDParam("ack_id", String.valueOf(this.ackID()));
        }

        if (this.time().source() != null) {
            LOGGER.debug("Setting time source");
            metadataSDE.addSDParam("time_source", this.time().source());
        }

        LOGGER.debug("Setting time_parsed and time");
        metadataSDE.addSDParam("time_parsed", String.valueOf(this.time().parsed()));
        metadataSDE.addSDParam("time", String.valueOf(this.time().instant(fallbackEpoch)));
        if (this.time().parsed()) {
            LOGGER.debug("TimeParsed was true");
            metadataSDE.addSDParam("generated", "false");
        }
        else {
            LOGGER.debug("TimeParsed was false");
            metadataSDE.addSDParam("generated", "true");
        }

        return metadataSDE;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        HECRecordImpl that = (HECRecordImpl) o;
        return Objects.equals(channel, that.channel) && Objects.equals(eventMessage, that.eventMessage) && Objects
                .equals(authenticationToken, that.authenticationToken) && Objects.equals(ackID, that.ackID)
                && Objects.equals(hecTime, that.hecTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(channel, eventMessage, authenticationToken, ackID, hecTime);
    }
}
