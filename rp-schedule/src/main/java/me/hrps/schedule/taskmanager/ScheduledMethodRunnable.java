package me.hrps.schedule.taskmanager;

import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * Description:
 * <pre>
 *    corresponding to a task, will be serialized to zookeeper
 * </pre>
 * Author: javahuang
 * Create: 2017/9/28 上午10:12
 */
public class ScheduledMethodRunnable implements Runnable, Serializable {

    private transient final Object target;
    private transient ScheduledTaskProcessor processor;
    private transient final Method method;
    private transient Object[] args = null;

    private transient volatile Thread currThread = null;    // current task is not running if currThread is null


    private String cron;
    private String taskName;    // taskName = beanName + methodName
    private volatile boolean running = false;    // task is running
    private String argStr = null; // task parameter,split by space,only support basic types
    private String startTime;   // begin task time
    private String endTime; // end task time
    private String msg; // error msg
    private boolean startRun = false;   // run task immediate


    public ScheduledMethodRunnable(Object target, Method method, String cron) {
        this.target = target;
        this.method = method;
        this.cron = cron;
    }

    @Override
    public void run() {
        currThread = Thread.currentThread();
        try {
            msg = null;
            running = true;
            processor.refreshTaskRunningInfo(this);
            parseArgStrToArgs();

            startTime = LocalDateTime.now().toString();
            ReflectionUtils.makeAccessible(this.method);
            this.method.invoke(this.target, args);
            endTime = LocalDateTime.now().toString();

            // begin next schedule
            running = false;
            processor.scheduleTask(this);
        } catch (InvocationTargetException ex) {
            ReflectionUtils.rethrowRuntimeException(ex.getTargetException());
        } catch (IllegalAccessException ex) {
            throw new UndeclaredThrowableException(ex);
        } finally {
            running = false;
            processor.refreshTaskRunningInfo(this);
            currThread = null;
        }
    }

    /**
     * parse argStr to task parameter
     */
    public void parseArgStrToArgs() {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes != null && parameterTypes.length > 0) {
            args = new Object[parameterTypes.length];
            try {
                String[] parametersStr = null;
                boolean needSetParameter = false;
                if (StringUtils.hasText(argStr)) {
                    parametersStr = argStr.split("\\s+");
                    if (parametersStr.length == parameterTypes.length) {
                        needSetParameter = true;
                    } else {
                        setMsg("task parameter set error");
                    }
                }
                for (int i = 0; i < parameterTypes.length; i++) {
                    Class<?> parameterType = parameterTypes[i];
                    String parameterTypeName = parameterType.getName();
                    if ("int".equals(parameterTypeName)) {
                        args[i] = needSetParameter ? new Integer(parametersStr[i]) : 0;
                    } else if (parameterTypeName.contains("Integer")) {
                        args[i] = needSetParameter ? new Integer(parametersStr[i]) : null;
                    } else if ("double".equals(parameterTypeName)) {
                        args[i] = needSetParameter ? new Double(parametersStr[i]) : 0;
                    } else if (parameterTypeName.contains("Double")) {
                        args[i] = needSetParameter ? new Double(parametersStr[i]) : null;
                    } else if ("long".equals(parameterTypeName)) {
                        args[i] = needSetParameter ? new Long(parametersStr[i]) : 0;
                    } else if (parameterTypeName.contains("Long")) {
                        args[i] = needSetParameter ? new Double(parametersStr[i]) : null;
                    } else if ("java.lang.String".equals(parameterTypeName)) {
                        args[i] = needSetParameter ? parametersStr[i] : null;
                    } else if ("java.util.Date".equals(parameterTypeName)) {
                        args[i] = needSetParameter ? new Date(Long.parseLong(parametersStr[i])) : null;
                    }
                }
            } catch (Exception e) {
                setMsg("parameter parse error");
                return;
            }
        }
    }

    public Object getTarget() {
        return target;
    }

    public ScheduledTaskProcessor getProcessor() {
        return processor;
    }

    public void setProcessor(ScheduledTaskProcessor processor) {
        this.processor = processor;
    }

    public Method getMethod() {
        return method;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public Thread getCurrThread() {
        return currThread;
    }

    public void setCurrThread(Thread currThread) {
        this.currThread = currThread;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public String getArgStr() {
        return argStr;
    }

    public void setArgStr(String argStr) {
        this.argStr = argStr;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public boolean isStartRun() {
        return startRun;
    }

    public void setStartRun(boolean startRun) {
        this.startRun = startRun;
    }
}
