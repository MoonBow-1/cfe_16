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
package com.teragrep.cfe_16.config;

import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * A Spring-utilizing class for getting configuration data.
 */

@Component
public final class Configuration {

    @Value("${syslog.server.host}")
    private String sysLogHost;

    @Value("${syslog.server.protocol}")
    private String sysLogProtocol;

    @Value("${syslog.server.port}")
    private int sysLogPort;

    @Value("${max.ack.value}")
    private int maxAckValue;

    @Value("${max.ack.age}")
    private int maxAckAge;

    @Value("${max.session.age}")
    private int maxSessionAge;

    @Value("${max.channels}")
    private int maxChannels;

    @Value("${max.ack.value}")
    private long pollTime;

    @Value("${server.print.times}")
    private boolean printTimes;

    public Configuration() {

    }

    public String getSyslogHost() {
        return this.sysLogHost;
    }

    public String getSysLogProtocol() {
        return this.sysLogProtocol;
    }

    public void setSysLogProtocol(String sysLogProtocol) {
        this.sysLogProtocol = sysLogProtocol;
    }

    public void setSyslogHost(String syslogHost) {
        this.sysLogHost = syslogHost;
    }

    public int getSyslogPort() {
        return this.sysLogPort;
    }

    public void setSyslogPort(int syslogPort) {
        this.sysLogPort = syslogPort;
    }

    public int getMaxAckValue() {
        return this.maxAckValue;
    }

    public void setMaxAckValue(int maxAckValue) {
        this.maxAckValue = maxAckValue;
    }

    public int getMaxAckAge() {
        return this.maxAckAge;
    }

    public int getMaxChannels() {
        return this.maxChannels;
    }

    public long getPollTime() {
        return this.pollTime;
    }

    public boolean getPrintTimes() {
        return this.printTimes;
    }

    public int getMaxSessionAge() {
        return this.maxSessionAge;
    }

    @Override
    public String toString() {
        return "Configuration [sysLogHost=" + this.sysLogHost + ", sysLogProtocol=" + this.sysLogProtocol
                + ", sysLogPort=" + this.sysLogPort + ", maxAckValue=" + this.maxAckValue + ", maxAckAge="
                + this.maxAckAge + ", maxSessionAge=" + this.maxSessionAge + ", maxChannels=" + this.maxChannels
                + ", pollTime=" + this.pollTime + ", printTimes=" + this.printTimes + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Configuration that = (Configuration) o;
        return sysLogPort == that.sysLogPort
                && getMaxAckValue() == that.getMaxAckValue() && getMaxAckAge() == that.getMaxAckAge()
                && getMaxSessionAge() == that.getMaxSessionAge() && getMaxChannels() == that.getMaxChannels()
                && getPollTime() == that.getPollTime() && getPrintTimes() == that.getPrintTimes() && Objects
                        .equals(sysLogHost, that.sysLogHost)
                && Objects.equals(getSysLogProtocol(), that.getSysLogProtocol());
    }

    @Override
    public int hashCode() {
        return Objects
                .hash(
                        sysLogHost, sysLogProtocol, sysLogPort, maxAckValue, maxAckAge, maxSessionAge, maxChannels,
                        pollTime, printTimes
                );
    }
}
