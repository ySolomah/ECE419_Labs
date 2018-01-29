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
        boolean A, B, C;
        try {
            ctx.putKV("A", "1");
            ctx.CacheStatus();
            A = ctx.inCache("A");
            B = ctx.inCache("B");
            C = ctx.inCache("C");
            if(!A || B || C) {
                fail("Fail FIFO cache");
            }
            ctx.putKV("B", "1");
            ctx.CacheStatus();
            A = ctx.inCache("A");
            B = ctx.inCache("B");
            C = ctx.inCache("C");
            if(!A || !B || C) {
                fail("Fail FIFO cache");
            }
            ctx.putKV("C", "1");
            ctx.CacheStatus();
            A = ctx.inCache("A");
            B = ctx.inCache("B");
            C = ctx.inCache("C");
            if(A || !B || !C) {
                fail("Fail FIFO cache");
            }
            ctx.putKV("A", "2");
            ctx.CacheStatus();
            A = ctx.inCache("A");
            B = ctx.inCache("B");
            C = ctx.inCache("C");
            if(!A || B || !C) {
                fail("Fail FIFO cache");
            }
            ctx.putKV("A", "");
            ctx.CacheStatus();
            A = ctx.inCache("A");
            B = ctx.inCache("B");
            C = ctx.inCache("C");
            if(A || B || !C) {
                fail("Fail FIFO cache");
            }
            try {
                ctx.getKV("A");
                fail("Found val for key A");
            } catch (Exception e) {

            }
            ctx.putKV("C", "");
            ctx.CacheStatus();
            A = ctx.inCache("A");
            B = ctx.inCache("B");
            C = ctx.inCache("C");
            if(A || B || C) {
                fail("Fail FIFO cache");
            }
           try {
               ctx.getKV("C");
               fail("Found val for key C");
           }
           catch (Exception e) {

           }
        } catch (Exception e) {
            logger.error("fifo test fail");
            e.printStackTrace();
        }
    }


    

}
