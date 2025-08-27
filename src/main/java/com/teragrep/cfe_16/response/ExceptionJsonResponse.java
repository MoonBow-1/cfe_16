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
package com.teragrep.cfe_16.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

public final class ExceptionJsonResponse implements Response {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionJsonResponse.class);
    private final UUID eventId;
    private final HttpStatus status;
    private final Throwable throwable;

    public ExceptionJsonResponse(final HttpStatus status, final Throwable throwable) {
        this(status, throwable, UUID.randomUUID());
    }

    public ExceptionJsonResponse(final HttpStatus status, final Throwable throwable, final UUID eventId) {
        this.status = status;
        this.throwable = throwable;
        this.eventId = eventId;
    }

    public HttpStatus status() {
        return status;
    }

    public ObjectNode body() {
        LOGGER.error("Event_{}", eventId, throwable);
        final ObjectMapper jsonObjectBuilder = new ObjectMapper();
        return jsonObjectBuilder
                .createObjectNode()
                .put(
                        "message",
                        "An error occurred while processing your Request. See event id " + eventId
                                + " in the technical log for details."
                );

    }

    public String contentType() {
        return MediaType.APPLICATION_JSON_VALUE;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ExceptionJsonResponse that = (ExceptionJsonResponse) o;
        return Objects.equals(eventId, that.eventId) && status == that.status
                && Objects.equals(throwable, that.throwable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, status, throwable);
    }
}
