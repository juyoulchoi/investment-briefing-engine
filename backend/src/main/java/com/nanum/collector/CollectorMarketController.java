package com.nanum.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Allowlisted typed bridge for existing V1/V2 collection service contracts. */
@RestController
@ConditionalOnProperty(name = "MARKET_SOURCE_ENABLED", havingValue = "true")
public class CollectorMarketController {
  public static final Set<String> SERVICES = Set.of(
      "com.nanum.investment.marketdata.application.KrxMarketDataService",
      "com.nanum.investment.marketdata.application.KrxCollectionJobService",
      "com.nanum.investment.marketdata.application.KrxBackfillService",
      "com.nanum.investment.marketdata.application.FredBondYieldService",
      "com.nanum.investment.marketdata.application.FredMacroService",
      "com.nanum.investment.marketdata.application.CollectionJobReprocessingService",
      "com.nanum.investment.marketdata.infrastructure.YahooIndexService",
      "com.nanum.investment.marketdata.infrastructure.OverseasStockService");
  private final ApplicationContext context;
  private final ObjectMapper mapper;

  public CollectorMarketController(ApplicationContext context, ObjectMapper mapper) {
    this.context = context;
    this.mapper = mapper;
  }

  public record Request(String service, String method, List<String> parameterTypes, List<JsonNode> arguments) {}

  @PostMapping("/internal/market-source/invoke")
  public synchronized Object invoke(@RequestBody Request request) throws Exception {
    if (!SERVICES.contains(request.service())) throw new IllegalArgumentException("Unsupported source service");
    if (request.service().endsWith("CollectionJobReprocessingService")
        && !request.arguments().isEmpty() && "KOFIA".equalsIgnoreCase(request.arguments().getFirst().asText())) {
      throw new IllegalArgumentException("Use the KOFIA collector API");
    }
    Object bean = context.getBean(Class.forName(request.service()));
    Method selected = Arrays.stream(ClassUtils.getUserClass(bean).getMethods())
        .filter(m -> m.getDeclaringClass() != Object.class && m.getName().equals(request.method()))
        .filter(m -> Arrays.stream(m.getParameterTypes()).map(Class::getName).toList().equals(request.parameterTypes()))
        .findFirst().orElseThrow(() -> new IllegalArgumentException("Unsupported source operation"));
    if (request.arguments().size() != selected.getParameterCount()) throw new IllegalArgumentException("Argument count differs");
    Object[] arguments = new Object[selected.getParameterCount()];
    for (int i = 0; i < arguments.length; i++) {
      arguments[i] = mapper.convertValue(request.arguments().get(i), mapper.constructType(selected.getGenericParameterTypes()[i]));
    }
    try {
      return selected.invoke(bean, arguments);
    } catch (InvocationTargetException error) {
      if (error.getCause() instanceof Exception cause) throw cause;
      throw error;
    }
  }
}
