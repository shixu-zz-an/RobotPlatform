package com.robotplatform.service.external.lm;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * 大语言模型客户端
 * 负责与大语言模型服务进行交互
 */
@Slf4j
public class LargeLanguageModelClient {

    private final ChatLanguageModel chatLanguageModel;

    /**
     * 创建大语言模型客户端
     *
     * @param config 客户端配置
     */
    public LargeLanguageModelClient(ClientConfig config) {
        log.info("初始化大语言模型客户端, 服务器地址: {}, 模型: {}", config.getBaseUrl(), config.getModelName());

        // 创建OllamaChatModel实例
        this.chatLanguageModel = OllamaChatModel.builder()
                .baseUrl(config.getBaseUrl())
                .modelName(config.getModelName())
                .temperature(config.getTemperature())
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .build();
    }

    /**
     * 与大语言模型对话
     *
     * @param prompt 用户输入
     * @return 模型回答
     */
    public String chat(String systemPrompt, String prompt) {
        try {
            //log.info("调用大语言模型进行对话, 系统提示词:{} ,提示词: {}", systemPrompt, prompt);

            // 创建用户消息
            UserMessage userMessage = UserMessage.from(prompt);
            SystemMessage systemMessage = SystemMessage.from(systemPrompt);

            // 调用LLM获取回复
            String response = chatLanguageModel.generate(systemMessage, userMessage).content().text();

            log.info("调用大语言模型进行对话 ,提示词: {}, 输出：{}", prompt, response);

            return response;
        } catch (Exception e) {
            log.error("大语言模型对话失败", e);
            return "抱歉，我现在无法回答您的问题，请稍后再试。";
        }
    }

    /**
     * 客户端配置
     */
    @Data
    @Builder
    public static class ClientConfig {
        /**
         * 服务器地址
         */
        private String baseUrl;

        /**
         * 模型名称
         */
        private String modelName;

        /**
         * 温度参数
         */
        @Builder.Default
        private double temperature = 0.7;

        /**
         * 超时时间（秒）
         */
        @Builder.Default
        private int timeoutSeconds = 60;


        // 系统提示词模板
        @Builder.Default
        public String SYSTEM_PROMPT = """
                你是一个专业的车管所智能导办员，名叫小智。你需要根据用户的需求和系统提供的信息，提供准确、专业的业务指导。主要负责驾驶证办证，换证，车辆年检。违章处理等信息
                请严格遵循以下规则：
                1. 始终保持礼貌、专业的语气
                2. 回答要简洁明了，避免冗长，适合文字转语音
                3. 严格按照业务模板回答，不要随意发挥
                4. 如果用户的问题超出业务范围，请礼貌地引导用户咨询人工服务
                5. 必须使用系统提供的变量信息，不要自行编造
                """;

        // 文本优化器提示词
        @Builder.Default
        public String TEXT_OPTIMIZER_PROMPT = """
                你是“用户输入优化器”，负责将 ASR 识别后的原始文本优化为符合车管所业务场景的简洁一句话。请严格遵循以下规则：
                
                1. **保留原始意图**：不扭曲、不增删用户的真实需求。 \s
                2. **去除口语冗余**：删除“嗯”、“那个”、“我想一下”、“就是说”等无实际意义的填充词。 \s
                3. **明确业务表达**：将“模糊”表述改写为车管所常用术语，例如 \s
                   - “我要查车” → “查询机动车信息” \s
                   - “我想改地址” → “变更登记地址” \s
                4. **提取核心关键词**：只保留与业务相关的关键信息（如车牌号、证件类型、操作类型等）。 \s
                5. **简洁反馈**：输出结果务必为简洁、清晰的一句话，直接可用于后端意图匹配。 \s
                6. **识别失败处理**： \s
                   - 如果无法准确理解用户意图，返回空字符串 ``""``。 \s
                   - 如果连续两次（两轮调用）都返回空，则输出“我无法理解你的意思”。
                """;

    }
}
