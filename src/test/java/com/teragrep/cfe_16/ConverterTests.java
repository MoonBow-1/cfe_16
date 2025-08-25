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

import com.cloudbees.syslog.*;
import com.teragrep.cfe_16.bo.HeaderInfo;
import com.teragrep.cfe_16.bo.HttpEventData;
import com.teragrep.cfe_16.bo.XForwardedForStub;
import com.teragrep.cfe_16.bo.XForwardedHostStub;
import com.teragrep.cfe_16.bo.XForwardedProtoStub;
import com.teragrep.cfe_16.event.EventMessageImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/*
 * Tests the functionality of Converter
 */
public class ConverterTests {

    private Converter converter;
    private HttpEventData eventData1;
    private HttpEventData eventData2;
    private HttpEventData eventData3;
    private Severity supposedSeverity;
    private Facility supposedFacility;
    private SyslogMessage supposedSyslogMessage1;
    private SyslogMessage supposedSyslogMessage2;
    private SyslogMessage supposedSyslogMessage3;
    private SDElement metadataSDE1;
    private SDElement metadataSDE2;
    private SDElement metadataSDE3;
    private SyslogMessage returnedMessage1;
    private SyslogMessage returnedMessage2;
    private SyslogMessage returnedMessage3;
    private Set<SDElement> returnedSDElements1;
    private Set<SDElement> returnedSDElements2;
    private Set<SDElement> returnedSDElements3;
    private Set<SDElement> supposedSDElements1;
    private Set<SDElement> supposedSDElements2;
    private Set<SDElement> supposedSDElements3;

    /*
     * Initializes 3 HttpEventData objects and the data for them, 3 Structured Data
     * Elements (SDE) with this data and 3 SyslogMessages with the help of those
     * SDE's. We then call Converter's httpToSyslog() method to convert our
     * HttpEventData objects so we have 3 supposed messages and 3 returned messages
     * from Converter. SDElements from all of those messages are saved in Sets so
     * that they can be compared to each other in the test methods.
     */
    @BeforeEach
    public void initialize() {

        converter = new Converter(
                new HeaderInfo(new XForwardedForStub(), new XForwardedHostStub(), new XForwardedProtoStub())
        );

        eventData1 = new HttpEventData();
        eventData2 = new HttpEventData();
        eventData3 = new HttpEventData();

        supposedSyslogMessage1 = null;
        supposedSyslogMessage2 = null;
        supposedSyslogMessage3 = null;
        metadataSDE1 = new SDElement("cfe_16-metadata@48577");
        metadataSDE2 = new SDElement("cfe_16-metadata@48577");
        metadataSDE3 = new SDElement("cfe_16-metadata@48577");

        supposedSeverity = Severity.INFORMATIONAL;
        supposedFacility = Facility.USER;

        eventData1.setAuthenticationToken("AUTH_TOKEN_11111");
        eventData1.setChannel("CHANNEL_11111");
        eventData1.setAckID(0);
        eventData1.setTimeSource("reported");
        eventData1.setTime("1433188255253");
        eventData1.setTimeParsed(true);
        eventData1.setTimeAsLong(1433188255253L);
        eventData1.setEvent(new EventMessageImpl("Event 1"));

        eventData2.setAuthenticationToken("AUTH_TOKEN_22222");
        eventData2.setChannel("CHANNEL_22222");
        eventData2.setAckID(1);
        eventData2.setTimeSource("generated");
        eventData2.setTimeParsed(false);
        eventData2.setEvent(new EventMessageImpl("Event 2"));

        eventData3.setAuthenticationToken("AUTH_TOKEN_33333");
        eventData3.setChannel("defaultchannel");
        eventData3.setTimeSource("generated");
        eventData3.setTimeParsed(false);
        eventData3.setEvent(new EventMessageImpl("Event 3"));

        metadataSDE1.addSDParam("authentication_token", eventData1.getAuthenticationToken());
        metadataSDE1.addSDParam("channel", eventData1.getChannel());
        metadataSDE1.addSDParam("ack_id", String.valueOf(eventData1.getAckID()));
        metadataSDE1.addSDParam("time_source", eventData1.getTimeSource());
        metadataSDE1.addSDParam("time_parsed", "true");
        metadataSDE1.addSDParam("time", eventData1.getTime());

        metadataSDE2.addSDParam("authentication_token", eventData2.getAuthenticationToken());
        metadataSDE2.addSDParam("channel", eventData2.getChannel());
        metadataSDE2.addSDParam("ack_id", String.valueOf(eventData2.getAckID()));
        metadataSDE2.addSDParam("time_source", eventData2.getTimeSource());

        metadataSDE3.addSDParam("authentication_token", eventData3.getAuthenticationToken());
        metadataSDE3.addSDParam("channel", eventData3.getChannel());
        metadataSDE3.addSDParam("time_source", eventData3.getTimeSource());

        supposedSyslogMessage1 = new SyslogMessage()
                .withTimestamp(eventData1.getTimeAsLong())
                .withSeverity(supposedSeverity)
                .withAppName("capsulated")
                .withHostname("cfe-16")
                .withFacility(supposedFacility)
                .withSDElement(metadataSDE1)
                .withMsg(eventData1.getEvent().asString());

        supposedSyslogMessage2 = new SyslogMessage()
                .withSeverity(supposedSeverity)
                .withAppName("capsulated")
                .withHostname("cfe-16")
                .withFacility(supposedFacility)
                .withSDElement(metadataSDE2)
                .withMsg(eventData2.getEvent().asString());

        supposedSyslogMessage3 = new SyslogMessage()
                .withSeverity(supposedSeverity)
                .withAppName("capsulated")
                .withHostname("cfe-16")
                .withFacility(supposedFacility)
                .withSDElement(metadataSDE3)
                .withMsg(eventData3.getEvent().asString());

        returnedMessage1 = converter.httpToSyslog(eventData1);
        returnedMessage2 = converter.httpToSyslog(eventData2);
        returnedMessage3 = converter.httpToSyslog(eventData3);

        returnedSDElements1 = returnedMessage1.getSDElements();
        returnedSDElements2 = returnedMessage2.getSDElements();
        returnedSDElements3 = returnedMessage3.getSDElements();
        supposedSDElements1 = supposedSyslogMessage1.getSDElements();
        supposedSDElements2 = supposedSyslogMessage2.getSDElements();
        supposedSDElements3 = supposedSyslogMessage3.getSDElements();

    }

    /*
     * Compares the severity from the supposed SyslogMessage and the SyslogMessage
     * returned from Converter. Severity should be hardcoded to INFORMATIONAL.
     */
    @Test
    public void severityTest() {
        assertEquals(
                "Severity should be INFORMATIONAL", supposedSyslogMessage1.getSeverity(), returnedMessage1.getSeverity()
        );
        assertEquals(
                "Severity should be INFORMATIONAL", supposedSyslogMessage2.getSeverity(), returnedMessage2.getSeverity()
        );
        assertEquals(
                "Severity should be INFORMATIONAL", supposedSyslogMessage3.getSeverity(), returnedMessage3.getSeverity()
        );
    }

    /*
     * Compares the facility from the supposed SyslogMessage and the SyslogMessage
     * returned from Converter. Facility should be hardcoded to USER.
     */
    @Test
    public void facilityTest() {
        assertEquals("Facility should be USER", supposedSyslogMessage1.getFacility(), returnedMessage1.getFacility());
        assertEquals("Facility should be USER", supposedSyslogMessage2.getFacility(), returnedMessage2.getFacility());
        assertEquals("Facility should be USER", supposedSyslogMessage3.getFacility(), returnedMessage3.getFacility());
    }

    /*
     * Compares the AppName and HostName from the supposed SyslogMessage and the
     * SyslogMessage returned from Converter. AppName should be hardcoded to
     * "capsulated" and HostName should be hardcoded to "cfe-16".
     */
    @Test
    public void appNameAndHostNameTest() {
        assertEquals(
                "App name should be '" + supposedSyslogMessage1.getAppName() + "'", supposedSyslogMessage1.getAppName(),
                returnedMessage1.getAppName()
        );
        assertEquals(
                "App name should be '" + supposedSyslogMessage2.getAppName() + "'", supposedSyslogMessage2.getAppName(),
                returnedMessage2.getAppName()
        );
        assertEquals(
                "App name should be '" + supposedSyslogMessage3.getAppName() + "'", supposedSyslogMessage3.getAppName(),
                returnedMessage3.getAppName()
        );

        assertEquals(
                "Host name should be '" + supposedSyslogMessage1.getHostname() + "'",
                supposedSyslogMessage1.getHostname(), returnedMessage1.getHostname()
        );
        assertEquals(
                "Host name should be '" + supposedSyslogMessage1.getHostname() + "'",
                supposedSyslogMessage2.getHostname(), returnedMessage2.getHostname()
        );
        assertEquals(
                "Host name should be '" + supposedSyslogMessage1.getHostname() + "'",
                supposedSyslogMessage3.getHostname(), returnedMessage3.getHostname()
        );
    }

    /*
     * Compares the msg from the supposed SyslogMessage and the SyslogMessage
     * returned from Converter. msg should be the event from HttpEventData object.
     */
    @Test
    public void msgTest() {
        assertEquals(
                "Msg should be '" + supposedSyslogMessage1.getMsg().toString() + "'",
                supposedSyslogMessage1.getMsg().toString(), returnedMessage1.getMsg().toString()
        );
        assertEquals(
                "Msg should be '" + supposedSyslogMessage2.getMsg().toString() + "'",
                supposedSyslogMessage2.getMsg().toString(), returnedMessage2.getMsg().toString()
        );
        assertEquals(
                "Msg should be '" + supposedSyslogMessage3.getMsg().toString() + "'",
                supposedSyslogMessage3.getMsg().toString(), returnedMessage3.getMsg().toString()
        );
    }

    /*
     * Compares the time stamp from the supposed SyslogMessage and the SyslogMessage
     * returned from Converter if the time stamp is assigned to the SyslogMessage.
     * Otherwise time stamp should be null.
     */
    @Test
    public void timestampTest() {

        assertEquals(
                "Timestamp should be: " + supposedSyslogMessage1.getTimestamp(), supposedSyslogMessage1.getTimestamp(),
                returnedMessage1.getTimestamp()
        );
        assertNull("Timestamp should be null", supposedSyslogMessage2.getTimestamp());
        assertNull("Timestamp should be null", supposedSyslogMessage3.getTimestamp());

    }

    /*
     * Compares the SDElements from the supposed SyslogMessage and the SyslogMessage
     * returned from Converter.
     */
    @Test
    public void SDElementsTest() {

        List<SDParam> supposedSDParams = new ArrayList<SDParam>();
        List<SDParam> returnedSDParams = new ArrayList<SDParam>();
        Iterator<SDElement> iterator;
        SDElement returnedSDE = null;
        SDElement supposedSDE = null;

        // Gets the SDParams from the SDEs from the first SyslogMessage returned from
        // Converter and saves them in a List
        for (iterator = returnedSDElements1.iterator(); iterator.hasNext();) {
            returnedSDE = iterator.next();
            for (int i = 0; i < returnedSDE.getSdParams().size(); i++) {
                returnedSDParams.add(returnedSDE.getSdParams().get(i));
            }
        }

        // Gets the SDParams from the SDEs from the first SyslogMessage created in
        // initialize() and saves them in a List
        for (iterator = supposedSDElements1.iterator(); iterator.hasNext();) {
            supposedSDE = iterator.next();
            for (int i = 0; i < supposedSDE.getSdParams().size(); i++) {
                supposedSDParams.add(supposedSDE.getSdParams().get(i));
            }
        }

        // Goes through all the returned SDParams and checks that they are all found in
        // supposed SDParams
        for (int i = 0; i < returnedSDParams.size(); i++) {
            assertTrue(
                    "SDParam '" + returnedSDParams.get(i) + "' should not be in returned SDElement.", supposedSDParams.contains(returnedSDParams.get(i))
            );
        }

        // Goes through all supposed SDParams and checks that they are all found in
        // returned SDParams
        for (int i = 0; i < supposedSDParams.size(); i++) {
            assertTrue(
                    "SDParam '" + supposedSDParams.get(i) + "' should be in returned SDElement.", returnedSDParams.contains(supposedSDParams.get(i))
            );
        }

        // Create new empty ArrayList that we can save SDParams from the next SDElement
        supposedSDParams = new ArrayList<SDParam>();
        returnedSDParams = new ArrayList<SDParam>();

        // Gets the SDParams from the SDEs from the second SyslogMessage returned from
        // Converter and saves them in a List
        for (iterator = returnedSDElements2.iterator(); iterator.hasNext();) {
            returnedSDE = iterator.next();
            for (int i = 0; i < returnedSDE.getSdParams().size(); i++)
                returnedSDParams.add(returnedSDE.getSdParams().get(i));
        }

        // Gets the SDParams from the SDEs from the second SyslogMessage created in
        // initialize() and saves them in a List
        for (iterator = supposedSDElements2.iterator(); iterator.hasNext();) {
            supposedSDE = iterator.next();
            for (int i = 0; i < supposedSDE.getSdParams().size(); i++) {
                supposedSDParams.add(supposedSDE.getSdParams().get(i));
            }
        }

        // Goes through all the returned SDParams and checks that they are all found in
        // supposed SDParams
        for (int i = 0; i < returnedSDParams.size(); i++) {
            assertTrue(
                    "SDParam '" + returnedSDParams.get(i) + "' should not be in returned SDElement.", supposedSDParams.contains(returnedSDParams.get(i))
            );
        }

        // Goes through all supposed SDParams and checks that they are all found in
        // returned SDParams
        for (int i = 0; i < supposedSDParams.size(); i++) {
            assertTrue(
                    "SDParam '" + supposedSDParams.get(i) + "' should be in returned SDElement.", returnedSDParams.contains(supposedSDParams.get(i))
            );
        }

        // Create new empty ArrayList that we can save SDParams from the next SDElement
        supposedSDParams = new ArrayList<SDParam>();
        returnedSDParams = new ArrayList<SDParam>();

        // Gets the SDParams from the SDEs from the third SyslogMessage returned from
        // Converter and saves them in a List
        for (iterator = returnedSDElements3.iterator(); iterator.hasNext();) {
            returnedSDE = iterator.next();
            for (int i = 0; i < returnedSDE.getSdParams().size(); i++)
                returnedSDParams.add(returnedSDE.getSdParams().get(i));
        }

        // Gets the SDParams from the SDEs from the third SyslogMessage created in
        // initialize() and saves them in a List
        for (iterator = supposedSDElements3.iterator(); iterator.hasNext();) {
            supposedSDE = iterator.next();
            for (int i = 0; i < supposedSDE.getSdParams().size(); i++) {
                supposedSDParams.add(supposedSDE.getSdParams().get(i));
            }
        }

        // Goes through all the returned SDParams and checks that they are all found in
        // supposed SDParams
        for (int i = 0; i < returnedSDParams.size(); i++) {
            assertTrue(
                    "SDParam '" + returnedSDParams.get(i) + "' should not be in returned SDElement.", supposedSDParams.contains(returnedSDParams.get(i))
            );
        }

        // Goes through all supposed SDParams and checks that they are all found in
        // returned SDParams
        for (int i = 0; i < supposedSDParams.size(); i++) {
            assertTrue(
                    "SDParam '" + supposedSDParams.get(i) + "' should be in returned SDElement.", returnedSDParams.contains(supposedSDParams.get(i))
            );
        }
    }
}
