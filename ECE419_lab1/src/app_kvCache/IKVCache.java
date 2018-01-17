package KVCache;

import java.util.concurrent.*;



public interface IKVCache {
    public void Clear();
    public String Get(String key);
    public void Delete(String key);
    public void Insert(String key, String value);
}
