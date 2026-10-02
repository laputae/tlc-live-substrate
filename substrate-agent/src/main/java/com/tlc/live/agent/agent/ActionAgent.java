package com.tlc.live.agent.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlc.live.agent.config.AgentProperties;
import com.tlc.live.agent.model.RedPacketDecision;
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
 * 场控执行 Agent：冷场时自主决定是否发红包、发多少。
 *
 * <p>LLM 只负责"建议"，金额与数量最终被预算围栏硬性裁剪（clamp）——
 * AI 永远花不出超过 {@code tlc.agent.budget-max-*} 的钱。
 */
@Component
public class ActionAgent {

    private static final Logger log = LoggerFactory.getLogger(ActionAgent.class);

    public record Decision(boolean shouldDrop, int totalFen, int count, String reason) {
    }

    private static final String SYSTEM_PROMPT = """
            你是直播间场控 Agent。当前冷场，需要决定是否发红包救场。
            只输出 JSON，格式：
            {"shouldDrop":true,"totalFen":5000,"count":50,"reason":"冷场超 30 秒"}
            totalFen 为总金额（分），count 为红包个数，count 不超过 50。禁止输出其他内容。
            """;

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final AgentProperties props;

    public ActionAgent(ChatModel chatModel, ObjectMapper objectMapper, AgentProperties props) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
        this.props = props;
    }

    public Decision decide(String roomId, SentimentAgent.Mood mood, List<String> danmakuSample) {
        String userPrompt = "直播间 " + roomId + " 情绪=" + mood.mood() + " 热度分=" + mood.score()
                + "。最近弹幕样本：\n" + String.join("\n", danmakuSample);
        try {
            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(SYSTEM_PROMPT),
                    new UserMessage(userPrompt)));
            AssistantMessage output = chatModel.call(prompt).getResult().getOutput();
            JsonNode node = objectMapper.readTree(output.getText());

            boolean shouldDrop = node.path("shouldDrop").asBoolean(false);
            if (!shouldDrop) {
                return new Decision(false, 0, 0, node.path("reason").asText("无需干预"));
            }
            // 预算围栏：LLM 建议值被硬性裁剪到配置上限内
            int totalFen = Math.clamp(node.path("totalFen").asInt(1000), 1, props.getBudgetMaxTotalFen());
            int count = Math.clamp(node.path("count").asInt(20), 1, Math.min(props.getBudgetMaxCount(), totalFen));
            return new Decision(true, totalFen, count, node.path("reason").asText("冷场救场"));
        } catch (Exception ex) {
            log.warn("决策失败，放弃本次干预: {}", ex.getMessage());
            return new Decision(false, 0, 0, "决策失败: " + ex.getMessage());
        }
    }
}
