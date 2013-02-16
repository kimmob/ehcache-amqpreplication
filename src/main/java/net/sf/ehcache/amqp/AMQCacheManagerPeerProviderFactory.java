package net.sf.ehcache.amqp;

import java.util.Properties;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.distribution.CacheManagerPeerProvider;
import net.sf.ehcache.distribution.CacheManagerPeerProviderFactory;
import net.sf.ehcache.util.PropertyUtil;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * @author James R. Carr <james.r.carr@gmail.com>
 */
public class AMQCacheManagerPeerProviderFactory extends CacheManagerPeerProviderFactory {
    private final String DEFAULT_EXCHANGE = "ehcache.replication";

    /**
     * @param cacheManager the CacheManager instance connected to this peer provider
     * @param properties   implementation specific properties. These are configured as comma
     *                     separated name value pairs in ehcache.xml
     * @return a provider, already connected to a message queue and exchange
     */
    @Override
    public CacheManagerPeerProvider createCachePeerProvider(
            CacheManager cacheManager, Properties properties) {

        // ConnectionFactory factory = ObjectMapper.createFrom(ConnectionFactory.class, properties);

        ConnectionFactory connectionFactory = new CachingConnectionFactory(properties.getProperty("host"));

        final String exchangeName = getExchangeName(properties);

        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.declareExchange(new TopicExchange(exchangeName) );

        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);

        AMQCacheManagerPeerProvider amqCacheManagerPeerProvider = new AMQCacheManagerPeerProvider(rabbitTemplate, rabbitAdmin, cacheManager, exchangeName);

        return amqCacheManagerPeerProvider;

    }

    private String getExchangeName(Properties properties) {
        String exchangeName = PropertyUtil.extractAndLogProperty("exchange", properties);
        return exchangeName == null ? DEFAULT_EXCHANGE : exchangeName;
    }

}