package KVCache;

import java.util.concurrent.*;

public class CacheNode {
    String key;
    String value;
    int policyVal;

    public CacheNode (
            String key,
            String value,
            int policyVal
            ) {
        this.key = key;
        this.value = value;
        this.policyVal = policyVal;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj.equals(this.key)) {
            return (true);
        } else {
            return (false);
        }
    }
}

public interface IKVCache {
    ConcurrentHashMap<String, String> cache;
    ConcurrentLinkedDeque<CacheNode> policyOrder;

    public int cacheSize;

    public void Clear();
    public String Get(String key);
    public void Delete(String key);
    public void Insert(String key, String value)
}
