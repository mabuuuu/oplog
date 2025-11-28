package com.cool.store.oplog.config;

import com.cool.store.oplog.core.OpLogExpressionEvaluator;
import com.cool.store.oplog.core.OpLogValueParser;
import com.cool.store.oplog.service.*;
import com.cool.store.oplog.service.impl.DefaultFunctionServiceImpl;
import com.cool.store.oplog.service.impl.DefaultOpLogRecordServiceImpl;
import com.cool.store.oplog.service.impl.DefaultOperatorGetServiceImpl;
import com.cool.store.oplog.service.impl.DefaultParseFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

import java.util.List;

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
}
