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
package com.teragrep.cfe_16.connection;

import com.cloudbees.syslog.SyslogMessage;
import com.cloudbees.syslog.sender.AbstractSyslogMessageSender;
import com.teragrep.rlp_01.RelpBatch;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public final class RelpConnection extends AbstractSyslogMessageSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelpConnection.class);
    private final com.teragrep.rlp_01.RelpConnection connection;
    private String hostname;
    private int port;

    // Unfortunately Spring requires an additional annotation here, since the Configuration class has multiple methods that return a String
    public RelpConnection(@Qualifier("syslogHost") final String syslogHost, final int syslogPort) {
        this.hostname = syslogHost;
        this.port = syslogPort;
        this.connection = new com.teragrep.rlp_01.RelpConnection();
        //settings for timeouts, if they are 0 that we skip them
        //default are 0
        this.connection.setConnectionTimeout(10000);
        this.connection.setReadTimeout(15000);
        this.connection.setWriteTimeout(5000);
    }

    @PostConstruct
    synchronized private void autoConnect() {
        this.connect();
    }

    synchronized private void connect() {
        boolean notConnected = true;
        while (notConnected) {
            boolean connected = false;
            try {
                LOGGER.debug("Connecting to RELP server");
                connected = this.connection.connect(this.hostname, this.port);
            }
            catch (Exception e) {
                LOGGER.warn("Failed to connect to RELP Server: ", e);
            }
            if (connected) {
                notConnected = false;
            }
            else {
                try {
                    final int reconnectInterval = 500;
                    LOGGER.debug("Sleeping for <[{}]> before reconnecting", reconnectInterval);
                    Thread.sleep(reconnectInterval);
                }
                catch (InterruptedException e) {
                    LOGGER.warn("Sleep interrupted: ", e);
                }
            }
        }
    }

    synchronized private void tearDown() {
        LOGGER.debug("Tearing down connection");
        this.connection.tearDown();
    }

    synchronized private void disconnect() {
        try {
            LOGGER.debug("Disconnecting from RELP server");
            this.connection.disconnect();
        }
        catch (IllegalStateException | IOException | TimeoutException e) {
            LOGGER.warn("Failed to disconnect from RELP Server: ", e);
        }
        finally {
            this.tearDown();
        }
    }

    @Override
    synchronized public void close() {
        this.disconnect();
    }

    @Override
    public void setSyslogServerHostname(final String syslogServerHostname) {
        this.hostname = syslogServerHostname;
    }

    @Override
    public void setSyslogServerPort(final int syslogServerPort) {
        this.port = syslogServerPort;
    }

    synchronized public void sendMessages(List<SyslogMessage> syslogMessages) throws IOException {
        final RelpBatch relpBatch = new RelpBatch();
        for (SyslogMessage syslogMessage : syslogMessages) {
            relpBatch.insert(syslogMessage.toRfc5424SyslogMessage().getBytes(StandardCharsets.UTF_8));
        }
        doSend(relpBatch);
    }

    @Override
    synchronized public void sendMessage(SyslogMessage syslogMessage) {
        final RelpBatch relpBatch = new RelpBatch();
        relpBatch.insert(syslogMessage.toRfc5424SyslogMessage().getBytes(StandardCharsets.UTF_8));
        doSend(relpBatch);
    }

    synchronized private void doSend(RelpBatch relpBatch) {
        boolean notSent = true;

        while (notSent) {
            try {
                LOGGER.debug("Committing a RELP batch");
                this.connection.commit(relpBatch);
            }
            catch (IllegalStateException | IOException | TimeoutException e) {
                LOGGER.warn("Failed to commit batch: ", e);
            }

            if (!relpBatch.verifyTransactionAll()) {
                LOGGER.debug("Failed to verify all transactions, retrying them");
                relpBatch.retryAllFailed();
                this.tearDown();
                this.connect();
            }
            else {
                notSent = false;
            }
        }
    }
}
