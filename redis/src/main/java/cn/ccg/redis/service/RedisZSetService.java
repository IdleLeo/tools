package cn.ccg.redis.service;

import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class RedisZSetService extends RedisBaseService {
	public double increase(String key, Object value, double score) {
		return redisTemplate.boundZSetOps(key).incrementScore(value, score);
	}
	
	public Set<Object> rangeByScore(String key, double min, double max) {
		return redisTemplate.boundZSetOps(key).rangeByScore(min, max);
	}
	
	public Set<Object> rangeRevByScore(String key, double min, double max) {
		return redisTemplate.boundZSetOps(key).reverseRangeByScore(min, max);
	}
	
	public void removeRange(String key, double min, double max) {
		redisTemplate.boundZSetOps(key).removeRangeByScore(min, max);
	}
	
	public void add(String key, Object value, double score) {
	    redisTemplate.opsForZSet().add(key, value, score);
	}
	
	public void remove(String key, String value) {
	    redisTemplate.opsForZSet().remove(key, value);
	}
	
	public Double score(String key, String value) {
	    return redisTemplate.opsForZSet().score(key, value);
	}
	
	public Set<Object> range(String key, int start, int end) {
	    return redisTemplate.opsForZSet().range(key, start, end);
	}
	
	public Set<Object> revRange(String key, int start, int end) {
	    return redisTemplate.opsForZSet().reverseRange(key, start, end);
	}
}
