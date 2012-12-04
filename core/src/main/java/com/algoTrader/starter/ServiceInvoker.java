package com.algoTrader.starter;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

import com.algoTrader.ServiceLocator;

public class ServiceInvoker {

    private static SimpleDateFormat dayFormat = new SimpleDateFormat("dd.MM.yy");
    private static SimpleDateFormat hourFormat = new SimpleDateFormat("dd.MM.yy hh:mm:ss");

    public static void main(String[] args) throws Exception {

        for (String arg : args) {
            invoke(arg);
        }
    }

    public static Object invoke(String arg) {

        if (arg == null) {
            throw new IllegalArgumentException("you must specifiy service and method");
        }

        StringTokenizer tokenizer = new StringTokenizer(arg, ":");

        int len = tokenizer.countTokens();
        if (len < 2) {
            throw new IllegalArgumentException("you must specifiy service and method");
        }

        String serviceName = tokenizer.nextToken();
        String methodName = tokenizer.nextToken();

        ServiceLocator serviceLocator = ServiceLocator.instance();

        serviceLocator.init(ServiceLocator.LOCAL_BEAN_REFERENCE_LOCATION);
        Object service = serviceLocator.getService(serviceName);

        Object result = null;
        boolean found = false;
        for (Method method : service.getClass().getMethods()) {
            if (method.getName().equals(methodName)) {
                found = true;

                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length != len - 2) {
                    throw new IllegalArgumentException("number of parameters does not match");
                }

                Object[] params = new Object[parameterTypes.length];
                for (int i = 0; i < parameterTypes.length; i++) {
                    Class<?> parameterType = parameterTypes[i];
                    String param = tokenizer.nextToken();
                    if (parameterType.equals(String.class)) {
                        params[i] = param;
                    } else if (parameterType.equals(int.class) || parameterType.equals(Integer.class)) {
                        params[i] = Integer.valueOf(param);
                    } else if (parameterType.equals(double.class) || parameterType.equals(Double.class)) {
                        params[i] = Double.valueOf(param);
                    } else if (parameterType.equals(Date.class)) {
                        try {
                            params[i] = hourFormat.parse(param);
                        } catch (ParseException e) {
                            try {
                                params[i] = dayFormat.parse(param);
                            } catch (ParseException e1) {
                                throw new IllegalStateException(e1);
                            }
                        }
                    }
                }

                try {
                    result = method.invoke(service, params);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }

                break;
            }
        }

        if (!found) {
            throw new IllegalArgumentException("method does not exist");
        }

        serviceLocator.shutdown();

        return result;
    }
}
