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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import com.teragrep.cfe_16.bo.HECRecord;
import com.teragrep.cfe_16.bo.HECRecordImpl;
import com.teragrep.cfe_16.bo.HeaderInfo;
import com.teragrep.cfe_16.bo.XForwardedForStub;
import com.teragrep.cfe_16.bo.XForwardedHostStub;
import com.teragrep.cfe_16.bo.XForwardedProtoStub;
import com.teragrep.cfe_16.event.EventMessageImpl;
import com.teragrep.cfe_16.event.time.HECTimeImpl;
import com.teragrep.cfe_16.event.time.HECTimeImplWithFallback;
import com.teragrep.cfe_16.event.time.HECTimeStub;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HECBatchTest {

    private static final String channel1 = "CHANNEL_11111";
    private static final String authToken1 = "AUTH_TOKEN_12223";

    @Test
    public void toHECRecordListTest() {
        String allEventsInJson = "{\"sourcetype\": \"mysourcetype\", \"event\": \"Hello, world!\", \"host\": \"localhost\", \"source\": \"mysource\", \"index\": \"myindex\", \"time\": 123456}";
        HECRecord supposedResponse = new HECRecordImpl(
                channel1,
                new EventMessageImpl("Hello, world!"),
                authToken1,
                0,
                new HECTimeImplWithFallback(new HECTimeImpl(new ObjectMapper().createObjectNode().numberNode(123456)), new HECTimeStub()), new HeaderInfo(new XForwardedForStub(), new XForwardedHostStub(), new XForwardedProtoStub())
        );

        final HECBatch HECBatch = new HECBatch(
                authToken1,
                channel1,
                allEventsInJson,
                new HeaderInfo(new XForwardedForStub(), new XForwardedHostStub(), new XForwardedProtoStub())
        );
        List<HECRecord> response = HECBatch.toHECRecordList();

        // Test the individual methods, since HECTimeImplWithFallback will have a stub, which does not implement equals or hashcode
        Assertions.assertEquals(1, response.size());
        Assertions.assertEquals(supposedResponse.event(), response.get(0).event(), "Event was not the one expected");
        Assertions
                .assertEquals(supposedResponse.channel(), response.get(0).channel(), "Event was not the one expected");
        Assertions
                .assertEquals(supposedResponse.authenticationToken(), response.get(0).authenticationToken(), "Authentication token was not the one expected");
        // Use different defaultValues for instant() methods, to ensure they're not the same if they use the defaultValue
        Assertions
                .assertEquals(supposedResponse.time().instant(0L), response.get(0).time().instant(1L), "Time was not the one expected");
    }

    /**
     * Tests for JsonSyntaxException
     */
    @Test
    public void toHECRecordListUsesAStubIfParsingFailsWithMalformedJSONTest() {
        String allEventsInJson = "{\"sourcetype\": \"mysourcetype\", \"event\": {{{{}}}}";
        final HECBatch HECBatch = new HECBatch(
                authToken1,
                channel1,
                allEventsInJson,
                new HeaderInfo(new XForwardedForStub(), new XForwardedHostStub(), new XForwardedProtoStub())
        );

        Assertions.assertThrowsExactly(JsonSyntaxException.class, () -> HECBatch.toHECRecordList().toString());
    }

    /**
     * Tests for EventStub existence, since the Event should not be valid
     */
    @Test
    public void toHECRecordListUsesAStubIfParsingFailsWithEmptyJSONTest() {
        String allEventsInJson = "{\"sourcetype\": \"mysourcetype\", \"event\": null}";
        String supposedResponse = "EventStub does not support this";
        final HECBatch HECBatch = new HECBatch(
                authToken1,
                channel1,
                allEventsInJson,
                new HeaderInfo(new XForwardedForStub(), new XForwardedHostStub(), new XForwardedProtoStub())
        );
        Exception exception = Assertions
                .assertThrowsExactly(UnsupportedOperationException.class, () -> HECBatch.toHECRecordList().toString());
        Assertions
                .assertEquals(
                        supposedResponse, exception.getMessage(), "Exception message was not what it was supposed to be"
                );
    }

    @Test
    public void noEventFieldInRequestTest() {
        String allEventsInJson = "{\"sourcetype\": \"mysourcetype\", \"host\": \"localhost\", \"source\": \"mysource\", \"index\": \"myindex\"}";
        final HECBatch HECBatch = new HECBatch(
                authToken1,
                channel1,
                allEventsInJson,
                new HeaderInfo(new XForwardedForStub(), new XForwardedHostStub(), new XForwardedProtoStub())
        );

        Assertions.assertThrows(UnsupportedOperationException.class, () -> HECBatch.toHECRecordList().toString());
    }

    @Test
    public void eventFieldBlankInRequestTest() {
        String allEventsInJson = "{\"sourcetype\": \"mysourcetype\", \"event\": \"\", \"host\": \"localhost\", \"source\": \"mysource\", \"index\": \"myindex\"}";
        final HECBatch HECBatch = new HECBatch(
                authToken1,
                channel1,
                allEventsInJson,
                new HeaderInfo(new XForwardedForStub(), new XForwardedHostStub(), new XForwardedProtoStub())
        );

        Assertions.assertThrows(UnsupportedOperationException.class, () -> HECBatch.toHECRecordList().toString());
    }
}
