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

import com.fasterxml.jackson.databind.JsonNode;
import com.teragrep.cfe_16.bo.HeaderInfo;
import com.teragrep.cfe_16.bo.XForwardedForStub;
import com.teragrep.cfe_16.bo.XForwardedHostStub;
import com.teragrep.cfe_16.bo.XForwardedProtoStub;
import java.util.List;
import java.util.UUID;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

class ExceptionJsonResponseTest {

    @Test
    @DisplayName("equalsVerifier")
    void equalsVerifier() {
        EqualsVerifier.forClass(ExceptionJsonResponse.class).verify();
    }

    @Test
    @DisplayName("asJsonNodeResponseEntity() returns the correct message")
    void asJsonNodeResponseEntityReturnsTheCorrectMessage() {
        final UUID uuid = UUID.randomUUID();
        final Response response = new ExceptionJsonResponse(
                new ExceptionEvent(
                        new ExceptionEventContext(
                                new HeaderInfo(
                                        new XForwardedForStub(),
                                        new XForwardedHostStub(),
                                        new XForwardedProtoStub()
                                ),
                                "user-agent",
                                "uriPath",
                                "host"
                        ),
                        uuid,
                        new Throwable()
                )
        );

        final String expectedString = "{\"message\":\"An error occurred while processing your Request. See event id in the technical log for details.\",\"id\":\""
                + uuid + "\"}";

        final JsonNode actualJsonNode = response.asJsonNodeResponseEntity().getBody();
        Assertions.assertNotNull(actualJsonNode);
        Assertions.assertEquals(expectedString, actualJsonNode.toString());
    }

    @Test
    @DisplayName("asJsonNodeResponseEntity() returns the correct status")
    void asJsonNodeResponseEntityReturnsTheCorrectStatus() {
        final UUID uuid = UUID.randomUUID();

        final Response response = new ExceptionJsonResponse(
                new ExceptionEvent(
                        new ExceptionEventContext(
                                new HeaderInfo(
                                        new XForwardedForStub(),
                                        new XForwardedHostStub(),
                                        new XForwardedProtoStub()
                                ),
                                "user-agent",
                                "uriPath",
                                "host"
                        ),
                        uuid,
                        new Throwable()
                )
        );

        final HttpStatus expectedStatusCode = HttpStatus.BAD_REQUEST;
        final HttpStatusCode actualHttpStatus = response.asJsonNodeResponseEntity().getStatusCode();
        Assertions.assertEquals(expectedStatusCode, actualHttpStatus);
    }

    @Test
    @DisplayName("contentType() returns the content type")
    void contentTypeReturnsTheContentType() {
        final Response response = new ExceptionJsonResponse(
                new ExceptionEvent(
                        new ExceptionEventContext(
                                new HeaderInfo(
                                        new XForwardedForStub(),
                                        new XForwardedHostStub(),
                                        new XForwardedProtoStub()
                                ),
                                "user-agent",
                                "uriPath",
                                "host"
                        ),
                        UUID.randomUUID(),
                        new Throwable()
                )
        );

        final ResponseEntity<JsonNode> responseEntity = response.asJsonNodeResponseEntity();
        final List<String> actualContentTypeHeader = responseEntity.getHeaders().get("Content-Type");
        Assertions.assertNotNull(actualContentTypeHeader);
        Assertions.assertEquals("application/json", actualContentTypeHeader.get(0));
    }
}
