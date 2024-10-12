package cn.ccg.redis.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import cn.ccg.redis.config.ApplicationContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class RedisGrpcService{
	private RedisTemplate<String, Object> grpcRedisTemplate;

	@Resource
	private ApplicationContextUtil appUtil;
	
	@Autowired
	protected Environment env;
	
	@SuppressWarnings("unchecked")
	@PostConstruct
	private void init() {
		if (appUtil.checkExistBean("grpcRedisTemplate")) {
			this.grpcRedisTemplate = (RedisTemplate<String, Object>)appUtil.getBean("grpcRedisTemplate");
		}
	}

	public void del(String key) {
		grpcRedisTemplate.delete(key);
	}
	
	public void del(String key, String field) {
		grpcRedisTemplate.boundHashOps(key).delete(field);
	}
	
	public void add(String key, String value, long expireTime) {
		if (expireTime > 0) {
			grpcRedisTemplate.boundValueOps(key).set(value, expireTime, TimeUnit.SECONDS);
		} else {
			grpcRedisTemplate.boundValueOps(key).set(value);
		}
	}
	
	public void add(String key, Object obj, long expireTime) {
		if (expireTime > 0) {
			grpcRedisTemplate.boundValueOps(key).set(JSON.toJSONString(obj), expireTime, TimeUnit.SECONDS);
		} else {
			grpcRedisTemplate.boundValueOps(key).set(JSON.toJSONString(obj));
		}
	}
	
	public String get(String key) {
		Object obj = grpcRedisTemplate.boundValueOps(key).get();
		if (obj != null) {
			return obj.toString();
		} else {
			return null;
		}
	}
	
	public <T> T get(String key, Class<T> cls) {
		Object obj = grpcRedisTemplate.boundValueOps(key).get();
		if (obj != null) {
			return JSONObject.parseObject(obj.toString(), cls);
		} else {
			return null;
		}
	}
	
	public void put(String key, String field, Object value) {
		grpcRedisTemplate.boundHashOps(key).put(field, value);
	}
	
	public void putAll(String key, Map<String, Object> map, long expireTime) {
		grpcRedisTemplate.boundHashOps(key).putAll(map);
		if (expireTime > 0) {
			grpcRedisTemplate.expire(key, expireTime, TimeUnit.SECONDS);
		}
	}
	
	public Map<Object, Object> getAll(String key) {
		Map<Object, Object> result = grpcRedisTemplate.boundHashOps(key).entries();
		if (result == null || result.size() == 0) {
			return null;
		} else {
			return result;
		}
	}
	
	public Object getField(String key, String field) {
		return grpcRedisTemplate.boundHashOps(key).get(field);
	}
}
