package com.criticollab.osgi.mavenindex;/**
 * Created by ses on 2/21/17.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Parameter;
import javax.persistence.TypedQuery;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.function.Function;

class EntityLoadingCacheLoader<K, T> {
    private final TypedQuery<T> query;
    private final EntityManager manager;
    private final String name;
    private final Class<T> resultType;
    private final Method getter;
    private final Method setter;

    public EntityLoadingCacheLoader(EntityManager manager, String queryName, Class<T> resultType) throws
                                                                                                  IntrospectionException {
        this.query = manager.createNamedQuery(queryName, resultType);
        this.manager = manager;
        this.resultType = resultType;
        Set<Parameter<?>> parameterSet = query.getParameters();
        if (parameterSet.size() != 1) {
            throw new IllegalArgumentException("named query should take 1 parameter");
        }
        Parameter<K> x = (Parameter<K>) parameterSet.iterator().next();
        Class<? extends K> parameterType = x.getParameterType();
        name = x.getName();
        Method[] methods = getMethods(resultType, parameterType);
        this.setter = methods[0];
        this.getter = methods[1];

    }

    public Method[] getMethods(Class<T> resultType, Class<? extends K> parameterType) throws IntrospectionException {
        BeanInfo beanInfo = Introspector.getBeanInfo(resultType);
        Method[] methods = new Method[2];
        boolean foundSetter = false;
        for (PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors()) {
            if (descriptor.getName().equals(name)) {
                Class<?> propertyType = descriptor.getPropertyType();
                if (parameterType.isAssignableFrom(propertyType)) {
                    methods[0] = descriptor.getWriteMethod();
                    methods[1] = descriptor.getReadMethod();
                    foundSetter = true;
                    break;
                }
            }
        }
        if (!foundSetter) {
            throw new IllegalArgumentException(
                    "Can't find setter for " + name + " in object of type " + resultType.getSimpleName());
        }
        return methods;
    }

    public Method getGetter() {
        return getter;
    }

    public Method getSetter() {
        return setter;
    }

    public T load(K key) throws Exception {
        return load(key, null);
    }

    public T load(K key, Function<T, T> transform) throws Exception {

        query.setParameter(name, key);
        T object;
        try {
            object = query.getSingleResult();
        } catch (NoResultException e) {
            object = resultType.newInstance();
            setter.invoke(object, key);
            if (transform != null) {
                object = transform.apply(object);
            }
            manager.persist(object);
        }
        return object;
    }

}
