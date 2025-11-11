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
package com.teragrep.cfe_16.bo;

import com.teragrep.cfe_16.event.EventMessage;

/**
 */
public final class HttpEventData {

    private String channel;
    private EventMessage event;
    private String authenticationToken;
    private String timeSource;
    private String time;
    private long timeAsLong;
    private boolean timeParsed;
    private Integer ackID;

    public EventMessage getEvent() {
        return event;
    }

    public void setEvent(final EventMessage event) {
        this.event = event;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getAuthenticationToken() {
        return authenticationToken;
    }

    public void setAuthenticationToken(String authenticationToken) {
        this.authenticationToken = authenticationToken;
    }

    public String getTimeSource() {
        return timeSource;
    }

    public void setTimeSource(String timeSource) {
        this.timeSource = timeSource;
    }

    public String getTime() {
        return time;
    }

    public long getTimeAsLong() {
        return timeAsLong;
    }

    public void setTimeAsLong(long timeAsLong) {
        this.timeAsLong = timeAsLong;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public boolean isTimeParsed() {
        return timeParsed;
    }

    public void setTimeParsed(boolean timeParsed) {
        this.timeParsed = timeParsed;
    }

    public Integer getAckID() {
        return ackID;
    }

    public void setAckID(Integer ackID) {
        this.ackID = ackID;
    }

    @Override
    public String toString() {
        return "HttpEventData [channel=" + channel + ", event=" + event + ", authenticationToken=" + authenticationToken
                + ", timeSource=" + timeSource + ", time=" + time + ", timeAsLong=" + timeAsLong + ", timeParsed="
                + timeParsed + ", ackID=" + ackID + "]";
    }
}
