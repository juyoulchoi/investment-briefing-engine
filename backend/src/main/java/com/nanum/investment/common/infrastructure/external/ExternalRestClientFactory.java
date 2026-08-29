package com.nanum.investment.common.infrastructure.external;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ExternalRestClientFactory {
  private final Duration defaultConnectTimeout;
  private final Duration defaultReadTimeout;

  public ExternalRestClientFactory(
      @Value("${external-api.connect-timeout:3s}") Duration connectTimeout,
      @Value("${external-api.read-timeout:10s}") Duration readTimeout) {
    this.defaultConnectTimeout = connectTimeout;
    this.defaultReadTimeout = readTimeout;
  }

  public RestClient.Builder builder(String baseUrl) {
    return builder(baseUrl, defaultConnectTimeout, defaultReadTimeout);
  }

  public RestClient.Builder builder(
      String baseUrl, Duration connectTimeout, Duration readTimeout) {
    HttpClient client = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(client);
    requestFactory.setReadTimeout(readTimeout);
    return RestClient.builder().requestFactory(requestFactory).baseUrl(baseUrl);
  }
}
