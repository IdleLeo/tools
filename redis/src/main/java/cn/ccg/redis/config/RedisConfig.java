package cn.ccg.redis.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

import javax.annotation.Resource;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableCaching
public class RedisConfig {
	
	@Resource
	private Environment environment;

	@Bean("redisConnectionFactory")
	@Primary
	public RedisConnectionFactory getRedisConnectionFactory() {
		JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory();  
        jedisConnectionFactory.setHostName(environment.getProperty("spring.redis.host"));  
        jedisConnectionFactory.setPort(Integer.parseInt(environment.getProperty("spring.redis.port", "6379")));
        if (environment.getProperty("spring.redis.password") != null) {
        	jedisConnectionFactory.setPassword(environment.getProperty("spring.redis.password"));  
        }
        
        if (environment.getProperty("spring.redis.database") != null) {
        	jedisConnectionFactory.setDatabase(Integer.parseInt(environment.getProperty("spring.redis.database")));
        } else {
        	jedisConnectionFactory.setDatabase(0); //默认=0
        }
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(Integer.parseInt(environment.getProperty("spring.redis.pool.max-active", "50")));
        jedisPoolConfig.setMaxIdle(Integer.parseInt(environment.getProperty("spring.redis.pool.max-idle", "20")));
        jedisPoolConfig.setMinIdle(Integer.parseInt(environment.getProperty("spring.redis.pool.min-idle", "20")));
        jedisPoolConfig.setMaxWaitMillis(Long.parseLong(environment.getProperty("spring.redis.pool.max-wait")));
        jedisPoolConfig.setTestOnBorrow(true);
        jedisPoolConfig.setTestOnReturn(true);

        jedisConnectionFactory.setPoolConfig(jedisPoolConfig);  
		
		return jedisConnectionFactory;
	}
	
	@Bean("grpcRedisConnectionFactory")
	@ConditionalOnProperty(name="spring.redis.host.database.grpc")
	public RedisConnectionFactory getGrpcRedisConnectionFactory() {
		JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory();  
        jedisConnectionFactory.setHostName(environment.getProperty("spring.redis.host"));  
        jedisConnectionFactory.setPort(Integer.parseInt(environment.getProperty("spring.redis.port", "6379")));
        if (environment.getProperty("spring.redis.password") != null) {
        	jedisConnectionFactory.setPassword(environment.getProperty("spring.redis.password"));  
        }
        
        if (environment.getProperty("spring.redis.host.database.grpc") != null) {
        	jedisConnectionFactory.setDatabase(Integer.parseInt(environment.getProperty("spring.redis.host.database.grpc")));
        } else {
        	jedisConnectionFactory.setDatabase(0); //默认=0
        }
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(Integer.parseInt(environment.getProperty("spring.redis.pool.max-active", "50")));
        jedisPoolConfig.setMaxIdle(Integer.parseInt(environment.getProperty("spring.redis.pool.max-idle", "20")));
        jedisPoolConfig.setMinIdle(Integer.parseInt(environment.getProperty("spring.redis.pool.min-idle", "20")));
        jedisPoolConfig.setMaxWaitMillis(Long.parseLong(environment.getProperty("spring.redis.pool.max-wait")));
        jedisPoolConfig.setTestOnBorrow(true);
        jedisPoolConfig.setTestOnReturn(true);

        jedisConnectionFactory.setPoolConfig(jedisPoolConfig);  
		
		return jedisConnectionFactory;
	}

	@Bean("fileRedisConnectionFactory")
	@ConditionalOnProperty(name="spring.redis.host.file")
	public RedisConnectionFactory getFileRedisConnectionFactory() {
		JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory();  
        jedisConnectionFactory.setHostName(environment.getProperty("spring.redis.host.file"));  
        jedisConnectionFactory.setPort(Integer.parseInt(environment.getProperty("spring.redis.port.file", "6379")));
        if (environment.getProperty("spring.redis.password.file") != null) {
        	jedisConnectionFactory.setPassword(environment.getProperty("spring.redis.password.file"));  
        }
        
        if (environment.getProperty("spring.redis.database.file") != null) {
        	jedisConnectionFactory.setDatabase(Integer.parseInt(environment.getProperty("spring.redis.database.file")));
        } else {
        	jedisConnectionFactory.setDatabase(0); //默认=0
        }
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(Integer.parseInt(environment.getProperty("spring.redis.pool.max-active.file", "50")));  
        jedisPoolConfig.setMaxIdle(Integer.parseInt(environment.getProperty("spring.redis.pool.max-idle.file", "20")));  
        jedisPoolConfig.setMinIdle(Integer.parseInt(environment.getProperty("spring.redis.pool.min-idle.file", "20")));  
        jedisPoolConfig.setMaxWaitMillis(Long.parseLong(environment.getProperty("spring.redis.pool.max-wait.file", "60000")));  
        jedisPoolConfig.setMaxWaitMillis(Long.parseLong(environment.getProperty("spring.redis.pool.max-wait", "60000")));  
        jedisPoolConfig.setTestOnBorrow(true);  
        jedisPoolConfig.setTestOnReturn(true);

        jedisConnectionFactory.setPoolConfig(jedisPoolConfig);  
		
		return jedisConnectionFactory;
	}
	
	@Bean("selfRedisTemplate")
	@Primary
    public RedisTemplate<String, Object> redisTemplate(@Qualifier("redisConnectionFactory") RedisConnectionFactory factory){
		RedisTemplate<String, Object> template = new RedisTemplate<String, Object>();

		Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<Object>(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);
		StringRedisSerializer stringSerializer = new StringRedisSerializer(Charset.forName("utf-8"));
		GenericToStringSerializer<String> genericStringSerializer = new GenericToStringSerializer<String>(String.class);
		template.setKeySerializer(stringSerializer);
		template.setValueSerializer(jackson2JsonRedisSerializer);
		template.setHashKeySerializer(stringSerializer);
		template.setHashValueSerializer(genericStringSerializer);
		template.setConnectionFactory(factory);
        template.afterPropertiesSet();
        return template;
    }
	
	@Bean("fileRedisTemplate")
	@ConditionalOnProperty(name="spring.redis.host.file")
    public RedisTemplate<String, Object> fileRedisTemplate(@Qualifier("fileRedisConnectionFactory") RedisConnectionFactory factory){
		RedisTemplate<String, Object> template = new RedisTemplate<String, Object>();
		JdkSerializationRedisSerializer jdkSerialization = new JdkSerializationRedisSerializer();
		StringRedisSerializer stringSerializer = new StringRedisSerializer(Charset.forName("utf-8"));
		template.setKeySerializer(stringSerializer);
		template.setValueSerializer(jdkSerialization);
		template.setConnectionFactory(factory);
        template.afterPropertiesSet();
        return template;
    }
	
	@Bean("grpcRedisTemplate")
	@ConditionalOnProperty(name="spring.redis.host.database.grpc")
    public RedisTemplate<String, Object> grpcRedisTemplate(@Qualifier("grpcRedisConnectionFactory") RedisConnectionFactory factory){
		RedisTemplate<String, Object> template = new RedisTemplate<String, Object>();

		Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<Object>(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);
		StringRedisSerializer stringSerializer = new StringRedisSerializer(Charset.forName("utf-8"));
		GenericToStringSerializer<String> genericStringSerializer = new GenericToStringSerializer<String>(String.class);
		template.setKeySerializer(stringSerializer);
		template.setValueSerializer(jackson2JsonRedisSerializer);
		template.setHashKeySerializer(stringSerializer);
		template.setHashValueSerializer(genericStringSerializer);
		template.setConnectionFactory(factory);
        template.afterPropertiesSet();
        return template;
    }
	
	@Bean 
    public CacheManager cacheManager(RedisTemplate<String, Object> redisTemplate) {
        RedisCacheManager cacheManager = new RedisCacheManager(redisTemplate);
        List<String> cacheNames = new ArrayList<>();
        if (environment.getProperty("redis.cache.names") != null) {
        	cacheNames = Arrays.asList(environment.getProperty("redis.cache.names").split(","));
        	cacheManager.setCacheNames(cacheNames);
        }
        
        if (environment.getProperty("redis.expiretime") != null) {
        	cacheManager.setDefaultExpiration(Integer.parseInt(environment.getProperty("redis.expiretime")));
        } else {
        	cacheManager.setDefaultExpiration(60*60*24L); //单位秒.默认24小时
        }
        cacheManager.afterPropertiesSet();
        return cacheManager;
    }
	
	@Bean
	@ConditionalOnProperty(name="spring.redis.host.file")
    RedisMessageListenerContainer container(@Qualifier("redisConnectionFactory") RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }
	
//	@Bean
//    public KeyGenerator customKeyGenerator(){
//        return new KeyGenerator() {
//            @Override
//            public Object generate(Object target, Method method, Object... params) {
//                StringBuilder sb = new StringBuilder();
//                sb.append(method.getName());
//                for (Object obj : params) {
//                    sb.append(obj.toString());
//                }
//                return sb.toString();
//            }
//        };
//    }
}
