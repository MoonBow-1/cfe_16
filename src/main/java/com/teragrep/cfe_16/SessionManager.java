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
package com.teragrep.cfe_16;

import com.teragrep.cfe_16.bo.Session;
import com.teragrep.cfe_16.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/*
 * Manager that handles creating sessions and getting already existing sessions.
 * Sessions are indexed by the authentication token.
 *
 */
@Component
public class SessionManager implements Runnable, LifeCycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionManager.class);
    /**
     * Maps auth token string => session object.
     */
    private final Map<String, Session> sessions;

    /**
     * Cleans up outdated Session objects.
     */
    private Thread cleanerThread;

    @Autowired
    private Configuration configuration;

    /**
     *
     */
    public SessionManager() {
        this.sessions = new HashMap<String, Session>();
    }

    @Override
    @PostConstruct
    public void start() {
        this.cleanerThread = new Thread(this, "Session cleaner");
        this.cleanerThread.start();
    }

    @Override
    public void stop() {
        this.cleanerThread.interrupt();
    }

    @Override
    public void run() {
        while (true) {
            try {
                LOGGER.debug("Sleeping for <{}>  while waiting for poll", this.configuration.getPollTime());
                Thread.sleep(this.configuration.getPollTime());
            }
            catch (InterruptedException e) {
                break;
            }
            synchronized (this) {
                Iterator<Map.Entry<String, Session>> iterator = this.sessions.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Session> entry = iterator.next();
                    long now = System.currentTimeMillis();
                    long thresholdInLong = entry.getValue().getLastTouchedTimestamp()
                            + this.configuration.getMaxSessionAge();
                    if (now >= thresholdInLong) {
                        iterator.remove();
                    }
                }
            }
        }
    }

    /*
     * Gets a session for provided authentication token. returns null if
     * there is no session for given authentication token
     */
    public Session getSession(String authenticationToken) {
        synchronized (this) {
            Session session = this.sessions.get(authenticationToken);
            return session;
        }
    }

    /**
     * Returns an existing Session object based on authentication token. If no Session exists, a new one is created.
     *
     * @param authenticationToken
     * @return
     */
    public Session getOrCreateSession(String authenticationToken) {
        LOGGER.debug("Getting or creating session");
        LOGGER.trace("Getting or creating session for authenticationToken: {}", authenticationToken);
        synchronized (this) {
            Session session = this.sessions.get(authenticationToken);
            if (session == null) {
                session = new Session(null, authenticationToken);
                this.sessions.put(authenticationToken, session);
            }
            return session;
        }
    }

    public void removeSession(String authenticationToken) {
        LOGGER.debug("Removing session");
        LOGGER.trace("Removing session for authenticationToken: {}", authenticationToken);
        synchronized (this) {
            this.sessions.remove(authenticationToken);
        }
    }

    /*
     * Creates a new session object
     */
    public Session createSession(String authenticationToken) {
        LOGGER.debug("Creating new session");
        LOGGER.trace("Creating new session for authenticationToken: {}", authenticationToken);
        synchronized (this) {
            Session session = new Session(authenticationToken);
            this.sessions.put(authenticationToken, session);
            return session;
        }
    }
}
