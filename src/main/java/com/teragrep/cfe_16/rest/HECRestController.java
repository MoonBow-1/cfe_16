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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teragrep.cfe_16.RequestBodyCleaner;
import com.teragrep.cfe_16.config.Configuration;
import com.teragrep.cfe_16.response.Response;
import com.teragrep.cfe_16.service.HECService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/")
public class HECRestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(HECRestController.class);
    @Autowired
    private HECService service;

    @Autowired
    private RequestBodyCleaner requestBodyCleaner;

    @Autowired
    private Configuration configuration;

    private ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("rawtypes")
    @RequestMapping(
            value = "services/collector",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String sendEvents(
            HttpServletRequest request,
            @RequestBody MultiValueMap body,
            @RequestParam(required = false) String channel
    ) {
        // TODO: Try to think an alternative way to implement getting the body of the
        // call
        final String eventInJson = requestBodyCleaner.cleanAckRequestBody(body.toString(), channel);

        long t1 = System.nanoTime();
        final Response response = service.sendEvents(request, channel, eventInJson);
        long t2 = System.nanoTime();
        long dt = t2 - t1;
        double us = (double) dt / 1000.0;
        if (this.configuration.getPrintTimes()) {
            LOGGER.info("sendEvents took <{}> nanoseconds, that is <{}> microseconds", dt, us);
        }
        return response.body().toString();
    }

    @RequestMapping(
            value = "services/collector",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public String sendEvents(
            HttpServletRequest request,
            @RequestBody String eventInJson,
            @RequestParam(required = false) String channel
    ) {
        long t1 = System.nanoTime();
        final Response response = service.sendEvents(request, channel, eventInJson);
        long t2 = System.nanoTime();
        long dt = t2 - t1;
        double us = (double) dt / 1000.0;
        if (this.configuration.getPrintTimes()) {
            LOGGER.info("sendEvents took <{}> nanoseconds, that is <{}> microseconds", dt, us);
        }
        return response.body().toString();
    }

    // @LogAnnotation(type = LogType.METRIC_DURATION)
    @RequestMapping(
            value = "services/collector/ack",
            method = {
                    RequestMethod.POST, RequestMethod.GET
            },
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public JsonNode getAcksWithPostMethod(
            @RequestBody JsonNode requestedAcksInJson,
            HttpServletRequest request,
            @RequestParam(required = false) String channel
    ) {

        long t1 = System.nanoTime();
        JsonNode response = service.getAcks(request, channel, requestedAcksInJson);
        long t2 = System.nanoTime();
        long dt = t2 - t1;
        double us = (double) dt / 1000.0;
        if (this.configuration.getPrintTimes()) {
            LOGGER.info("getAcks took <{}> nanoseconds, that is <{}> microseconds", dt, us);
        }
        return response;
    }

    // @LogAnnotation(type = LogType.METRIC_DURATION)
    @SuppressWarnings("rawtypes")
    @RequestMapping(
            value = "services/collector/ack",
            method = {
                    RequestMethod.POST, RequestMethod.GET
            },
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public @ResponseBody JsonNode getAcks(
            @RequestBody MultiValueMap body,
            HttpServletRequest request,
            @RequestParam(required = false) String channel
    ) {
        // TODO: Try to think an alternative way to implement getting the body of the
        // call
        String bodyString = requestBodyCleaner.cleanAckRequestBody(body.toString(), channel);

        JsonNode requestedAcksInJson = null;
        try {
            requestedAcksInJson = objectMapper.readValue(bodyString, JsonNode.class);
        }
        catch (Exception e) {
            // TODO: handle the error in a proper way
            LOGGER.warn("Failed to handle response: ", e);
        }

        long t1 = System.nanoTime();
        JsonNode response = service.getAcks(request, channel, requestedAcksInJson);
        long t2 = System.nanoTime();
        long dt = t2 - t1;
        double us = (double) dt / 1000.0;
        if (this.configuration.getPrintTimes()) {
            LOGGER.info("getAcks took <{}> nanoseconds, that is <{}> microseconds", dt, us);
        }
        return response;
    }

    @SuppressWarnings("rawtypes")
    @RequestMapping(
            value = "services/collector/event",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public String sendEventsWithFormatOption(
            HttpServletRequest request,
            @RequestBody MultiValueMap body,
            @RequestParam(required = false) String channel
    ) {

        // TODO: Try to think an alternative way to implement getting the body of the
        // call
        String eventInJson = requestBodyCleaner.cleanAckRequestBody(body.toString(), channel);

        long t1 = System.nanoTime();
        final Response response = service.sendEvents(request, channel, eventInJson);
        long t2 = System.nanoTime();
        long dt = t2 - t1;
        double us = (double) dt / 1000.0;
        if (this.configuration.getPrintTimes()) {
            LOGGER.info("sendEvents took <{}> nanoseconds, that is <{}> microseconds", dt, us);
        }
        return response.body().toString();
    }

    @RequestMapping(
            value = "services/collector/event",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public String sendEventsWithFormatOption(
            HttpServletRequest request,
            @RequestBody String eventInJson,
            @RequestParam(required = false) String channel
    ) {
        // FIXME: Fix implementation to known standards
        // This endpoint works identically to services/collector but introduces a format
        // option for future scalability.
        long t1 = System.nanoTime();
        final Response response = service.sendEvents(request, channel, eventInJson);
        long t2 = System.nanoTime();
        long dt = t2 - t1;
        double us = (double) dt / 1000.0;
        if (this.configuration.getPrintTimes()) {
            LOGGER.info("sendEvents took <{}> nanoseconds, that is <{}> microseconds", dt, us);
        }
        return response.body().toString();
    }

    // @LogAnnotation(type = LogType.METRIC_DURATION)
    @PostMapping("services/collector/event/1.0")
    public String sendEventsWithProtocolVersion(
            HttpServletRequest request,
            @RequestBody String eventInJson,
            @RequestParam(required = false) String channel
    ) {
        // FIXME: Fix implementation to known standards
        // This endpoint works identically to services/collector/event but introduces a
        // protocol version for future scalability
        long t1 = System.nanoTime();
        final Response response = service.sendEvents(request, channel, eventInJson);
        long t2 = System.nanoTime();
        long dt = t2 - t1;
        double us = (double) dt / 1000.0;
        if (this.configuration.getPrintTimes()) {
            LOGGER.info("sendEvents took <{}¦ nanoseconds, that is <{}> microseconds", dt, us);
        }
        return response.body().toString();
    }

    // @LogAnnotation(type = LogType.METRIC_DURATION)
    @GetMapping("services/collector/health")
    public ResponseEntity<String> getHealth(HttpServletRequest request) {
        // FIXME: Fix implementation to known standards
        return service.healthCheck(request);
    }

    // @LogAnnotation(type = LogType.METRIC_DURATION)
    @GetMapping("services/collector/health/1.0")
    public String getHealthWithProtocolVersion() {
        // TODO: Implement endpoint
        return null;
    }

    // @LogAnnotation(type = LogType.METRIC_DURATION)
    @PostMapping("services/collector/mint")
    public void sendMintData(@RequestBody String mintData) {
        // TODO: Implement endpoint
    }

    // @LogAnnotation(type = LogType.METRIC_DURATION)
    @PostMapping("services/collector/mint/1.0")
    public void sendMintDataWithProtocolVersion(@RequestBody String mintData) {
        // TODO: Implement endpoint
    }

    // @LogAnnotation(type = LogType.METRIC_DURATION)
    @PostMapping("services/collector/raw")
    public void sendRawData(@RequestBody String rawData) {
        // TODO: Implement endpoint
    }

    // @LogAnnotation(type = LogType.METRIC_DURATION)
    @PostMapping("services/collector/raw/1.0")
    public void sendRawDataWithProtocolVersion(@RequestBody String rawData) {
        // TODO: Implement endpoint
    }
}
