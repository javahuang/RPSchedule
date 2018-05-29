# RPSchedule

SpringMVC provide annotation `@Scheduled` to running the task.but we can't change it dynamically.
this project provides some useful features to change the default behavior, which only need **zookeeper** installed.

## future:

- dynamic change the task cron expression
- run task immediate at any time
- dynamic set task parameter
- watch the task status,such as start time,end time, if running,etc...

## 特性：
- 允许动态改变任务的 cron 表达式
- 可以立即执行任务
- 可以动态设置任务参数
- 可以查看任务状态，任务开始、结束时间、任务是否正在执行等

## examples

see rp-schedule-samples `me.hrps.DynamicScheduledTests`

![tp-scheduled](http://o6mo1i54c.bkt.clouddn.com/tp-scheduled.gif)


