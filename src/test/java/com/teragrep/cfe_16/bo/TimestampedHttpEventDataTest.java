/*
 * HTTP Event Capture to RFC5424 CFE_16
 * Copyright (C) 2019-2025 Suomen Kanuuna Oy
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TimestampedHttpEventDataTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    public void initialize() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Test handleTime with epoch seconds, 10 digits")
    void testHandleTimeWithEpochSeconds10Digits() {
        String content = "{\"event\": \"Pony 1 has left the barn\", \"sourcetype\": "
            + "\"mysourcetype\", \"time\": 1277464192}";
        TimestampedHttpEventData eventData = new TimestampedHttpEventData();

        JsonNode jsonNode;

        try {
            jsonNode = objectMapper.readTree(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final TimestampedHttpEventData httpEventDataWithHandledTime = eventData.handleTime(
            jsonNode,
            null
        );
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                "reported",
                httpEventDataWithHandledTime.getTimeSource(),
                "Time source should be 'reported' when the time is specified in a request"),
            () -> Assertions.assertTrue(
                httpEventDataWithHandledTime.isTimeParsed(),
                "timeParsed should be true when the time is specified in a request"),
            () -> Assertions.assertEquals(
                1277464192000L,
                httpEventDataWithHandledTime.getTimeAsLong(),
                "Time should have been converted to epoch milliseconds")
        );
    }

    @Test
    @DisplayName("Test handleTime with epoch seconds, no time")
    void testHandleTimeWithEpochSecondsNoTime() {
        String content = "{\"event\": \"Pony 1 has left the barn\", \"sourcetype\": "
            + "\"mysourcetype\"}";
        TimestampedHttpEventData eventData = new TimestampedHttpEventData();

        JsonNode jsonNode;

        try {
            jsonNode = objectMapper.readTree(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final TimestampedHttpEventData httpEventDataWithHandledTime = eventData.handleTime(
            jsonNode,
            null
        );

        Assertions.assertAll(
            () -> Assertions.assertEquals(
                "generated",
                httpEventDataWithHandledTime.getTimeSource(),
                "Time source should be 'generated' when it's not specified in a request"),
            () -> Assertions.assertFalse(
                httpEventDataWithHandledTime.isTimeParsed(),
                "timeParsed should be false when time is not specified in a request"),
            () -> Assertions.assertEquals(
                0,
                httpEventDataWithHandledTime.getTimeAsLong(),
                "Time as long should be 0 when time is not specified in a request")
        );
    }

    @Test
    @DisplayName("Test handleTime with epoch seconds and decimal milliseconds")
    void testHandleTimeWithEpochSecondsAndDecimalMilliseconds() {
        String content = "{\"event\": \"Pony 1 has left the barn\", \"sourcetype\": "
            + "\"mysourcetype\", \"time\": 1433188255.253}";
        TimestampedHttpEventData eventData = new TimestampedHttpEventData();

        JsonNode jsonNode;

        try {
            jsonNode = objectMapper.readTree(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final TimestampedHttpEventData httpEventDataWithHandledTime = eventData.handleTime(
            jsonNode,
            null
        );

        Assertions.assertAll(
            () -> Assertions.assertEquals(
                "reported",
                httpEventDataWithHandledTime.getTimeSource(),
                "Time source should be 'reported' when the time is specified in a request"),
            () -> Assertions.assertTrue(
                httpEventDataWithHandledTime.isTimeParsed(),
                "timeParsed should be true when time is specified in a request"),
            () -> Assertions.assertEquals(
                1433188255253L,
                httpEventDataWithHandledTime.getTimeAsLong(),
                "Time should be converted to epoch milliseconds when it's provided in a request in "
                    + "epoch seconds with decimals"
            )
        );
    }

    @Test
    @DisplayName("Test handleTime with epoch milliseconds, 13 digits")
    void testHandleTimeWithEpochMilliseconds13digits() {
        String content = "{\"event\": \"Pony 1 has left the barn\", "
            + "\"sourcetype\":\"mysourcetype\", \"time\": 1433188255253}";
        TimestampedHttpEventData eventData = new TimestampedHttpEventData();

        JsonNode jsonNode;

        try {
            jsonNode = objectMapper.readTree(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final TimestampedHttpEventData httpEventDataWithHandledTime = eventData.handleTime(
            jsonNode,
            null
        );

        Assertions.assertAll(
            () -> Assertions.assertEquals(
                "reported",
                httpEventDataWithHandledTime.getTimeSource(),
                "Time source should be 'reported' when the time is specified in a request"),
            () -> Assertions.assertTrue(
                httpEventDataWithHandledTime.isTimeParsed(),
                "timeParsed should be true when time is specified in a request"),
            () -> Assertions.assertEquals(
                1433188255253L,
                httpEventDataWithHandledTime.getTimeAsLong(),
                "Time should be converted to epoch milliseconds when it's provided in a request in "
                    + "epoch seconds with decimals")
        );
    }

    @Test
    @DisplayName("Test handleTime with time as String")
    void testHandleTimeWithTimeAsString() {
        String content = "{\"event\": \"Pony 1 has left the barn\", "
            + "\"sourcetype\":\"mysourcetype\", \"time\": \"1433188255253\"}";
        TimestampedHttpEventData eventData = new TimestampedHttpEventData();

        JsonNode jsonNode;

        try {
            jsonNode = objectMapper.readTree(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final TimestampedHttpEventData httpEventDataWithHandledTime = eventData.handleTime(
            jsonNode,
            null
        );

        Assertions.assertAll(
            () -> Assertions.assertEquals(
                "generated",
                httpEventDataWithHandledTime.getTimeSource(),
                "Time source should be 'generated' when time is given as a string in a request"),
            () -> Assertions.assertFalse(
                httpEventDataWithHandledTime.isTimeParsed(),
                "timeParsed should be false when time is given as a string in a request"),
            () -> Assertions.assertEquals(
                0,
                httpEventDataWithHandledTime.getTimeAsLong(),
                "Time should be 0 when time is given as a string in a request")
        );
    }

    @Test
    @DisplayName("Test handleTime with too little digits")
    void testHandleTimeWithTooLittleDigits() {
        String content = "{\"event\": \"Pony 1 has left the barn\", "
            + "\"sourcetype\":\"mysourcetype\", \"time\": 143318}";
        TimestampedHttpEventData eventData = new TimestampedHttpEventData();

        JsonNode jsonNode;

        try {
            jsonNode = objectMapper.readTree(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final TimestampedHttpEventData httpEventDataWithHandledTime = eventData.handleTime(
            jsonNode,
            null
        );

        Assertions.assertAll(
            () -> Assertions.assertEquals(
                "generated",
                httpEventDataWithHandledTime.getTimeSource(),
                "Time source should be 'generated' when time is given as an integer with less "
                    + "than 10"
                    + " digits"),
            () -> Assertions.assertFalse(
                httpEventDataWithHandledTime.isTimeParsed(),
                "timeParsed should be false when time is given as an integer with less than 10 "
                    + "digits"),
            () -> Assertions.assertEquals(
                143318,
                httpEventDataWithHandledTime.getTimeAsLong(),
                "Time as long should be as provided in the request")
        );
    }

    @Test
    @DisplayName("Test handleTime with epoch centiseconds")
    void testHandleTimeWithEpochCentiseconds() {
        String content = "{\"event\": \"Pony 1 has left the barn\", "
            + "\"sourcetype\":\"mysourcetype\", \"time\": 143318825525}";
        TimestampedHttpEventData eventData = new TimestampedHttpEventData();

        JsonNode jsonNode;

        try {
            jsonNode = objectMapper.readTree(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final TimestampedHttpEventData httpEventDataWithHandledTime = eventData.handleTime(
            jsonNode,
            null
        );
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                "reported",
                httpEventDataWithHandledTime.getTimeSource(),
                "Time source should be 'reported' when the time is specified in a request "
                    + "with 10-13 "
                    + "digits"),
            () -> Assertions.assertTrue(
                httpEventDataWithHandledTime.isTimeParsed(),
                "timeParsed should be true when time is specified in a request with 10-13 digits"),
            () -> Assertions.assertEquals(
                1433188255250L,
                httpEventDataWithHandledTime.getTimeAsLong(),
                "Time should be converted to epoch milliseconds when provided in a request with "
                    + "10-13 digits")
        );
    }

    @Test
    @DisplayName("Test handleTime with too many digits")
    void testHandleTimeWithTooManyDigits() {
        String content = "{\"event\": \"Pony 1 has left the barn\", "
            + "\"sourcetype\":\"mysourcetype\", \"time\": 1433188255252321}";
        TimestampedHttpEventData eventData = new TimestampedHttpEventData();

        JsonNode jsonNode;

        try {
            jsonNode = objectMapper.readTree(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final TimestampedHttpEventData httpEventDataWithHandledTime = eventData.handleTime(
            jsonNode,
            null
        );
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                "generated",
                httpEventDataWithHandledTime.getTimeSource(),
                "Time source should be 'generated' when time is given as an integer with more "
                    + "than 13 digits"),
            () -> Assertions.assertFalse(
                httpEventDataWithHandledTime.isTimeParsed(),
                "timeParsed should be false when time is given as an integer with more than 13 "
                    + "digits"),
            () -> Assertions.assertEquals(
                1433188255252321L,
                httpEventDataWithHandledTime.getTimeAsLong(),
                "Time should be as it's provided in a request."
            )
        );
    }
}