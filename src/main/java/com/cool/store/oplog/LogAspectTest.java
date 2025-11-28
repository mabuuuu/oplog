package com.cool.store.oplog;

import com.cool.store.oplog.annotation.OpLog;
import com.cool.store.oplog.aspect.OpLogAspect;
import com.cool.store.oplog.core.OpLogContext;
import com.cool.store.oplog.core.OpLogExpressionEvaluator;
import com.cool.store.oplog.core.OpLogValueParser;
import com.cool.store.oplog.service.IParseFunction;
import com.cool.store.oplog.service.ParseFunctionFactory;
import com.cool.store.oplog.service.impl.DefaultFunctionServiceImpl;
import com.cool.store.oplog.service.impl.DefaultOpLogRecordServiceImpl;
import com.cool.store.oplog.service.impl.DefaultOperatorGetServiceImpl;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;

/**
 * <p>
 *
 * </p>
 *
 * @author wangff
 * @since 2025/11/24
 */
@SpringBootTest(classes = {LogAspectTest.TestConfig.class})
class LogAspectTest {

//    @Configuration
//    @EnableAspectJAutoProxy
    static class TestConfig {
        // 注册你的切面
        @Bean
        public OpLogAspect logAspect() {
            OpLogValueParser opLogValueParser = new OpLogValueParser(new OpLogExpressionEvaluator());
            // 自定义函数
            IParseFunction getNewAddress = new IParseFunction() {
                @Override
                public String functionName() {
                    return "getNewAddress";
                }

                @Override
                public String apply(Object value) {
                    TestObj testObj = (TestObj) value;
                    return testObj.getAddress() + "_new";
                }
            };
            IParseFunction getOldAddress = new IParseFunction() {
                @Override
                public String functionName() {
                    return "getOldAddress";
                }

                @Override
                public String apply(Object value) {
                    return value + "_old";
                }

                @Override
                public boolean executeBefore() {
                    return true;
                }
            };
            ParseFunctionFactory parseFunctionFactory = new ParseFunctionFactory(Arrays.asList(getOldAddress, getNewAddress));
            DefaultOperatorGetServiceImpl operatorGetService = new DefaultOperatorGetServiceImpl();
            DefaultOpLogRecordServiceImpl opLogRecordService = new DefaultOpLogRecordServiceImpl();
            DefaultFunctionServiceImpl functionService = new DefaultFunctionServiceImpl(parseFunctionFactory);
            return new OpLogAspect(opLogValueParser, functionService, operatorGetService, opLogRecordService);
        }

        // 模拟被切面监控的Service
        @Service
        public static class TestService {
            @Resource
            private Test2Service test2Service;

            @OpLog(success = "修改了订单的配送地址[{{#oldAddress}}-{getOldAddress{#oldAddress}}]到[{getNewAddress{#request}}], 这里是上下文变量:{{#ttttt}}")
            public String doSomething(String oldAddress, TestObj request) {
                OpLogContext.putVariable("ttttt", "123123123");
                test2Service.test2(2);
                return "result";
            }


        }

        @Service
        public static class Test2Service {
            @OpLog(
                    success = "这里是入参:{{#aaa}}, 自定义变量:{{#ttttt}}",
                    module = "模块2",
                    category = "分类2",
                    condition = "#aaa > 1"
            )
            public String test2(Integer aaa) {
                OpLogContext.putVariable("ttttt", "aaaaaaaa");
                return aaa.toString() + "result";
            }
        }
    }

    @Data
    @AllArgsConstructor
    public static class TestObj{
        private String address;
    }

    @Autowired
    private TestConfig.TestService testService;

    @Test
    void testAspectIsApplied() {
        // 调用方法，验证切面是否生效
        String result = testService.doSomething("杭州市", new TestObj("上海市"));
        // 验证日志记录逻辑
    }
}