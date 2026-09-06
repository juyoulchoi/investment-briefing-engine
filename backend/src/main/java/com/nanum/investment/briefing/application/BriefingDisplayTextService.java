package com.nanum.investment.briefing.application;

import java.util.Comparator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class BriefingDisplayTextService {
  public String localize(String text, Map<String, String> labels) {
    if (text == null || text.isBlank() || labels == null || labels.isEmpty()) return text;

    String localized = text;
    for (Map.Entry<String, String> entry :
        labels.entrySet().stream()
            .filter(entry -> entry.getKey() != null && entry.getValue() != null)
            .sorted(Map.Entry.comparingByKey(Comparator.comparingInt(String::length).reversed()))
            .toList()) {
      String codePattern = escapedMarkdownCodePattern(entry.getKey());
      Pattern pattern =
          Pattern.compile(
              "(?<![A-Z0-9_])" + codePattern + "(?![A-Z0-9_])(?:(은|는|이|가|을|를|과|와|으로|로)(?![가-힣]))?");
      Matcher matcher = pattern.matcher(localized);
      StringBuffer result = new StringBuffer();
      while (matcher.find()) {
        String replacement =
            entry.getValue() + adjustedParticle(entry.getValue(), matcher.group(1));
        matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
      }
      matcher.appendTail(result);
      localized = result.toString();
    }
    return localized;
  }

  private String escapedMarkdownCodePattern(String code) {
    return Pattern.quote(code).replace("_", "\\E(?:_|\\\\_)\\Q");
  }

  private String adjustedParticle(String label, String particle) {
    if (particle == null || particle.isEmpty()) return "";
    boolean hasFinalConsonant = hasFinalConsonant(label);
    return switch (particle) {
      case "은", "는" -> hasFinalConsonant ? "은" : "는";
      case "이", "가" -> hasFinalConsonant ? "이" : "가";
      case "을", "를" -> hasFinalConsonant ? "을" : "를";
      case "과", "와" -> hasFinalConsonant ? "과" : "와";
      case "로", "으로" -> hasFinalConsonant && !endsWithRieul(label) ? "으로" : "로";
      default -> particle;
    };
  }

  private boolean hasFinalConsonant(String value) {
    for (int index = value.length() - 1; index >= 0; index--) {
      char character = value.charAt(index);
      if (character >= '가' && character <= '힣') return (character - '가') % 28 != 0;
    }
    return false;
  }

  private boolean endsWithRieul(String value) {
    for (int index = value.length() - 1; index >= 0; index--) {
      char character = value.charAt(index);
      if (character >= '가' && character <= '힣') return (character - '가') % 28 == 8;
    }
    return false;
  }
}
