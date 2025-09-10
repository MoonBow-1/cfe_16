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

import com.cloudbees.syslog.SyslogMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonParseException;
import com.google.gson.JsonStreamParser;
import com.teragrep.cfe_16.bo.Ack;
import com.teragrep.cfe_16.bo.HeaderInfo;
import com.teragrep.cfe_16.bo.HttpEventData;
import com.teragrep.cfe_16.bo.Session;
import com.teragrep.cfe_16.config.Configuration;
import com.teragrep.cfe_16.event.EventMessageStub;
import com.teragrep.cfe_16.event.JsonEvent;
import com.teragrep.cfe_16.event.JsonEventImpl;
import com.teragrep.cfe_16.exceptionhandling.InternalServerErrorException;
import com.teragrep.cfe_16.connection.AbstractConnection;
import com.teragrep.cfe_16.connection.ConnectionFactory;
import com.teragrep.cfe_16.response.AcknowledgedJsonResponse;
import com.teragrep.cfe_16.response.ExceptionEvent;
import com.teragrep.cfe_16.response.ExceptionJsonResponse;
import com.teragrep.cfe_16.response.JsonResponse;
import com.teragrep.cfe_16.response.Response;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/*
 * Manager that handles the event sent in a request.
 *
 */
@Component
public class EventManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventManager.class);

    @Autowired
    private Configuration configuration;

    private AbstractConnection connection;

    public EventManager() {
    }

    @PostConstruct
    public void setupSender() {
        LOGGER.debug("Setting up connection");
        try {
            this.connection = ConnectionFactory
                    .createSender(
                            this.configuration.getSysLogProtocol(), this.configuration.getSyslogHost(),
                            this.configuration.getSyslogPort()
                    );
        }
        catch (IOException e) {
            LOGGER.error("Error creating connection", e);
            throw new InternalServerErrorException();
        }
    }

    /*
     * Method used when converting data and the channel is specified in the request.
     * Takes authentication token, all events sent in a request (in JSON format) and
     * the channel name as string parameters. Returns a JSON node with ack id if
     * everything is successful. Example: {"text":"Success","code":0,"ackID":0}
     */
    public Response convertData(
            final HttpServletRequest request,
            final String authToken,
            final String channel,
            final String allEventsInJson,
            final HeaderInfo headerInfo,
            final Acknowledgements acknowledgements
    ) {
        HttpEventData previousEvent;

        acknowledgements.initializeContext(authToken, channel);
        int ackId = acknowledgements.getCurrentAckValue(authToken, channel);
        boolean incremented = acknowledgements.incrementAckValue(authToken, channel);
        if (!incremented) {
            throw new InternalServerErrorException("Ack value couldn't be incremented.");
        }
        Ack ack = new Ack(ackId, false);
        boolean addedAck = acknowledgements.addAck(authToken, channel, ack);
        if (!addedAck) {
            throw new InternalServerErrorException("Ack ID " + ackId + " couldn't be added to the Ack set.");
        }
        JsonStreamParser parser = new JsonStreamParser(allEventsInJson);

        /*
         * There can be multiple events in one request. Here they are handled one by
         * one. The event is saved in a string variable and is converted into
         * HttpEventData object. Metadata is assigned to the object. HttpEventData is
         * converted into SyslogMessage and saved in a list in a RequestInfo object.
         * After the event is handled, it is assigned as a value to previousEvent
         * variable.
         */
        HttpEventData eventData = new HttpEventData();
        final Converter converter = new Converter(headerInfo);
        final List<SyslogMessage> syslogMessages = new ArrayList<>();

        // Shared EventMessageStub that is used in case parsed Event is a Stub
        final EventMessageStub eventMessageStub = new EventMessageStub();
        while (parser.hasNext()) {
            try {
                previousEvent = eventData;
                final String eventAsString = parser.next().toString();
                final JsonNode jsonNode = new ObjectMapper().readTree(eventAsString);
                final JsonEvent jsonEvent = new JsonEventImpl(jsonNode, eventMessageStub);
                eventData.setEvent(jsonEvent.asEventMessage());
                eventData = handleTime(eventData, jsonNode, previousEvent);
                eventData = assignMetaData(eventData, authToken, channel);

                final SyslogMessage syslogMessage = converter.httpToSyslog(eventData);
                syslogMessages.add(syslogMessage);
            }
            catch (final JsonProcessingException | JsonParseException e) {
                final ExceptionEvent event = new ExceptionEvent(request, UUID.randomUUID(), e);
                event.logException();
                return new ExceptionJsonResponse(HttpStatus.BAD_REQUEST, event);
            }
        }

        // create a new object to avoid blocking of threads because
        // the SyslogMessageSender.sendMessage() is synchronized
        try {
            this.connection.sendMessages(syslogMessages);
        }
        catch (IOException e) {
            throw new InternalServerErrorException(e);
        }

        boolean shouldAck = channel != null && !channel.equals(Session.DEFAULT_CHANNEL);

        if (shouldAck) {
            boolean acked = acknowledgements.acknowledge(authToken, channel, ackId);
            if (!acked) {
                throw new InternalServerErrorException("Ack ID " + ackId + " not Acked.");
            }
        }

        if (shouldAck) {
            return new AcknowledgedJsonResponse(HttpStatus.OK, "Success", ackId);
        }
        else {
            return new JsonResponse(HttpStatus.OK, "Success");
        }
    }

    /*
     * The time stamp of the event can be given as epoch time in the request.
     */
    private HttpEventData handleTime(HttpEventData eventData, JsonNode jsonObject, HttpEventData previousEvent) {
        JsonNode timeObject = jsonObject.get("time");

        /*
         * If the time is given as a string rather than as a numeral value, the time is
         * handled in a same way as it is handled when time is not given in a request.
         */
        if (timeObject == null || timeObject.isTextual()) {
            eventData.setTimeParsed(false);
            eventData.setTimeSource("generated");
            if (previousEvent != null) {
                if (previousEvent.isTimeParsed()) {
                    eventData.setTimeAsLong(previousEvent.getTimeAsLong());
                    eventData.setTime(previousEvent.getTime());
                    eventData.setTimeParsed(true);
                    eventData.setTimeSource("reported");
                }
            }
            /*
             * If the time is given as epoch seconds with a decimal (example:
             * 1433188255.253), the decimal point must be removed and time is assigned to
             * HttpEventData object as a long value. convertTimeToEpochMillis() will check
             * that correct time format is used.
             */
        }
        else if (timeObject.isDouble()) {
            eventData.setTimeAsLong(removeDecimal(timeObject.asDouble()));
            eventData.setTime(String.valueOf(eventData.getTimeAsLong()));
            eventData.setTimeParsed(true);
            eventData.setTimeSource("reported");
            eventData = convertTimeToEpochMillis(eventData);
            /*
             * If the time is given in a numeral value, it is assigned to HttpEventData
             * object as a long value. convertTimeToEpochMillis() will check that correct
             * time format is used.
             */
        }
        else if (timeObject.canConvertToLong()) {
            eventData.setTimeAsLong(timeObject.asLong());
            eventData.setTime(jsonObject.get("time").asText());
            eventData.setTimeParsed(true);
            eventData.setTimeSource("reported");
            eventData = convertTimeToEpochMillis(eventData);
        }
        else {
            eventData.setTimeParsed(false);
            eventData.setTimeSource("generated");
        }

        return eventData;
    }

    /*
     * Takes a double value as a parameter, removes the decimal point from that
     * value and returns the number as a long value.
     */
    private long removeDecimal(double doubleValue) {
        BigDecimal doubleValueWithDecimal = BigDecimal.valueOf(doubleValue);
        String stringValue = doubleValueWithDecimal.toString();
        String stringValueWithoutDecimal = stringValue.replace(".", "");
        long longValue = Long.parseLong(stringValueWithoutDecimal);

        return longValue;
    }

    /*
     * Converts the given time stamp into epoch milliseconds. Takes a HttpEventData
     * object as a parameter. Gets the time from the variable set in the
     * HttpEventData object. If the time value in the object has 13 digits, it means
     * that time has been already given in epoch milliseconds.
     */
    private HttpEventData convertTimeToEpochMillis(HttpEventData eventData) {
        String timeString = eventData.getTime();
        if (timeString.length() == 13) {
            return eventData;
        }
        else if (timeString.length() >= 10 && timeString.length() < 13) {
            eventData.setTimeAsLong(eventData.getTimeAsLong() * (long) Math.pow(10, ((13 - timeString.length()))));
        }
        else {
            eventData.setTimeParsed(false);
            eventData.setTimeSource("generated");
        }
        return eventData;
    }

    /*
     * Assigns the metadata (authentication token and channel name) to the
     * HttpEventData object.
     */
    private HttpEventData assignMetaData(HttpEventData eventData, String authToken, String channel) {

        eventData.setAuthenticationToken(authToken);
        eventData.setChannel(channel);

        return eventData;
    }
}
