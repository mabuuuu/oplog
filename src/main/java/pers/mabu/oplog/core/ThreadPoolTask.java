package pers.mabu.oplog.core;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * <p>
 *
 * </p>
 *
 * @author wangff
 * @since 2026/6/11
 */
@Configuration
public class ThreadPoolTask {

    @Bean
    public TaskExecutor opLogThreadPool() {
        int cores = Runtime.getRuntime().availableProcessors();

        ThreadPoolTaskExecutor executor = new MdcTaskExecutor();
        // 核心线程数目
        executor.setCorePoolSize(cores*2);
        // 指定最大线程数
        executor.setMaxPoolSize(100);
        // 队列中最大的数目
        executor.setQueueCapacity(10000);
        // 线程名称前缀
        executor.setThreadNamePrefix("opLogThreadPool_");
        // 对拒绝task的处理策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 线程空闲后的最大存活时间
        executor.setKeepAliveSeconds(60);
        // 加载
        executor.initialize();
        return executor;
    }

    public static class MdcTaskExecutor extends ThreadPoolTaskExecutor {
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
                            this.getThreadNamePrefix(),this.getThreadPoolExecutor().getCompletedTaskCount(),this.getThreadPoolExecutor().getTaskCount(),
                            this.getActiveCount(),this.getThreadPoolExecutor().getQueue().size(),this.getCorePoolSize(),
                            this.getPoolSize(),this.getThreadPoolExecutor().getLargestPoolSize());
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
                            this.getThreadNamePrefix(),this.getThreadPoolExecutor().getCompletedTaskCount(),this.getThreadPoolExecutor().getTaskCount(),
                            this.getActiveCount(),this.getThreadPoolExecutor().getQueue().size(),this.getCorePoolSize(),
                            this.getPoolSize(),this.getThreadPoolExecutor().getLargestPoolSize());
                    try {
                        MDC.clear();
                    } catch (Exception e) {
                        log.warn("MDC clear exception", e);
                    }
                }
            });
        }
    }
}
