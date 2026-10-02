package com.tlc.live.agent.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 情绪感知 Agent：聚合弹幕样本，让 LLM 输出严格 JSON 的情绪判定。
 */
@Component
public class SentimentAgent {

    private static final Logger log = LoggerFactory.getLogger(SentimentAgent.class);

    public record Mood(String mood, int score) {
    }

    private static final String SYSTEM_PROMPT = """
            你是直播间情绪分析器。根据最近一批弹幕判断直播间氛围。
            mood 取值：HOT(热烈) / WARM(正常) / COLD(冷场，需要救场)。
            只输出 JSON，格式：{"mood":"WARM","score":60}，score 为 0-100 的热度分，禁止输出其他内容。
            """;

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    public SentimentAgent(ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    public Mood analyze(String roomId, List<String> danmaku) {
        String userPrompt = "直播间 " + roomId + " 最近弹幕(" + danmaku.size() + " 条)：\n"
                + String.join("\n", danmaku);
        try {
            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(SYSTEM_PROMPT),
                    new UserMessage(userPrompt)));
            AssistantMessage output = chatModel.call(prompt).getResult().getOutput();
            JsonNode node = objectMapper.readTree(output.getText());
            return new Mood(node.path("mood").asText("UNKNOWN"), node.path("score").asInt(50));
        } catch (Exception ex) {
            log.warn("情绪分析失败，按 UNKNOWN 处理: {}", ex.getMessage());
            return new Mood("UNKNOWN", 50);
        }
    }
}
