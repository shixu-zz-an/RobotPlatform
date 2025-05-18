package com.robotplatform.service.external.cosyvoice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;

import javax.sound.sampled.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CosyVoiceClientTest {

    @Mock
    private CosyVoiceConfig config;

    @InjectMocks
    private CosyVoiceClient cosyVoiceClient;

    private static final int SAMPLE_RATE = 24000;
    private static final int BITS_PER_SAMPLE = 16;
    private static final int CHANNELS = 1;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private SourceDataLine audioLine;

    @BeforeEach
    void setUp() {
        // 设置真实的服务配置
        CosyVoiceConfig.Server server = new CosyVoiceConfig.Server();
        server.setHost("100.103.248.120");
        server.setPort(50000);
        
        CosyVoiceConfig.Voice voice = new CosyVoiceConfig.Voice();
        voice.setId("中文女");
        
        when(config.getServer()).thenReturn(server);
        when(config.getVoice()).thenReturn(voice);
        when(config.getConnectTimeout()).thenReturn(50000);
        when(config.getReadTimeout()).thenReturn(300000);
    }

    /**
     * 初始化音频播放器
     */
    private void initAudioPlayer() {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, BITS_PER_SAMPLE, CHANNELS, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            audioLine = (SourceDataLine) AudioSystem.getLine(info);
            audioLine.open(format);
            audioLine.start();
        } catch (LineUnavailableException e) {
            System.err.println("初始化音频播放器失败: " + e.getMessage());
        }
    }

    /**
     * 关闭音频播放器
     */
    private void closeAudioPlayer() {
        if (audioLine != null) {
            audioLine.drain();
            audioLine.close();
            audioLine = null;
        }
    }

    /**
     * 实时播放音频数据
     */
    private void playAudioData(byte[] audioData) {
        if (audioLine != null && audioData != null && audioData.length > 0) {
            audioLine.write(audioData, 0, audioData.length);
        }
    }

    /**
     * 写入WAV文件头
     */
    private void writeWavHeader(FileOutputStream fos, int dataSize) throws IOException {
        // RIFF头
        fos.write("RIFF".getBytes());
        // 文件大小（不包括RIFF和大小字段）
        writeInt(fos, 36 + dataSize);
        // WAVE标识
        fos.write("WAVE".getBytes());
        // fmt子块
        fos.write("fmt ".getBytes());
        // fmt子块大小
        writeInt(fos, 16);
        // 音频格式（1表示PCM）
        writeShort(fos, (short) 1);
        // 声道数
        writeShort(fos, (short) CHANNELS);
        // 采样率
        writeInt(fos, SAMPLE_RATE);
        // 字节率
        writeInt(fos, SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8);
        // 块对齐
        writeShort(fos, (short) (CHANNELS * BITS_PER_SAMPLE / 8));
        // 位深度
        writeShort(fos, (short) BITS_PER_SAMPLE);
        // data子块
        fos.write("data".getBytes());
        // data子块大小
        writeInt(fos, dataSize);
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

    @Test
    void testSynthesizeSpeechStreamSft() throws Exception {
        // 准备测试数据
        String text = """
                尊敬的老师，亲爱的同学们：
                今天，让我们以青春之名，许下奋斗的誓言！校园里的每一次早读，每一道习题，都是成长的基石。少年的肩上，应担起星辰与朝阳，用汗水浇灌梦想，以坚持书写荣光。愿我们心怀热忱，在逐梦路上，眼里有光，脚下有力，不负韶华，不负时代！
                谢谢大家！
                """;
        String speakerId = "中文女";
        float speed = 1.0f;
        String userSessionId = UUID.randomUUID().toString();

        // 用于同步等待所有音频块接收完成
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger chunkCount = new AtomicInteger(0);
        AtomicInteger totalBytes = new AtomicInteger(0);

        // 创建输出文件
        File outputFile = new File("test-output/test_sft.wav");
        outputFile.getParentFile().mkdirs();

        // 初始化音频播放器
        initAudioPlayer();

        try {
            // 使用FileOutputStream支持多次写入
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                // 先写入一个临时的WAV头，后面会更新
                writeWavHeader(fos, 0);

                // 执行测试
                cosyVoiceClient.synthesizeSpeechStream(userSessionId, text, speakerId, speed, message -> {
                    try {
                        if (message instanceof TextMessage) {
                            String payload = ((TextMessage) message).getPayload();
                            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
                            String type = (String) data.get("type");

                            switch (type) {
                                case "header":
                                    System.out.println("收到头部信息: " + data);
                                    break;
                                case "chunk_info":
                                    System.out.println("收到块信息: " + data);
                                    break;
                                case "end":
                                    System.out.println("收到结束标记: " + data);
                                    // 更新WAV头
                                    fos.getChannel().position(0);
                                    writeWavHeader(fos, totalBytes.get());
                                    latch.countDown();
                                    break;
                                case "error":
                                    String errorMessage = (String) data.get("message");
                                    fail("收到错误消息: " + errorMessage);
                                    break;
                            }
                        } else if (message instanceof BinaryMessage) {
                            byte[] audioData = ((BinaryMessage) message).getPayload().array();
                            if (audioData.length > 0) {
                                // 实时播放音频数据
                                playAudioData(audioData);
                                // 同时保存到文件
                                fos.write(audioData);
                                totalBytes.addAndGet(audioData.length);
                                chunkCount.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        fail("处理消息失败: " + e.getMessage());
                    }
                });

                // 等待流式处理完成
                boolean completed = latch.await(300, TimeUnit.SECONDS);
                if (!completed) {
                    fail("等待音频数据超时");
                }
            }

            // 验证是否收到了音频数据
            assertTrue(chunkCount.get() > 0, "没有收到任何音频数据");
            System.out.println("音频文件已保存: " + outputFile.getAbsolutePath() + ", 共接收 " + chunkCount.get() + " 个音频块, 总字节数: " + totalBytes.get());

        } finally {
            Thread.sleep(5000L);
            // 关闭音频播放器
            closeAudioPlayer();
        }
    }

    @Test
    void testSynthesizeSpeechStreamZeroShot() throws Exception {
        // 准备测试数据
        String text = """
                Java 集面向对象、平台无关性与强大生态于一身。其自动垃圾回收机制让内存管理更简单；多线程特性支持并发编程；丰富的类库和框架（如 Spring、Hibernate）大幅提升开发效率。凭借编译与解释并存的执行模式，Java 在企业级应用与 Android 开发领域始终占据核心地位。
                """;
        String promptText = "希望你以后做的比我还好吆";
        byte[] promptAudio = loadAudioFile("/Users/hobart/workspace/robotplatform/robotplatform/robotplatform-service/prompt-video/zero_shot_prompt.wav");
        float speed = 1.0f;
        String userSessionId = UUID.randomUUID().toString();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger chunkCount = new AtomicInteger(0);
        AtomicInteger totalBytes = new AtomicInteger(0);

        // 创建输出文件
        File outputFile = new File("test-output/test_zero_shot.wav");
        outputFile.getParentFile().mkdirs();

        // 初始化音频播放器
        initAudioPlayer();

        try {
            // 使用FileOutputStream支持多次写入
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                // 先写入一个临时的WAV头，后面会更新
                writeWavHeader(fos, 0);

                // 执行测试
                cosyVoiceClient.synthesizeSpeechStreamZeroShot(userSessionId, text, promptText, promptAudio, speed, message -> {
                    try {
                        if (message instanceof TextMessage) {
                            String payload = ((TextMessage) message).getPayload();
                            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
                            String type = (String) data.get("type");

                            switch (type) {
                                case "header":
                                    System.out.println("收到头部信息: " + data);
                                    break;
                                case "chunk_info":
                                    System.out.println("收到块信息: " + data);
                                    break;
                                case "end":
                                    System.out.println("收到结束标记: " + data);
                                    // 更新WAV头
                                    fos.getChannel().position(0);
                                    writeWavHeader(fos, totalBytes.get());
                                    latch.countDown();
                                    break;
                                case "error":
                                    String errorMessage = (String) data.get("message");
                                    fail("收到错误消息: " + errorMessage);
                                    break;
                            }
                        } else if (message instanceof BinaryMessage) {
                            byte[] audioData = ((BinaryMessage) message).getPayload().array();
                            if (audioData.length > 0) {
                                // 实时播放音频数据
                                playAudioData(audioData);
                                // 同时保存到文件
                                fos.write(audioData);
                                totalBytes.addAndGet(audioData.length);
                                chunkCount.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        fail("处理消息失败: " + e.getMessage());
                    }
                });

                boolean completed = latch.await(400, TimeUnit.SECONDS);
                if (!completed) {
                    fail("等待音频数据超时");
                }
            }

            assertTrue(chunkCount.get() > 0, "没有收到任何音频数据");
            System.out.println("音频文件已保存: " + outputFile.getAbsolutePath() + ", 共接收 " + chunkCount.get() + " 个音频块, 总字节数: " + totalBytes.get());

        } finally {
            Thread.sleep(5000L);
            // 关闭音频播放器
            closeAudioPlayer();
        }
    }

    @Test
    void testSynthesizeSpeechStreamCrossLingual() throws Exception {
        String text = "Hello, World";
        byte[] promptAudio = loadAudioFile("/Users/hobart/workspace/robotplatform/robotplatform/robotplatform-service/prompt-video/cross_lingual_prompt.wav");
        float speed = 1.0f;
        String userSessionId = UUID.randomUUID().toString();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger chunkCount = new AtomicInteger(0);
        AtomicInteger totalBytes = new AtomicInteger(0);

        // 创建输出文件
        File outputFile = new File("test-output/test_cross_lingual.wav");
        outputFile.getParentFile().mkdirs();

        // 初始化音频播放器
        initAudioPlayer();

        try {
            // 使用FileOutputStream支持多次写入
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                // 先写入一个临时的WAV头，后面会更新
                writeWavHeader(fos, 0);

                cosyVoiceClient.synthesizeSpeechStreamCrossLingual(userSessionId, text, promptAudio, speed, message -> {
                    try {
                        if (message instanceof TextMessage) {
                            String payload = ((TextMessage) message).getPayload();
                            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
                            String type = (String) data.get("type");

                            switch (type) {
                                case "header":
                                    System.out.println("收到头部信息: " + data);
                                    break;
                                case "chunk_info":
                                    System.out.println("收到块信息: " + data);
                                    break;
                                case "end":
                                    System.out.println("收到结束标记: " + data);
                                    // 更新WAV头
                                    fos.getChannel().position(0);
                                    writeWavHeader(fos, totalBytes.get());
                                    latch.countDown();
                                    break;
                                case "error":
                                    String errorMessage = (String) data.get("message");
                                    fail("收到错误消息: " + errorMessage);
                                    break;
                            }
                        } else if (message instanceof BinaryMessage) {
                            byte[] audioData = ((BinaryMessage) message).getPayload().array();
                            if (audioData.length > 0) {
                                // 实时播放音频数据
                                playAudioData(audioData);
                                // 同时保存到文件
                                fos.write(audioData);
                                totalBytes.addAndGet(audioData.length);
                                chunkCount.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        fail("处理消息失败: " + e.getMessage());
                    }
                });

                boolean completed = latch.await(300, TimeUnit.SECONDS);
                if (!completed) {
                    fail("等待音频数据超时");
                }
            }

            assertTrue(chunkCount.get() > 0, "没有收到任何音频数据");
            System.out.println("音频文件已保存: " + outputFile.getAbsolutePath() + ", 共接收 " + chunkCount.get() + " 个音频块, 总字节数: " + totalBytes.get());

        } finally {
            Thread.sleep(5000L);
            // 关闭音频播放器
            closeAudioPlayer();
        }
    }
    @Test
    void testSynthesizeSpeechStreamInstruct2() throws Exception {
        String text = "Java 集面向对象、平台无关性与强大生态于一身。其自动垃圾回收机制让内存管理更简单；多线程特性支持并发编程；丰富的类库和框架（如 Spring、Hibernate）大幅提升开发效率。凭借编译与解释并存的执行模式，Java 在企业级应用与 Android 开发领域始终占据核心地位。";
        String instructText = "请用温柔的语气说话";
        byte[] promptAudio = loadAudioFile("/Users/hobart/workspace/robotplatform/robotplatform/robotplatform-service/prompt-video/zero_shot_prompt.wav");
        float speed = 1.0f;
        String userSessionId = UUID.randomUUID().toString();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger chunkCount = new AtomicInteger(0);
        AtomicInteger totalBytes = new AtomicInteger(0);

        // 创建输出文件
        File outputFile = new File("test-output/test_instruct2.wav");
        outputFile.getParentFile().mkdirs();

        // 初始化音频播放器
        initAudioPlayer();

        try {
            // 使用FileOutputStream支持多次写入
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                // 先写入一个临时的WAV头，后面会更新
                writeWavHeader(fos, 0);

                cosyVoiceClient.synthesizeSpeechStreamInstruct2(userSessionId, text, instructText, promptAudio, speed, message -> {
                    try {
                        if (message instanceof TextMessage) {
                            String payload = ((TextMessage) message).getPayload();
                            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
                            String type = (String) data.get("type");

                            switch (type) {
                                case "header":
                                    System.out.println("收到头部信息: " + data);
                                    break;
                                case "chunk_info":
                                    System.out.println("收到块信息: " + data);
                                    break;
                                case "end":
                                    System.out.println("收到结束标记: " + data);
                                    // 更新WAV头
                                    fos.getChannel().position(0);
                                    writeWavHeader(fos, totalBytes.get());
                                    latch.countDown();
                                    break;
                                case "error":
                                    String errorMessage = (String) data.get("message");
                                    fail("收到错误消息: " + errorMessage);
                                    break;
                            }
                        } else if (message instanceof BinaryMessage) {
                            byte[] audioData = ((BinaryMessage) message).getPayload().array();
                            if (audioData.length > 0) {
                                // 实时播放音频数据
                                playAudioData(audioData);
                                // 同时保存到文件
                                fos.write(audioData);
                                totalBytes.addAndGet(audioData.length);
                                chunkCount.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        fail("处理消息失败: " + e.getMessage());
                    }
                });

                boolean completed = latch.await(300, TimeUnit.SECONDS);
                if (!completed) {
                    fail("等待音频数据超时");
                }
            }

            assertTrue(chunkCount.get() > 0, "没有收到任何音频数据");
            System.out.println("音频文件已保存: " + outputFile.getAbsolutePath() + ", 共接收 " + chunkCount.get() + " 个音频块, 总字节数: " + totalBytes.get());

        } finally {
            Thread.sleep(10000L);
            // 关闭音频播放器
            closeAudioPlayer();
        }
    }

    /**
     * 加载音频文件
     */
    private byte[] loadAudioFile(String filename) {
        try {
            File file = new File(filename);
            return java.nio.file.Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            System.err.println("加载音频文件失败: " + e.getMessage());
            return new byte[0];
        }
    }
} 