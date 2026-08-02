package com.nanum.investment.external;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class ExternalRestClientFactory {
 private final JdkClientHttpRequestFactory requestFactory;
 public ExternalRestClientFactory(@Value("${external-api.connect-timeout:3s}") Duration connectTimeout,@Value("${external-api.read-timeout:10s}") Duration readTimeout){
  HttpClient client=HttpClient.newBuilder().connectTimeout(connectTimeout).build();requestFactory=new JdkClientHttpRequestFactory(client);requestFactory.setReadTimeout(readTimeout);
 }
 public RestClient.Builder builder(String baseUrl){return RestClient.builder().requestFactory(requestFactory).baseUrl(baseUrl);}
}
