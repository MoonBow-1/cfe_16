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
package com.teragrep.cfe_16.it;

import com.teragrep.cfe_16.response.AcknowledgedJsonResponse;
import com.teragrep.cfe_16.response.Response;
import com.teragrep.cfe_16.service.HECService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.TestPropertySource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.Selector;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@TestPropertySource(properties = {
        "syslog.server.host=127.0.0.1",
        "syslog.server.port=1236",
        "syslog.server.protocol=TCP",
        "max.channels=1000000",
        "max.ack.value=1000000",
        "max.ack.age=20000",
        "max.session.age=30000",
        "poll.time=30000",
        "server.print.times=true"
})
@SpringBootTest
public class SendEventsIT implements Runnable {

    @Autowired
    private HECService service;

    private ServerSocket serverSocket;
    private Thread thread;
    private AtomicInteger numberOfRequestsMade;

    private MockHttpServletRequest request1;
    private String eventInJson;
    private String channel1;
    private Selector selector;
    private final static int SERVER_PORT = 1236;
    private CountDownLatch countDownLatch = new CountDownLatch(0);

    @BeforeEach
    public void init() throws IOException {
        this.thread = new Thread(this);
        this.numberOfRequestsMade = new AtomicInteger(0);
        this.serverSocket = new ServerSocket(SERVER_PORT);
        this.selector = Selector.open();

        this.request1 = new MockHttpServletRequest();
        this.request1.addHeader("Authorization", "AUTH_TOKEN_11111");
        this.channel1 = "CHANNEL_11111";
        this.eventInJson = "{\"sourcetype\":\"access\", \"source\":\"/var/log/access.log\", \"event\": {\"message\":\"Access log test message 1\"}} {\"sourcetype\":\"access\", \"source\":\"/var/log/access.log\", \"event\": {\"message\":\"Access log test message 2\"}}";

        this.thread.start();
    }

    @AfterEach
    public void shutdown() {
        this.thread.interrupt();
    }

    public void run() {
        while (true) {
            try {
                Socket socket = this.serverSocket.accept();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    this.numberOfRequestsMade.getAndIncrement();
                    countDownLatch.countDown();
                }
                bufferedReader.close();
                socket.close();
                if (this.numberOfRequestsMade.get() % 5 == 0) {
                    this.serverSocket.close();
                    this.serverSocket = new ServerSocket(SERVER_PORT);
                }
            }
            catch (InterruptedIOException e) {
                break;
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void sendEventsTest() throws InterruptedException, ExecutionException {
        final int NUMBER_OF_EVENTS_TO_BE_SENT = 100;
        countDownLatch = new CountDownLatch(NUMBER_OF_EVENTS_TO_BE_SENT);
        ExecutorService es = Executors.newFixedThreadPool(8);
        final List<CompletableFuture<Response>> futures = new ArrayList<>();

        for (int i = 0; i < NUMBER_OF_EVENTS_TO_BE_SENT; i++) {
            final CompletableFuture<Response> future = CompletableFuture
                    .supplyAsync(() -> service.sendEvents(request1, channel1, eventInJson));
            futures.add(future);
        }
        final List<Response> supposedResponses = new ArrayList<>();
        for (int i = 0; i < NUMBER_OF_EVENTS_TO_BE_SENT; i++) {
            final Response supposedResponse = new AcknowledgedJsonResponse(HttpStatus.OK, "Success", i);
            supposedResponses.add(supposedResponse);
        }
        int countFuture = 0;
        for (final Future<Response> future : futures) {
            final Response actualResponse = future.get();
            Assertions
                    .assertTrue(supposedResponses.contains(actualResponse), "Service should return JSON object with fields 'text', 'code' and 'ackID' (ackID should be " + countFuture + ")");
            countFuture++;
        }
        countDownLatch.await(1, TimeUnit.SECONDS);
        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            while (NUMBER_OF_EVENTS_TO_BE_SENT * 2 != this.numberOfRequestsMade.get()) {
                Thread.sleep(500);
            }
        });
        es.shutdownNow();
    }

    @Disabled
    @Test
    public void send1EventTest() throws IOException, InterruptedException {
        countDownLatch = new CountDownLatch(1);
        String supposedResponse = "{\"text\":\"Success\",\"code\":0,\"ackID\":" + 0 + "}";
        Assertions
                .assertEquals(service.sendEvents(request1, channel1, eventInJson).toString(), supposedResponse, "Service should return JSON object with fields 'text', 'code' and 'ackID' (ackID should be " + 0 + ")");

        countDownLatch.await(5, TimeUnit.SECONDS);
        Assertions
                .assertEquals(
                        2, this.numberOfRequestsMade, "Number of events received should match the number of sent ones"
                );
    }
}
