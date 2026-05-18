package com.debugflow.sdk;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.reactive.function.client.WebClient;

@AutoConfiguration
@ConditionalOnClass({OncePerRequestFilter.class, WebClient.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(DebugFlowProperties.class)
@ConditionalOnProperty(prefix = "debugflow", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DebugFlowAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "debugFlowWebClient")
    WebClient debugFlowWebClient(DebugFlowProperties properties) {
        WebClient.Builder builder = WebClient.builder()
                .baseUrl(properties.collectorUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        if (properties.hasApiKey()) {
            builder.defaultHeader("X-DebugFlow-Api-Key", properties.apiKey());
        }

        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    DebugFlowClient debugFlowClient(
            @Qualifier("debugFlowWebClient") WebClient debugFlowWebClient,
            DebugFlowProperties properties) {
        return new DebugFlowClient(debugFlowWebClient, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    HeaderMasker debugFlowHeaderMasker() {
        return new HeaderMasker();
    }

    @Bean
    @ConditionalOnMissingBean
    TraceContextExtractor debugFlowTraceContextExtractor() {
        return new TraceContextExtractor();
    }

    @Bean
    @ConditionalOnMissingBean
    DebugFlowCaptureFilter debugFlowCaptureFilter(
            DebugFlowProperties properties,
            DebugFlowClient debugFlowClient,
            HeaderMasker headerMasker,
            TraceContextExtractor traceContextExtractor) {
        return new DebugFlowCaptureFilter(properties, debugFlowClient, headerMasker, traceContextExtractor);
    }

    @Bean
    @ConditionalOnMissingBean(name = "debugFlowCaptureFilterRegistration")
    FilterRegistrationBean<DebugFlowCaptureFilter> debugFlowCaptureFilterRegistration(
            DebugFlowCaptureFilter debugFlowCaptureFilter) {
        FilterRegistrationBean<DebugFlowCaptureFilter> registration = new FilterRegistrationBean<>(debugFlowCaptureFilter);
        registration.setName("debugFlowCaptureFilter");
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.LOWEST_PRECEDENCE - 20);
        return registration;
    }
}
