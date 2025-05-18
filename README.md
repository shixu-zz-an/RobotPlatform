# RobotPlatform项目

基于Spring Boot和JDK 17的机器人AI管理系统，支持WebSocket语音对话。

## 项目结构

项目采用Maven多模块结构，包含以下子模块：

- **robotplatform-client**: 客户端模块，包含模型类和接口定义
- **robotplatform-service**: 服务模块，包含业务逻辑实现
- **robotplatform-web**: Web接口模块，包含控制器和API接口
- **robotplatform-start**: 应用启动模块，包含应用入口和配置

## 技术栈

- JDK 17
- Spring Boot 3.1.5
- Spring WebSocket
- Maven
- LangChain4j (大语言模型集成)
- FunASR (语音识别)
- CosyVoice (语音合成)

## 如何启动

1. 确保已安装JDK 17和Maven
2. 确保已启动FunASR服务（默认地址：localhost:10095）
3. 确保已启动CosyVoice服务（默认地址：http://localhost:5000）
4. 确保已启动Ollama服务（默认地址：http://localhost:11434）并加载qwen2.5:14b模型
5. 在项目根目录执行：`mvn clean install`
6. 启动应用：`java -jar robotplatform-start/target/robotplatform-start-1.0.0-SNAPSHOT.jar`
7. 访问：`http://localhost:8080/robotplatform`

## 语音对话WebSocket服务

WebSocket服务支持实时语音对话，可接收客户端发送的音频流，进行语音识别、大语言模型处理和语音合成，最后将合成的音频流返回给客户端。该服务基于Spring WebSocket实现，提供高效的双向通信能力。

### WebSocket端点

- 地址：`ws://localhost:8080/robotplatform/ws/voice/{sessionId}`
- 其中`{sessionId}`为会话ID，可以是任意字符串，用于标识会话

### 消息格式

消息采用JSON格式，结构如下：

```json
{
  "type": "audio|text",
  "audioData": "base64编码的音频数据",
  "text": "文本内容",
  "sessionId": "会话ID"
}
```

- `type`: 消息类型，可选值为：
  - `audio`: 音频数据
  - `text`: 文本数据
- `audioData`: Base64编码的音频数据（type为audio时必须）
- `text`: 文本内容（type为text时必须）
- `sessionId`: 会话ID，用于标识会话

### 处理流程

1. 客户端连接WebSocket端点
2. 客户端发送音频数据（type=audio）
3. 服务端将音频数据发送给FunASR进行语音识别
4. FunASR通过异步listener返回识别结果
5. 服务端将识别结果实时返回给客户端（type=text）
6. 客户端发送结束标记（type=end）
7. 服务端将所有识别出的文本发送给大语言模型
8. 大语言模型生成回答
9. 服务端将文本回答发送给客户端（type=text）
10. 服务端将文本回答发送给CosyVoice进行语音合成
11. 服务端将合成的音频发送给客户端（type=audio）

## REST API接口

除了WebSocket服务外，还提供了以下HTTP接口：

### 获取所有机器人
- URL: GET `/robotplatform/api/robots`

### 获取指定机器人
- URL: GET `/robotplatform/api/robots/{robotId}`

### 创建机器人
- URL: POST `/robotplatform/api/robots`
- 请求体：
```json
{
  "robotId": "3",
  "robotName": "新机器人",
  "robotType": "家用型",
  "status": "在线"
}
```

### 更新机器人
- URL: PUT `/robotplatform/api/robots/{robotId}`
- 请求体：
```json
{
  "robotName": "更新机器人",
  "robotType": "家用型",
  "status": "在线"
}
```

### 语音识别
- URL: POST `/robotplatform/api/voice/stt`
- 请求体：
```json
{
  "audioData": "base64编码的音频数据",
  "sessionId": "可选的会话ID"
}
```

### 语音合成
- URL: POST `/robotplatform/api/voice/tts`
- 请求体：
```json
{
  "text": "要合成的文本内容"
}
```

### 大语言模型对话
- URL: POST `/robotplatform/api/voice/chat`
- 请求体：
```json
{
  "prompt": "用户提问内容"
}
```

### 完整语音对话流程
- URL: POST `/robotplatform/api/voice/dialog`
- 请求体：
```json
{
  "audioData": "base64编码的音频数据",
  "sessionId": "可选的会话ID"
}
``` 