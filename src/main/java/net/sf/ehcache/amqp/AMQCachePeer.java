/**
 *  Copyright 2011 James Carr
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package net.sf.ehcache.amqp;

import static net.sf.ehcache.distribution.LegacyEventMessage.PUT;
import static net.sf.ehcache.distribution.LegacyEventMessage.REMOVE;
import static net.sf.ehcache.distribution.LegacyEventMessage.REMOVE_ALL;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.List;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.distribution.CachePeer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * Description Here.
 *
 * @author James R. Carr <james.r.carr@gmail.com>
 */
public class AMQCachePeer implements CachePeer, ChannelAwareMessageListener {
    private static final String MESSAGE_TYPE_NAME = AMQEventMessage.class.getName();
    private static final Logger LOG = LoggerFactory.getLogger(AMQCachePeer.class);

    private final RabbitTemplate rabbitTemplate;

    private final CacheManager cacheManager;
    private final String exchangeName;

    public AMQCachePeer(RabbitTemplate rabbitTemplate, CacheManager cacheManager, String exchangeName) {
        this.rabbitTemplate = rabbitTemplate;
        this.cacheManager = cacheManager;
        this.exchangeName = exchangeName;
    }

    public void put(Element element) throws IllegalArgumentException,
            IllegalStateException, RemoteException {
        throw new RemoteException("Not implemented for AMQP");
    }

    public boolean remove(Serializable key) throws IllegalStateException,
            RemoteException {
        throw new RemoteException("Not implemented for AMQP");
    }

    public void removeAll() throws RemoteException, IllegalStateException {
        throw new RemoteException("Not implemented for AMQP");

    }

    public void send(List eventMessages) throws RemoteException {
        AMQEventMessage ourMessage = (AMQEventMessage) eventMessages.get(0);
        MessageProperties basicProperties = new MessageProperties();
        basicProperties.setContentType("application/x-java-serialized-object");
        basicProperties.setType(AMQEventMessage.class.getName());

        if (LOG.isDebugEnabled()) {
            LOG.info("Publishing element with key " + ourMessage.getSerializableKey() + " with event of " + ourMessage
                    .getEvent
                            () + " on cache: " + ourMessage.getCacheName());
        }

        Message message = new Message(ourMessage.toBytes(), basicProperties);

        rabbitTemplate.send(exchangeName, ourMessage.getRoutingKey(), message);

    }

    public String getName() throws RemoteException {
        return cacheManager.getName() + " AMQCachePeer";
    }

    public String getGuid() throws RemoteException {
        throw new RemoteException("Not implemented for AMQP");
    }

    public String getUrl() throws RemoteException {
        throw new RemoteException("Not implemented for AMQP");
    }

    public String getUrlBase() throws RemoteException {
        throw new RemoteException("Not implemented for AMQP");
    }

    public List<?> getKeys() throws RemoteException {
        throw new RemoteException("Not implemented for AMQP");
    }

    public Element getQuiet(Serializable key) throws RemoteException {
        throw new RemoteException("Not implemented for AMQP");
    }

    public List getElements(List keys) throws RemoteException {
        throw new RemoteException("Not implemented for AMQP");
    }

    public void handleDelivery(String consumerTag, Envelope envelope,
                               BasicProperties properties, byte[] body) throws IOException {


    }

    private void handleMissingCache(String cacheName) {
        LOG.warn("Recieved replication update for cache not present: " + cacheName + ". This could me the cache no " +
                "longer exists.");
    }

    private void handleCacheEvent(AMQEventMessage message, Cache cache) {
        switch (message.getEvent()) {
            case PUT:
                cache.put(message.getElement(), true);
                break;
            case REMOVE:
                cache.remove(message.getElement().getKey(), true);
                break;
            case REMOVE_ALL:
                cache.removeAll(true);
                break;
            default:
                LOG.warn("Don't understand how to handle event of type " + message.getEvent());
                break;
        }
    }

    private AMQEventMessage readMessageIn(byte[] body) throws IOException {
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(
                body));
        Object o = null;
        try {
            o = in.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return (AMQEventMessage) o;
    }


    public void onMessage(Message message, Channel channel) throws Exception {

        if (MESSAGE_TYPE_NAME.equals(message.getMessageProperties().getType())) {

            AMQEventMessage ourMessage = readMessageIn(message.getBody());

            if (LOG.isDebugEnabled()) {
                LOG.debug("Received cache update " + ourMessage.getEvent() + " with element " + ourMessage.getElement
                        ());
            }

            Cache cache = cacheManager.getCache(ourMessage.getCacheName());
            if (cache == null) {
                handleMissingCache(ourMessage.getCacheName());
            } else if (!cache.getGuid().equals(ourMessage.getCacheGuid())) {
                // only handle the events that were published by other peers

                if (LOG.isDebugEnabled()) {
                    LOG.info("Cache-message for element with key " + ourMessage.getSerializableKey() + " with event of " + ourMessage
                            .getEvent
                                    () + " on cache: " + ourMessage.getCacheName() + " from peer=" + ourMessage.getCacheGuid());
                }

                handleCacheEvent(ourMessage, cache);
            }
        } else {
            LOG.warn("Received non cache message of unknown type");
        }

    }


}


