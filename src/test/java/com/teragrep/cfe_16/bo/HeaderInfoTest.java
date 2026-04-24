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
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class HeaderInfoTest {

    @Test
    @DisplayName("asSDElement() uses all three parameters if they are not stubs")
    void asSdElementUsesAllThreeParametersIfTheyAreNotStubs() {
        final MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.addHeader("X-Forwarded-For", "forwardedFor");
        mockHttpServletRequest.addHeader("X-Forwarded-Host", "forwardedHost");
        mockHttpServletRequest.addHeader("X-Forwarded-Proto", "forwardedProto");
        final HeaderInfo headerInfo = new HeaderInfo(mockHttpServletRequest);

        final SDElement expectedSDElement = new SDElement("cfe_16-origin@48577");
        expectedSDElement.addSDParam("X-Forwarded-For", "forwardedFor");
        expectedSDElement.addSDParam("X-Forwarded-Host", "forwardedHost");
        expectedSDElement.addSDParam("X-Forwarded-Proto", "forwardedProto");

        Assertions.assertEquals(expectedSDElement, headerInfo.asSDElement());
    }

    @Test
    @DisplayName("asSDElement ignores stubs")
    void asSdElementIgnoresStubs() {
        final MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.addHeader("X-Forwarded-For", "forwardedFor");
        mockHttpServletRequest.addHeader("X-Forwarded-Proto", "forwardedProto");
        final HeaderInfo headerInfo = new HeaderInfo(mockHttpServletRequest);

        final SDElement expectedSDElement = new SDElement("cfe_16-origin@48577");
        expectedSDElement.addSDParam("X-Forwarded-For", "forwardedFor");
        expectedSDElement.addSDParam("X-Forwarded-Proto", "forwardedProto");

        Assertions.assertEquals(2, headerInfo.asSDElement().getSdParams().size());
        Assertions.assertEquals(expectedSDElement, headerInfo.asSDElement());
    }

    @Test
    @DisplayName("EqualsVerifier")
    void equalsVerifier() {
        EqualsVerifier.forClass(HeaderInfo.class).verify();
    }
}
