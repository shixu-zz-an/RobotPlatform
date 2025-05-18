package com.robotplatform.service.dify.api.appinit;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ParametersReponse {
    
    @JSONField(name = "opening_statement")
    private String openingStatement;
    
    @JSONField(name = "suggested_questions")
    private List<String> suggestedQuestions;
    
    @JSONField(name = "suggested_questions_after_answer")
    private Feature suggestedQuestionsAfterAnswer;
    
    @JSONField(name = "speech_to_text")
    private Feature speechToText;
    
    @JSONField(name = "text_to_speech")
    private TextToSpeech textToSpeech;
    
    @JSONField(name = "retriever_resource")
    private Feature retrieverResource;
    
    @JSONField(name = "annotation_reply")
    private Feature annotationReply;
    
    @JSONField(name = "more_like_this")
    private Feature moreLikeThis;
    
    @JSONField(name = "user_input_form")
    private List<Object> userInputForm;
    
    @JSONField(name = "sensitive_word_avoidance")
    private Feature sensitiveWordAvoidance;
    
    @JSONField(name = "file_upload")
    private FileUpload fileUpload;
    
    @JSONField(name = "system_parameters")
    private SystemParameters systemParameters;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Feature {
        private boolean enabled;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextToSpeech extends Feature {
        private String language;
        private String voice;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileUpload extends Feature {
        private ImageConfig image;
        
        @JSONField(name = "allowed_file_types")
        private List<String> allowedFileTypes;
        
        @JSONField(name = "allowed_file_extensions")
        private List<String> allowedFileExtensions;
        
        @JSONField(name = "allowed_file_upload_methods")
        private List<String> allowedFileUploadMethods;
        
        @JSONField(name = "number_limits")
        private int numberLimits;
        
        @JSONField(name = "fileUploadConfig")
        private FileUploadConfig fileUploadConfig;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageConfig {
        private boolean enabled;
        
        @JSONField(name = "number_limits")
        private int numberLimits;
        
        @JSONField(name = "transfer_methods")
        private List<String> transferMethods;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileUploadConfig {
        @JSONField(name = "file_size_limit")
        private int fileSizeLimit;
        
        @JSONField(name = "batch_count_limit")
        private int batchCountLimit;
        
        @JSONField(name = "image_file_size_limit")
        private int imageFileSizeLimit;
        
        @JSONField(name = "video_file_size_limit")
        private int videoFileSizeLimit;
        
        @JSONField(name = "audio_file_size_limit")
        private int audioFileSizeLimit;
        
        @JSONField(name = "workflow_file_upload_limit")
        private int workflowFileUploadLimit;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemParameters {
        @JSONField(name = "image_file_size_limit")
        private int imageFileSizeLimit;
        
        @JSONField(name = "video_file_size_limit")
        private int videoFileSizeLimit;
        
        @JSONField(name = "audio_file_size_limit")
        private int audioFileSizeLimit;
        
        @JSONField(name = "file_size_limit")
        private int fileSizeLimit;
        
        @JSONField(name = "workflow_file_upload_limit")
        private int workflowFileUploadLimit;
    }
}
