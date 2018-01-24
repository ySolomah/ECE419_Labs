package KVCache;

import java.util.concurrent.*;
import org.apache.log4j.Logger;

public class KVFIFOCache implements IKVCache {

    public ConcurrentHashMap<String, String> cache;
    public ConcurrentLinkedDeque<CacheNode> policyOrder;

    public int cacheSize;

    private static Logger logger = Logger.getRootLogger();
 
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
        for(CacheNode node : policyOrder) {
            if(node.key.equals(key)) {
                policyOrder.remove(node);
            }
        }
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
            CacheNode removeNode = policyOrder.removeLast();
            cache.remove(removeNode.key);
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
