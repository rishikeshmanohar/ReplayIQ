package com.debugflow.collector.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.debugflow.collector.domain.ApiFailureEvent;
import com.debugflow.collector.repository.ApiFailureEventRepository;
import com.debugflow.collector.repository.ReplayAttemptRepository;
import com.debugflow.collector.repository.RootCauseAnalysisRepository;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class FailureIngestionApiIntegrationTest {

    private static final String LOCAL_API_KEY = "debugflow-local-dev-key";
    private static final String ADMIN_USER = "debugflow";
    private static final String ADMIN_PASSWORD = "debugflow";

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("debugflow_test")
            .withUsername("debugflow")
            .withPassword("debugflow");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ApiFailureEventRepository apiFailureEventRepository;

    @Autowired
    RootCauseAnalysisRepository rootCauseAnalysisRepository;

    @Autowired
    ReplayAttemptRepository replayAttemptRepository;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("debugflow.security.username", () -> ADMIN_USER);
        registry.add("debugflow.security.password", () -> ADMIN_PASSWORD);
    }

    @BeforeEach
    void cleanDatabase() {
        replayAttemptRepository.deleteAll();
        rootCauseAnalysisRepository.deleteAll();
        apiFailureEventRepository.deleteAll();
    }

    @Test
    void ingestionApiStoresFailureEvent() throws Exception {
        MvcResult result = performIngest(defaultPayload("local"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.traceId").value("trace-test-001"))
                .andReturn();

        Long id = JsonTestUtil.longValue(result, "id");
        ApiFailureEvent saved = apiFailureEventRepository.findById(id).orElseThrow();

        assertThat(saved.getProjectId()).isEqualTo(1L);
        assertThat(saved.getServiceName()).isEqualTo("checkout-service");
        assertThat(saved.getEnvironment()).isEqualTo("local");
        assertThat(saved.getHttpMethod()).isEqualTo("POST");
        assertThat(saved.getPath()).isEqualTo("/api/orders");
        assertThat(saved.getStatusCode()).isEqualTo(500);
    }

    @Test
    void missingApiKeyReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/events/failures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(defaultPayload("local")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidApiKeyReturns403() throws Exception {
        mockMvc.perform(post("/api/v1/events/failures")
                        .header("X-DebugFlow-Api-Key", "not-a-valid-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(defaultPayload("local")))
                .andExpect(status().isForbidden());
    }

    @Test
    void sensitiveHeadersAreMasked() throws Exception {
        Long id = ingest(defaultPayload("local")).andReturnId();

        ApiFailureEvent saved = apiFailureEventRepository.findById(id).orElseThrow();

        assertThat(saved.getRequestHeadersJson().get("Authorization").asText()).isEqualTo("[masked]");
        assertThat(saved.getRequestHeadersJson().get("Cookie").asText()).isEqualTo("[masked]");
        assertThat(saved.getRequestHeadersJson().get("X-DebugFlow-Api-Key").asText()).isEqualTo("[masked]");
        assertThat(saved.getRequestHeadersJson().get("Content-Type").asText()).isEqualTo("application/json");
        assertThat(saved.getResponseHeadersJson().get("Set-Cookie").asText()).isEqualTo("[masked]");
    }

    @Test
    void requestAndResponseBodiesAreTruncated() throws Exception {
        Long id = ingest(payloadWithBodies("local", "r".repeat(12 * 1024), "s".repeat(12 * 1024))).andReturnId();

        ApiFailureEvent saved = apiFailureEventRepository.findById(id).orElseThrow();

        assertThat(saved.getRequestBody().getBytes(StandardCharsets.UTF_8)).hasSize(10 * 1024);
        assertThat(saved.getResponseBody().getBytes(StandardCharsets.UTF_8)).hasSize(10 * 1024);
    }

    @Test
    void analyzeEndpointStoresRootCauseAnalysis() throws Exception {
        Long id = ingest(payloadWithException("local", "NullPointerException while charging payment token")).andReturnId();

        mockMvc.perform(post("/api/v1/events/failures/{id}/analyze", id)
                        .with(httpBasic(ADMIN_USER, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.failureEventId").value(id))
                .andExpect(jsonPath("$.summary").isNotEmpty())
                .andExpect(jsonPath("$.confidence").isNumber());

        assertThat(rootCauseAnalysisRepository.findTopByFailureEventIdOrderByCreatedAtDesc(id)).isPresent();

        mockMvc.perform(get("/api/v1/events/failures/{id}", id)
                        .with(httpBasic(ADMIN_USER, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rootCauseAnalysis.failureEventId").value(id));
    }

    @Test
    void replayBlocksProductionEvents() throws Exception {
        Long id = ingest(defaultPayload("production")).andReturnId();

        mockMvc.perform(post("/api/v1/events/failures/{id}/replay", id)
                        .with(httpBasic(ADMIN_USER, ADMIN_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetBaseUrl": "http://localhost:8081",
                                  "allowPaymentReplay": false
                                }
                                """))
                .andExpect(status().isForbidden());

        assertThat(replayAttemptRepository.findByFailureEventIdOrderByReplayedAtDesc(id)).isEmpty();
    }

    @Test
    void listEndpointUsesAdminBasicAuth() throws Exception {
        ingest(defaultPayload("local"));

        mockMvc.perform(get("/api/v1/events/failures")
                        .with(httpBasic(ADMIN_USER, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    private IngestResult ingest(String payload) throws Exception {
        MvcResult result = performIngest(payload)
                .andExpect(status().isCreated())
                .andReturn();
        return new IngestResult(result);
    }

    private ResultActions performIngest(String payload) throws Exception {
        return mockMvc.perform(post("/api/v1/events/failures")
                        .header("X-DebugFlow-Api-Key", LOCAL_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload));
    }

    private String defaultPayload(String environment) {
        return payloadWithBodies(environment, "{\"sku\":\"sku_123\"}", "{\"error\":\"database timeout\"}");
    }

    private String payloadWithException(String environment, String exceptionMessage) {
        return """
                {
                  "serviceName": "checkout-service",
                  "environment": "%s",
                  "traceId": "trace-test-001",
                  "spanId": "span-test-001",
                  "httpMethod": "POST",
                  "path": "/api/orders",
                  "queryString": "debug=true",
                  "statusCode": 500,
                  "latencyMs": 1250,
                  "exceptionType": "java.lang.NullPointerException",
                  "exceptionMessage": "%s",
                  "stackTrace": "java.lang.NullPointerException: %s",
                  "requestHeaders": {
                    "Authorization": "Bearer secret",
                    "Cookie": "session=secret",
                    "X-DebugFlow-Api-Key": "secret",
                    "Content-Type": "application/json"
                  },
                  "requestBody": "{}",
                  "responseHeaders": {
                    "Set-Cookie": "session=secret",
                    "Content-Type": "application/json"
                  },
                  "responseBody": "{\\"error\\":\\"failed\\"}"
                }
                """.formatted(environment, exceptionMessage, exceptionMessage);
    }

    private String payloadWithBodies(String environment, String requestBody, String responseBody) {
        return """
                {
                  "serviceName": "checkout-service",
                  "environment": "%s",
                  "traceId": "trace-test-001",
                  "spanId": "span-test-001",
                  "httpMethod": "POST",
                  "path": "/api/orders",
                  "queryString": "debug=true",
                  "statusCode": 500,
                  "latencyMs": 1250,
                  "exceptionType": "java.sql.SQLTransientConnectionException",
                  "exceptionMessage": "database timeout",
                  "stackTrace": "java.sql.SQLTransientConnectionException: database timeout",
                  "requestHeaders": {
                    "Authorization": "Bearer secret",
                    "Cookie": "session=secret",
                    "X-DebugFlow-Api-Key": "secret",
                    "Content-Type": "application/json"
                  },
                  "requestBody": %s,
                  "responseHeaders": {
                    "Set-Cookie": "session=secret",
                    "Content-Type": "application/json"
                  },
                  "responseBody": %s
                }
                """.formatted(environment, JsonTestUtil.quote(requestBody), JsonTestUtil.quote(responseBody));
    }

    private record IngestResult(MvcResult result) {

        Long andReturnId() throws Exception {
            return JsonTestUtil.longValue(result, "id");
        }
    }
}
