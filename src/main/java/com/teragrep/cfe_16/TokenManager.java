/*
 * HTTP Event Capture to RFC5424 CFE_16
 * Copyright (C) 2019-2025 Suomen Kanuuna Oy
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

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/*
 * Manager that handles the authentication token
 *
 */
@Component
public class TokenManager {

    public TokenManager() {
    }

    /*
     * Checks if the authentication token is in the request. Returns true if the
     * authentication token is NOT in the request. Returns false if authentication
     * token is found.
     */
    // @LogAnnotation(type = LogType.DEBUG)
    public boolean tokenIsMissing(HttpServletRequest request) {

        // AspectLoggerWrapper.log(new PayloadWrapper(OffsetDateTime.MAX, 4,
        // LogValues.LogClass.audit, LogValues.GdprData.identification, "application",
        // "environment",
        // "component", "instance", LogValues.RetentionTime.P1Y,
        // UUID.randomUUID().toString(), null));
        String authHeader = request.getHeader("Authorization");
        return (authHeader == null || authHeader.isEmpty());
    }

    /*
     * Checks if the authentication token is given in basic authentication. Returns
     * true if the token is in basic and false if not. Authorization header is given
     * as a string parameter.
     */
    // @LogAnnotation(type = LogType.DEBUG)
    public boolean isTokenInBasic(String authHeader) {

        boolean isInBasic = authHeader != null && authHeader.toLowerCase().startsWith("basic");

        return isInBasic;
    }

    /*
     * Trims the authorization header to get the authentication token from basic
     * authentication form Authorization header is given as a string parameter.
     * First the word "Basic" is trimmed out of the string. After that the
     * credentials are decoded from Base64 and they are saved in a string that
     * consists of username and a password. Here password is the authenication
     * token. Username and password is separated into an array. Then we return the
     * password (authentication token).
     */
    // @LogAnnotation(type = LogType.DEBUG)
    public String getTokenFromBasic(String authHeader) {
        String base64Credentials = authHeader.substring("Basic".length()).trim();
        byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
        String credentials = new String(credDecoded, StandardCharsets.UTF_8);

        // credentials = username:password
        final String[] values = credentials.split(":", 2);

        return values[1];
    }
}
