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
import com.teragrep.rlp_03.Server;
import com.teragrep.rlp_03.ServerFactory;
import com.teragrep.rlp_03.config.Config;
import com.teragrep.rlp_03.delegate.DefaultFrameDelegate;
import com.teragrep.rlp_03.delegate.FrameDelegate;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@TestPropertySource(properties = {
        "syslog.server.host=127.0.0.1",
        "syslog.server.port=1236",
        "syslog.server.protocol=RELP",
        "max.channels=1000000",
        "max.ack.value=1000000",
        "max.ack.age=20000",
        "max.session.age=30000",
        "poll.time=30000",
        "server.print.times=true"
})
@SpringBootTest
public class SendEventsIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendEventsIT.class);
    @Autowired
    private HECService service;
    private static AtomicInteger numberOfRequestsMade;
    private static Server server;
    private MockHttpServletRequest request1;
    private String eventInJson;
    private String channel1;
    private CountDownLatch countDownLatch = new CountDownLatch(0);

    /**
     * Initialize a server where the EventManager can connect to in its postConstruct
     */
    @BeforeAll
    static void initServer() {
        final int SERVER_PORT = 1236;

        final Supplier<FrameDelegate> frameDelegateSupplier = () -> new DefaultFrameDelegate((frame) -> {
            LOGGER.debug(frame.relpFrame().payload().toString());
            numberOfRequestsMade.incrementAndGet();
        });
        final Config config = new Config(SERVER_PORT, 1);
        final ServerFactory serverFactory = new ServerFactory(config, frameDelegateSupplier);

        server = Assertions.assertDoesNotThrow(serverFactory::create);
        final Thread serverThread = new Thread(server);
        serverThread.start();
        Assertions.assertDoesNotThrow(server.startup::waitForCompletion);
    }

    @BeforeEach
    void init() {
        numberOfRequestsMade = new AtomicInteger(0);

        this.request1 = new MockHttpServletRequest();
        this.request1.addHeader("Authorization", "AUTH_TOKEN_11111");
        this.channel1 = "CHANNEL_11111";
        this.eventInJson = "{\"sourcetype\":\"access\", \"source\":\"/var/log/access.log\", \"event\": {\"message\":\"Access log test message 1\"}} {\"sourcetype\":\"access\", \"source\":\"/var/log/access.log\", \"event\": {\"message\":\"Access log test message 2\"}}";

    }

    @AfterAll
    static void stop() {
        Assertions.assertDoesNotThrow(server::stop);
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
            final Response supposedResponse = new AcknowledgedJsonResponse("Success", i);
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
            while (NUMBER_OF_EVENTS_TO_BE_SENT * 2 != numberOfRequestsMade.get()) {
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
                .assertEquals(2, numberOfRequestsMade, "Number of events received should match the number of sent ones");
    }
}
