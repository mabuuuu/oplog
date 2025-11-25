package com.cool.store.oplog.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * 操作日志正则提取
 * </p>
 *
 * @author wangff
 * @since 2025/11/24
 */
public class OpLogRegular {
//    private final static Pattern METHOD = Pattern.compile("\\{((?:[^{}]*\\{[^}]*\\})*[^{}]*)\\}");
    private final static Pattern METHOD = Pattern.compile("\\{([^{}]+?\\{[^}]+?\\})\\}");
    private final static Pattern METHOD_PARAM = Pattern.compile("\\{([^}]*)\\}");
    private final static Pattern PARAM = Pattern.compile("\\{\\{([^}]*)\\}\\}");

    public static List<CustomMethodParams> getMethod(String input) {
        Matcher matcher = METHOD.matcher(input);
        List<CustomMethodParams> list = new ArrayList<>();
        while (matcher.find()) {
            String methodSourceExpression = matcher.group(0);
            String methodExpression = matcher.group(1);
            Matcher paramMatcher = METHOD_PARAM.matcher(methodExpression);
            String paramExpression = paramMatcher.find() ? paramMatcher.group(1) : null;
            String methodName = methodExpression.substring(0, methodExpression.indexOf("{"));
            list.add(new CustomMethodParams(methodSourceExpression, methodName, paramExpression));
        }
        return list;
    }

    public static List<CustomParams> getParams(String input) {
        Matcher matcher = PARAM.matcher(input);
        List<CustomParams> list = new ArrayList<>();
        while (matcher.find()) {
            list.add(new CustomParams(matcher.group(0), matcher.group(1)));
        }
        return list;
    }
}
