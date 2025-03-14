package com.teragrep.cfe_16.bo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TimestampedHttpEventDataTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    public void initialize() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Test handleTime with epoch seconds, 10 digits")
    void testHandleTimeWithEpochSeconds10Digits() {
        String content = "{\"event\": \"Pony 1 has left the barn\", \"sourcetype\": "
            + "\"mysourcetype\", \"time\": 1277464192}";
        TimestampedHttpEventData eventData = new TimestampedHttpEventData();

        JsonNode jsonNode;

        try {
            jsonNode = objectMapper.readTree(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final TimestampedHttpEventData httpEventDataWithHandledTime = eventData.handleTime(
            jsonNode,
            null
        );
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                "reported",
                httpEventDataWithHandledTime.getTimeSource(),
                "Time source should be 'reported' when the time is specified in a request"),
            () -> Assertions.assertTrue(
                httpEventDataWithHandledTime.isTimeParsed(),
                "timeParsed should be true when the time is specified in a request"),
            () -> Assertions.assertEquals(
                1277464192000L,
                httpEventDataWithHandledTime.getTimeAsLong(),
                "Time should have been converted to epoch milliseconds")
        );
    }

    @Test
    @DisplayName("Test handleTime with epoch seconds, no time")
    void testHandleTimeWithEpochSecondsNoTime() {
        String content = "{\"event\": \"Pony 1 has left the barn\", \"sourcetype\": "
            + "\"mysourcetype\"}";
        TimestampedHttpEventData eventData = new TimestampedHttpEventData();

        JsonNode jsonNode;

        try {
            jsonNode = objectMapper.readTree(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final TimestampedHttpEventData httpEventDataWithHandledTime = eventData.handleTime(
            jsonNode,
            null
        );

        Assertions.assertAll(
            () -> Assertions.assertEquals(
                "generated",
                httpEventDataWithHandledTime.getTimeSource(),
                "Time source should be 'generated' when it's not specified in a request"),
            () -> Assertions.assertFalse(
                httpEventDataWithHandledTime.isTimeParsed(),
                "timeParsed should be false when time is not specified in a request"),
            () -> Assertions.assertEquals(
                0,
                httpEventDataWithHandledTime.getTimeAsLong(),
                "Time as long should be 0 when time is not specified in a request")
        );
    }

    @Test
    @DisplayName("Test handleTime with epoch seconds and decimal milliseconds")
    void testHandleTimeWithEpochSecondsAndDecimalMilliseconds() {
        String content = "{\"event\": \"Pony 1 has left the barn\", \"sourcetype\": "
            + "\"mysourcetype\", \"time\": 1433188255.253}";
        TimestampedHttpEventData eventData = new TimestampedHttpEventData();

        JsonNode jsonNode;

        try {
            jsonNode = objectMapper.readTree(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final TimestampedHttpEventData httpEventDataWithHandledTime = eventData.handleTime(
            jsonNode,
            null
        );

        Assertions.assertAll(
            () -> Assertions.assertEquals(
                "reported",
                httpEventDataWithHandledTime.getTimeSource(),
                "Time source should be 'reported' when the time is specified in a request"),
            () -> Assertions.assertTrue(
                httpEventDataWithHandledTime.isTimeParsed(),
                "timeParsed should be true when time is specified in a request"),
            () -> Assertions.assertEquals(
                1433188255253L,
                httpEventDataWithHandledTime.getTimeAsLong(),
                "Time should be converted to epoch milliseconds when it's provided in a request in "
                    + "epoch seconds with decimals"
            )
        );
    }

    @Test
    @DisplayName("Test handleTime with epoch milliseconds, 13 digits")
    void testHandleTimeWithEpochMilliseconds13digits() {
        String content = "{\"event\": \"Pony 1 has left the barn\", "
            + "\"sourcetype\":\"mysourcetype\", \"time\": 1433188255253}";
        TimestampedHttpEventData eventData = new TimestampedHttpEventData();

        JsonNode jsonNode;

        try {
            jsonNode = objectMapper.readTree(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final TimestampedHttpEventData httpEventDataWithHandledTime = eventData.handleTime(
            jsonNode,
            null
        );

        Assertions.assertAll(
            () -> Assertions.assertEquals(
                "reported",
                httpEventDataWithHandledTime.getTimeSource(),
                "Time source should be 'reported' when the time is specified in a request"),
            () -> Assertions.assertTrue(
                httpEventDataWithHandledTime.isTimeParsed(),
                "timeParsed should be true when time is specified in a request"),
            () -> Assertions.assertEquals(
                1433188255253L,
                httpEventDataWithHandledTime.getTimeAsLong(),
                "Time should be converted to epoch milliseconds when it's provided in a request in "
                    + "epoch seconds with decimals")
        );
    }

    @Test
    @DisplayName("Test handleTime with time as String")
    void testHandleTimeWithTimeAsString() {
        String content = "{\"event\": \"Pony 1 has left the barn\", "
            + "\"sourcetype\":\"mysourcetype\", \"time\": \"1433188255253\"}";
        TimestampedHttpEventData eventData = new TimestampedHttpEventData();

        JsonNode jsonNode;

        try {
            jsonNode = objectMapper.readTree(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final TimestampedHttpEventData httpEventDataWithHandledTime = eventData.handleTime(
            jsonNode,
            null
        );

        Assertions.assertAll(
            () -> Assertions.assertEquals(
                "generated",
                httpEventDataWithHandledTime.getTimeSource(),
                "Time source should be 'generated' when time is given as a string in a request"),
            () -> Assertions.assertFalse(
                httpEventDataWithHandledTime.isTimeParsed(),
                "timeParsed should be false when time is given as a string in a request"),
            () -> Assertions.assertEquals(
                0,
                httpEventDataWithHandledTime.getTimeAsLong(),
                "Time should be 0 when time is given as a string in a request")
        );
    }

    @Test
    @DisplayName("Test handleTime with too little digits")
    void testHandleTimeWithTooLittleDigits() {
        String content = "{\"event\": \"Pony 1 has left the barn\", "
            + "\"sourcetype\":\"mysourcetype\", \"time\": 143318}";
        TimestampedHttpEventData eventData = new TimestampedHttpEventData();

        JsonNode jsonNode;

        try {
            jsonNode = objectMapper.readTree(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final TimestampedHttpEventData httpEventDataWithHandledTime = eventData.handleTime(
            jsonNode,
            null
        );

        Assertions.assertAll(
            () -> Assertions.assertEquals(
                "generated",
                httpEventDataWithHandledTime.getTimeSource(),
                "Time source should be 'generated' when time is given as an integer with less "
                    + "than 10"
                    + " digits"),
            () -> Assertions.assertFalse(
                httpEventDataWithHandledTime.isTimeParsed(),
                "timeParsed should be false when time is given as an integer with less than 10 "
                    + "digits"),
            () -> Assertions.assertEquals(
                143318,
                httpEventDataWithHandledTime.getTimeAsLong(),
                "Time as long should be as provided in the request")
        );
    }

    @Test
    @DisplayName("Test handleTime with epoch centiseconds")
    void testHandleTimeWithEpochCentiseconds() {
        String content = "{\"event\": \"Pony 1 has left the barn\", "
            + "\"sourcetype\":\"mysourcetype\", \"time\": 143318825525}";
        TimestampedHttpEventData eventData = new TimestampedHttpEventData();

        JsonNode jsonNode;

        try {
            jsonNode = objectMapper.readTree(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final TimestampedHttpEventData httpEventDataWithHandledTime = eventData.handleTime(
            jsonNode,
            null
        );
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                "reported",
                httpEventDataWithHandledTime.getTimeSource(),
                "Time source should be 'reported' when the time is specified in a request "
                    + "with 10-13 "
                    + "digits"),
            () -> Assertions.assertTrue(
                httpEventDataWithHandledTime.isTimeParsed(),
                "timeParsed should be true when time is specified in a request with 10-13 digits"),
            () -> Assertions.assertEquals(
                1433188255250L,
                httpEventDataWithHandledTime.getTimeAsLong(),
                "Time should be converted to epoch milliseconds when provided in a request with "
                    + "10-13 digits")
        );
    }

    @Test
    @DisplayName("Test handleTime with too many digits")
    void testHandleTimeWithTooManyDigits() {
        String content = "{\"event\": \"Pony 1 has left the barn\", "
            + "\"sourcetype\":\"mysourcetype\", \"time\": 1433188255252321}";
        TimestampedHttpEventData eventData = new TimestampedHttpEventData();

        JsonNode jsonNode;

        try {
            jsonNode = objectMapper.readTree(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final TimestampedHttpEventData httpEventDataWithHandledTime = eventData.handleTime(
            jsonNode,
            null
        );
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                "generated",
                httpEventDataWithHandledTime.getTimeSource(),
                "Time source should be 'generated' when time is given as an integer with more "
                    + "than 13 digits"),
            () -> Assertions.assertFalse(
                httpEventDataWithHandledTime.isTimeParsed(),
                "timeParsed should be false when time is given as an integer with more than 13 "
                    + "digits"),
            () -> Assertions.assertEquals(
                1433188255252321L,
                httpEventDataWithHandledTime.getTimeAsLong(),
                "Time should be as it's provided in a request."
            )
        );
    }


    /*
     * EventManager needs to handle the time stamp, if it is provided in the
     * request. This method tests the handling of time.
     */
//    @Test
//    public void handleTimeTest() {
//        // Content strings are created with different kinds of "time"-fields amd they
//        // are read into a JsonNode object.
//
//        // content1: time is in epoch seconds (10 digits)
//        String content1 = "{\"event\": \"Pony 1 has left the barn\", \"sourcetype\":
//        \"mysourcetype\", \"time\": 1277464192}";
//        // content2: "time"-field is not given
//        String content2 = "{\"event\": \"Pony 1 has left the barn\", \"sourcetype\":
//        \"mysourcetype\"}";
//        // content3: time is given in epoch seconds and a decimal giving the epoch
//        // milliseconds
//        String content3 = "{\"event\": \"Pony 1 has left the barn\", \"sourcetype\":
//        \"mysourcetype\", \"time\": 1433188255.253}";
//        // content4: time is given in epoch milliseconds (13 digits)
//        String content4 = "{\"event\": \"Pony 1 has left the barn\", \"sourcetype\":
//        \"mysourcetype\", \"time\": 1433188255253}";
//        // content5: time is given as a string
//        String content5 = "{\"event\": \"Pony 1 has left the barn\", \"sourcetype\":
//        \"mysourcetype\", \"time\": \"1433188255253\"}";
//        // content6: time is given with too small amount of digits
//        String content6 = "{\"event\": \"Pony 1 has left the barn\", \"sourcetype\":
//        \"mysourcetype\", \"time\": 143318}";
//        // content7: time is given in epoch centiseconds
//        String content7 = "{\"event\": \"Pony 1 has left the barn\", \"sourcetype\":
//        \"mysourcetype\", \"time\": 143318825525}";
//        // content8: time is given with too many digits
//        String content8 = "{\"event\": \"Pony 1 has left the barn\", \"sourcetype\":
//        \"mysourcetype\", \"time\": 1433188255252321}";
//
//        JsonNode node1 = null;
//        JsonNode node2 = null;
//        JsonNode node3 = null;
//        JsonNode node4 = null;
//        JsonNode node5 = null;
//        JsonNode node6 = null;
//        JsonNode node7 = null;
//        JsonNode node8 = null;
//
//        try {
//            node1 = objectMapper.readTree(content1);
//            node2 = objectMapper.readTree(content2);
//            node3 = objectMapper.readTree(content3);
//            node4 = objectMapper.readTree(content4);
//            node5 = objectMapper.readTree(content5);
//            node6 = objectMapper.readTree(content6);
//            node7 = objectMapper.readTree(content7);
//            node8 = objectMapper.readTree(content8);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
//        DefaultHttpEventData testData1 = new DefaultHttpEventData();
//        DefaultHttpEventData testData2 = new DefaultHttpEventData();
//        DefaultHttpEventData testData3 = new DefaultHttpEventData();
//        DefaultHttpEventData testData4 = new DefaultHttpEventData();
//        DefaultHttpEventData testData5 = new DefaultHttpEventData();
//        DefaultHttpEventData testData6 = new DefaultHttpEventData();
//        DefaultHttpEventData testData7 = new DefaultHttpEventData();
//        DefaultHttpEventData testData8 = new DefaultHttpEventData();
//
//        /*
//         * Testing the getTimeSource(), isTimeParsed() and getTimeAsLong() methods from
//         * the HttpEventData objects that were returned from EventManager's handleTime()
//         * method.
//         */
//        assertEquals("Time source should be 'reported' when the time is specified in a
//        request", "reported",
//            testData1.getTimeSource());
//        assertTrue("timeParsed should be true when the time is specified in a request",
//        testData1.isTimeParsed());
//        assertEquals("Time should have been converted to epoch milliseconds", 1277464192000L,
//            testData1.getTimeAsLong());
//
//        assertEquals("Time source should be 'generated' when it's not specified in a request",
//        "generated",
//            testData2.getTimeSource());
//        assertFalse("timeParsed should be false when time is not specified in a request",
//        testData2.isTimeParsed());
//        assertEquals("Time as long should be 0 when time is not specified in a request", 0,
//        testData2.getTimeAsLong());
//
//        assertEquals("Time source should be 'reported' when the time is specified in a
//        request", "reported",
//            testData3.getTimeSource());
//        assertTrue("timeParsed should be true when time is specified in a request.", testData3
//        .isTimeParsed());
//        assertEquals(
//            "Time should be converted to epoch milliseconds when it's provided in a request in
//            epoch seconds with decimals.",
//            1433188255253L, testData3.getTimeAsLong());
//
//        assertEquals("Time source should be 'reported' when the time is specified in a
//        request", "reported",
//            testData4.getTimeSource());
//        assertTrue("timeParsed should be true when time is specified in a request.", testData4
//        .isTimeParsed());
//        assertEquals("Time should be in epoch milliseconds when it is provided as epoch
//        milliseconds in the request",
//            1433188255253L, testData4.getTimeAsLong());
//
//        assertEquals("Time source should be 'generated' when time is given as a string in a
//        request", "generated",
//            testData5.getTimeSource());
//        assertFalse("timeParsed should be false when time is given as a string in a request",
//        testData5.isTimeParsed());
//        assertEquals("Time should be 0 when time is given as a string in a request", 0,
//        testData5.getTimeAsLong());
//
//        assertEquals("Time source should be 'generated' when time is given as an integer with
//        less than 10 digits",
//            "generated", testData6.getTimeSource());
//        assertFalse("timeParsed should be false when time is given as an integer with less than
//        10 digits",
//            testData6.isTimeParsed());
//        assertEquals("Time as long should be as provided in the request.", 143318, testData6
//        .getTimeAsLong());
//
//        assertEquals("Time source should be 'reported' when the time is specified in a request
//        with 10-13 digits",
//            "reported", testData7.getTimeSource());
//        assertTrue("timeParsed should be true when time is specified in a request with 10-13
//        digits",
//            testData7.isTimeParsed());
//        assertEquals("Time should be converted to epoch milliseconds when provided in a request
//        with 10-13 digits",
//            1433188255250L, testData7.getTimeAsLong());
//
//        assertEquals("Time source should be 'generated' when time is given as an integer with
//        more than 13 digits",
//            "generated", testData8.getTimeSource());
//        assertFalse("timeParsed should be false when time is given as an integer with more than
//        13 digits",
//            testData8.isTimeParsed());
//        assertEquals("Time should be as it's provided in a request.", 1433188255252321L,
//        testData8.getTimeAsLong());
//    }
}