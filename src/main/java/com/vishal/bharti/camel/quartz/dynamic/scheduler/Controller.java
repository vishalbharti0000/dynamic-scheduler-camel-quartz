package com.vishal.bharti.camel.quartz.dynamic.scheduler;

import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class Controller {

    @Autowired
    private ProducerTemplate template;

    @GetMapping("/sch")
    public String schedule() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("jobName", "mySingleRunJob");
        headers.put("startTime", "2025-01-17T00:49:00"); // ISO-8601 format

        template.asyncRequestBodyAndHeaders("direct:scheduleJob", null, headers);
        return "scheduled";
    }

    @GetMapping("/dsh")
    public String deschedule() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("jobName", "mySingleRunJob");

        template.asyncRequestBodyAndHeaders("direct:cancelJob", null, headers);
        return "descheduled";
    }
}
