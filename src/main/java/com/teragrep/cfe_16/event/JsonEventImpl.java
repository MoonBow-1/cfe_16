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
package com.teragrep.cfe_16.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;

public final class JsonEventImpl implements JsonEvent {

    private final JsonNode jsonNode;
    private final EventMessageStub eventMessageStub;

    public JsonEventImpl(final JsonNode jsonNode, final EventMessageStub eventMessageStub) {
        this.jsonNode = jsonNode;
        this.eventMessageStub = eventMessageStub;
    }

    public JsonEventImpl(final JsonNode jsonNode) {
        this(jsonNode, new EventMessageStub());
    }

    @Override
    public EventMessage asEventMessage() {
        final EventMessage eventMessage;
        // Event field completely missing
        if (!this.asPayloadJsonNode().has("event")) {
            eventMessage = eventMessageStub;
        }
        // Event field contains subfield "message"
        else if (
            this.asPayloadJsonNode().get("event").isObject() && this.asPayloadJsonNode().get("event").has("message")
        ) {
            if (
                this.asPayloadJsonNode().get("event").get("message").isTextual()
                        && !Objects.equals(this.asPayloadJsonNode().get("event").get("message").asText(), "")
            ) {
                eventMessage = new EventMessageImpl(this.asPayloadJsonNode().get("event").get("message").asText());
            }
            else {
                eventMessage = eventMessageStub;
            }
        }
        // Event field has a String value
        else if (
            this.asPayloadJsonNode().get("event").isTextual()
                    && !Objects.equals(this.asPayloadJsonNode().get("event").asText(), "")
        ) {
            eventMessage = new EventMessageImpl(this.jsonNode.get("event").asText());
        }
        else {
            eventMessage = eventMessageStub;
        }

        return eventMessage;
    }

    @Override
    public JsonNode asPayloadJsonNode() {
        if (this.jsonNode != null && this.jsonNode.isObject()) {
            return this.jsonNode;
        }
        else {
            return new ObjectMapper().createObjectNode();
        }
    }

    @Override
    public JsonNode asTimeJsonNode() {
        return this.jsonNode.get("time");
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final JsonEventImpl that = (JsonEventImpl) o;
        return Objects.equals(jsonNode, that.jsonNode) && Objects.equals(eventMessageStub, that.eventMessageStub);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jsonNode, eventMessageStub);
    }
}
