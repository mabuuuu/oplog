package pers.mabu.oplog.config;

import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import pers.mabu.oplog.aspect.OpLogAspect;
import pers.mabu.oplog.core.MdcTaskExecutor;
import pers.mabu.oplog.core.OpLogExpressionEvaluator;
import pers.mabu.oplog.core.OpLogValueParser;
import pers.mabu.oplog.service.*;
import pers.mabu.oplog.service.impl.DefaultFunctionServiceImpl;
import pers.mabu.oplog.service.impl.DefaultOpLogRecordServiceImpl;
import pers.mabu.oplog.service.impl.DefaultOperatorGetServiceImpl;
import pers.mabu.oplog.service.impl.DefaultParseFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * <p>
 * 操作日志自动装配
 * </p>
 *
 * @author wangff
 * @since 2025/11/24
 */
@Configuration
@Slf4j
@ConditionalOnProperty(name = "oplog.enable", havingValue = "true", matchIfMissing = true)
public class OpLogProxyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(IFunctionService.class)
    public IFunctionService functionService(ParseFunctionFactory parseFunctionFactory) {
        return new DefaultFunctionServiceImpl(parseFunctionFactory);
    }

    @Bean
    public ParseFunctionFactory parseFunctionFactory(@Autowired List<IParseFunction> parseFunctions) {
        return new ParseFunctionFactory(parseFunctions);
    }

    @Bean
    public OpLogExpressionEvaluator opLogExpressionEvaluator() {
        return new OpLogExpressionEvaluator();
    }

    @Bean
    public OpLogValueParser opLogValueParser() {
        return new OpLogValueParser(opLogExpressionEvaluator());
    }

    @Bean
    @ConditionalOnMissingBean(IParseFunction.class)
    public DefaultParseFunction parseFunction() {
        return new DefaultParseFunction();
    }

    @Bean
    @ConditionalOnMissingBean(IOperatorGetService.class)
    @Role(BeanDefinition.ROLE_APPLICATION)
    public IOperatorGetService operatorGetService() {
        return new DefaultOperatorGetServiceImpl();
    }

    @Bean
    @ConditionalOnMissingBean(IOpLogRecordService.class)
    @Role(BeanDefinition.ROLE_APPLICATION)
    public IOpLogRecordService recordService() {
        return new DefaultOpLogRecordServiceImpl();
    }

    @Bean
    @ConditionalOnMissingBean(OpLogAspect.class)
    public OpLogAspect opLogAspect(OpLogValueParser opLogValueParser,
                                   IFunctionService functionService,
                                   IOperatorGetService operatorGetService,
                                   IOpLogRecordService opLogRecordService,
                                   @Qualifier("opLogTaskExecutor") TaskExecutor opLogTaskExecutor) {
        return new OpLogAspect(opLogValueParser, functionService, operatorGetService, opLogRecordService, opLogTaskExecutor);
    }

    @Bean
    @ConditionalOnMissingBean(name = "opLogTaskExecutor")
    public TaskExecutor opLogTaskExecutor() {
        int cores = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = new MdcTaskExecutor();
        executor.setCorePoolSize(cores * 2);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(10000);
        executor.setThreadNamePrefix("opLogThreadPool_");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setKeepAliveSeconds(60);
        executor.initialize();
        return executor;
    }
}
