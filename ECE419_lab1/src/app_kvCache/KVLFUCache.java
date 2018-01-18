package KVCache;

import java.util.concurrent.*;

public class KVLFUCache implements IKVCache {
    
    public ConcurrentHashMap<String, String> cache;
    public ConcurrentLinkedDeque<CacheNode> policyOrder;

    public int cacheSize;

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

    public void Delete(String key) {
        cache.remove(key);
        policyOrder.remove(key);
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
                if(nodeToRemove.policyVal <= min) {
                    min = nodeToRemove.policyVal;
                    candidate = nodeToRemove;
                }
            }
            Delete(candidate.key);
        }
        return;
    }
}
