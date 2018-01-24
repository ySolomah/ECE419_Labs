package testing;

import org.junit.Test;
import app_kvServer.KVServer;

import junit.framework.TestCase;
import logger.LogSetup;
import org.apache.log4j.Logger;

public class KVOverWriteServerTest extends TestCase {

    private KVServer ctx;
    private static Logger logger = Logger.getRootLogger();
    
    public void setUp() {
        ctx = new KVServer(8000, 2, "FIFO");
    }

    public void tearDown() {
        //clearCacheAndStorage();
    }


    public void clearCacheAndStorage() {
        logger.info("CLEAR CACHE AND STORAGE");
        ctx.clearCache();
        ctx.clearStorage();
    }
    
    @Test
    public void testSimpleOverWrite() {
        logger.info("SIMPLE OVER WRITE TEST");
        clearCacheAndStorage();
        String A;
        String B;
        String C;
        try {
            ctx.putKV("Hello", "1");
            ctx.putKV("HelloThere", "2");
            ctx.putKV("Hello", "3");
            A = ctx.searchStorage("Hello");
            B = ctx.searchStorage("HellowThere");
            C = ctx.searchStorage("HelloThere");
            if(!A.equals("3") || B != null || !C.equals("2")) {
                fail("Failed overwrite test");
            }
            ctx.putKV("Hello", "");
            A = ctx.searchStorage("Hello");
            B = ctx.searchStorage("HellowThere");
            C = ctx.searchStorage("HelloThere");
            if(A != null || B != null || !C.equals("2")) {
                fail("Failed overwrite test");
            }
        } catch (Exception e) {
            logger.error("Failed Simple test");
            e.printStackTrace();
        }
    }
}
