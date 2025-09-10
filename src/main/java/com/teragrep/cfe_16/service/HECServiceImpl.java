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
package com.teragrep.cfe_16.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonSyntaxException;
import com.teragrep.cfe_16.*;
import com.teragrep.cfe_16.bo.HeaderInfo;
import com.teragrep.cfe_16.bo.Session;
import com.teragrep.cfe_16.bo.XForwardedFor;
import com.teragrep.cfe_16.bo.XForwardedForImpl;
import com.teragrep.cfe_16.bo.XForwardedForStub;
import com.teragrep.cfe_16.bo.XForwardedHost;
import com.teragrep.cfe_16.bo.XForwardedHostImpl;
import com.teragrep.cfe_16.bo.XForwardedHostStub;
import com.teragrep.cfe_16.bo.XForwardedProto;
import com.teragrep.cfe_16.bo.XForwardedProtoImpl;
import com.teragrep.cfe_16.bo.XForwardedProtoStub;
import com.teragrep.cfe_16.exceptionhandling.AuthenticationTokenMissingException;
import com.teragrep.cfe_16.exceptionhandling.ChannelNotFoundException;
import com.teragrep.cfe_16.exceptionhandling.ChannelNotProvidedException;
import com.teragrep.cfe_16.exceptionhandling.SessionNotFoundException;
import com.teragrep.cfe_16.response.ExceptionEvent;
import com.teragrep.cfe_16.response.ExceptionEventContext;
import com.teragrep.cfe_16.response.ExceptionJsonResponse;
import com.teragrep.cfe_16.response.Response;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Implementation of the REST Service back end.
 */
@Service
public class HECServiceImpl implements HECService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HECServiceImpl.class);
    @Autowired
    private Acknowledgements acknowledgements;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private TokenManager tokenManager;

    @Autowired
    private EventManager eventManager;

    private ObjectMapper objectMapper = new ObjectMapper();

    private final XForwardedForStub xForwardedForStub;
    private final XForwardedHostStub xForwardedHostStub;
    private final XForwardedProtoStub xForwardedProtoStub;

    public HECServiceImpl() {
        this.xForwardedForStub = new XForwardedForStub();
        this.xForwardedHostStub = new XForwardedHostStub();
        this.xForwardedProtoStub = new XForwardedProtoStub();
    }

    @Override
    // @LogAnnotation(type = LogType.METRIC_COUNTER)
    public Response sendEvents(HttpServletRequest request, String channel, String eventInJson) {
        LOGGER.debug("Sending events to channel <{}>", channel);
        if (this.tokenManager.tokenIsMissing(request)) {
            throw new AuthenticationTokenMissingException("Authentication token must be provided");
        }
        // AspectLoggerWrapper.logMetricCounter(null, "metric_counter", 10);
        // AspectLoggerWrapper.logMetricDuration(null, "new_metric",
        // MetricDurationOptionsImpl.MetricDuration.P10S);
        String authHeader = request.getHeader("Authorization");
        LOGGER.debug("Creating new Header Info");

        final XForwardedFor xForwardedFor;
        final XForwardedHost xForwardedHost;
        final XForwardedProto xForwardedProto;

        LOGGER.debug("Setting X-Forwarded-For");
        if (request.getHeader("X-Forwarded-For") == null) {
            xForwardedFor = this.xForwardedForStub;
        }
        else {
            xForwardedFor = new XForwardedForImpl(request.getHeader("X-Forwarded-For"));
        }
        LOGGER.trace("Setting X-Forwarded-For to value <[{}]>", xForwardedFor);

        LOGGER.debug("Setting X-Forwarded-Host");
        if (request.getHeader("X-Forwarded-Host") == null) {
            xForwardedHost = this.xForwardedHostStub;
        }
        else {
            xForwardedHost = new XForwardedHostImpl(request.getHeader("X-Forwarded-Host"));
        }
        LOGGER.trace("Setting X-Forwarded-Host to value <[{}]>", xForwardedHost);

        LOGGER.debug("Setting X-Forwarded-Proto");
        if (request.getHeader("X-Forwarded-Proto") == null) {
            xForwardedProto = this.xForwardedProtoStub;
        }
        else {
            xForwardedProto = new XForwardedProtoImpl(request.getHeader("X-Forwarded-Proto"));
        }
        LOGGER.trace("Setting X-Forwarded-Proto to value <[{}]>", xForwardedProto);

        final HeaderInfo headerInfo = new HeaderInfo(xForwardedFor, xForwardedHost, xForwardedProto);

        String authToken;
        if (tokenManager.isTokenInBasic(authHeader)) {
            LOGGER.debug("Token was provided via Basic");
            authToken = this.tokenManager.getTokenFromBasic(authHeader);
        }
        else {
            LOGGER.debug("Token was provided via header");
            authToken = authHeader;
        }

        // if there is no channel, we'll use the default channel
        if (channel == null) {
            channel = Session.DEFAULT_CHANNEL;
            LOGGER.debug("Channel was not provided, using <{}>", channel);
        }

        Session session = this.sessionManager.getOrCreateSession(authToken);

        // if the channel is not in the session, let's add the channel into it
        if (!session.doesChannelExist(channel)) {
            LOGGER.debug("Adding channel <{}>", channel);
            session.addChannel(channel);
        }

        // TODO: find a nice way of not passing Acknowledgements
        try {
            return this.eventManager.convertData(authToken, channel, eventInJson, headerInfo, this.acknowledgements);
        }
        catch (final JsonProcessingException | JsonSyntaxException e) {
            final ExceptionEventContext exceptionEventContext = new ExceptionEventContext(
                    headerInfo,
                    request.getHeader("user-agent"),
                    request.getRequestURI(),
                    request.getRemoteHost()
            );
            final ExceptionEvent event = new ExceptionEvent(exceptionEventContext, UUID.randomUUID(), e);
            event.logException();
            return new ExceptionJsonResponse(HttpStatus.BAD_REQUEST, event);
        }
    }

    // @LogAnnotation(type = LogType.RESPONSE)
    @SuppressWarnings("deprecation")
    @Override
    public JsonNode getAcks(HttpServletRequest request, String channel, JsonNode requestedAcksInJson) {

        // filter out error cases
        // authentication header is required always
        if (this.tokenManager.tokenIsMissing(request)) {
            throw new AuthenticationTokenMissingException("Authentication token must be provided");
        }

        String authHeader = request.getHeader("Authorization");

        String authToken;
        if (tokenManager.isTokenInBasic(authHeader)) {
            LOGGER.debug("Token was provided via Basic");
            authToken = this.tokenManager.getTokenFromBasic(authHeader);
        }
        else {
            LOGGER.debug("Token was provided via header");
            authToken = authHeader;
        }

        // channel is required
        if (channel == null) {
            throw new ChannelNotProvidedException("Channel must be provided when requesting ack statuses");
        }

        // session is also required
        Session session = this.sessionManager.getSession(authToken);
        if (session == null) {
            throw new SessionNotFoundException("Session not found for auth token " + authToken);
        }

        // if channel is not inside Session, it is considered an error case
        if (!session.doesChannelExist(channel)) {
            throw new ChannelNotFoundException();
        }
        session.touch();

        ObjectNode responseNode = objectMapper.createObjectNode();
        JsonNode requestedAckStatuses = this.acknowledgements
                .getRequestedAckStatuses(authToken, channel, requestedAcksInJson);
        responseNode.put("acks", requestedAckStatuses);
        return responseNode;
    }

    // @LogAnnotation(type = LogType.RESPONSE)
    @Override
    public ResponseEntity<String> healthCheck(HttpServletRequest request) {
        if (this.tokenManager.tokenIsMissing(request)) {
            return new ResponseEntity<String>("Invalid HEC token", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<String>("HEC is available and accepting input", HttpStatus.OK);
    }
}
