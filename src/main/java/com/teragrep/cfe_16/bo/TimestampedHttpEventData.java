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
import java.math.BigDecimal;

public final class TimestampedHttpEventData implements HttpEventData {

    private final DefaultHttpEventData eventData;
    private final String timeSource;
    private final String time;
    private final long timeAsLong;
    private final boolean timeParsed;

    public TimestampedHttpEventData(
        DefaultHttpEventData eventData,
        String timeSource,
        String time,
        long timeAsLong,
        boolean timeParsed
    ) {
        this.eventData = eventData;
        this.timeSource = timeSource;
        this.time = time;
        this.timeAsLong = timeAsLong;
        this.timeParsed = timeParsed;
    }

    /**
     *
     */
    public TimestampedHttpEventData(final DefaultHttpEventData defaultHttpEventData) {
        this(
            defaultHttpEventData,
            "timeSource",
            "time",
            0L,
            false
        );
    }

    /**
     *
     */
    public TimestampedHttpEventData() {
        this(
            new DefaultHttpEventData()
        );
    }


    public TimestampedHttpEventData handleTime(
        final JsonNode jsonObject,
        final TimestampedHttpEventData previousEvent
    ) {
        JsonNode timeObject = jsonObject.get("time");
        /*
         * If the time is given as a string rather than as a numeral value, the time is
         * handled in a same way as it is handled when time is not given in a request.
         */
        String timeSource;
        final String time;
        final long timeAsLong;
        boolean timeParsed;

        if (timeObject == null || timeObject.isTextual()) {
            timeParsed = false;
            timeSource = "generated";
            if (previousEvent != null) {
                if (previousEvent.isTimeParsed()) {
                    time = previousEvent.getTime();
                    timeAsLong = new EpochTimeString(
                        time,
                        previousEvent.getTimeAsLong()
                    ).asEpochMillis();
                    timeParsed = true;
                    timeSource = "reported";
                } else {
                    time = previousEvent.getTime();
                    timeAsLong = previousEvent.getTimeAsLong();
                }
            } else {
                time = null;
                timeAsLong = 0;
            }
            /*
             * If the time is given as epoch seconds with a decimal (example:
             * 1433188255.253), the decimal point must be removed and time is assigned to
             * HttpEventData object as a long value. convertTimeToEpochMillis() will check
             * that correct time format is used.
             */
        } else if (timeObject.isDouble()) {
            final long decimalRemoved = removeDecimal(timeObject.asDouble());
            time = String.valueOf(decimalRemoved);
            timeAsLong = new EpochTimeString(
                time,
                decimalRemoved
            ).asEpochMillis();
            timeParsed = true;
            timeSource = "reported";
            /*
             * If the time is given in a numeral value, it is assigned to HttpEventData
             * object as a long value. convertTimeToEpochMillis() will check that correct
             * time format is used.
             */
        } else if (timeObject.canConvertToLong()) {
            time = jsonObject.get("time").asText();
            timeAsLong = new EpochTimeString(time, timeObject.asLong()).asEpochMillis();
            timeParsed = true;
            timeSource = "reported";
        } else {
            time = previousEvent.getTime();
            timeAsLong = previousEvent.getTimeAsLong();
            timeParsed = false;
            timeSource = "generated";
        }

        return new TimestampedHttpEventData(
            this.eventData,
            timeSource,
            time,
            timeAsLong,
            timeParsed
        );
    }

    /**
     * Takes a double value as a parameter, removes the decimal point from that value and returns
     * the number as a long value.
     */
    private long removeDecimal(double doubleValue) {
        BigDecimal doubleValueWithDecimal = BigDecimal.valueOf(doubleValue);
        String stringValue = doubleValueWithDecimal.toString();
        String stringValueWithoutDecimal = stringValue.replace(".", "");

        return Long.parseLong(stringValueWithoutDecimal);
    }

    public DefaultHttpEventData eventData() {
        return eventData;
    }

    public String getEvent() {
        return this.eventData.getEvent();
    }

    public String getChannel() {
        return this.eventData.getChannel();
    }

    public String getAuthenticationToken() {
        return this.eventData.getAuthenticationToken();
    }

    public String getTimeSource() {
        if (this.time == null) {
            return "generated";
        }
        if (this.time.length() < 10 || this.time.length() > 13) {
            return "generated";
        }
        return this.timeSource;
    }

    public String getTime() {
        return this.time;
    }

    public long getTimeAsLong() {
        return this.timeAsLong;
    }

    public boolean isTimeParsed() {
        if (this.time == null) {
            return false;
        }
        if (this.time.length() < 10 || this.time.length() > 13) {
            return false;
        }
        return this.timeParsed;
    }
}
