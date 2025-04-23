package com.vishal.bharti.camel.quartz.dynamic.scheduler;

import org.apache.camel.Route;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@Component
public class QuartzRoute extends RouteBuilder {

    @Autowired
    private RepoService repoService;

    @Override
    public void configure() throws Exception {
        from("direct:cancelJob")
                .log("Cancelling job: ${header.jobName}")
                .process(exchange -> {
                    String jobName = exchange.getIn().getHeader("jobName", String.class);
                    String routeUri = "dynamicGroup/" + jobName;

                    String routeIdToDelete = exchange.getContext().getRoutes().stream()
                            .filter(route -> route.getEndpoint().getEndpointUri().contains(routeUri))
                            .map(Route::getRouteId)
                            .findFirst()
                            .orElse(null);

                    if (routeIdToDelete != null) {
                        exchange.getContext().getRouteController().stopRoute(routeIdToDelete);
                        exchange.getContext().removeRoute(routeIdToDelete);
                        System.out.println("Route with ID " + routeIdToDelete + " removed successfully.");
                    } else {
                        System.out.println("Route not found for URI: " + routeUri);
                    }

//                    System.out.println(exchange.getContext().getRoutes());
                })
                .log("Job ${header.jobName} cancelled successfully.");

        from("direct:scheduleJob")
                .log("Scheduling job: ${header.jobName} to run at: ${header.startTime} (Current time: ${date:now:yyyy-MM-dd HH:mm:ss})")
                .process(exchange -> {
                    // Convert startTime (String) to Cron Expression
                    String startTimeStr = exchange.getIn().getHeader("startTime", String.class);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                    Date startTime;
                    try {
                        startTime = sdf.parse(startTimeStr); // Parse the ISO-8601 string to Date
                    } catch (ParseException e) {
                        throw new IllegalArgumentException("Invalid startTime format: " + startTimeStr, e);
                    }

                    Calendar cal = Calendar.getInstance();
                    cal.setTime(startTime);

                    // Extract time components
                    int seconds = cal.get(Calendar.SECOND);
                    int minutes = cal.get(Calendar.MINUTE);
                    int hours = cal.get(Calendar.HOUR_OF_DAY);
                    int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
                    int month = cal.get(Calendar.MONTH) + 1; // Month is 0-based

                    // Build the cron expression
                    String cronExpression =
//                            "0 * * * * ?";
                            String.format("%d %d %d %d %d ?", seconds, minutes, hours, dayOfMonth, month);

                    exchange.getIn().setHeader("cronExpression", cronExpression);
                    exchange.getIn().setHeader("resolvedStartTime", startTime);
                })
                .process(exchange -> {
                    String jobName = exchange.getIn().getHeader("jobName", String.class);
                    String cronExpression = exchange.getIn().getHeader("cronExpression", String.class);

                    String routeUri = String.format(
                            "quartz://dynamicGroup/%s?cron=%s&stateful=true",
                            jobName, cronExpression
                    );
                    exchange.getIn().setHeader("routeUri", routeUri);

                    // Dynamically create the Quartz route
                    exchange.getContext().addRoutes(new RouteBuilder() {
                        @Override
                        public void configure() {
                            from(routeUri)
                                    .log("Quartz job triggered for job " + jobName + " at ${date:now:yyyy-MM-dd HH:mm:ss}")
                                    .log("Task executed successfully for job " + jobName)
                                    .setHeader("jobName", simple(jobName))
                                    .to("direct:cancelJob");
                        }
                    });
                    exchange.getMessage().setBody(String.format("Job '%s' scheduled successfully.", jobName));
                })
                .log("Job ${header.jobName} scheduled successfully for ${header.startTime}");


        from("quartz://postConstruct?trigger.repeatCount=0")
                .log("scheuled job post construct")
                .process(exchange -> repoService.test())
                .log("task execution")
                .log("post construct job completed");
    }
}
