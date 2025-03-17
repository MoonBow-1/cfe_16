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
            time = String.valueOf(this.timeAsLong);
            timeAsLong = new EpochTimeString(
                time,
                removeDecimal(timeObject.asDouble())
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
