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

import com.teragrep.cfe_16.event.time.HECTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HECRecordStubTest {

    @Test
    @DisplayName("event() throws UnsupportedOperationException if called")
    void eventThrowsUnsupportedOperationExceptionIfCalled() {
        final HECRecordStub stub = new HECRecordStub();

        final UnsupportedOperationException unsupportedOperationException = Assertions
                .assertThrowsExactly(UnsupportedOperationException.class, stub::event);

        Assertions
                .assertEquals(
                        "HECRecordStub does not support this", unsupportedOperationException.getMessage(),
                        "Exception message was not what was expected"
                );
    }

    @Test
    @DisplayName("channel() throws UnsupportedOperationException if called")
    void channelThrowsUnsupportedOperationExceptionIfCalled() {
        final HECRecordStub stub = new HECRecordStub();

        final UnsupportedOperationException unsupportedOperationException = Assertions
                .assertThrowsExactly(UnsupportedOperationException.class, stub::channel);

        Assertions
                .assertEquals(
                        "HECRecordStub does not support this", unsupportedOperationException.getMessage(),
                        "Exception message was not what was expected"
                );
    }

    @Test
    @DisplayName("authenticationToken() throws UnsupportedOperationException if called")
    void authenticationTokenThrowsUnsupportedOperationExceptionIfCalled() {
        final HECRecordStub stub = new HECRecordStub();

        final UnsupportedOperationException unsupportedOperationException = Assertions
                .assertThrowsExactly(UnsupportedOperationException.class, stub::authenticationToken);

        Assertions
                .assertEquals(
                        "HECRecordStub does not support this", unsupportedOperationException.getMessage(),
                        "Exception message was not what was expected"
                );
    }

    @Test
    @DisplayName("ackID() throws UnsupportedOperationException if called")
    void ackIDThrowsUnsupportedOperationExceptionIfCalled() {
        final HECRecordStub stub = new HECRecordStub();

        final UnsupportedOperationException unsupportedOperationException = Assertions
                .assertThrowsExactly(UnsupportedOperationException.class, stub::ackID);

        Assertions
                .assertEquals(
                        "HECRecordStub does not support this", unsupportedOperationException.getMessage(),
                        "Exception message was not what was expected"
                );
    }

    @Test
    @DisplayName("time() returns a HECTimeStub if called")
    void timeReturnsAHecTimeStubIfCalled() {
        final HECRecordStub stub = new HECRecordStub();

        final HECTime unsupportedOperationException = Assertions.assertDoesNotThrow(stub::time);

        Assertions
                .assertThrowsExactly(
                        UnsupportedOperationException.class, unsupportedOperationException::source,
                        "HECTimeStub did not throw an exception"
                );
    }

    @Test
    @DisplayName("hashCode() throws UnsupportedOperationException if called")
    void hashCodeThrowsUnsupportedOperationExceptionIfCalled() {
        final HECRecordStub stub = new HECRecordStub();

        final UnsupportedOperationException unsupportedOperationException = Assertions
                .assertThrowsExactly(UnsupportedOperationException.class, stub::hashCode);

        Assertions
                .assertEquals(
                        "HECRecordStub does not support this", unsupportedOperationException.getMessage(),
                        "Exception message was not what was expected"
                );
    }

    @Test
    @DisplayName("equals() throws UnsupportedOperationException if called")
    void equalsThrowsUnsupportedOperationExceptionIfCalled() {
        final HECRecordStub stub = new HECRecordStub();

        final UnsupportedOperationException unsupportedOperationException = Assertions
                .assertThrowsExactly(UnsupportedOperationException.class, () -> stub.equals(new Object()));

        Assertions
                .assertEquals(
                        "HECRecordStub does not support this", unsupportedOperationException.getMessage(),
                        "Exception message was not what was expected"
                );
    }

    @Test
    @DisplayName("isStub() returns true")
    void isStubReturnsTrue() {
        final HECRecordStub stub = new HECRecordStub();
        Assertions.assertTrue(stub::isStub, "isStub() did not return true");
    }
}
