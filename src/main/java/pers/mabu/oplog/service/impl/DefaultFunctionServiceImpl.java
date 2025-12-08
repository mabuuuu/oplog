package pers.mabu.oplog.service.impl;

import pers.mabu.oplog.service.IFunctionService;
import pers.mabu.oplog.service.IParseFunction;
import pers.mabu.oplog.service.ParseFunctionFactory;

/**
 * <p>
 * 默认方法实现类
 * </p>
 *
 * @author wangff
 * @since 2025/11/24
 */
public class DefaultFunctionServiceImpl implements IFunctionService {
    private final ParseFunctionFactory parseFunctionFactory;

    public DefaultFunctionServiceImpl(ParseFunctionFactory parseFunctionFactory) {
        this.parseFunctionFactory = parseFunctionFactory;
    }

    @Override
    public String apply(String functionName, Object value) {
        IParseFunction function = parseFunctionFactory.getFunction(functionName);
        if (function == null) {
            return "";
        }
        return function.apply(value);
    }

    @Override
    public boolean beforeFunction(String functionName) {
        IParseFunction function = parseFunctionFactory.getFunction(functionName);
        if (function == null) {
            return false;
        }
        return function.executeBefore();
    }
}
