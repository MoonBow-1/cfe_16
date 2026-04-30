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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/*
 * Tests the functionality of SessionManager
 */
final class SessionManagerTests {

    /*
     * Tests creating a session with SessionManager and getting that same session
     * from SessionManager
     */
    @Test
    void createSessionAndGetItWithAuthTokenTest() {
        final SessionManager sessionManager = new SessionManager(new Configuration());

        String authToken1 = "AUTH_TOKEN_12345";
        String authToken2 = "AUTH_TOKEN_54321";
        String authToken3 = "AUTH_TOKEN_99999";

        Session session1 = sessionManager.createSession(authToken1);
        Session session2 = sessionManager.createSession(authToken2);
        Assertions
                .assertSame(session1, sessionManager.getSession(authToken1), "Same session should be returned with the same authentication token");
        Assertions
                .assertSame(session2, sessionManager.getSession(authToken2), "Same session should be returned with the same authentication token");
        Assertions
                .assertNotSame(session1, sessionManager.getSession(authToken2), "Different session should be returned with a different authentication token");
        Assertions
                .assertNotSame(session2, sessionManager.getSession(authToken1), "Different session should be returned with a different authentication token");
        Assertions
                .assertNull(sessionManager.getSession(authToken3), "Getting a session with an unused authentication token should return null");
    }

    @Test
    void sessionCreationAndDeletionTests() {
        final SessionManager sessionManager = new SessionManager(new Configuration());

        Session session = sessionManager.createSession("AUTH");
        Assertions.assertTrue(session.addChannel(Session.DEFAULT_CHANNEL));
        Assertions.assertFalse(session.addChannel(Session.DEFAULT_CHANNEL));
        Assertions.assertTrue(session.doesChannelExist(Session.DEFAULT_CHANNEL));
        Assertions.assertTrue(session.removeChannel(Session.DEFAULT_CHANNEL));
        Assertions.assertFalse(session.doesChannelExist(Session.DEFAULT_CHANNEL));
    }
}
