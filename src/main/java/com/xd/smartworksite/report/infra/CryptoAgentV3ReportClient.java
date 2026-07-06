package com.xd.smartworksite.report.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
@EnableConfigurationProperties(CryptoAgentV3Properties.class)
public class CryptoAgentV3ReportClient {

    private static final Logger log =
            LoggerFactory.getLogger(CryptoAgentV3ReportClient.class);

    private final CryptoAgentV3Properties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public CryptoAgentV3ReportClient(
            CryptoAgentV3Properties properties,
            ObjectMapper objectMapper) {

        this.properties = properties;
        this.objectMapper = objectMapper;

        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(
                        Duration.ofSeconds(
                                properties.getConnectTimeoutSeconds()))
                .build();
    }

    public CryptoAgentGenerateResponse generate(
            CryptoAgentGenerateRequest requestPayload) {

        try {
            String requestJson =
                    objectMapper.writeValueAsString(requestPayload);

            String requestUrl =
                    properties.getBaseUrl().replaceAll("/+$", "")
                            + properties.getInvokePath();

            // 联调阶段必须打印
            log.info(
                    "Calling CryptoAgentV3, url={}, requestBody={}",
                    requestUrl,
                    requestJson
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .version(HttpClient.Version.HTTP_1_1)
                    .timeout(
                            Duration.ofSeconds(
                                    properties.getReadTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString());

            log.info(
                    "CryptoAgentV3 response, status={}, body={}",
                    response.statusCode(),
                    response.body()
            );

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                throw new BusinessException(
                        ErrorCode.EXTERNAL_SERVICE_ERROR,
                        "CryptoAgentV3调用失败: HTTP "
                                + response.statusCode()
                                + ", body="
                                + response.body());
            }

            return objectMapper.readValue(
                    response.body(),
                    CryptoAgentGenerateResponse.class);

        } catch (IOException ex) {
            throw new BusinessException(
                    ErrorCode.EXTERNAL_SERVICE_ERROR,
                    "CryptoAgentV3调用失败: " + ex.getMessage());

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();

            throw new BusinessException(
                    ErrorCode.EXTERNAL_SERVICE_ERROR,
                    "CryptoAgentV3调用被中断");
        }
    }
}
