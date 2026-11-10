package com.campuslands.incidents.config;

import java.net.http.HttpClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    public static final String WEBHOOK_SECRET_HEADER = "X-Webhook-Secret";

    @Bean
    public RestClient n8nRestClient(N8nProperties props) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(props.connectTimeout())
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(props.readTimeout());

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(props.baseUrl())
                .requestFactory(requestFactory);

        if (StringUtils.hasText(props.webhookSecret())) {
            builder.defaultHeader(WEBHOOK_SECRET_HEADER, props.webhookSecret());
        }
        return builder.build();
    }
}
