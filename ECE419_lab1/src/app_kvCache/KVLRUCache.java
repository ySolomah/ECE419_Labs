package KVCache;

import java.util.concurrent.*;
import org.apache.log4j.Logger;

public class KVLRUCache implements IKVCache {


    public ConcurrentHashMap<String, String> cache;
    public ConcurrentLinkedDeque<CacheNode> policyOrder;

    public int cacheSize;

    private static Logger logger = Logger.getRootLogger();

    public KVLRUCache(int cacheSize) {
        cache = new ConcurrentHashMap<String, String>();
        policyOrder = new ConcurrentLinkedDeque<CacheNode>();
        this.cacheSize = cacheSize;
    }

    public void Clear() {
        cache.clear();
        policyOrder.clear();
        return;
    }

    public String Get(String key) {
        String foundVal = cache.get(key);
        if(foundVal != null) {
            for (CacheNode node : policyOrder) {
                if(node.key.equals(key)) {
                    boolean gotNode = policyOrder.remove(node);
                    if(!gotNode) {
                        logger.info("Failed to remove node: " + node.key);
                    }
                    policyOrder.push(node);
                }
            }
        }
        return(foundVal);
    }

    public void Delete(String key) {
        cache.remove(key);
        for(CacheNode node : policyOrder) {
            if(node.key.equals(key)) {
                policyOrder.remove(node);
            }
        }
    }

    public void Insert(String key, String value) {
        
        for (CacheNode node : policyOrder) {
            if(node.key.equals(key)) {
                logger.info("Replacing " + key);
                node.value = value;
                boolean gotNode = policyOrder.remove(node);
                if(!gotNode) {
                    logger.info("Failed to remove node: " + node.key);
                }
                policyOrder.push(node);
                cache.replace(key, value);
                return;
            }
        }
        
        logger.info("Pushing: " + key + " with value: " + value);
        CacheNode mruNode = new CacheNode(key, value, -1);
        policyOrder.push(mruNode);
        cache.put(key, value);
        while(policyOrder.size() > cacheSize) {
            CacheNode removeNode = policyOrder.removeLast();
            cache.remove(removeNode.key);
            logger.info("Removing: " + removeNode.key + " with value: " + removeNode.value);
        }
        return;
    }

    public void CacheStatus() {
        System.out.println("\n FIFO CACHE STATUS");
        for (CacheNode node : policyOrder) {
           System.out.println("Node : " + node.key
                   + "; Value : " + node.value
                   + "; policyVal : " + node.policyVal
                   );
        }
    }
}
