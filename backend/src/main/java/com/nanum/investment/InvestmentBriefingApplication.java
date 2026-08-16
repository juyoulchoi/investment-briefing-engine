package com.nanum.investment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InvestmentBriefingApplication {
  public static void main(String[] args) {
    SpringApplication.run(InvestmentBriefingApplication.class, args);
  }
}
