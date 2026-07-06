package com.xd.smartworksite.report.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.report.crypto-agent-v3")
public class CryptoAgentV3Properties {

    private String baseUrl = "http://127.0.0.1:8000";
    private String invokePath = "/report-generation/invoke";
    private int connectTimeoutSeconds = 5;
    private int readTimeoutSeconds = 300;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getInvokePath() { return invokePath; }
    public void setInvokePath(String invokePath) { this.invokePath = invokePath; }
    public int getConnectTimeoutSeconds() { return connectTimeoutSeconds; }
    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) { this.connectTimeoutSeconds = connectTimeoutSeconds; }
    public int getReadTimeoutSeconds() { return readTimeoutSeconds; }
    public void setReadTimeoutSeconds(int readTimeoutSeconds) { this.readTimeoutSeconds = readTimeoutSeconds; }
}
