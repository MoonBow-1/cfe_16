package com.teragrep.cfe_16.bo;

public final class EpochTimeString {
    private final String timeString;
    private final long timeAsLong;

    public EpochTimeString(
        String timeString,
        long timeAsLong
    ) {
        this.timeString = timeString;
        this.timeAsLong = timeAsLong;
    }

    /**
     * Converts the given time stamp into epoch milliseconds. Takes a HttpEventData object as a
     * parameter. Gets the time from the variable set in the HttpEventData object. If the time value
     * in the object has 13 digits, it means that time has been already given in epoch
     * milliseconds.
     */
    public long asEpochMillis()  {
        if (timeString.length() >= 10 && timeString.length() < 13) {
            return this.timeAsLong * (long) Math.pow(10, ((13 - timeString.length())));
        } else {
            return timeAsLong; // Time should already be in Epoch milliseconds
        }
    }
}
