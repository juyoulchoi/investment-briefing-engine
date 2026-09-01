package com.nanum.investment.marketdata.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.nanum.investment.marketdata.domain.KofiaDataset;
import com.nanum.investment.marketdata.infrastructure.KofiaCatalogClient;
import com.nanum.investment.marketdata.infrastructure.KofiaCatalogRepository;
import com.nanum.investment.marketdata.infrastructure.KofiaRequestRateLimiter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class KofiaCatalogService {
  private final KofiaCatalogClient client;
  private final KofiaCatalogRepository repository;
  private final KofiaRequestRateLimiter limiter;

  public KofiaCatalogService(
      KofiaCatalogClient client,
      KofiaCatalogRepository repository,
      KofiaRequestRateLimiter limiter) {
    this.client = client;
    this.repository = repository;
    this.limiter = limiter;
  }

  public SyncView sync() {
    JsonNode favorites = client.favorites();
    Set<String> normalized =
        Arrays.stream(KofiaDataset.values()).map(KofiaDataset::serviceId).collect(Collectors.toSet());
    int count = 0;
    for (JsonNode favorite : favorites.path("dsResultList")) {
      limiter.acquire(0);
      String serviceId = favorite.path("TMPV2").asText();
      repository.save(favorite, client.metadata(serviceId), normalized.contains(serviceId));
      count++;
    }
    return new SyncView(count, normalized.size());
  }

  public List<Map<String, Object>> services() {
    return repository.services();
  }

  public record SyncView(int discoveredCount, int normalizedCount) {}
}
