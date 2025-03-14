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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Session keeps track of channels that are contained inside one Session. This class is not thread-safe.
 */
public class Session {

    public static final String DEFAULT_CHANNEL = "defaultchannel";
    private static final Logger LOGGER = LoggerFactory.getLogger(Session.class);
    /**
     * Channels of this Session object.
     */
    private Set<String> channels;

    /**
     * Authentication key of this Session.
     */
    private String authenticationToken;

    private long lastTouchedTimestamp;

    @SuppressWarnings("unchecked")
    public Session(String channel, String authenticationToken) {
        LOGGER.info("Creating new session with channel <{}>", channel);
        this.channels = Collections.synchronizedSet(new HashSet<String>());
        if (channel != null) {
            LOGGER.info("Adding channel <[{}]>", channel);
            this.channels.add(channel);
        }
        this.authenticationToken = authenticationToken;
        this.lastTouchedTimestamp = System.currentTimeMillis();
    }

    public Session(String authenticationToken) {
        this(null, authenticationToken);
    }

    public String getAuthenticationToken() {
        return authenticationToken;
    }

    public boolean addChannel(String channel) {
        return this.channels.add(channel);
    }

    public boolean doesChannelExist(String channel) {
        return this.channels.contains(channel);
    }

    public boolean removeChannel(String channel) {
        return this.channels.remove(channel);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.authenticationToken == null) ? 0 : this.authenticationToken.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Session other = (Session) obj;
        if (this.authenticationToken == null) {
            if (other.authenticationToken != null)
                return false;
        }
        else if (!this.authenticationToken.equals(other.authenticationToken))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "channels=" + this.channels + ", authenticationToken=" + this.authenticationToken + "]";
    }

    public void touch() {
        this.lastTouchedTimestamp = System.currentTimeMillis();
    }

    public long getLastTouchedTimestamp() {
        return this.lastTouchedTimestamp;
    }
}
