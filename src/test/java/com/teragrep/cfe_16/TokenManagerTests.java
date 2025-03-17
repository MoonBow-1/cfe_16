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

package com.teragrep.cfe_16;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Base64;

import static org.junit.Assert.*;

/*
 * Tests the functionality of TokenManager
 */
public class TokenManagerTests {

    TokenManager manager = new TokenManager();

    /*
     * Tests TokenManager's tokenIsMissing() method which checks if
     * HttpServletRequest's header has an authentication token in it.
     */
    @Test
    public void tokenCheckingTest() {

        MockHttpServletRequest requestWithHttpHeaderAuth = new MockHttpServletRequest();
        MockHttpServletRequest requestWithBasicAuth = new MockHttpServletRequest();
        MockHttpServletRequest requestWithoutAuth = new MockHttpServletRequest();
        String authToken = "AUTH_TOKEN_11111";

        requestWithHttpHeaderAuth.addHeader("Authorization", authToken);
        requestWithBasicAuth.addHeader("Authorization", "Basic x:" + authToken);

        assertFalse("Token should be found from the request",
            manager.tokenIsMissing(requestWithHttpHeaderAuth));
        assertFalse("Token should be found from the request",
            manager.tokenIsMissing(requestWithBasicAuth));
        assertTrue("Token should not be found from the request",
            manager.tokenIsMissing(requestWithoutAuth));
    }

    /*
     * Tests TokenManager's isTokenInBasic() method which checks if the
     * HttpServletRequest's header's authentication token is in basic authentication
     * format.
     */
    @Test
    public void basicAuthCheckingTest() {
        String authToken = "AUTH_TOKEN_11111";
        String basicAuthHeader = "Basic x:" + authToken;

        assertTrue("Authorization header should be in basic format",
            manager.isTokenInBasic(basicAuthHeader));
        assertFalse(
            "Authorization should not be in basic format when querying with only the "
                + "authentication token.",
            manager.isTokenInBasic(authToken));
    }

    /*
     * Tests TokenManager's getTokenFromBasic() method which returns the
     * authentication token when it is given in basic authentication format.
     */
    @Test
    public void getTokenFromBasicAuthTest() {
        String authToken = "AUTH_TOKEN_11111";
        String basicAuthCredentials = "x:" + authToken;
        String credentialsEncoded = Base64.getEncoder()
            .encodeToString(basicAuthCredentials.getBytes());
        String basicAuthHeader = "Basic " + credentialsEncoded;

        assertEquals(
            "Method should return the authentication token extracted from the Basic "
                + "Authentication format",
            authToken, manager.getTokenFromBasic(basicAuthHeader));
    }

}
