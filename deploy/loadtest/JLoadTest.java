import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.regex.*;
import java.util.*;

public class JLoadTest {

    static final AtomicInteger ACK_PASS = new AtomicInteger();
    static final AtomicInteger ACK_REJECT = new AtomicInteger();
    static final AtomicInteger SENT = new AtomicInteger();

    static void log(String msg) {
        System.out.println(msg);
        System.out.flush();
    }

    public static void main(String[] args) throws Exception {
        int users = args.length > 0 ? Integer.parseInt(args[0]) : 2000;
        int conns = args.length > 1 ? Integer.parseInt(args[1]) : 500;
        int perConn = users / conns;
        String gwUri = "ws://gateway:8081/ws/upstream";
        String seckill = "http://seckill:8084";
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

        log("step1: drop red packet, users=" + users);
        String body = "{\"roomId\":\"load-1\",\"totalFen\":" + users * 10 + ",\"count\":" + users + "}";
        HttpResponse<String> drop = http.send(HttpRequest.newBuilder(URI.create(seckill + "/api/seckill/drop"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        Matcher m = Pattern.compile("\\d+").matcher(drop.body());
        if (!m.find()) {
            log("FATAL: drop failed: " + drop.body());
            System.exit(1);
        }
        long rpId = Long.parseLong(m.group());
        log("step2: prepared redPacketId=" + rpId + " count=" + users);

        CountDownLatch connLatch = new CountDownLatch(conns);
        List<WebSocket> sockets = new CopyOnWriteArrayList<>();
        ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();
        for (int i = 0; i < conns; i++) {
            pool.submit(() -> {
                try {
                    WebSocket ws = http.newWebSocketBuilder()
                            .connectTimeout(Duration.ofSeconds(15))
                            .buildAsync(URI.create(gwUri), new WebSocket.Listener() {
                                @Override
                                public CompletionStage<?> onText(WebSocket w, CharSequence data, boolean last) {
                                    String msg = data.toString();
                                    if (msg.contains("\"risk\":\"PASS\"")) {
                                        ACK_PASS.incrementAndGet();
                                    } else if (msg.contains("risk")) {
                                        ACK_REJECT.incrementAndGet();
                                    }
                                    w.request(1);
                                    return null;
                                }
                            }).join();
                    sockets.add(ws);
                    connLatch.countDown();
                } catch (Exception e) {
                    log("conn fail: " + e);
                }
            });
        }
        if (!connLatch.await(60, TimeUnit.SECONDS)) {
            log("FATAL: connections timeout, ready=" + sockets.size());
            System.exit(1);
        }
        log("step3: connections ready=" + sockets.size());
        Thread.sleep(1000);

        long sendStart = System.currentTimeMillis();
        CountDownLatch sendLatch = new CountDownLatch(sockets.size());
        for (int i = 0; i < sockets.size(); i++) {
            final WebSocket ws = sockets.get(i);
            final int base = i * perConn;
            pool.submit(() -> {
                try {
                    for (int j = 0; j < perConn; j++) {
                        long uid = 100000L + base + j;
                        ws.sendText("{\"type\":\"RUSH\",\"userId\":" + uid
                                + ",\"roomId\":\"load-1\",\"redPacketId\":" + rpId + "}", true);
                        SENT.incrementAndGet();
                    }
                } finally {
                    sendLatch.countDown();
                }
            });
        }
        sendLatch.await();
        long sendEnd = System.currentTimeMillis();
        double secs = (sendEnd - sendStart) / 1000.0;
        log(String.format("step4: SENT %d rushes in %.2fs -> %.0f msg/s", SENT.get(), secs, SENT.get() / secs));

        long deadline = System.currentTimeMillis() + 240_000;
        int last = -1;
        while (System.currentTimeMillis() < deadline) {
            int ack = ACK_PASS.get() + ACK_REJECT.get();
            if (ack >= SENT.get()) {
                break;
            }
            if (ack != last) {
                log(String.format("step5: acks %d/%d (PASS=%d REJECT=%d)", ack, SENT.get(), ACK_PASS.get(), ACK_REJECT.get()));
                last = ack;
            }
            Thread.sleep(2000);
        }
        log(String.format("FINAL acks PASS=%d REJECT=%d, drain+ack %.1fs after send end",
                ACK_PASS.get(), ACK_REJECT.get(), (System.currentTimeMillis() - sendEnd) / 1000.0));
        log("DONE-LOADTEST");
        System.exit(0);
    }
}