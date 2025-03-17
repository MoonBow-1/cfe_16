/*
 * HTTP Event Capture to RFC5424 CFE_16
 * Copyright (C) 2025 Suomen Kanuuna Oy
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
package com.teragrep.cfe_16.ThirdParty.SyslogMessageSender;

import com.cloudbees.syslog.SyslogMessage;
import com.cloudbees.syslog.sender.AbstractSyslogMessageSender;
import com.cloudbees.syslog.util.CachingReference;
import com.cloudbees.syslog.util.IoUtils;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * See <a href="http://tools.ietf.org/html/rfc6587">RFC 6587 - Transmission of Syslog Messages over
 * TCP</a>
 *
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class SyslogMessageSender extends AbstractSyslogMessageSender implements Closeable {

    public final static int SETTING_SOCKET_CONNECT_TIMEOUT_IN_MILLIS_DEFAULT_VALUE = 500;
    public final static int SETTING_MAX_RETRY = 2;
    /**
     * Number of exceptions trying to send message.
     */
    protected final AtomicInteger trySendErrorCounter = new AtomicInteger();
    /**
     * {@link java.net.InetAddress InetAddress} of the remote Syslog Server.
     *
     * The {@code InetAddress} is refreshed regularly to handle DNS changes (default
     * {@link #DEFAULT_INET_ADDRESS_TTL_IN_MILLIS})
     *
     * Default value: {@link #DEFAULT_SYSLOG_HOST}
     */
    protected CachingReference<InetAddress> syslogServerHostnameReference;
    /**
     * Listen port of the remote Syslog server.
     *
     * Default: {@link #DEFAULT_SYSLOG_PORT}
     */
    protected int syslogServerPort = DEFAULT_SYSLOG_PORT;
    private Socket socket;
    private Writer writer;
    private int socketConnectTimeoutInMillis =
        SETTING_SOCKET_CONNECT_TIMEOUT_IN_MILLIS_DEFAULT_VALUE;
    private boolean ssl;
    private SSLContext sslContext;
    /**
     * Number of retries to send a message before throwing an exception.
     */
    private int maxRetryCount = SETTING_MAX_RETRY;
    // use the CR LF non transparent framing as described in "3.4.2.  Non-Transparent-Framing"
    private String postfix = "\n";

    @Override
    public synchronized void sendMessage(SyslogMessage message) throws IOException {
        sendCounter.incrementAndGet();
        long nanosBefore = System.nanoTime();

        try {
            Exception lastException = null;
            for (int i = 0; i <= maxRetryCount; i++) {
                try {
                    if (logger.isLoggable(Level.FINEST)) {
                        logger.finest(
                            "Send syslog message " + message.toSyslogMessage(messageFormat));
                    }
                    ensureSyslogServerConnection();
                    message.toSyslogMessage(messageFormat, writer);
                    writer.write(postfix);
                    writer.flush();
                    return;
                } catch (IOException e) {
                    lastException = e;
                    IoUtils.closeQuietly(socket, writer);
                    trySendErrorCounter.incrementAndGet();
                } catch (RuntimeException e) {
                    lastException = e;
                    IoUtils.closeQuietly(socket, writer);
                    trySendErrorCounter.incrementAndGet();
                }
            }
            if (lastException != null) {
                sendErrorCounter.incrementAndGet();
                if (lastException instanceof IOException) {
                    throw (IOException) lastException;
                } else if (lastException instanceof RuntimeException) {
                    throw (RuntimeException) lastException;
                }
            }
        } finally {
            sendDurationInNanosCounter.addAndGet(System.nanoTime() - nanosBefore);
        }
    }

    private synchronized void ensureSyslogServerConnection() throws IOException {
        InetAddress inetAddress = syslogServerHostnameReference.get();
        if (socket != null && !Objects.equals(socket.getInetAddress(), inetAddress)) {
            logger.info("InetAddress of the Syslog Server have changed, create a new connection. " +
                "Before=" + socket.getInetAddress() + ", new=" + inetAddress);
            IoUtils.closeQuietly(socket, writer);
            writer = null;
            socket = null;
        }
        boolean socketIsValid;
        try {
            socketIsValid = socket != null &&
                socket.isConnected()
                && socket.isBound()
                && !socket.isClosed()
                && !socket.isInputShutdown()
                && !socket.isOutputShutdown();
        } catch (Exception e) {
            socketIsValid = false;
        }
        if (!socketIsValid) {
            writer = null;
            try {
                if (ssl) {
                    if (sslContext == null) {
                        socket = SSLSocketFactory.getDefault().createSocket();
                    } else {
                        socket = sslContext.getSocketFactory().createSocket();
                    }
                } else {
                    socket = SocketFactory.getDefault().createSocket();
                }
                socket.setKeepAlive(true);
                socket.connect(
                    new InetSocketAddress(inetAddress, syslogServerPort),
                    socketConnectTimeoutInMillis);

                if (socket instanceof SSLSocket && logger.isLoggable(Level.FINER)) {
                    try {
                        SSLSocket sslSocket = (SSLSocket) socket;
                        SSLSession session = sslSocket.getSession();
                        logger.finer("The Certificates used by peer");
                        for (Certificate certificate : session.getPeerCertificates()) {
                            if (certificate instanceof X509Certificate x509Certificate) {
                                logger.finer("" + x509Certificate.getSubjectDN());
                            } else {
                                logger.finer("" + certificate);
                            }
                        }
                        logger.finer("Peer host is " + session.getPeerHost());
                        logger.finer("Cipher is " + session.getCipherSuite());
                        logger.finer("Protocol is " + session.getProtocol());
                        logger.finer("ID is " + new BigInteger(session.getId()));
                        logger.finer("Session created in " + session.getCreationTime());
                        logger.finer("Session accessed in " + session.getLastAccessedTime());
                    } catch (Exception e) {
                        logger.warn("Exception dumping debug info for " + socket, e);
                    }
                }
            } catch (IOException e) {
                ConnectException ce = new ConnectException(
                    "Exception connecting to " + inetAddress + ":" + syslogServerPort);
                ce.initCause(e);
                throw ce;
            }
        }
        if (writer == null) {
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8));
        }
    }

    public String getSyslogServerHostname() {
        if (syslogServerHostnameReference == null) {return null;}
        InetAddress inetAddress = syslogServerHostnameReference.get();
        return inetAddress == null ? null : inetAddress.getHostName();
    }

    @Override
    public void setSyslogServerHostname(final String syslogServerHostname) {
        this.syslogServerHostnameReference = new CachingReference<InetAddress>(
            DEFAULT_INET_ADDRESS_TTL_IN_NANOS) {

            @Override
            protected InetAddress newObject() {
                try {
                    return InetAddress.getByName(syslogServerHostname);
                } catch (UnknownHostException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }

    public int getSyslogServerPort() {
        return syslogServerPort;
    }

    @Override
    public void setSyslogServerPort(int syslogServerPort) {
        this.syslogServerPort = syslogServerPort;
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public synchronized SSLContext getSSLContext() {
        return this.sslContext;
    }

    public synchronized void setSSLContext(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    public int getSocketConnectTimeoutInMillis() {
        return socketConnectTimeoutInMillis;
    }

    public void setSocketConnectTimeoutInMillis(int socketConnectTimeoutInMillis) {
        this.socketConnectTimeoutInMillis = socketConnectTimeoutInMillis;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public int getTrySendErrorCounter() {
        return trySendErrorCounter.get();
    }

    public synchronized void setPostfix(String postfix) {
        this.postfix = postfix;
    }

    @Override
    public String toString() {
        return getClass().getName() + "{" +
            "syslogServerHostname='" + this.getSyslogServerHostname() + '\'' +
            ", syslogServerPort='" + this.getSyslogServerPort() + '\'' +
            ", ssl=" + ssl +
            ", maxRetryCount=" + maxRetryCount +
            ", socketConnectTimeoutInMillis=" + socketConnectTimeoutInMillis +
            ", defaultAppName='" + defaultAppName + '\'' +
            ", defaultFacility=" + defaultFacility +
            ", defaultMessageHostname='" + defaultMessageHostname + '\'' +
            ", defaultSeverity=" + defaultSeverity +
            ", messageFormat=" + messageFormat +
            ", sendCounter=" + sendCounter +
            ", sendDurationInNanosCounter=" + sendDurationInNanosCounter +
            ", sendErrorCounter=" + sendErrorCounter +
            ", trySendErrorCounter=" + trySendErrorCounter +
            '}';
    }

    @Override
    public void close() throws IOException {
        this.socket.close();
    }
}
