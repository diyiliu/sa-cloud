package com.tiza.air.cluster;

import com.diyiliu.plugin.util.JacksonUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.util.Set;

/**
 * Description: redis.RedisUtil
 * Author: DIYILIU
 * Update: 2019-04-16 19:49
 */
public class RedisUtil {
    private JedisPool pool;

    public RedisUtil() {

    }

    public RedisUtil(JedisPool pool) {
        this.pool = pool;
    }

    /**
     * 批量删除对应的value
     *
     * @param keys
     */
    public void remove(final String... keys) {
        for (String key : keys) {
            remove(key);
        }
    }

    /**
     * 批量删除key
     *
     * @param pattern
     */
    public void removePattern(final String pattern) {
        try (Jedis jedis = pool.getResource()) {
            Set<String> keys = jedis.keys(pattern);
            if (keys.size() > 0) {
                String[] ks = keys.toArray(new String[]{});
                jedis.del(ks);
            }
        }
    }

    /**
     * 删除对应的value
     *
     * @param key
     */
    public void remove(final String key) {
        if (exists(key)) {
            try (Jedis jedis = pool.getResource()) {
                jedis.del(key);
            }
        }
    }

    /**
     * 判断缓存中是否有对应的value
     *
     * @param key
     * @return
     */
    public boolean exists(final String key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.exists(key);
        }
    }

    /**
     * 读取缓存
     *
     * @param key
     * @return
     */
    public String get(final String key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.get(key);
        }
    }

    public <T> T get(final String key, Class<T> clz) {
        String value = get(key);
        return stringToBean(value, clz);
    }

    /**
     * 写入缓存
     *
     * @param key
     * @param value
     * @return
     */
    public boolean set(final String key, String value) {
        boolean result = false;
        try (Jedis jedis = pool.getResource()) {
            jedis.set(key, value);
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public <T> boolean set(final String key, T value) {
        boolean result = false;
        try (Jedis jedis = pool.getResource()) {
            String str = beanToString(value);
            jedis.set(key, str);
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }


    /**
     * 写入缓存 + (过期时间)
     *
     * @param key
     * @param value
     * @return
     */
    public boolean set(final String key, String value, int expireTime) {
        boolean result = false;
        try (Jedis jedis = pool.getResource()) {
            jedis.setex(key, expireTime, value);
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 写入缓存 + (过期时间)
     *
     * @param key
     * @param value
     * @return
     */
    public <T> boolean set(final String key, T value, int expireTime) {
        boolean result = false;
        try (Jedis jedis = pool.getResource()) {
            String str = JacksonUtil.toJson(value);
            jedis.setex(key, expireTime, str);
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static <T> String beanToString(T value) {
        if (value == null) {

            return null;
        }

        Class<?> clazz = value.getClass();
        if (clazz == int.class || clazz == Integer.class) {

            return "" + value;
        } else if (clazz == String.class) {

            return (String) value;
        } else if (clazz == long.class || clazz == Long.class) {

            return "" + value;
        } else {

            return JacksonUtil.toJson(value);
        }
    }

    public static <T> T stringToBean(String str, Class<T> clazz) {
        if (str == null || str.length() <= 0 || clazz == null) {

            return null;
        }

        if (clazz == int.class || clazz == Integer.class) {

            return (T) Integer.valueOf(str);
        } else if (clazz == String.class) {

            return (T) str;
        } else if (clazz == long.class || clazz == Long.class) {

            return (T) Long.valueOf(str);
        } else {
            try {
                return JacksonUtil.toObject(str, clazz);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    public void setPool(JedisPool pool) {
        this.pool = pool;
    }

    public JedisPool getPool() {
        return pool;
    }
}
