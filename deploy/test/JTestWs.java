import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.*;
import java.util.regex.*;

public class JTestWs {

    static final CompletableFuture<String> DROP_ID = new CompletableFuture<>();

    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        WebSocket push = client.newWebSocketBuilder()
                .buildAsync(URI.create("ws://push:8085/ws/live"), new WebSocket.Listener() {
                    StringBuilder sb = new StringBuilder();

                    @Override
                    public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                        sb.append(data);
                        if (last) {
                            String msg = sb.toString();
                            sb.setLength(0);
                            System.out.println("[push] " + msg);
                            Matcher m = Pattern.compile("\"redPacketId\":(\\d+)").matcher(msg);
                            if (msg.contains("REDPACKET_DROP") && m.find()) {
                                DROP_ID.complete(m.group(1));
                            }
                        }
                        ws.request(1);
                        return null;
                    }
                }).join();

        WebSocket gw = client.newWebSocketBuilder()
                .buildAsync(URI.create("ws://gateway:8081/ws/upstream"), new WebSocket.Listener() {
                    @Override
                    public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                        System.out.println("[gw-ack] " + data);
                        ws.request(1);
                        return null;
                    }
                }).join();

        String[] contents = {
                "zhubo zai ma", "hao wu liao", "mei yi si", "kun le", "leng chang", "tai an jing"};

        System.out.println(">>> send 6 cold danmaku...");
        for (int i = 0; i < contents.length; i++) {
            gw.sendText("{\"type\":\"DANMAKU\",\"userId\":" + (201 + i)
                    + ",\"roomId\":\"room-1\",\"content\":\"" + contents[i] + "\"}", true);
            Thread.sleep(300);
        }

        System.out.println(">>> wait for REDPACKET_DROP (max 75s)...");
        String id = DROP_ID.get(75, TimeUnit.SECONDS);
        System.out.println(">>> DROP id=" + id + ", rush via gateway->risk->MQ...");

        gw.sendText("{\"type\":\"RUSH\",\"userId\":201,\"roomId\":\"room-1\",\"redPacketId\":" + id + "}", true);
        Thread.sleep(2500);
        gw.sendText("{\"type\":\"RUSH\",\"userId\":201,\"roomId\":\"room-1\",\"redPacketId\":" + id + "}", true);
        Thread.sleep(2500);

        System.out.println("DONE.");
        System.exit(0);
    }
}