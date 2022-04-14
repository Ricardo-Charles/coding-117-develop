package com.imooc.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Created by 廖师兄
 * 2017-08-07 23:55
 */
@Component
@Slf4j
public class RedisLock {

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 加锁
     * @param key productId
     * @param value 当前时间+最长持有锁的时间 long time = System.currentTimeMillis()+TIMEOUT;
     * @return
     */
    public boolean lock(String key, String value) {
        if(redisTemplate.opsForValue().setIfAbsent(key, value)) { //给key加锁
            return true; //加锁成功
        }

        //通过currentValue判断是否锁过期
        String currentValue = redisTemplate.opsForValue().get(key);

        //如果锁过期,说明当前持有锁的线程出问题了,没有及时释放锁(此时线程1,2进来加锁)
        if (!StringUtils.isEmpty(currentValue)  //锁过期: 当前线程持有锁的时间太久了,超过了过期时间
                // e.g. 线程A第100s上的锁, 规定最长持有锁的时间为10秒 那么currentValue = 100+10 =110秒
                //假设此时当前时间System.currentTimeMillis()为第120s, 那么说明线程A持有锁的时间为20s, 那么其他线程就可以拿到这把锁了
                && Long.parseLong(currentValue) < System.currentTimeMillis()) {

            //获取上一个锁的时间
            String oldValue = redisTemplate.opsForValue().getAndSet(key, value);

            //getAndSet()对应redis的操作GETSET, 先get再set. 举例子: 此时线程1运行上一行,先执行get方法返回上一个线程的value(时间),赋值给oldValue
            //然后把value设置成线程1新携带的B,


            //然后线程1执行if语句,获得资源,开始加锁.
            /*
             即使此时线程2抢到执行权,执行String oldValue = redisTemplate.opsForValue().getAndSet(key, value);
             此时oldValue通过get方法赋值成为了B, 线程2就不能执行下面的if语句,也就是保证线程1,2不会同时获得锁
             */
            if (!StringUtils.isEmpty(oldValue) && oldValue.equals(currentValue)) {
                return true;
            }


        }

        //如果锁过期的注释的简化版本
        if (!StringUtils.isEmpty(currentValue)  //锁过期: 当前线程持有锁的时间太久了,超过了过期时间
                && Long.parseLong(currentValue) < System.currentTimeMillis()) {

            //获取上一个锁的时间,
            String oldValue = redisTemplate.opsForValue().getAndSet(key, value);

            if (!StringUtils.isEmpty(oldValue) && oldValue.equals(currentValue)) {
                return true;
            }
        }
        return false;

//    /**
//     * 解锁,如果某个持有锁的服务器一切正常执行完的话,就可以自己解锁
//     * @param key
//     * @param value
//     */
//    public void unlock(String key, String value) {
//        try {
//            String currentValue = redisTemplate.opsForValue().get(key);
//            if (!StringUtils.isEmpty(currentValue) && currentValue.equals(value)) {
//                // currentValue.equals(value)保证了每个线程只能释放自己的锁,不能去释放别人的锁
//                redisTemplate.opsForValue().getOperations().delete(key);
//            }
//        }catch (Exception e) {
//            log.error("【redis分布式锁】解锁异常, {}", e);
//        }
//    }

}
