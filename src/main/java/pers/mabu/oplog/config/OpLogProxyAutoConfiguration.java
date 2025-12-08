package pers.mabu.oplog.config;

import pers.mabu.oplog.core.OpLogExpressionEvaluator;
import pers.mabu.oplog.core.OpLogValueParser;
import pers.mabu.oplog.service.*;
import pers.mabu.oplog.service.impl.DefaultFunctionServiceImpl;
import pers.mabu.oplog.service.impl.DefaultOpLogRecordServiceImpl;
import pers.mabu.oplog.service.impl.DefaultOperatorGetServiceImpl;
import pers.mabu.oplog.service.impl.DefaultParseFunction;
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
