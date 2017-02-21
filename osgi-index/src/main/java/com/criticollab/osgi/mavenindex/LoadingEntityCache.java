package com.criticollab.osgi.mavenindex;/**
 * Created by ses on 2/21/17.
 */

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheStats;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

class LoadingEntityCache<K, V> implements Cache<K, V> {
    private static Logger logger = LoggerFactory.getLogger(LoadingEntityCache.class);
    private final Cache<K, V> cache;
    private final EntityLoadingCacheLoader<K, V> entityLoadingCacheLoader;
    private final EntityManager manager;
    private final Class<V> resultClass;

    LoadingEntityCache(EntityManager manager, String queryName, Class<V> resultClass, boolean shouldPreloadCache,
                       int size) throws IntrospectionException, InvocationTargetException, IllegalAccessException {
        this.manager = manager;
        this.resultClass = resultClass;
        entityLoadingCacheLoader = new EntityLoadingCacheLoader(manager, queryName, resultClass);
        cache = CacheBuilder.newBuilder().maximumSize(size).recordStats().build();

        if (shouldPreloadCache) {
            preloadCache(manager, resultClass);
        }
    }

    private void preloadCache(EntityManager manager, Class<V> resultClass) throws IllegalAccessException,
                                                                                  InvocationTargetException {
        Method getter = entityLoadingCacheLoader.getGetter();
        logger.info("preload groups");
        List<V> resultList = manager.createQuery("from " + resultClass.getSimpleName(), resultClass).getResultList();
        for (V v : resultList) {
            cache.put((K) getter.invoke(v), v);
        }
        logger.info("preload done");
    }

    @Override
    @Nullable
    public V getIfPresent(Object key) {
        return cache.getIfPresent(key);
    }

    public V get(K key) throws ExecutionException {
        return this.get(key, () -> {
            return entityLoadingCacheLoader.load(key);
        });
    }

    public V get(K key, Function<V, V> transform) throws ExecutionException {
        return this.get(key, () -> {
            return entityLoadingCacheLoader.load(key, transform);
        });
    }

    @Override
    public V get(K key, Callable<? extends V> valueLoader) throws ExecutionException {
        return cache.get(key, valueLoader);
    }

    @Override
    public ImmutableMap<K, V> getAllPresent(Iterable<?> keys) {
        return cache.getAllPresent(keys);
    }

    @Override
    public void put(K key, V value) {
        cache.put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        cache.putAll(m);
    }

    @Override
    public void invalidate(Object key) {
        cache.invalidate(key);
    }

    @Override
    public void invalidateAll(Iterable<?> keys) {
        cache.invalidateAll(keys);
    }

    @Override
    public void invalidateAll() {
        cache.invalidateAll();
    }

    @Override
    public long size() {
        return cache.size();
    }

    @Override
    public CacheStats stats() {
        return cache.stats();
    }

    @Override
    public ConcurrentMap<K, V> asMap() {
        return cache.asMap();
    }

    @Override
    public void cleanUp() {
        cache.cleanUp();
    }
}
