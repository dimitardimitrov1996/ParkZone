package bg.softuni.parkzone.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class BillingFeignConfiguration {

    private static final String API_KEY_HEADER = "X-API-Key";

    @Bean
    public RequestInterceptor billingApiKeyRequestInterceptor(
            @Value("${billing.service.api.key}") String apiKey) {

        return requestTemplate -> requestTemplate.header(API_KEY_HEADER, apiKey);
    }
}
