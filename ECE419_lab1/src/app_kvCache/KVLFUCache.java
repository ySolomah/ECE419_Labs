package KVCache;

import java.util.concurrent.*;
import org.apache.log4j.Logger;

public class KVLFUCache implements IKVCache {
    
    public ConcurrentHashMap<String, String> cache;
    public ConcurrentLinkedDeque<CacheNode> policyOrder;

    public int cacheSize;

    private static Logger logger = Logger.getRootLogger();
    public KVLFUCache(int cacheSize) {
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
                    node.policyVal++;
                    break;
                }
            }
        }
        return(foundVal);
    }

    public String GetNonUpdate(String key) {
        return(cache.get(key));
    }

    public void Delete(String key) {
        cache.remove(key);
        for (CacheNode node : policyOrder) {
            if(node.key.equals(key)) {
                policyOrder.remove(node);
            }
        }
    }

    public void Insert(String key, String value) {
        for (CacheNode node : policyOrder) {
            if(node.key.equals(key)) {
                node.value = value;
                node.policyVal++;
                cache.replace(key, value);
                return;
            }
        }

 
        CacheNode node = new CacheNode(key, value, 1);
        policyOrder.push(node);
        cache.put(key, value);

        while(policyOrder.size() > cacheSize) {
            int min = Integer.MAX_VALUE;
            CacheNode candidate = null;
            for (CacheNode nodeToRemove : policyOrder) {
                if(nodeToRemove.policyVal <= min && !nodeToRemove.key.equals(key)) {
                    if(!nodeToRemove.key.equals(key)) {
                        min = nodeToRemove.policyVal;
                        candidate = nodeToRemove;
                    }
                }
                if(candidate == null) {
                    candidate = node;
                }
            }
            Delete(candidate.key);
        }
               return;
    }

    public void CacheStatus() {
        System.out.println("\n LFU CACHE STATUS");
        for (CacheNode node : policyOrder) {
           System.out.println("Node : " + node.key
                   + "; Value : " + node.value
                   + "; policyVal : " + node.policyVal
                   );
        }
    }
}
