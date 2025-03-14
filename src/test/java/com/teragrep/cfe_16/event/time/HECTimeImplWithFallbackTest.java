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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HECTimeImplWithFallbackTest {

    @Test
    @DisplayName("instant() returns the defaultValue if currentTime uses the defaultValue and fallbackTime is a stub")
    void instantReturnsTheDefaultValueIfCurrentTimeUsesTheDefaultValueAndFallbackTimeIsAStub() {
        final String content = "{}";
        final JsonNode jsonNode = Assertions.assertDoesNotThrow(() -> new ObjectMapper().readTree(content));

        final HECTime currentTimeWithTrueParsedValue = new HECTimeImpl(jsonNode);

        final HECTimeImplWithFallback hecTimeImplWithFallback = new HECTimeImplWithFallback(
                currentTimeWithTrueParsedValue,
                new HECTimeStub()
        );
        final long currentEpoch = Instant.now().toEpochMilli();

        Assertions.assertEquals(currentEpoch, hecTimeImplWithFallback.instant(currentEpoch));
    }

    @Test
    @DisplayName("instant() returns the non-default value from currentTime")
    void instantReturnsTheNonDefaultValueFromCurrentTime() {
        final String content = "1433188255.253";
        final JsonNode jsonNode = Assertions.assertDoesNotThrow(() -> new ObjectMapper().readTree(content));

        final HECTime currentTimeWithTrueParsedValue = new HECTimeImpl(jsonNode);

        final HECTimeImplWithFallback hecTimeImplWithFallback = new HECTimeImplWithFallback(
                currentTimeWithTrueParsedValue,
                new HECTimeStub()
        );

        final long currentEpoch = Instant.now().toEpochMilli();
        final long expectedTime = 1433188255253L;
        Assertions.assertEquals(expectedTime, hecTimeImplWithFallback.instant(currentEpoch));
    }

    @Test
    @DisplayName(
        "instant() returns the instant() value from the fallbackTime is currentTime.instant() is teh defaultValue"
    )
    void instantReturnsTheInstantValueFromTheFallbackTimeIsCurrentTimeInstantIsTehDefaultValue() {
        final String jsonThatWillBeDefaultTime = "{}";
        final String usableJsonTime = "1433188255.253";

        final JsonNode jsonNode1 = Assertions
                .assertDoesNotThrow(() -> new ObjectMapper().readTree(jsonThatWillBeDefaultTime));
        final JsonNode jsonNode2 = Assertions.assertDoesNotThrow(() -> new ObjectMapper().readTree(usableJsonTime));

        final HECTime currentTime = new HECTimeImpl(jsonNode1);
        final HECTime fallbackTime = new HECTimeImpl(jsonNode2);

        final HECTimeImplWithFallback hecTimeImplWithFallback = new HECTimeImplWithFallback(currentTime, fallbackTime);

        final long currentEpoch = Instant.now().toEpochMilli();
        final long expectedTime = 1433188255253L;
        Assertions.assertEquals(expectedTime, hecTimeImplWithFallback.instant(currentEpoch));
    }

    @Test
    @DisplayName("parsed() returns false if current and fallback times are both stubs")
    void parsedReturnsFalseIfCurrentAndFallbackTimesAreBothStubs() {
        final HECTimeImplWithFallback hecTimeImplWithFallback = new HECTimeImplWithFallback(
                new HECTimeStub(),
                new HECTimeStub()
        );

        Assertions.assertFalse(hecTimeImplWithFallback.parsed());
    }

    @Test
    @DisplayName("parsed() returns the parsed() value from currentTime if it is not a stub and fallbackTime is a stub")
    void parsedReturnsTheParsedValueFromCurrentTimeIfItIsNotAStubAndFallbackTimeIsAStub() {
        final String content = "1433188255.253";
        final JsonNode jsonNode = Assertions.assertDoesNotThrow(() -> new ObjectMapper().readTree(content));

        final HECTime currentTimeWithTrueParsedValue = new HECTimeImpl(jsonNode);

        final HECTimeImplWithFallback hecTimeImplWithFallback = new HECTimeImplWithFallback(
                currentTimeWithTrueParsedValue,
                new HECTimeStub()
        );

        Assertions.assertTrue(hecTimeImplWithFallback.parsed());
    }

    @Test
    @DisplayName("parsed() returns the parsed() value from the fallback time if the currentTime is a stub")
    void parsedReturnsTheParsedValueFromTheFallbackTimeIfTheCurrentTimeIsAStub() {
        final String content = "1433188255.253";
        final JsonNode jsonNode = Assertions.assertDoesNotThrow(() -> new ObjectMapper().readTree(content));

        final HECTime fallbackTimeWithTrueParsedValue = new HECTimeImpl(jsonNode);

        final HECTimeImplWithFallback hecTimeImplWithFallback = new HECTimeImplWithFallback(
                new HECTimeStub(),
                fallbackTimeWithTrueParsedValue
        );

        Assertions.assertTrue(hecTimeImplWithFallback.parsed());
    }

    @Test
    @DisplayName("source() returns \"generated\" if current and fallback times are both stubs")
    void sourceReturnsGeneratedIfCurrentAndFallbackTimesAreBothStubs() {
        final HECTimeImplWithFallback hecTimeImplWithFallback = new HECTimeImplWithFallback(
                new HECTimeStub(),
                new HECTimeStub()
        );

        Assertions.assertEquals("generated", hecTimeImplWithFallback.source());
    }

    @Test
    @DisplayName(
        "source() returns the source() value from the current time if it is not a stub and the fallback time is a stub"
    )
    void sourceReturnsTheSourceValueFromTheCurrentTimeIfItIsNotAStubAndTheFallbackTimeIsAStub() {
        final String content = "1433188255.253";
        final JsonNode jsonNode = Assertions.assertDoesNotThrow(() -> new ObjectMapper().readTree(content));

        final HECTime currentTime = new HECTimeImpl(jsonNode);

        final HECTimeImplWithFallback hecTimeImplWithFallback = new HECTimeImplWithFallback(
                currentTime,
                new HECTimeStub()
        );

        Assertions.assertEquals("reported", hecTimeImplWithFallback.source());
    }

    @Test
    @DisplayName("source() returns the source() value from the fallback time if the current time is a stub")
    void sourceReturnsTheSourceValueFromTheFallbackTimeIfTheCurrentTimeIsAStub() {
        final String content = "1433188255.253";
        final JsonNode jsonNode = Assertions.assertDoesNotThrow(() -> new ObjectMapper().readTree(content));

        final HECTime fallbackTime = new HECTimeImpl(jsonNode);

        final HECTimeImplWithFallback hecTimeImplWithFallback = new HECTimeImplWithFallback(
                new HECTimeStub(),
                fallbackTime
        );

        Assertions.assertEquals("reported", hecTimeImplWithFallback.source());
    }

    @Test
    @DisplayName("happy equals test")
    void happyEqualsTest() {
        final String content = "1433188255.253";
        final JsonNode jsonNode = Assertions.assertDoesNotThrow(() -> new ObjectMapper().readTree(content));

        final HECTime currentTime = new HECTimeImpl(jsonNode);
        final HECTime fallbackTime = new HECTimeImpl(jsonNode);

        final HECTimeImplWithFallback hecTimeImplWithFallback1 = new HECTimeImplWithFallback(currentTime, fallbackTime);
        final HECTimeImplWithFallback hecTimeImplWithFallback2 = new HECTimeImplWithFallback(currentTime, fallbackTime);

        Assertions.assertEquals(hecTimeImplWithFallback1, hecTimeImplWithFallback2);
    }

    @Test
    @DisplayName("unhappy equals test")
    void unhappyEqualsTest() {
        final String content1 = "1433188255.253";
        final String content2 = "1433188255";
        final JsonNode jsonNode1 = Assertions.assertDoesNotThrow(() -> new ObjectMapper().readTree(content1));
        final JsonNode jsonNode2 = Assertions.assertDoesNotThrow(() -> new ObjectMapper().readTree(content2));

        final HECTime time1 = new HECTimeImpl(jsonNode1);
        final HECTime time2 = new HECTimeImpl(jsonNode2);

        final HECTimeImplWithFallback hecTimeImplWithFallback1 = new HECTimeImplWithFallback(time1, time1);
        final HECTimeImplWithFallback hecTimeImplWithFallback2 = new HECTimeImplWithFallback(time1, time2);

        Assertions.assertNotEquals(hecTimeImplWithFallback1, hecTimeImplWithFallback2);
    }
}
