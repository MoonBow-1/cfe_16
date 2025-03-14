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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HECTimeImplTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    public void initialize() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Time is generated, not parsed and uses the defaultValue when time is missing from the event")
    void timeIsGeneratedNotParsedAndUsesTheDefaultValueWhenTimeIsMissingFromTheEvent() throws JsonProcessingException {
        String content = "{}";

        final JsonNode jsonNode = objectMapper.readTree(content);

        final long currentEpoch = Instant.now().toEpochMilli();

        final HECTime HECTime = new HECTimeImpl(jsonNode);

        Assertions
                .assertAll(
                        () -> Assertions
                                .assertEquals(
                                        "generated", HECTime.source(),
                                        "Time source should be 'generated' when it's not specified in a request"
                                ),
                        () -> Assertions
                                .assertFalse(
                                        HECTime.parsed(),
                                        "timeParsed should be false when time is not specified in a request"
                                ),
                        () -> Assertions.assertEquals(currentEpoch, HECTime.instant(currentEpoch), "Time as long should be the defaultValue provided when time is not specified in a request")
                );
    }

    @Test
    @DisplayName("Time is reported and parsed when time is a double")
    void timeIsReportedAndParsedWhenTimeIsADouble() throws JsonProcessingException {
        String content = "1433188255.253";

        final JsonNode jsonNode = objectMapper.readTree(content);
        final long currentEpoch = Instant.now().toEpochMilli();

        final HECTime HECTime = new HECTimeImpl(jsonNode);

        Assertions
                .assertAll(
                        () -> Assertions
                                .assertEquals(
                                        "reported", HECTime.source(),
                                        "Time source should be 'reported' when the time is specified in a request"
                                ),
                        () -> Assertions
                                .assertTrue(
                                        HECTime.parsed(),
                                        "timeParsed should be true when time is specified in a request"
                                ),
                        () -> Assertions.assertEquals(1433188255253L, HECTime.instant(currentEpoch), "Time should be converted to epoch milliseconds when it's provided in a request in " + "epoch seconds with decimals")
                );
    }

    @Test
    @DisplayName("Time is reported and parsed when time is exactly 13 digits")
    void timeIsReportedAndParsedWhenTimeIsExactly13Digits() throws JsonProcessingException {
        String content = "1433188255253";

        final JsonNode jsonNode = objectMapper.readTree(content);

        final long currentEpoch = Instant.now().toEpochMilli();
        final HECTime HECTime = new HECTimeImpl(jsonNode);

        Assertions
                .assertAll(
                        () -> Assertions
                                .assertEquals(
                                        "reported", HECTime.source(),
                                        "Time source should be 'reported' when the time is specified in a request"
                                ),
                        () -> Assertions
                                .assertTrue(
                                        HECTime.parsed(),
                                        "timeParsed should be true when time is specified in a request"
                                ),
                        () -> Assertions.assertEquals(1433188255253L, HECTime.instant(currentEpoch), "Time should be converted to epoch milliseconds when it's provided in a request in " + "epoch seconds with decimals")
                );
    }

    @Test
    @DisplayName("Time is reported and parsed when time is a string with numbers")
    void timeIsReportedAndParsedWhenTimeIsAStringWithNumbers() throws JsonProcessingException {
        String content = "\"1433188255253\"";

        final JsonNode jsonNode = objectMapper.readTree(content);

        final long currentEpoch = Instant.now().toEpochMilli();

        final HECTime HECTime = new HECTimeImpl(jsonNode);

        Assertions
                .assertAll(
                        () -> Assertions
                                .assertEquals(
                                        "reported", HECTime.source(),
                                        "Time source should be 'generated' when time is given as a string in a request"
                                ),
                        () -> Assertions
                                .assertTrue(
                                        HECTime.parsed(),
                                        "timeParsed should be false when time is given as a string in a request"
                                ),
                        () -> Assertions.assertEquals(1433188255253L, HECTime.instant(currentEpoch), "Time should be converted to long when time is given as a string in a request")
                );
    }

    @Test
    @DisplayName("Time is reported and parsed with less than 13 digits")
    void timeIsReportedAndParsedWithLessThan13Digits() throws JsonProcessingException {
        String content = "143318";

        final JsonNode jsonNode = objectMapper.readTree(content);

        final long currentEpoch = Instant.now().toEpochMilli();

        final HECTime HECTime = new HECTimeImpl(jsonNode);

        Assertions
                .assertAll(
                        () -> Assertions
                                .assertEquals(
                                        "reported", HECTime.source(),
                                        "Time source should be 'reported' when time is given as an integer with less "
                                                + "than 10" + " digits"
                                ),
                        () -> Assertions
                                .assertTrue(
                                        HECTime.parsed(),
                                        "timeParsed should be false when time is given as an integer with less than 10 "
                                                + "digits"
                                ),
                        () -> Assertions.assertEquals(143318L, HECTime.instant(currentEpoch), "Time as long should be as provided in the request")
                );
    }

    @Test
    @DisplayName("Time is reported and parsed when time is longer than 13 digits")
    void timeIsReportedAndParsedWhenTimeIsLongerThan13Digits() throws JsonProcessingException {
        String content = "14331882552523";

        final JsonNode jsonNode = objectMapper.readTree(content);
        final long currentEpoch = Instant.now().toEpochMilli();

        final HECTime HECTime = new HECTimeImpl(jsonNode);

        Assertions
                .assertAll(
                        () -> Assertions
                                .assertEquals(
                                        "reported", HECTime.source(),
                                        "Time source should be 'reported' when time is given as an integer with more "
                                                + "than 13 digits"
                                ),
                        () -> Assertions
                                .assertTrue(
                                        HECTime.parsed(),
                                        "timeParsed should be false when time is given as an integer with more than 13 "
                                                + "digits"
                                ),
                        () -> Assertions.assertEquals(14331882552523L, HECTime.instant(currentEpoch), "Time should be as it's provided in a request.")
                );
    }

    @Test
    @DisplayName("Happy equals test")
    void happyEqualsTest() throws JsonProcessingException {
        String content = "1433188255252321";

        final JsonNode jsonNode = objectMapper.readTree(content);

        final HECTime HECTime1 = new HECTimeImpl(jsonNode);
        final HECTime HECTime2 = new HECTimeImpl(jsonNode);

        Assertions.assertEquals(HECTime1, HECTime2);
    }

    @Test
    @DisplayName("Unhappy equals test")
    void unhappyEqualsTest() throws JsonProcessingException {
        String content1 = "1433188255252321";
        String content2 = "14331882552";

        final HECTime HECTime1 = new HECTimeImpl(objectMapper.readTree(content1));
        final HECTime HECTime2 = new HECTimeImpl(objectMapper.readTree(content2));

        Assertions.assertNotEquals(HECTime1, HECTime2);
    }
}
