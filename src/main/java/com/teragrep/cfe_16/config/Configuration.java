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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * A Spring-utilizing class for getting configuration data.
 */

@org.springframework.context.annotation.Configuration
public class Configuration {

    @Value("${syslog.server.host}")
    private String syslogHost;

    @Value("${syslog.server.port}")
    private int syslogPort;

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

    public Configuration(
            final String syslogHost,
            final int syslogPort,
            final int maxAckValue,
            final int maxAckAge,
            final int maxSessionAge,
            final int maxChannels,
            final long pollTime,
            final boolean printTimes
    ) {
        this.syslogHost = syslogHost;
        this.syslogPort = syslogPort;
        this.maxAckValue = maxAckValue;
        this.maxAckAge = maxAckAge;
        this.maxSessionAge = maxSessionAge;
        this.maxChannels = maxChannels;
        this.pollTime = pollTime;
        this.printTimes = printTimes;
    }

    @Bean
    public String syslogHost() {
        return this.syslogHost;
    }

    @Bean
    public int syslogPort() {
        return this.syslogPort;
    }

    @Bean
    public int maxAckValue() {
        return this.maxAckValue;
    }

    public void setMaxAckValue(int maxAckValue) {
        this.maxAckValue = maxAckValue;
    }

    @Bean
    public int maxAckAge() {
        return this.maxAckAge;
    }

    @Bean
    public int maxChannels() {
        return this.maxChannels;
    }

    @Bean
    public long pollTime() {
        return this.pollTime;
    }

    @Bean
    public boolean printTimes() {
        return this.printTimes;
    }

    @Bean
    public int maxSessionAge() {
        return this.maxSessionAge;
    }

    @Override
    public String toString() {
        return "Configuration{" + "syslogHost=" + syslogHost + ", syslogPort=" + syslogPort + ", maxAckValue="
                + maxAckValue + ", maxAckAge=" + maxAckAge + ", maxSessionAge=" + maxSessionAge + ", maxChannels="
                + maxChannels + ", pollTime=" + pollTime + ", printTimes=" + printTimes + '}';
    }

}
