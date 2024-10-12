package cn.ccg.redis.service;

import cn.ccg.redis.config.ApplicationContextUtil;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Map;

@Service
public class RedisMapService extends RedisBaseService {
	
	@Resource
	private ApplicationContextUtil appUtil;
	
	@SuppressWarnings("unchecked")
	@PostConstruct
	private void init() {
		if (appUtil.checkExistBean("selfRedisTemplate")) {
			super.redisTemplate = (RedisTemplate<String, Object>)appUtil.getBean("selfRedisTemplate");
		}
	}
	
	public void add(String key, String field, Object value) {
		redisTemplate.boundHashOps(key).put(field, value);
	}
	
	public void add(String key, Map<String, Object> map, long expireTime) {
		redisTemplate.boundHashOps(key).putAll(map);
		if (expireTime > 0) {
			super.expire(key, expireTime);
		}
	}

	public void del(String key,String fields){
		redisTemplate.boundHashOps(key).delete(fields);
	}
	
	public Map<Object, Object> getAll(String key) {
		Map<Object, Object> result = redisTemplate.boundHashOps(key).entries();
		if (result == null || result.size() == 0) {
			return null;
		} else {
			return result;
		}
	}
	
	public Object getField(String key, String field) {
		return redisTemplate.boundHashOps(key).get(field);
	}
}
