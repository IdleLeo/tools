package cn.ccg.redis.service;

import cn.ccg.redis.config.ApplicationContextUtil;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Service
public class RedisPubSubService {
	
	
	private RedisConnectionFactory  redisConnectionFactory;
	
	private RedisTemplate<String, Object> redisTemplate;
	 
	private RedisMessageListenerContainer container = new RedisMessageListenerContainer();
	
	@Resource
	private ApplicationContextUtil appUtil;
	
	@SuppressWarnings("unchecked")
	@PostConstruct
	private void init() {
		if (appUtil.checkExistBean("selfRedisTemplate")) {
			this.redisTemplate = (RedisTemplate<String, Object>)appUtil.getBean("selfRedisTemplate");
		}
		
		if (appUtil.checkExistBean("redisConnectionFactory")) {
			this.redisConnectionFactory = (RedisConnectionFactory)appUtil.getBean("redisConnectionFactory");
		}
		
		container.setConnectionFactory(redisConnectionFactory);
		container.afterPropertiesSet();
	}
	
	public MessageListener subscribe(Object delegate, String topic, String messageHandlerName) {
		MessageListenerAdapter adapter = new MessageListenerAdapter(delegate, messageHandlerName);
		adapter.afterPropertiesSet();
		container.addMessageListener(adapter, new PatternTopic(topic));
		if (!container.isRunning()) {
			container.start();
		}
		return adapter;
	}
	
	public void unsubscribe(MessageListener listener, String topic) {
		container.removeMessageListener(listener, new PatternTopic(topic));
	}
	
	public void publish(String topic, String message) {
		redisTemplate.convertAndSend(topic, message);
	}
    
}

