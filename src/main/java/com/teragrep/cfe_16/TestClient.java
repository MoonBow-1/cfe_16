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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.SocketException;

/**
 * A multithreaded load test client for the cfe_16 server.
 */
public class TestClient implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestClient.class);
    /**
     * How many loops a single thread does.
     */
    private final int n;

    /**
     * Hostname or IP address of the cfe_16 server.
     */
    private final String host;

    /**
     * TCP port of the cfe_16 server.
     */
    private final int port;

    public TestClient(int n, String host, int port) throws IOException {
        this.n = n;
        this.host = host;
        this.port = port;
        LOGGER.info("Initialized TestClient, sending <[{}]> messages to <[{}]>:<[{}]>", n, host, port);
    }

    public void run() {
        Socket socket;
        try {
            socket = createSocket();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (int i = 0; i < this.n; i++) {

            if (i % 10 == 0) {
                System.out.print('.');
            }

            socket = testSendEvent(socket);
            socket = testSendEventWithAckID(socket);
            socket = testSendEventWithoutAuthorization(socket);
            socket = testSendEventWithoutEventField(socket);
            socket = testSendEventWithBlankEventField(socket);
            socket = testGetAcksWithoutChannel(socket);
            socket = testGetAcksWithInvalidChannel(socket);
        }
        try {
            socket.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Socket testGetAcksWithInvalidChannel(Socket socket) {
        String path = "/services/collector/ack?channel=CHANNEL_22222";
        String request = "{\"acks\": [1,3,4]}";
        String expectedRegex = "\\{\"text\":\"Invalid data channel\",\"code\":11,\"invalid-event-number\":0\\}";
        String authorization = "AUTH_TOKEN_11111";
        socket = doRequestAndVerifyReply(
                socket, path, expectedRegex, request, authorization, HttpURLConnection.HTTP_BAD_REQUEST
        );
        return socket;
    }

    private Socket testGetAcksWithoutChannel(Socket socket) {
        String path = "/services/collector/ack";
        String request = "{\"acks\": [1,3,4]}";
        String expectedRegex = "\\{\"text\":\"Data channel is missing\",\"code\":10,\"invalid-event-number\":0\\}";
        String authorization = "AUTH_TOKEN_11111";
        socket = doRequestAndVerifyReply(
                socket, path, expectedRegex, request, authorization, HttpURLConnection.HTTP_BAD_REQUEST
        );
        return socket;
    }

    private Socket testSendEventWithBlankEventField(Socket socket) {
        String path = "/services/collector?channel=00872DC6-AC83-4EDE-8AFE-8413C3825C4C";
        String request = "{\"sourcetype\": \"mysourcetype\", \"event\": \"\"}";
        String expectedRegex = "\\{\"text\":\"Event field cannot be blank\",\"code\":13,\"invalid-event-number\":0\\}";
        String authorization = "AUTH_TOKEN_11111";
        socket = doRequestAndVerifyReply(
                socket, path, expectedRegex, request, authorization, HttpURLConnection.HTTP_BAD_REQUEST
        );
        return socket;
    }

    private Socket testSendEventWithoutEventField(Socket socket) {
        String path = "/services/collector?channel=00872DC6-AC83-4EDE-8AFE-8413C3825C4C";
        String request = "{\"sourcetype\": \"mysourcetype\"}";
        String expectedRegex = "\\{\"text\":\"Event field is required\",\"code\":12,\"invalid-event-number\":0\\}";
        String authorization = "AUTH_TOKEN_11111";
        socket = doRequestAndVerifyReply(
                socket, path, expectedRegex, request, authorization, HttpURLConnection.HTTP_BAD_REQUEST
        );
        return socket;
    }

    private Socket testSendEventWithoutAuthorization(Socket socket) {
        String path = "/services/collector?channel=00872DC6-AC83-4EDE-8AFE-8413C3825C4C";
        String request = "{\"sourcetype\": \"mysourcetype\", \"event\": \"Hello, world!\"}}";
        String expectedRegex = "\\{\"text\":\"Token is required\",\"code\":2,\"invalid-event-number\":0\\}";
        socket = doRequestAndVerifyReply(
                socket, path, expectedRegex, request, null, HttpURLConnection.HTTP_UNAUTHORIZED
        );
        return socket;
    }

    private Socket testSendEventWithAckID(Socket socket) {
        String path = "/services/collector?channel=CHANNEL_11111";
        String request = "{\"sourcetype\": \"mysourcetype\", \"event\": \"Hello, world!\"}";
        String expectedRegex = "\\{\"text\":\"Success\",\"code\":0,\"ackID\":([0-9])+\\}";
        String authorization = "AUTH_TOKEN_11111";
        socket = doRequestAndVerifyReply(
                socket, path, expectedRegex, request, authorization, HttpURLConnection.HTTP_OK
        );
        return socket;
    }

    private Socket testSendEvent(Socket socket) {
        String path = "/services/collector";
        String request = "{\"sourcetype\":\"access\", \"source\":\"/var/log/access.log\", \"event\": {\"message\":\"Access log test message\"}} {\"sourcetype\":\"access\", \"source\":\"/var/log/access.log\", \"event\": {\"message\":\"Access log test message 2\"}}";
        String expectedRegex = "\\{\"text\":\"Success\",\"code\":0\\}";
        String authorization = "AUTH_TOKEN_11111";
        socket = doRequestAndVerifyReply(
                socket, path, expectedRegex, request, authorization, HttpURLConnection.HTTP_OK
        );
        return socket;
    }

    private Socket createSocket() throws IOException {
        Socket socket = new Socket(this.host, this.port);
        socket.setKeepAlive(true);
        socket.setTcpNoDelay(true);
        return socket;
    }

    private Socket doRequestAndVerifyReply(
            Socket socket,
            String path,
            String expectedRegex,
            String request,
            String authorization,
            int expectedHttpStatusCode
    ) {
        while (true) {
            try {
                // ensure that the TCP connection is alive
                socket = ensureTcpConnection(socket);

                // send the http request
                sendRequest(socket, path, request, authorization);

                BufferedReader bufferedReader = getReader(socket);
                // read first line of the HTTP response
                String line;
                try {
                    line = bufferedReader.readLine();
                    if (line == null) {
                        // retry
                        cleanup(socket, bufferedReader);
                        continue;
                    }
                }
                catch (SocketException e) {
                    // server RST'ed connection, retry
                    cleanup(socket, bufferedReader);
                    continue;
                }

                // parse the status code
                checkHttpStatusCode(expectedHttpStatusCode, line);

                // skip headers, check for Connection: close
                // if connection is closed, a new socket is created
                // but the response can still be read from bufferedReader
                socket = readHeaders(socket, bufferedReader);

                // read the response
                String responseBody = readResponseBody(bufferedReader);
                // check if the response matches to regex
                validateResponseBody(expectedRegex, responseBody);
                // we are done
                break;
            }
            catch (SocketException e) {
                // try again
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return socket;
    }

    /**
     * Validates the response body line against the given regural expression.
     *
     * @param expectedRegex
     * @param responseBody
     */
    private void validateResponseBody(String expectedRegex, String responseBody) {
        if (!responseBody.matches(expectedRegex)) {
            throw new RuntimeException(expectedRegex + " did not match to " + responseBody);
        }
    }

    /**
     * Verifies that the HTTP reply status code was what it was supposed to me.
     *
     * @param expectedHttpStatusCode
     * @param line
     */
    private void checkHttpStatusCode(int expectedHttpStatusCode, String line) {
        String statusCodeText = line.substring(9, 12);
        int responseCode = Integer.parseInt(statusCodeText);
        if (responseCode != expectedHttpStatusCode) {
            throw new RuntimeException("HTTP/" + responseCode + " which was not expected.");
        }
    }

    /**
     * Advances buffered reader through HTTP headers. If "Connection: close" header is seen, the socket is closed and a
     * new one is created.
     *
     * @param socket
     * @param bufferedReader
     * @return
     * @throws IOException
     */
    private Socket readHeaders(Socket socket, BufferedReader bufferedReader) throws IOException {
        String line;
        while (true) {
            line = bufferedReader.readLine();
            if (line.equals("Connection: close\r\n")) {
                cleanup(socket, bufferedReader);
                socket = this.createSocket();
                break;
            }
            if (line.equals("")) {
                break;
            }
        }
        return socket;
    }

    /**
     * Reads the reply line from the servere. Chunked transfer is assumed.
     *
     * @param bufferedReader
     * @return
     * @throws IOException
     */
    private String readResponseBody(BufferedReader bufferedReader) throws IOException {
        // skip chunked tag before body
        bufferedReader.readLine();
        // read the json response body
        String responseBody = bufferedReader.readLine();
        bufferedReader.readLine();
        return responseBody;
    }

    private BufferedReader getReader(Socket socket) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        return bufferedReader;
    }

    /**
     * Checks that the TCP connection is open, otherwis a new connection is created and returned.
     *
     * @param socket
     * @return
     * @throws IOException
     */
    private Socket ensureTcpConnection(Socket socket) throws IOException {
        if (socket.isInputShutdown() || socket.isClosed() || !socket.isConnected()) {
            socket.close();
            socket = createSocket();
        }
        return socket;
    }

    /**
     * Does a HTTP request and keeps connection open.
     *
     * @param socket
     * @param path
     * @param request
     * @param authorization
     * @throws IOException
     */
    private void sendRequest(Socket socket, String path, String request, String authorization) throws IOException {
        OutputStream outputStream = socket.getOutputStream();
        PrintWriter printWriter = new PrintWriter(outputStream);
        printWriter.write("POST " + path + " HTTP/1.1\r\n");
        printWriter.write("Connection: keep-alive\r\n");
        printWriter.write("Content-Type: application/json;charset=UTF-8\r\n");
        printWriter.write("Accept: application/json\r\n");
        if (authorization != null) {
            printWriter.write("Authorization: " + authorization + "\r\n");
        }
        printWriter.write("User-Agent: cfe_16 test client v1\r\n");
        printWriter.write("Host: localhost:" + this.port + "\r\n");
        printWriter.write("Content-Length: " + request.length() + "\r\n");
        printWriter.write("\r\n");
        printWriter.write(request);
        printWriter.flush();
    }

    private void cleanup(Socket socket, BufferedReader bufferedReader) throws IOException {
        bufferedReader.close();
        socket.close();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length >= 4) {
            String host = args[0];
            int port = Integer.valueOf(args[1]);
            LOGGER.info("Connecting to <[{}]>:<[{}]>", host, port);
            int numberOfThreads = Integer.valueOf(args[2]);
            int numberOfLoops = Integer.valueOf(args[3]);
            TestClient[] testClients = createTestClients(host, port, numberOfThreads, numberOfLoops);
            Thread[] threads = createThreads(numberOfThreads, testClients);
            long t1 = System.currentTimeMillis();
            startThreads(numberOfThreads, threads);
            waitThreadsToFinish(numberOfThreads, threads);
            long t2 = System.currentTimeMillis();
            long dt = t2 - t1;
            int numberOfRequests = numberOfThreads * numberOfLoops;
            double millisecsPerRequest = (double) dt / (double) numberOfRequests;
            double throughput = 1000.0 / millisecsPerRequest;
            LOGGER
                    .info(
                            "Did <[{}]> requests in <{}> milliseconds, that is <{}> ms/req., which is <{}> transactions/sec.",
                            numberOfRequests, dt, millisecsPerRequest, throughput
                    );
            FileOutputStream fileOutputStream = new FileOutputStream("stats.csv", true);
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(fileOutputStream));
            String line = numberOfThreads + "," + millisecsPerRequest + "," + throughput + "\n";
            bufferedWriter.write(line);
            bufferedWriter.close();
            fileOutputStream.close();
        }
        else {
            LOGGER.error("Usage: Usage: <host> <port> <n threads> <n loops>");
        }
    }

    private static void waitThreadsToFinish(int numberOfThreads, Thread[] threads) throws InterruptedException {
        for (int i = 0; i < numberOfThreads; i++) {
            LOGGER.debug("Waiting for thread <{}> out of <{}> threads to finish", i, numberOfThreads);
            threads[i].join();
            LOGGER.debug("Thread <[{}]> of <[{}]> threads finished", i, numberOfThreads);
        }
    }

    private static void startThreads(int numberOfThreads, Thread[] threads) {
        LOGGER.debug("Starting <[{}]> threads", numberOfThreads);
        for (int i = 0; i < numberOfThreads; i++) {
            LOGGER.debug("Starting thread <{}> of <{}>", i, numberOfThreads);
            threads[i].start();
        }
    }

    private static Thread[] createThreads(int numberOfThreads, TestClient[] testClients) {
        LOGGER.debug("Creating <[{}]> threads", numberOfThreads);
        Thread[] threads = new Thread[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            LOGGER.debug("Creating thread <{}> of <[{}]>", i, numberOfThreads);
            threads[i] = new Thread(testClients[i]);
        }
        return threads;
    }

    private static TestClient[] createTestClients(String host, int port, int numberOfThreads, int numberOfLoops)
            throws IOException {
        LOGGER.debug("Creating <[{}]> test clients", numberOfThreads);
        TestClient[] testClients = new TestClient[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            LOGGER.debug("Creating testClient <{}> of <[{}]>", i, numberOfThreads);
            testClients[i] = new TestClient(numberOfLoops, host, port);
        }
        return testClients;
    }
}
