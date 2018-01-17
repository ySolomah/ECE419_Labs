package KVCache;

import java.util.concurrent.*;

public class KVLRUCache implements IKVCache {
    
    public KVLRUCache(int cacheSize) {
        cache = new ConcurentHashMap<String, String>();
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
            CacheNode mruNode = policyOrder.remove(key);
            policyOrder.push(mruNode);
        }
        return(foundVal);
    }

    public void Delete(String key) {
        cache.remove(key);
        policyCache.remove(key);
    }

    public void Insert(String key, String value) {
        for (CacheNode node : policyOrder) {
            if(node.key.equals(key)) {
                node.value = value;
                cache.replace(key, value);
                return;
            }
        }
        
        CacheNode mruNode = new CacheNode(key, value, -1);
        policyOrder.push(mruNode);
        cache.put(key, value);
        if(policyOrder.size() > cacheSize) {
            CacheNode removeNode = policyOrder.removeLast();
            cache.remove(removeNode.key);
        }
        return;
    }
}
