package KVCache;

import java.util.concurrent.*;

public class KVFIFOCache implements IKVCache {

    public KVFIFOCache(int cacheSize) {
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
        return(cache.get(key));
    }

    public void Delete(String key) {
        cache.remove(key);
        policyCache.remove(key);
        return;
    }

    public void Insert(String key, String value) {
        for (CacheNode node : policyOrder) {
            if(node.key.equals(key)) {
                node.value = value;
                cache.replace(key, value);
                return;
            }
        }

        CacheNode fifoNode = new CacheNode(key, value, -1);
        policyOrder.push(fifoNode);
        cache.put(key, value);
        if(policyOrder.size() > cacheSize) {
            CacheNode removeNode = policyOrder.removeFirst();
            cache.remove(removeNode.key);
        }
        return;
    }
}
