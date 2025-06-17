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
package com.teragrep.cfe_16.sender;

import com.cloudbees.syslog.SyslogMessage;
import com.teragrep.rlp_01.RelpBatch;
import com.teragrep.rlp_01.RelpConnection;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public final class RelpSender extends AbstractSender {

    //settings for timeouts, if they are 0 that we skip them
    //default are 0
    private static final int RECONNECT_INTERVAL = 500;
    private static final int CONNECTION_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 15000;
    private static final int WRITE_TIMEOUT = 5000;
    private final RelpConnection sender;
    private static final Logger LOGGER = LoggerFactory.getLogger(RelpSender.class);

    public RelpSender(String hostname, int port) {
        super(hostname, port);
        this.sender = new RelpConnection();
        sender.setConnectionTimeout(CONNECTION_TIMEOUT);
        sender.setReadTimeout(READ_TIMEOUT);
        sender.setWriteTimeout(WRITE_TIMEOUT);
        connect();
    }

    synchronized private void connect() {
        boolean notConnected = true;
        while (notConnected) {
            boolean connected = false;
            try {
                LOGGER.debug("Connecting to RELP server");
                connected = this.sender.connect(this.hostname, this.port);
            }
            catch (Exception e) {
                LOGGER.warn("Failed to connect to RELP Server: ", e);
            }
            if (connected) {
                notConnected = false;
            }
            else {
                try {
                    LOGGER.debug("Sleeping for <[{}]> before reconnecting", RECONNECT_INTERVAL);
                    Thread.sleep(RECONNECT_INTERVAL);
                }
                catch (InterruptedException e) {
                    LOGGER.warn("Sleep interrupted: ", e);
                }
            }
        }
    }

    synchronized private void tearDown() {
        LOGGER.debug("Tearing down connection");
        this.sender.tearDown();
    }

    synchronized private void disconnect() {
        try {
            LOGGER.debug("Disconnecting from RELP server");
            this.sender.disconnect();
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
    synchronized public void sendMessages(SyslogMessage[] syslogMessages) throws IOException {
        final RelpBatch relpBatch = new RelpBatch();
        for (SyslogMessage syslogMessage : syslogMessages) {
            relpBatch.insert(syslogMessage.toRfc5424SyslogMessage().getBytes("UTF-8"));
        }
        doSend(relpBatch);
    }

    @Override
    synchronized public void sendMessage(SyslogMessage syslogMessage) throws IOException {
        final RelpBatch relpBatch = new RelpBatch();
        relpBatch.insert(syslogMessage.toRfc5424SyslogMessage().getBytes("UTF-8"));
        doSend(relpBatch);
    }

    synchronized private void doSend(RelpBatch relpBatch) {
        boolean notSent = true;

        while (notSent) {
            try {
                LOGGER.debug("Committing a RELP batch");
                this.sender.commit(relpBatch);
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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RelpSender that = (RelpSender) o;
        return Objects.equals(defaultAppName, that.defaultAppName) && Objects
                .equals(defaultFacility, that.defaultFacility)
                && Objects.equals(defaultMessageHostname, that.defaultMessageHostname) && Objects.equals(defaultSeverity, that.defaultSeverity) && Objects.equals(messageFormat, that.messageFormat) && Objects.equals(sendCounter, that.sendCounter) && Objects.equals(sendDurationInNanosCounter, that.sendDurationInNanosCounter) && Objects.equals(sendErrorCounter, that.sendErrorCounter) && Objects.equals(hostname, that.hostname) && Objects.equals(port, that.port) && Objects.equals(sender, that.sender);
    }

    @Override
    public int hashCode() {
        return Objects
                .hash(
                        defaultAppName, defaultFacility, defaultMessageHostname, defaultSeverity, messageFormat,
                        sendCounter, sendDurationInNanosCounter, sendErrorCounter, hostname, port, sender
                );
    }
}
