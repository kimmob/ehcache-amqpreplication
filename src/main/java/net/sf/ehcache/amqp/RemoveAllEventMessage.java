package net.sf.ehcache.amqp;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.distribution.LegacyEventMessage;

/**
 * Description Here.
 *
 * @author James R. Carr <james.r.carr@gmail.com>
 */
public class RemoveAllEventMessage extends AMQEventMessage {

    public RemoveAllEventMessage(Ehcache cache) {
        super(LegacyEventMessage.REMOVE_ALL, null, null, cache.getName(), cache.getGuid());
    }

}