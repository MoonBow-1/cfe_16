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

import com.cloudbees.syslog.SDElement;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HeaderInfo {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeaderInfo.class);
    private final XForwardedFor xForwardedFor;
    private final XForwardedHost xForwardedHost;
    private final XForwardedProto xForwardedProto;

    public HeaderInfo(
            final XForwardedFor xForwardedFor,
            final XForwardedHost xForwardedHost,
            final XForwardedProto xForwardedProto
    ) {
        this.xForwardedFor = xForwardedFor;
        this.xForwardedHost = xForwardedHost;
        this.xForwardedProto = xForwardedProto;
    }

    public SDElement asSDElement() {
        LOGGER.debug("Setting Structured Data headers");
        final SDElement headerSDE = new SDElement("cfe_16-origin@48577");

        if (!this.xForwardedFor.isStub()) {
            LOGGER.debug("Adding X-Forwarded-For header to headerSDE");
            headerSDE.addSDParam("X-Forwarded-For", this.xForwardedFor.value());
        }
        if (!this.xForwardedHost.isStub()) {
            LOGGER.debug("Adding X-Forwarder-Host to headerSDE");
            headerSDE.addSDParam("X-Forwarded-Host", this.xForwardedHost.value());
        }
        if (!this.xForwardedProto.isStub()) {
            LOGGER.debug("Adding X-Forwarded-Proto to headerSDE");
            headerSDE.addSDParam("X-Forwarded-Proto", this.xForwardedProto.value());
        }

        return headerSDE;
    }

    @Override
    public String toString() {
        return "HeaderInfo{" + "xForwardedFor=" + xForwardedFor + ", xForwardedHost=" + xForwardedHost
                + ", xForwardedProto=" + xForwardedProto + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        HeaderInfo that = (HeaderInfo) o;
        return Objects.equals(xForwardedFor, that.xForwardedFor) && Objects.equals(xForwardedHost, that.xForwardedHost)
                && Objects.equals(xForwardedProto, that.xForwardedProto);
    }

    @Override
    public int hashCode() {
        return Objects.hash(xForwardedFor, xForwardedHost, xForwardedProto);
    }
}
