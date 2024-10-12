package cn.ccg.redis.service;

import cn.ccg.redis.config.ApplicationContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Service

public class RedisFileService{
	private RedisTemplate<String, Object> fileRedisTemplate;

	@Resource
	private ApplicationContextUtil appUtil;
	
	@Autowired
	protected Environment env;
	
	private int dbIdx = 0;
	
	@SuppressWarnings("unchecked")
	@PostConstruct
	private void init() {
		if (appUtil.checkExistBean("fileRedisTemplate")) {
			this.fileRedisTemplate = (RedisTemplate<String, Object>)appUtil.getBean("fileRedisTemplate");
		}
		
		dbIdx = Integer.parseInt(env.getProperty("spring.redis.database.file", "0"));
		
	}

	public void del(String key) {
		fileRedisTemplate.delete(key);
	}
	
	public boolean set(String key, byte[] data, long expire) {
//		long existingKeyCount = getKeyCount();
		double usedMemRatio = getUserMememoryRate();
		if (usedMemRatio > Double.parseDouble(env.getProperty("spring.redis.host.file.maxUsedRate", "0.8"))) {
			return false;
		}
		if (expire == 0) {
			fileRedisTemplate.boundValueOps(key).set(data);
		} else {
			fileRedisTemplate.boundValueOps(key).set(data, expire, TimeUnit.MILLISECONDS);
		}
		return true;
	}
	
	public byte[] get(String key) {
		Object data = fileRedisTemplate.boundValueOps(key).get();
		if (data != null) {
			return (byte[]) data;
		} else {
			return null;
		}
	}
	
	public int getKeyCount() {
		Object info = getInfo("Keyspace", "db" + dbIdx);
		if (info != null) {
			//keys=28,expires=0,avg_ttl=0,
			int idx = info.toString().indexOf(",");
			return Integer.parseInt(info.toString().substring("keys=".length(), idx));
		}
		return -1;
	}
	
	public double getUserMememoryRate() {
		Object usedMem = getInfo("Memory", "used_memory");
		Object maxMem = getInfo("Memory", "maxmemory");
		if (usedMem != null && maxMem != null) {
			//used_memory:1205699256
			//maxmemory:2147483648
			long used = Long.parseLong(usedMem.toString());
			long max = Long.parseLong(maxMem.toString());
			
			if (max > 0) {
				return used / new Double(max);
			}
		}
		return 1;
	}
	
	private Object getInfo(String part, String key) {
		 return fileRedisTemplate.execute(new RedisCallback<Object>() {
		    @Override
		    public Object doInRedis(RedisConnection connection) throws DataAccessException {
		        return connection.info(part).get(key);
		    }
		});
	}
}
