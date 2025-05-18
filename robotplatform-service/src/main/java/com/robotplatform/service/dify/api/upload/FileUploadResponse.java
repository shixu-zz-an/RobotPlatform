package com.robotplatform.service.dify.api.upload;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Dify文件上传API的响应对象
 */
public class FileUploadResponse {
    private String id;          // 文件ID (UUID)
    private String name;        // 文件名
    private int size;           // 文件大小（byte）
    private String extension;   // 文件后缀
    
    @JsonProperty("mime_type")
    private String mimeType;    // 文件mime-type
    
    @JsonProperty("created_by")
    private String createdBy;   // 上传人ID
    
    @JsonProperty("created_at")
    private long createdAt;     // 上传时间（时间戳）

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "FileUploadResponse{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", size=" + size +
                ", extension='" + extension + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", createdBy='" + createdBy + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
