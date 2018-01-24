package KVCache;

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
