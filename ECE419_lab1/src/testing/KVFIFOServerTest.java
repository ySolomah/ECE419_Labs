package testing;

import org.junit.Test;
import app_kvServer.KVServer;

import junit.framework.TestCase;
import logger.LogSetup;
import org.apache.log4j.Logger;

public class KVFIFOServerTest extends TestCase {

    private KVServer ctx;
    private static Logger logger = Logger.getRootLogger();
    
    public void setUp() {
        ctx = new KVServer(8000, 2, "FIFO");
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
    public void testSimpleFifoCache() {
        logger.info("SIMPLE FIFO CACHE TEST");
        clearCacheAndStorage();
        try {
            ctx.putKV("A", "1");
            ctx.CacheStatus();
            ctx.inCache("A");
            ctx.inCache("B");
            ctx.inCache("C");
            ctx.putKV("B", "1");
            ctx.CacheStatus();
            ctx.inCache("A");
            ctx.inCache("B");
            ctx.inCache("C");
            ctx.putKV("C", "1");
            ctx.CacheStatus();
            ctx.inCache("A");
            ctx.inCache("B");
            ctx.inCache("C");
            ctx.putKV("A", "2");
            ctx.CacheStatus();
            ctx.inCache("A");
            ctx.inCache("B");
            ctx.inCache("C");
        } catch (Exception e) {
            logger.error("fifo test fail");
            e.printStackTrace();
        }
    }


    

}
