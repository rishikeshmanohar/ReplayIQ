package com.failframe.sdk;

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
@EnableConfigurationProperties(FailFrameProperties.class)
@ConditionalOnProperty(prefix = "failframe", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FailFrameAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "failFrameWebClient")
    WebClient failFrameWebClient(FailFrameProperties properties) {
        WebClient.Builder builder = WebClient.builder()
                .baseUrl(properties.collectorUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        if (properties.hasApiKey()) {
            builder.defaultHeader("X-FailFrame-Api-Key", properties.apiKey());
        }

        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    FailFrameClient failFrameClient(
            @Qualifier("failFrameWebClient") WebClient failFrameWebClient,
            FailFrameProperties properties) {
        return new FailFrameClient(failFrameWebClient, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    HeaderMasker failFrameHeaderMasker() {
        return new HeaderMasker();
    }

    @Bean
    @ConditionalOnMissingBean
    TraceContextExtractor failFrameTraceContextExtractor() {
        return new TraceContextExtractor();
    }

    @Bean
    @ConditionalOnMissingBean
    FailFrameCaptureFilter failFrameCaptureFilter(
            FailFrameProperties properties,
            FailFrameClient failFrameClient,
            HeaderMasker headerMasker,
            TraceContextExtractor traceContextExtractor) {
        return new FailFrameCaptureFilter(properties, failFrameClient, headerMasker, traceContextExtractor);
    }

    @Bean
    @ConditionalOnMissingBean(name = "failFrameCaptureFilterRegistration")
    FilterRegistrationBean<FailFrameCaptureFilter> failFrameCaptureFilterRegistration(
            FailFrameCaptureFilter failFrameCaptureFilter) {
        FilterRegistrationBean<FailFrameCaptureFilter> registration = new FilterRegistrationBean<>(failFrameCaptureFilter);
        registration.setName("failFrameCaptureFilter");
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.LOWEST_PRECEDENCE - 20);
        return registration;
    }
}
