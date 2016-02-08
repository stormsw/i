package org.igov.util.cache;

import org.aopalliance.aop.Advice;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcut;

import java.lang.reflect.Method;

public class MethodCacheAdvisor extends AbstractPointcutAdvisor {

    private static final long serialVersionUID = 1L;

    private transient final StaticMethodMatcherPointcut pointcut = new
            StaticMethodMatcherPointcut() {
                @Override
                public boolean matches(Method method, Class<?> targetClass) {
                    return method.isAnnotationPresent(EnableCaching.class);
                }
            };

    private transient MethodCacheInterceptor interceptor;

    public void setInterceptor(MethodCacheInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public Pointcut getPointcut() {
        return this.pointcut;
    }

    @Override
    public Advice getAdvice() {
        return this.interceptor;
    }
}  