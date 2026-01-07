package com.eventbooking.services;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DailyTaskScheduler {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final CreditPointsService creditService = CreditPointsService.getInstance();

    public void start() {
        // Define the task to be executed
        Runnable dailyTask = () -> {
            try {
                System.out.println("SCHEDULER: Executing daily tasks at " + LocalDateTime.now());
                creditService.awardDailyCredits();
            } catch (Exception e) {
                System.err.println("SCHEDULER: An error occurred during the daily scheduled task.");
                e.printStackTrace();
            }
        };

        // Calculate the initial delay until the next midnight
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.with(LocalTime.MIDNIGHT).plusDays(1);
        long initialDelay = Duration.between(now, nextRun).getSeconds();

        // Schedule the task to run at midnight, and then every 24 hours
        scheduler.scheduleAtFixedRate(dailyTask, initialDelay, TimeUnit.DAYS.toSeconds(1), TimeUnit.SECONDS);

        System.out.println("Daily Task Scheduler Initialized.");
        System.out.println("Next automatic credit point distribution will be at: " + nextRun);
    }

    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException ex) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("Daily Task Scheduler has been shut down.");
    }
}