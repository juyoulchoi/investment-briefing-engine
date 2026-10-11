package com.nanum.investment.marketdata.infrastructure;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.ClassUtils;
import org.springframework.web.client.RestClient;

/** Reads remain on the common DB; provider writes and source jobs execute in the collector. */
@Configuration
@ConditionalOnProperty(name = "unified.market-source.consumer", havingValue = "true")
public class UnifiedMarketSourceConsumerConfiguration {
  private static final Set<String> SERVICES = Set.of("KrxMarketDataService", "KrxCollectionJobService",
      "KrxBackfillService", "FredBondYieldService", "FredMacroService", "YahooIndexService", "OverseasStockService", "CollectionJobReprocessingService");

  @Bean
  static BeanFactoryPostProcessor removeLocalSourceRecovery() {
    return factory -> {
      BeanDefinitionRegistry registry = (BeanDefinitionRegistry) factory;
      for (String name : registry.getBeanDefinitionNames()) {
        String type = registry.getBeanDefinition(name).getBeanClassName();
        if (type != null && (type.endsWith(".FredMacroRecovery") || type.endsWith(".KrxBackfillRecovery"))) {
          registry.removeBeanDefinition(name);
        }
      }
    };
  }

  @Bean
  static BeanPostProcessor routeSourceWrites(Environment environment) {
    String baseUrl = environment.getRequiredProperty("unified.market-source.base-url");
    var requestFactory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
    requestFactory.setReadTimeout(Duration.ofMinutes(10));
    RestClient client = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    return new BeanPostProcessor() {
      @Override
      public Object postProcessAfterInitialization(Object bean, String name) {
        Class<?> type = ClassUtils.getUserClass(bean);
        if (!type.getPackageName().startsWith("com.nanum.investment.marketdata.") || !SERVICES.contains(type.getSimpleName())) return bean;
        ProxyFactory proxy = new ProxyFactory(bean);
        proxy.setProxyTargetClass(true);
        proxy.addAdvice((MethodInterceptor) invocation -> {
          String method = invocation.getMethod().getName();
          if (type.getSimpleName().equals("CollectionJobReprocessingService") && invocation.getArguments().length>0
              && "KOFIA".equalsIgnoreCase(String.valueOf(invocation.getArguments()[0]))) return invocation.proceed();
          if (invocation.getMethod().getDeclaringClass() == Object.class
              || !(method.startsWith("collect") || method.startsWith("refresh") || method.startsWith("start")
                  || method.startsWith("retry") || method.startsWith("cancel") || method.startsWith("sync")
                  || method.equals("register") || method.equals("setActive") || method.equals("pause") || method.equals("resume")
                  || method.equals("markPermanentFailure"))) {
            return invocation.proceed();
          }
          Map<String,Object> request = new LinkedHashMap<>();
          request.put("service", type.getName());
          request.put("method", method);
          request.put("parameterTypes", Arrays.stream(invocation.getMethod().getParameterTypes()).map(Class::getName).toList());
          request.put("arguments", Arrays.asList(invocation.getArguments()));
          return client.post().uri("/internal/market-source/invoke").body(request).retrieve()
              .body(ParameterizedTypeReference.forType(invocation.getMethod().getGenericReturnType()));
        });
        return proxy.getProxy();
      }
    };
  }
}
