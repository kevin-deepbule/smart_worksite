package com.xd.smartworksite.review.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.review")
public class ReviewProperties {
    private int parsePollLimit = 20;
    private long parsePollDelayMs = 2000;
    private int maxModelCalls = 10000;
    private Chunk chunk = new Chunk();

    public int getParsePollLimit() { return parsePollLimit; }
    public void setParsePollLimit(int parsePollLimit) { this.parsePollLimit = parsePollLimit; }
    public long getParsePollDelayMs() { return parsePollDelayMs; }
    public void setParsePollDelayMs(long parsePollDelayMs) { this.parsePollDelayMs = parsePollDelayMs; }
    public int getMaxModelCalls() { return maxModelCalls; }
    public void setMaxModelCalls(int maxModelCalls) { this.maxModelCalls = maxModelCalls; }
    public Chunk getChunk() { return chunk; }
    public void setChunk(Chunk chunk) { this.chunk = chunk; }

    public static class Chunk {
        private int maxChars = 6000;
        private int overlapChars = 600;
        private int maxChunks = 500;
        private int maxDocumentChars = 2000000;

        public int getMaxChars() { return maxChars; }
        public void setMaxChars(int maxChars) { this.maxChars = maxChars; }
        public int getOverlapChars() { return overlapChars; }
        public void setOverlapChars(int overlapChars) { this.overlapChars = overlapChars; }
        public int getMaxChunks() { return maxChunks; }
        public void setMaxChunks(int maxChunks) { this.maxChunks = maxChunks; }
        public int getMaxDocumentChars() { return maxDocumentChars; }
        public void setMaxDocumentChars(int maxDocumentChars) { this.maxDocumentChars = maxDocumentChars; }
    }
}
