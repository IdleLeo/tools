package cn.ccg.redis.service;

import cn.ccg.redis.config.ApplicationContextUtil;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class RedisListService extends RedisBaseService {
	
	@Resource
	private ApplicationContextUtil appUtil;
	
	@SuppressWarnings("unchecked")
	@PostConstruct
	private void init() {
		if (appUtil.checkExistBean("selfRedisTemplate")) {
			super.redisTemplate = (RedisTemplate<String, Object>)appUtil.getBean("selfRedisTemplate");
		}
	}
	
	public void addR(String key, Object value) {
		redisTemplate.boundListOps(key).rightPush(value);
	}
	
	public void add(String key, List<Object> list, long expireInSeconds) {
		list.forEach(obj -> {
			addR(key, obj);
		});
		super.expire(key, expireInSeconds);
	}
	
	public void addL(String key, Object value) {
		redisTemplate.boundListOps(key).leftPush(value);
	}
	
	public Object popR(String key) {
		return redisTemplate.boundListOps(key).rightPop();
	}
	
	public Object popL(String key) {
		return redisTemplate.boundListOps(key).leftPop();
	}
	
	public List<Object> get(String key, long start, long end) {
		if (end > redisTemplate.boundListOps(key).size()) {
			end = redisTemplate.boundListOps(key).size();
		}
		return redisTemplate.boundListOps(key).range(start, end);
	}
	
	public Object blPop(String key) {
		return redisTemplate.boundListOps(key).leftPop(0, TimeUnit.SECONDS);
	}
	
	public Object brPop(String key) {
		return redisTemplate.boundListOps(key).rightPop(0, TimeUnit.SECONDS);
	}
}
