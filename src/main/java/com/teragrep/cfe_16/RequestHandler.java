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

import com.teragrep.cfe_16.bo.HeaderInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component
public class RequestHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestHandler.class);
    private String xForwardedFor;
    private String xForwardedHost;
    private String xForwardedProto;

    public HeaderInfo createHeaderInfoObject(HttpServletRequest request) {
        LOGGER.debug("Creating new Header Info");
        HeaderInfo headerInfo = new HeaderInfo();
        xForwardedFor = request.getHeader("X-Forwarded-For");
        xForwardedHost = request.getHeader("X-Forwarded-Host");
        xForwardedProto = request.getHeader("X-Forwarded-Proto");
        if (xForwardedFor != null) {
            LOGGER.debug("Setting X-Forwarded-For");
            LOGGER.trace("Setting X-Forwarded-For to value <[{}]>", xForwardedFor);
            headerInfo.setxForwardedFor(xForwardedFor);
        }
        if (xForwardedHost != null) {
            LOGGER.debug("Setting X-Forwarded-Host");
            LOGGER.trace("Setting X-Forwarded-Host to value <[{}]>", xForwardedHost);
            headerInfo.setxForwardedHost(xForwardedHost);
        }
        if (xForwardedProto != null) {
            LOGGER.debug("Setting X-Forwarded-Proto");
            LOGGER.trace("Setting X-Forwarded-Proto to value <[{}]>", xForwardedProto);
            headerInfo.setxForwardedProto(xForwardedProto);
        }

        return headerInfo;
    }

}
