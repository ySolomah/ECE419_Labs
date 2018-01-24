package testing;

import org.junit.Test;
import app_kvServer.KVServer;

import junit.framework.TestCase;
import logger.LogSetup;
import org.apache.log4j.Logger;

public class KVLRUServerTest extends TestCase {

    private KVServer ctx;
    private static Logger logger = Logger.getRootLogger();
    
    public void setUp() {
        ctx = new KVServer(8000, 2, "LRU");
    }

    public void tearDown() {
        clearCacheAndStorage();
    }


    public void clearCacheAndStorage() {
        logger.info("CLEAR CACHE AND STORAGE");
        ctx.clearCache();
        ctx.clearStorage();
    }

    @Test
    public void testSimpleLruCache() {
        logger.info("SIMPLE LRU CACHE TEST");
        clearCacheAndStorage();
        try {
            ctx.putKV("A", "1");
            ctx.CacheStatus();
            ctx.putKV("B", "1");
            ctx.CacheStatus();
            ctx.putKV("A", "2");
            ctx.CacheStatus();
            ctx.putKV("C", "1");
            ctx.CacheStatus();
            ctx.putKV("A", "3");
            ctx.CacheStatus();
        } catch (Exception e) {
            logger.error("LRU test fail");
            e.printStackTrace();
        }
    }



    

}
