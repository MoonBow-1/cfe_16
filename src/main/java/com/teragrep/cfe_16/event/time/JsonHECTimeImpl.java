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
package com.teragrep.cfe_16.event.time;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.Objects;

public final class JsonHECTimeImpl implements JsonHECTime {

    private final JsonNode timeNode;

    public JsonHECTimeImpl(final JsonNode timeNode) {
        this.timeNode = timeNode;
    }

    @Override
    public long asInstant(final long defaultValue) {
        final long instant;
        // No time provided in the event
        if (timeNode == null || timeNode.asText().isEmpty()) {
            // Use default value
            instant = defaultValue;
        }
        // Check if time is a double and convert to long
        else if (timeNode.isDouble()) {
            instant = this.removeDecimal(timeNode.asDouble());
        }
        // Time is a number, no calculations required
        else if (timeNode.canConvertToLong()) {
            instant = timeNode.asLong();
        }
        // Time is a String
        else if (timeNode.isTextual()) {
            // Try to convert the String to a long (if not convertable, default to 0L)
            final long tryAsLong = timeNode.asLong(0L);
            if (tryAsLong != 0L) {
                instant = tryAsLong;
            }
            // No time found in current or previous event
            else {
                // Use default value
                instant = defaultValue;
            }
        }
        // Unknown format
        else {
            // Use default value
            instant = defaultValue;
        }

        return instant;
    }

    /**
     * Takes a double value as a parameter, removes the decimal point from that value and returns the number as a long
     * value.
     */
    private long removeDecimal(final double doubleValue) {
        final BigDecimal doubleValueWithDecimal = BigDecimal.valueOf(doubleValue);
        final String stringValue = doubleValueWithDecimal.toString();
        final String stringValueWithoutDecimal = stringValue.replace(".", "");

        return Long.parseLong(stringValueWithoutDecimal);
    }

    @Override
    public boolean parsed() {
        final boolean parsed;
        // No time provided in the event
        if (timeNode == null || timeNode.asText().isEmpty()) {
            parsed = false;
        }
        // Check if time is a double and convert to long
        else if (timeNode.isDouble()) {
            parsed = true;

        }
        // Time is a number, no calculations required
        else if (timeNode.canConvertToLong()) {
            parsed = true;

        }
        // Time is a String
        else if (timeNode.isTextual()) {
            // Try to convert the String to a long (if not convertable, default to 0L)
            final long tryAsLong = timeNode.asLong(0L);
            parsed = tryAsLong != 0L;
        }
        // Unknown format
        else {
            parsed = false;
        }

        return parsed;
    }

    @Override
    public String source() {
        final String source;
        // No time provided in the event
        if (timeNode == null || timeNode.asText().isEmpty()) {
            // Use default value
            source = "generated";
        }
        // Check if time is a double and convert to long
        else if (timeNode.isDouble()) {
            source = "reported";
        }
        // Time is a String
        else if (timeNode.isTextual()) {
            // Try to convert the String to a long (if not convertable, default to 0L)
            final long tryAsLong = timeNode.asLong(0L);
            if (tryAsLong != 0L) {
                source = "reported";
            }
            // No time found in current or previous event
            else {
                // Use default value
                source = "generated";
            }
        }
        // Time is a number, no calculations required
        else if (timeNode.canConvertToLong()) {
            source = "reported";
        }
        // Unknown format
        else {
            // Use default value
            source = "generated";
        }

        return source;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final JsonHECTimeImpl hecTime = (JsonHECTimeImpl) o;
        return Objects.equals(timeNode, hecTime.timeNode);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(timeNode);
    }

    @Override
    public boolean isStub() {
        return false;
    }
}
