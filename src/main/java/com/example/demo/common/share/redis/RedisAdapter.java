package com.example.demo.common.share.redis;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class RedisAdapter implements IRedis {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper; // Dùng để convert data an toàn khi get
    private static final String CAS_SCRIPT_SOURCE = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "   redis.call('set', KEYS[1], ARGV[2], 'PX', ARGV[3]) " +
            "   return 1 " +
            "else " +
            "   return 0 " +
            "end";

    private static final DefaultRedisScript<Long> CAS_SCRIPT = new DefaultRedisScript<>(CAS_SCRIPT_SOURCE, Long.class);

    public RedisAdapter(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void setWithExpiration(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    @Override
    public <T> T get(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null)
            return null;
        return objectMapper.convertValue(value, clazz);
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    @Override
    public Long decrement(String key, long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }

    @Override
    public boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit));
    }

    @Override
    public String getAsString(String key) {
        // Trả về String thuần, tự động xử lý trả về null nếu key không tồn tại
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? value.toString() : null;
    }

    @Override
    public Long getExpire(String key, TimeUnit unit) {
        Long expire = redisTemplate.getExpire(key, unit);
        return expire;
    }

    @Override
    public boolean compareAndSet(String key, String expectedValue, String newValue, long timeout, TimeUnit unit) {
        // Chuyển đổi tham số thời gian sang Milliseconds vì cờ 'PX' trong lệnh Set của
        // Redis yêu cầu Milliseconds
        long timeoutMs = unit.toMillis(timeout);

        // Thực thi kịch bản nguyên tử
        Long result = redisTemplate.execute(
                CAS_SCRIPT,
                Collections.singletonList(key), // KEYS[1]: Bắt buộc truyền vào dưới dạng List
                expectedValue, // ARGV[1]: Giá trị kỳ vọng
                newValue, // ARGV[2]: Giá trị mới cần đè lên
                String.valueOf(timeoutMs) // ARGV[3]: TTL (Bắt buộc parse sang String vì ARGV trong Lua là String)
        );

        // Trả về true nếu kịch bản Lua return 1 (nghĩa là Compare và Set đều thành
        // công)
        return result != null && result == 1L;
    }
}
