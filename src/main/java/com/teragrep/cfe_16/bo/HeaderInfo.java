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
package com.teragrep.cfe_16.bo;

import com.teragrep.rlo_14.SDElement;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HeaderInfo {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeaderInfo.class);
    private final static XForwardedForStub xForwardedForStub = new XForwardedForStub();
    private final static XForwardedHostStub xForwardedHostStub = new XForwardedHostStub();
    private final static XForwardedProtoStub xForwardedProtoStub = new XForwardedProtoStub();
    private final HttpServletRequest httpServletRequest;

    public HeaderInfo(final HttpServletRequest httpServletRequest) {
        this.httpServletRequest = httpServletRequest;
    }

    public SDElement asSDElement() {
        LOGGER.debug("Setting Structured Data headers");
        final SDElement headerSDE = new SDElement("cfe_16-origin@48577");

        final XForwardedFor xForwardedFor = this.xForwardedFor();
        final XForwardedHost xForwardedHost = this.xForwardedHost();
        final XForwardedProto xForwardedProto = this.xForwardedProto();

        if (!xForwardedFor.isStub()) {
            LOGGER.debug("Adding X-Forwarded-For header to headerSDE");
            headerSDE.addSDParam("X-Forwarded-For", xForwardedFor.value());
        }
        if (!xForwardedHost.isStub()) {
            LOGGER.debug("Adding X-Forwarder-Host to headerSDE");
            headerSDE.addSDParam("X-Forwarded-Host", xForwardedHost.value());
        }
        if (!xForwardedProto.isStub()) {
            LOGGER.debug("Adding X-Forwarded-Proto to headerSDE");
            headerSDE.addSDParam("X-Forwarded-Proto", xForwardedProto.value());
        }

        return headerSDE;
    }

    private XForwardedFor xForwardedFor() {
        final XForwardedFor xForwardedFor;
        LOGGER.debug("Setting X-Forwarded-For");
        if (httpServletRequest.getHeader("X-Forwarded-For") == null) {
            xForwardedFor = xForwardedForStub;
        }
        else {
            xForwardedFor = new XForwardedForImpl(httpServletRequest.getHeader("X-Forwarded-For"));
        }
        LOGGER.trace("Setting X-Forwarded-For to value <[{}]>", xForwardedFor);

        return xForwardedFor;
    }

    private XForwardedHost xForwardedHost() {
        final XForwardedHost xForwardedHost;
        LOGGER.debug("Setting X-Forwarded-Host");
        if (httpServletRequest.getHeader("X-Forwarded-Host") == null) {
            xForwardedHost = xForwardedHostStub;
        }
        else {
            xForwardedHost = new XForwardedHostImpl(httpServletRequest.getHeader("X-Forwarded-Host"));
        }
        LOGGER.trace("Setting X-Forwarded-Host to value <[{}]>", xForwardedHost);
        return xForwardedHost;
    }

    private XForwardedProto xForwardedProto() {
        final XForwardedProto xForwardedProto;
        LOGGER.debug("Setting X-Forwarded-Proto");
        if (httpServletRequest.getHeader("X-Forwarded-Proto") == null) {
            xForwardedProto = xForwardedProtoStub;
        }
        else {
            xForwardedProto = new XForwardedProtoImpl(httpServletRequest.getHeader("X-Forwarded-Proto"));
        }
        LOGGER.trace("Setting X-Forwarded-Proto to value <[{}]>", xForwardedProto);
        return xForwardedProto;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final HeaderInfo that = (HeaderInfo) o;
        return Objects.equals(httpServletRequest, that.httpServletRequest);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(httpServletRequest);
    }

    @Override
    public String toString() {
        return "HeaderInfo{" + "httpServletRequest=" + httpServletRequest + '}';
    }
}
