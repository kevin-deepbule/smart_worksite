package com.xd.smartworksite.review.domain;

import java.time.LocalDateTime;

public class ReviewDocumentChunk {
    private Long id;
    private Long projectId;
    private Long reviewRecordId;
    private Long parseRecordId;
    private Integer chunkNo;
    private String chunkCode;
    private String headingPath;
    private Integer pageStart;
    private Integer pageEnd;
    private String sheetName;
    private Integer rowStart;
    private Integer rowEnd;
    private String contentObjectName;
    private String contentHash;
    private Integer charCount;
    private Integer tokenCount;
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getReviewRecordId() { return reviewRecordId; }
    public void setReviewRecordId(Long reviewRecordId) { this.reviewRecordId = reviewRecordId; }
    public Long getParseRecordId() { return parseRecordId; }
    public void setParseRecordId(Long parseRecordId) { this.parseRecordId = parseRecordId; }
    public Integer getChunkNo() { return chunkNo; }
    public void setChunkNo(Integer chunkNo) { this.chunkNo = chunkNo; }
    public String getChunkCode() { return chunkCode; }
    public void setChunkCode(String chunkCode) { this.chunkCode = chunkCode; }
    public String getHeadingPath() { return headingPath; }
    public void setHeadingPath(String headingPath) { this.headingPath = headingPath; }
    public Integer getPageStart() { return pageStart; }
    public void setPageStart(Integer pageStart) { this.pageStart = pageStart; }
    public Integer getPageEnd() { return pageEnd; }
    public void setPageEnd(Integer pageEnd) { this.pageEnd = pageEnd; }
    public String getSheetName() { return sheetName; }
    public void setSheetName(String sheetName) { this.sheetName = sheetName; }
    public Integer getRowStart() { return rowStart; }
    public void setRowStart(Integer rowStart) { this.rowStart = rowStart; }
    public Integer getRowEnd() { return rowEnd; }
    public void setRowEnd(Integer rowEnd) { this.rowEnd = rowEnd; }
    public String getContentObjectName() { return contentObjectName; }
    public void setContentObjectName(String contentObjectName) { this.contentObjectName = contentObjectName; }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    public Integer getCharCount() { return charCount; }
    public void setCharCount(Integer charCount) { this.charCount = charCount; }
    public Integer getTokenCount() { return tokenCount; }
    public void setTokenCount(Integer tokenCount) { this.tokenCount = tokenCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
}
