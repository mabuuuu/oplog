package pers.mabu.oplog.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * <p>
 * MDC 上下文传递的线程池执行器
 * </p>
 *
 * @author wangff
 * @since 2026/6/11
 */
public class MdcTaskExecutor extends ThreadPoolTaskExecutor {

    private final Logger log = LoggerFactory.getLogger(MdcTaskExecutor.class);

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        Map<String, String> context = MDC.getCopyOfContextMap();
        return super.submit(() -> {
            T result;
            if (context != null) {
                //将父线程的MDC内容传给子线程
                MDC.setContextMap(context);
            }
            try {
                //执行任务
                result = task.call();
            } finally {
                log.info("ThreadMonitor:{}info:ExecutedTasks->{},totalTask->{}, RunningTasks->{}, PendingTasks->{},corePoolSize-{},currentPoolSize->{},LargestPoolSize->{}",
                        this.getThreadNamePrefix(), this.getThreadPoolExecutor().getCompletedTaskCount(), this.getThreadPoolExecutor().getTaskCount(),
                        this.getActiveCount(), this.getThreadPoolExecutor().getQueue().size(), this.getCorePoolSize(),
                        this.getPoolSize(), this.getThreadPoolExecutor().getLargestPoolSize());
                try {
                    MDC.clear();
                } catch (Exception e) {
                    log.warn("MDC clear exception", e);
                }
            }
            return result;
        });
    }

    @Override
    public void execute(Runnable task) {
        log.info("mdc thread pool task executor execute");
        Map<String, String> context = MDC.getCopyOfContextMap();
        super.execute(() -> {
            if (context != null) {
                //将父线程的MDC内容传给子线程
                MDC.setContextMap(context);
            }
            try {
                //执行任务
                task.run();
            } finally {
                log.info("ThreadMonitor:{}info:ExecutedTasks->{},totalTask->{}, RunningTasks->{}, PendingTasks->{},corePoolSize-{},currentPoolSize->{},LargestPoolSize->{}",
                        this.getThreadNamePrefix(), this.getThreadPoolExecutor().getCompletedTaskCount(), this.getThreadPoolExecutor().getTaskCount(),
                        this.getActiveCount(), this.getThreadPoolExecutor().getQueue().size(), this.getCorePoolSize(),
                        this.getPoolSize(), this.getThreadPoolExecutor().getLargestPoolSize());
                try {
                    MDC.clear();
                } catch (Exception e) {
                    log.warn("MDC clear exception", e);
                }
            }
        });
    }

}
