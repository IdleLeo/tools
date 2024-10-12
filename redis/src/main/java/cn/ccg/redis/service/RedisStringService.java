package cn.ccg.redis.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import cn.ccg.redis.config.ApplicationContextUtil;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Service
public class RedisStringService extends RedisBaseService{
	
	@Resource
	private ApplicationContextUtil appUtil;
	
	@SuppressWarnings("unchecked")
	@PostConstruct
	private void init() {
		if (appUtil.checkExistBean("selfRedisTemplate")) {
			super.redisTemplate = (RedisTemplate<String, Object>)appUtil.getBean("selfRedisTemplate");
		}
	}
	
	public void add(String key, Object value) {
		redisTemplate.boundValueOps(key).set(value);
	}
	
	public void add(String key, String value, long expireTime) {
		if (expireTime > 0) {
			redisTemplate.boundValueOps(key).set(value, expireTime, TimeUnit.SECONDS);
		} else {
			redisTemplate.boundValueOps(key).set(value);
		}
	}
	
	public void add(String key, Object obj, long expireTime) {
		if (expireTime > 0) {
			redisTemplate.boundValueOps(key).set(JSON.toJSONString(obj), expireTime, TimeUnit.SECONDS);
		} else {
			redisTemplate.boundValueOps(key).set(JSON.toJSONString(obj));
		}
	}
	
	public String get(String key) {
		Object obj = redisTemplate.boundValueOps(key).get();
		if (obj != null) {
			return obj.toString();
		} else {
			return null;
		}
	}
	
	public <T> T get(String key, Class<T> cls) {
		Object obj = redisTemplate.boundValueOps(key).get();
		if (obj != null) {
			return JSONObject.parseObject(obj.toString(), cls);
		} else {
			return null;
		}
	}
}
