package com.example.demo.common.share.redis;

import java.util.concurrent.TimeUnit;

public interface IRedis {
    void set(String key, Object value);

    void setWithExpiration(String key, Object value, long timeout, TimeUnit unit);

    <T> T get(String key, Class<T> clazz);

    void delete(String key);

    boolean hasKey(String key);

    // Atomic operations (Cực kỳ quan trọng cho Flash Sales/Inventory)
    Long increment(String key, long delta);

    Long decrement(String key, long delta);

    // Distributed Lock basic
    boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit);

    String getAsString(String key);

    Long getExpire(String key, TimeUnit unit);

    boolean compareAndSet(String key, String expectedValue, String newValue, long timeout, TimeUnit unit);
}
