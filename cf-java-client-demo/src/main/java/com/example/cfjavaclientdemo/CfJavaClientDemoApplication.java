package com.example.cfjavaclientdemo;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.applications.SummaryApplicationRequest;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.RestageApplicationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@SpringBootApplication
public class CfJavaClientDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CfJavaClientDemoApplication.class, args);
    }
}

@Component
@Slf4j
class Restager {

    private final String buildpack;
    private final CloudFoundryOperations cf;
    private final CloudFoundryClient client;

    Restager(CloudFoundryOperations cf, CloudFoundryClient client, @Value("${restager.buildpack:}") String bp) {
        this.cf = cf;
        this.client = client;
        this.buildpack = (!StringUtils.hasText(bp) ? "python" : bp).trim();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ready(ApplicationReadyEvent evt) {
        cf.applications()
                .list()
                .filter(ar -> ar.getRunningInstances() > 0)
                .flatMap(as -> client
                        .applicationsV2()
                        .summary(SummaryApplicationRequest.builder()
                                  .applicationId(as.getId()).build()))
                .filter(sar -> buildpack(sar.getBuildpack(), sar.getDetectedBuildpack())
                          .contains(this.buildpack))
                .flatMap(as1 -> cf.applications().restage(RestageApplicationRequest.builder().name(as1.getName())
                                        .build()))
                .subscribe();
    }

    private String buildpack(String bp, String dbp) {
        return StringUtils.hasText(bp) ? bp : 
                (StringUtils.hasText(dbp) ? dbp : "");
    }
}
