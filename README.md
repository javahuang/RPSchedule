# RPSchedule

SpringMVC provide annotation `@Scheduled` to running the task.but we can't change it dynamically.
this project provides some useful features to change the default behavior, which only need **zookeeper** installed.

## future:

- dynamic change the task cron expression
- run task immediate at any time
- dynamic set task parameter
- watch the task status,such as start time,end time, if running,etc...

## examples

see rp-schedule-samples `me.hrps.DynamicScheduledTests`

![tp-scheduled](http://o6mo1i54c.bkt.clouddn.com/tp-scheduled.gif)


