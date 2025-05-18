package com.robotplatform.service.impl;

import com.robotplatform.client.model.VoiceMessage;
import com.robotplatform.service.*;
import com.robotplatform.service.dify.DifyApiManager;
import com.robotplatform.service.dify.api.chatmessge.ChatMessageResponse;
import com.robotplatform.service.external.funasr.TranscriptionListener;
import com.robotplatform.service.external.funasr.TranscriptionResult;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.AbstractWebSocketMessage;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 语音对话服务实现
 */
@Slf4j
@Service
public class VoiceDialogServiceImpl implements VoiceDialogService {

    @Autowired
    private SpeechToTextService speechToTextService;

    @Autowired
    private LargeLanguageModelService languageModelService;

    @Autowired
    private TextToSpeechService textToSpeechService;

    @Autowired
    private UserService userService;

    @Resource
    private DifyApiManager difyApiManager;

    // 存储会话信息
    private final Map<String, SessionData> sessionDataMap = new ConcurrentHashMap<>();

    // 存储会话回调函数
    private final Map<String, Consumer<VoiceMessage>> sessionCallbacks = new ConcurrentHashMap<>();

    @Override
    public void processVoiceMessage(VoiceMessage voiceMessage, Consumer<VoiceMessage> responseCallback) {

        String sessionId = voiceMessage.getSessionId();
        //log.info("处理语音消息, 会话ID: {}, 消息类型: {}", sessionId, voiceMessage.getType());

        // 保存回调函数
        sessionCallbacks.put(sessionId, responseCallback);

        // 获取或创建会话数据
        SessionData sessionData = sessionDataMap.computeIfAbsent(sessionId, this::createSessionData);

        // 更新会话历史
        sessionData.getHistory().add(voiceMessage);

        // 根据消息类型处理
        if ("audio".equals(voiceMessage.getType())) {
            processAudioMessage(sessionId, voiceMessage);
        } else if ("text".equals(voiceMessage.getType())) {
            processTextMessage(sessionId, voiceMessage);
        } else {
            //log.warn("未知的消息类型: {}", voiceMessage.getType());
        }
    }

    @Override
    public void resetSession(String sessionId) {
        // 添加参数有效性检查
        if (sessionId == null) {
            log.warn("尝试重置空会话ID的对话会话");
            return;
        }

        // 清理会话数据
        SessionData sessionData = sessionDataMap.remove(sessionId);
        sessionCallbacks.remove(sessionId);

        if (sessionData != null) {
            // 更新WAV文件头
            if (sessionData.getCurrentAudioFile() != null) {
                try {
                    // 获取累积的音频数据大小
                    int totalDataSize = sessionData.getAudioBuffer().size();
                    
                    // 更新WAV文件头
                    try (RandomAccessFile raf = new RandomAccessFile(sessionData.getCurrentAudioFile(), "rw")) {
                        // 更新文件大小（不包括RIFF和大小字段）
                        raf.seek(4);
                        writeInt(raf, 36 + totalDataSize);
                        
                        // 更新data子块大小
                        raf.seek(40);
                        writeInt(raf, totalDataSize);
                    }
                    
                    log.info("已更新WAV文件头, 文件: {}, 总数据大小: {} 字节", 
                            sessionData.getCurrentAudioFile(), totalDataSize);
                } catch (Exception e) {
                    log.error("更新WAV文件头失败, 会话ID: {}", sessionId, e);
                }
            }

            // 关闭语音识别会话
            try {
                String asrSessionId = sessionData.getAsrSessionId();
                if (asrSessionId != null) {
                    speechToTextService.resetSession(asrSessionId);
                }
            } catch (Exception e) {
                log.error("重置语音识别会话失败, 会话ID: {}", sessionId, e);
            }

            // 清理大语言模型上下文
            try {
                languageModelService.clearContext(sessionId);
            } catch (Exception e) {
                log.error("清理大语言模型上下文失败, 会话ID: {}", sessionId, e);
            }
            try {
                textToSpeechService.closeSession(sessionId);
            } catch (Exception e) {
                log.error("关闭文本转语音会话失败, 会话ID: {}", sessionId, e);
            }

            log.info("重置会话状态, 会话ID: {}", sessionId);
        } else {
            log.debug("找不到对话会话数据, 会话ID: {}", sessionId);
        }
    }

    @Override
    public List<VoiceMessage> getSessionHistory(String sessionId) {
        SessionData sessionData = sessionDataMap.get(sessionId);
        if (sessionData != null) {
            return new ArrayList<>(sessionData.getHistory());
        }
        return Collections.emptyList();
    }

    @Override
    public String createSession() {
        String sessionId = "dialog-" + UUID.randomUUID().toString();
        createSessionData(sessionId);
        log.info("创建新会话, 会话ID: {}", sessionId);
        return sessionId;
    }

    /**
     * 保存音频数据为WAV文件
     */
    private void saveAudioToWav(String sessionId, byte[] audioData) {
        try {
            SessionData sessionData = sessionDataMap.get(sessionId);
            if (sessionData == null) {
                log.error("无法找到会话数据, 会话ID: {}", sessionId);
                return;
            }

            // 将新的音频数据添加到缓冲区
            sessionData.getAudioBuffer().write(audioData);

            // 如果是第一次保存，创建文件并写入WAV头
            if (sessionData.getCurrentAudioFile() == null) {
                // 创建保存目录
                String saveDir = "audio_records";
                Path dirPath = Paths.get(saveDir);
                if (!Files.exists(dirPath)) {
                    Files.createDirectories(dirPath);
                }

                // 生成文件名：sessionId_时间戳.wav
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String fileName = String.format("%s_%s.wav", sessionId, timestamp);
                Path filePath = dirPath.resolve(fileName);
                sessionData.setCurrentAudioFile(filePath.toString());

                // 创建音频格式
                AudioFormat format = new AudioFormat(
                        16000, // 采样率
                        16,    // 采样位数
                        1,     // 声道数
                        true,  // 有符号
                        false  // 大端序
                );

                // 写入WAV头
                try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                    // RIFF头
                    fos.write("RIFF".getBytes());
                    // 文件大小（不包括RIFF和大小字段）
                    writeInt(fos, 36 + audioData.length);
                    // WAVE标识
                    fos.write("WAVE".getBytes());
                    // fmt子块
                    fos.write("fmt ".getBytes());
                    // fmt子块大小
                    writeInt(fos, 16);
                    // 音频格式（1表示PCM）
                    writeShort(fos, (short) 1);
                    // 声道数
                    writeShort(fos, (short) 1);
                    // 采样率
                    writeInt(fos, 16000);
                    // 字节率
                    writeInt(fos, 16000 * 2);
                    // 块对齐
                    writeShort(fos, (short) 2);
                    // 位深度
                    writeShort(fos, (short) 16);
                    // data子块
                    fos.write("data".getBytes());
                    // data子块大小
                    writeInt(fos, audioData.length);
                }
            }

            // 追加音频数据到文件
            try (FileOutputStream fos = new FileOutputStream(sessionData.getCurrentAudioFile(), true)) {
                fos.write(audioData);
            }
        } catch (Exception e) {
            log.error("保存音频文件失败, 会话ID: {}", sessionId, e);
        }
    }

    private void writeInt(FileOutputStream fos, int value) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(value);
        fos.write(buffer.array());
    }

    private void writeShort(FileOutputStream fos, short value) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort(value);
        fos.write(buffer.array());
    }

    /**
     * 处理音频消息
     */
    private void processAudioMessage(String sessionId, VoiceMessage voiceMessage) {
        SessionData sessionData = sessionDataMap.get(sessionId);
        if (sessionData == null) {
            log.error("无法找到会话数据, 会话ID: {}", sessionId);
            return;
        }

        try {
            // 确保已创建ASR会话
            if (sessionData.getAsrSessionId() == null) {
                // 创建ASR会话和转录监听器
                TranscriptionListener listener = new TranscriptionListener() {
                    @Override
                    public void onTranscriptionResult(TranscriptionResult result) {
                        handleTranscriptionResult(sessionId, result);
                    }
                };

                String asrSessionId = speechToTextService.createSession(listener);
                sessionData.setAsrSessionId(asrSessionId);
                log.info("创建语音识别会话, 对话会话ID: {}, ASR会话ID: {}", sessionId, asrSessionId);
            }

            // 解码Base64音频数据
            byte[] audioData = Base64.getDecoder().decode(voiceMessage.getAudioData().getBytes(StandardCharsets.UTF_8));

            // 发送音频数据进行识别
            speechToTextService.sendAudioData(sessionData.getAsrSessionId(), audioData);
        } catch (Exception e) {
            log.error("处理音频消息失败", e);
            sendErrorMessage(sessionId, "处理音频失败: " + e.getMessage());
        }
    }

    /**
     * 处理文本消息
     */
    private void processTextMessage(String sessionId, VoiceMessage voiceMessage) {
        try {
            String text = voiceMessage.getText();
            if (text == null || text.trim().isEmpty()) {
                log.warn("收到空文本消息, 会话ID: {}", sessionId);
                return;
            }

            // 直接处理文本输入
            log.info("处理文本消息, 会话ID: {}, 文本长度: {}", sessionId, text.length());

            // 获取会话数据
            SessionData sessionData = sessionDataMap.get(sessionId);
            if (sessionData == null) {
                log.error("无法找到会话数据, 会话ID: {}", sessionId);
                return;
            }
            
            // 获取Dify会话ID，如果不存在则传入null创建新会话
            String difyConversationId = sessionData.getDifyConversationId();
            
            // 调用Dify API处理消息并生成回复
            ChatMessageResponse response = difyApiManager.sendMessageByChatMessageResponse("default", text, sessionId, difyConversationId);
            
            // 保存返回的会话ID供后续使用
            sessionData.setDifyConversationId(response.getConversationId());
            
            // 生成并发送语音回复
            generateAndSendSpeechResponse(sessionId, response.getAnswer());
        } catch (Exception e) {
            log.error("处理文本消息失败", e);
            sendErrorMessage(sessionId, "处理文本失败: " + e.getMessage());
        }
    }

    /**
     * 处理语音识别结果
     */
    private void handleTranscriptionResult(String sessionId, TranscriptionResult result) {
        try {
            String transcriptionText = result.getText();
            if (transcriptionText == null || transcriptionText.trim().isEmpty()) {
                log.warn("收到空转录结果, 会话ID: {}", sessionId);
                return;
            }

            // 获取会话数据
            SessionData sessionData = sessionDataMap.get(sessionId);
            if (sessionData == null) {
                log.error("无法找到会话数据, 会话ID: {}", sessionId);
                return;
            }
            
            // 更新最后一次转录时间
            sessionData.lastTranscriptionTime = System.currentTimeMillis();
            
            // 如果是实时转录结果（online模式）
            if (Boolean.TRUE.equals(result.getRealtime())) {
                // 标记用户正在说话
                sessionData.isSpeaking = true;
                
                // 更新实时转录缓冲区
                sessionData.realtimeTranscriptionBuffer.setLength(0);
                sessionData.realtimeTranscriptionBuffer.append(transcriptionText);
                
                // 更新最后一次实时转录时间
                sessionData.lastOnlineTranscriptionTime = System.currentTimeMillis();
//
//                // 发送实时转录结果给客户端显示，但标记为非最终结果
//                VoiceMessage realtimeMessage = VoiceMessage.builder()
//                        .sessionId(sessionId)
//                        .type("transcription")
//                        .text(transcriptionText)
//                        .timestamp(System.currentTimeMillis())
//                        .isFinal(false)
//                        .build();
//
//                sendResponseToClient(sessionId, realtimeMessage);
                return; // 实时结果不进一步处理
            }
            
            // 如果是离线转录结果（offline模式）
            if (Boolean.TRUE.equals(result.getIsFinal())) {
                // 处理标点符号
                transcriptionText = processPunctuation(sessionData, transcriptionText);
                
                log.info("收到离线转录结果, 会话ID: {}, 文本: {}", sessionId, transcriptionText);
                
                // 更新最后一次离线转录时间
                sessionData.lastOfflineTranscriptionTime = System.currentTimeMillis();
                
                // 将离线转录结果添加到缓冲区
                sessionData.offlineTranscriptionBuffer.add(transcriptionText);

                // 生成缓冲区内容的完整文本（用于显示）
                StringBuilder fullBuffer = new StringBuilder();
                for (String text : sessionData.offlineTranscriptionBuffer) {
                    if (fullBuffer.length() > 0) {
                        fullBuffer.append(" ");
                    }
                    fullBuffer.append(text);
                }

                // 发送离线转录结果给客户端显示（注意：显示缓冲区的所有数据）
                VoiceMessage transcriptionMessage = VoiceMessage.builder()
                        .sessionId(sessionId)
                        .type("transcription")
                        .text(fullBuffer.toString())
                        .timestamp(System.currentTimeMillis())
                        .isFinal(true)
                        .build();

                sendResponseToClient(sessionId, transcriptionMessage);
                 
                // 将转录结果添加到会话历史
//                sessionData.getHistory().add(transcriptionMessage);
                // 如果离线结果后没有实时数据回来，则在定时任务中处理
                // 在checkSpeechActivity方法中实现延时处理逻辑
            }
        } catch (Exception e) {
            log.error("处理语音识别结果失败", e);
            sendErrorMessage(sessionId, "处理转录结果失败: " + e.getMessage());
        }
    }

    /**
     * 处理标点符号
     */
    private String processPunctuation(SessionData sessionData, String currentText) {
        // 获取上一次的文本
        String lastText = sessionData.getLastTranscriptionText();
        
        // 保存当前文本
        sessionData.setLastTranscriptionText(currentText);

        // 如果是第一次识别，直接返回
        if (lastText == null) {
            return currentText;
        }

        // 检查当前文本是否以标点符号开头
        if (currentText.matches("^[，。！？,.!?].*")) {
            // 将标点符号移到上一句末尾
            String punctuation = currentText.substring(0, 1);
            String newLastText = lastText + punctuation;
            String newCurrentText = currentText.substring(1);

            // 更新历史记录中的最后一条转录文本
            updateLastTranscriptionInHistory(sessionData, newLastText);

            return newCurrentText;
        }

        return currentText;
    }

    /**
     * 更新历史记录中的最后一条转录文本
     */
    private void updateLastTranscriptionInHistory(SessionData sessionData, String newText) {
        List<VoiceMessage> history = sessionData.getHistory();
        for (int i = history.size() - 1; i >= 0; i--) {
            VoiceMessage message = history.get(i);
            if ("transcription".equals(message.getType())) {
                message.setText(newText);
                break;
            }
        }
    }

    /**
     * 检查文本是否包含触发词
     */
    private boolean containsTriggerWord(String text) {
        if (StringUtils.isBlank(text)) {
            return false;
        }

        // 1. 检查是否包含明确的触发词
        for (String triggerWord : TRIGGER_WORDS) {
            if (text.contains(triggerWord)) {
                return true;
            }
        }

        // 2. 智能触发机制
        // 2.1 检查是否是问句
        if (text.contains("？") || text.contains("?") || text.contains("吗") || text.contains("呢")) {
            return true;
        }

        // 2.2 检查是否是命令式语句
        if (text.contains("请") || text.contains("帮我") || text.contains("告诉") || text.contains("解释")) {
            return true;
        }

        // 2.3 检查是否是对话式语句
        if (text.contains("你好") || text.contains("您好") || text.contains("请问") || text.contains("打扰")) {
            return true;
        }

        if (text.contains("我想") || text.contains("我要") || text.contains("为什么") || text.contains("怎么")||  text.contains("如何")||  text.contains("原因") || text.contains("问题")||  text.contains("解决")) {
            return true;
        }

        // 2.4 检查是否是特定场景的语句
        if (text.contains("天气") || text.contains("时间") || text.contains("日期") || text.contains("星期")) {
            return true;
        }

        return false;
    }

    private static final List<String> TRIGGER_WORDS = Arrays.asList(
        "小爱", "小度", "小冰", "小艺", "小布", "小智", "小贝", "小可", "小新", "小美",
        "你好", "您好", "请问", "打扰", "麻烦", "谢谢", "感谢", "帮忙", "帮助", "协助",
        "天气", "时间", "日期", "星期", "温度", "湿度", "气压", "风速", "风向", "降水",
        "新闻", "资讯", "消息", "通知", "提醒", "备忘", "日程", "计划", "安排", "预约",
        "音乐", "歌曲", "播放", "暂停", "继续", "停止", "下一首", "上一首", "音量", "静音",
        "导航", "地图", "位置", "路线", "距离", "时间", "公里", "米", "分钟", "小时",
        "翻译", "解释", "说明", "介绍", "描述", "定义", "概念", "原理", "方法", "步骤",
        "计算", "换算", "转换", "比较", "分析", "统计", "预测", "评估", "判断", "决策"
    );

    /**
     * 生成并发送语音回复
     */
    private void generateAndSendSpeechResponse(String sessionId, String responseText) {
        try {
            log.info("生成语音回复, 会话ID: {}, 文本信息: {}", sessionId, responseText);

            if (StringUtils.isBlank(responseText)) {
                log.info("生成语音回复为空, 会话ID: {}, 文本信息: {}", sessionId, responseText);
                return ;
            }
            // 获取默认说话人ID
            String speakerId = textToSpeechService.getDefaultSpeakerId();
            // 大模型回复后
            VoiceMessage textMsg = VoiceMessage.builder()
                    .sessionId(sessionId)
                    .type("text")
                    .text(responseText)
                    .timestamp(System.currentTimeMillis())
                    .build();
            sendResponseToClient(sessionId, textMsg);
            // 将回复添加到会话历史
            SessionData sessionData = sessionDataMap.get(sessionId);
            if (sessionData != null) {
                sessionData.getHistory().add(textMsg);
            }

            Consumer<AbstractWebSocketMessage> callback = message -> {
                if (message instanceof BinaryMessage) {
                    BinaryMessage binaryMessage = (BinaryMessage) message;
                    VoiceMessage responseMessage = VoiceMessage.builder()
                            .sessionId(sessionId)
                            .type("audio")
                            .audioData(Base64.getEncoder().encodeToString(binaryMessage.getPayload().array()))
                            .speakerId(speakerId)
                            .timestamp(System.currentTimeMillis())
                            .isFinal(false)
                            .build();
                    // 创建回复消息
                    sendResponseToClient(sessionId, responseMessage);
                } else if (message instanceof TextMessage) {
                    TextMessage textMessage = (TextMessage) message;
                    VoiceMessage responseMessage = VoiceMessage.builder()
                            .sessionId(sessionId)
                            .type("audio-meta")
                            .text(textMessage.getPayload())
                            .timestamp(System.currentTimeMillis())
                            .isFinal(false)
                            .build();
                    sendResponseToClient(sessionId, responseMessage);
                }
            };

            textToSpeechService.synthesizeSpeech(sessionId, responseText, speakerId, callback);
        } catch (Exception e) {
            log.error("生成语音回复失败", e);
            sendErrorMessage(sessionId, "生成语音回复失败: " + e.getMessage());
        }
    }

    /**
     * 发送响应给客户端
     */
    private void sendResponseToClient(String sessionId, VoiceMessage responseMessage) {
        Consumer<VoiceMessage> callback = sessionCallbacks.get(sessionId);
        if (callback != null) {
            callback.accept(responseMessage);
        } else {
            log.warn("无法找到回调函数, 会话ID: {}", sessionId);
        }
    }

    /**
     * 发送错误消息
     */
    private void sendErrorMessage(String sessionId, String errorMessage) {
        VoiceMessage errorResponseMessage = VoiceMessage.builder()
                .sessionId(sessionId)
                .type("error")
                .errorMessage(errorMessage)
                .timestamp(System.currentTimeMillis())
                .build();

        sendResponseToClient(sessionId, errorResponseMessage);
    }

    /**
     * 创建会话数据
     */
    private SessionData createSessionData(String sessionId) {
        SessionData sessionData = new SessionData();
        sessionData.setSessionId(sessionId);
        sessionData.setHistory(new ArrayList<>());
        sessionData.setCreatedAt(System.currentTimeMillis());
        return sessionData;
    }

    /**
     * 会话数据类
     */
    private static class SessionData {
        private String sessionId;
        private String asrSessionId;
        private List<VoiceMessage> history = new ArrayList<>();
        private long createdAt;
        private ByteArrayOutputStream audioBuffer;  // 音频缓冲区
        private String currentAudioFile;  // 当前音频文件路径
        private String lastTranscriptionText; // 上一次转录文本
        private String difyConversationId; // 存储Dify API的会话ID
        
        // 实时转录相关字段
        private StringBuilder realtimeTranscriptionBuffer = new StringBuilder(); // 实时转录结果缓冲区
        private long lastTranscriptionTime = 0; // 最后一次转录结果的时间
        private boolean isSpeaking = false; // 用户是否正在说话
        
        // 离线转录缓冲相关字段
        private List<String> offlineTranscriptionBuffer = new ArrayList<>(); // 离线转录结果缓冲区
        private long lastOfflineTranscriptionTime = 0; // 最后一次离线转录结果的时间
        private long lastOnlineTranscriptionTime = 0; // 最后一次实时转录结果的时间

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getAsrSessionId() {
            return asrSessionId;
        }

        public void setAsrSessionId(String asrSessionId) {
            this.asrSessionId = asrSessionId;
        }

        public List<VoiceMessage> getHistory() {
            return history;
        }

        public void setHistory(List<VoiceMessage> history) {
            this.history = history;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(long createdAt) {
            this.createdAt = createdAt;
        }

        public ByteArrayOutputStream getAudioBuffer() {
            if (audioBuffer == null) {
                audioBuffer = new ByteArrayOutputStream();
            }
            return audioBuffer;
        }

        public String getCurrentAudioFile() {
            return currentAudioFile;
        }

        public void setCurrentAudioFile(String currentAudioFile) {
            this.currentAudioFile = currentAudioFile;
        }

        public String getLastTranscriptionText() {
            return lastTranscriptionText;
        }

        public void setLastTranscriptionText(String lastTranscriptionText) {
            this.lastTranscriptionText = lastTranscriptionText;
        }
        
        public String getDifyConversationId() {
            return difyConversationId;
        }
        
        public void setDifyConversationId(String difyConversationId) {
            this.difyConversationId = difyConversationId;
        }
    }

    /**
     * 检查语音活动并处理离线转录结果
     * 1. 如果有离线转录结果，则检查是否在一定时间内有新的实时转录结果
     * 2. 如果超过2秒没有新的实时转录结果，则将缓冲区中的所有离线转录结果合并并发送给大模型
     */
    @Scheduled(fixedRate = 500) // 每500毫秒检查一次
    public void checkSpeechActivity() {
        // 添加调试日志，验证定时任务是否正在执行
//        log.debug("执行语音活动检查任务，时间: {}", System.currentTimeMillis());
        
        long currentTime = System.currentTimeMillis();
        long waitThreshold = 3000; // 离线转录后等待2秒没有新的实时转录结果则处理缓冲区
        
        for (Map.Entry<String, SessionData> entry : sessionDataMap.entrySet()) {
            String sessionId = entry.getKey();
            SessionData sessionData = entry.getValue();
            
            // 检查是否有离线转录结果在缓冲区中
            if (!sessionData.offlineTranscriptionBuffer.isEmpty() && sessionData.lastOfflineTranscriptionTime > 0) {
                // 如果最后一次实时转录结果时间晚于最后一次离线转录结果时间
                // 说明离线转录后还有实时转录结果回来，用户可能还在说话
                if (sessionData.lastOnlineTranscriptionTime > sessionData.lastOfflineTranscriptionTime) {
                    // 用户还在说话，继续等待
                    sessionData.isSpeaking = true;
                    continue;
                }
                
                // 如果超过等待阈值时间没有新的实时转录结果
                if (currentTime - sessionData.lastOfflineTranscriptionTime > waitThreshold) {
                    log.info("离线转录后{}ms没有新的实时转录结果，处理缓冲区中的离线转录结果, 会话ID: {}", 
                            waitThreshold, sessionId);
                    
                    // 合并缓冲区中的所有离线转录结果
                    StringBuilder mergedText = new StringBuilder();
                    for (String text : sessionData.offlineTranscriptionBuffer) {
                        if (mergedText.length() > 0) {
                            mergedText.append(" ");
                        }
                        mergedText.append(text);
                    }
                    
                    String finalText = mergedText.toString();
                    log.info("合并后的离线转录结果, 会话ID: {}, 文本: {}", sessionId, finalText);
                    
                    // 标记用户已停止说话
                    sessionData.isSpeaking = false;
                    
                    // 获取Dify会话ID，如果不存在则传入null创建新会话
                    String difyConversationId = sessionData.getDifyConversationId();
                    
                    try {
                        // 调用Dify API处理消息并生成回复
                        ChatMessageResponse response = difyApiManager.sendMessageByChatMessageResponse("default", finalText, sessionId, difyConversationId);
                        
                        // 保存返回的会话ID供后续使用
                        sessionData.setDifyConversationId(response.getConversationId());
                        
                        // 生成并发送语音回复
                        generateAndSendSpeechResponse(sessionId, response.getAnswer());
                    } catch (Exception e) {
                        log.error("处理离线转录结果失败", e);
                        sendErrorMessage(sessionId, "处理转录结果失败: " + e.getMessage());
                    }
                    
                    // 清空缓冲区
                    sessionData.offlineTranscriptionBuffer.clear();
                    sessionData.lastOfflineTranscriptionTime = 0;
                    sessionData.lastOnlineTranscriptionTime = 0;
                }
            }
            
            // 检查实时转录活动
            if (sessionData.isSpeaking && sessionData.lastTranscriptionTime > 0) {
                // 如果超过静音阈值时间没有新的转录结果
                if (currentTime - sessionData.lastTranscriptionTime > 3000) { // 设置为3秒，比离线转录等待时间长
                    log.info("长时间没有转录结果，重置说话状态, 会话ID: {}", sessionId);
                    sessionData.isSpeaking = false;
                    sessionData.realtimeTranscriptionBuffer.setLength(0);
                    sessionData.lastTranscriptionTime = 0;
                }
            }
        }
    }
    
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void cleanupOldAudioFiles() {
        try {
            String saveDir = "audio_records";
            Path dirPath = Paths.get(saveDir);
            if (!Files.exists(dirPath)) {
                return;
            }

            // 获取7天前的时间戳
            long cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);

            Files.list(dirPath)
                    .filter(path -> path.toString().endsWith(".wav"))
                    .filter(path -> {
                        try {
                            return Files.getLastModifiedTime(path).toMillis() < cutoffTime;
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            log.info("删除旧音频文件: {}", path);
                        } catch (IOException e) {
                            log.error("删除音频文件失败: {}", path, e);
                        }
                    });
        } catch (Exception e) {
            log.error("清理旧音频文件失败", e);
        }
    }

    private void writeInt(RandomAccessFile raf, int value) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(value);
        raf.write(buffer.array());
    }
}