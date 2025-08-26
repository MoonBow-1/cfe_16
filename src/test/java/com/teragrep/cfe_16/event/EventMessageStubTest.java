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
package com.teragrep.cfe_16.event;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EventMessageStubTest {

    @Test
    @DisplayName("isStub() returns true")
    void isStubReturnsTrue() {
        final EventMessageStub eventStub = new EventMessageStub();

        Assertions.assertTrue(eventStub::isStub);
    }

    @Test
    @DisplayName("asString() throws UnsupportedOperationException if called")
    void asStringThrowsUnsupportedOperationExceptionIfCalled() {
        final EventMessageStub eventStub = new EventMessageStub();

        final Exception exception = Assertions
                .assertThrowsExactly(UnsupportedOperationException.class, eventStub::asString);

        Assertions.assertEquals("EventStub does not support this", exception.getMessage());
    }

    @Test
    @DisplayName("hashCode() matches with two EventMessageStubs")
    void hashCodeMatchesWithTwoEventMessageStubs() {
        final EventMessageStub eventStub1 = new EventMessageStub();
        final EventMessageStub eventStub2 = new EventMessageStub();

        Assertions.assertEquals(eventStub1.hashCode(), eventStub2.hashCode());
    }

    @Test
    @DisplayName("Two EventMessageStubs are equal")
    void twoEventMessageStubsAreEqual() {
        final EventMessageStub eventStub1 = new EventMessageStub();
        final EventMessageStub eventStub2 = new EventMessageStub();

        Assertions.assertEquals(eventStub1, eventStub2);
    }
}
