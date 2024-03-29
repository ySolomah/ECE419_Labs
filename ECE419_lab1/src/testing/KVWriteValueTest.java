package testing;

import org.junit.Test;
import app_kvServer.KVServer;

import junit.framework.TestCase;
import logger.LogSetup;
import org.apache.log4j.Logger;

public class KVWriteValueTest extends TestCase {

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
    public void testSimpleReadWrite() {
        logger.info("SIMPLE READ WRITE TEST");
        clearCacheAndStorage();
        String A, B;
        try{
            ctx.putKV("Hello", "  1 hello 1 ");
            ctx.putKV("Jum", "3  bgwai    4   ");
            A = ctx.searchStorage("Hello");
            B = ctx.searchStorage("Jum");
            if(!A.equals("  1 hello 1 ") || !B.equals("3  bgwai    4   ")) {
                System.out.println("Given A: " + "'" + A + "'");
                System.out.println("Given B: " + "'" + B + "'");
                fail("Failed simple write test");
            }
        } catch (Exception e) {
            logger.error("Failed Simple test");
            e.printStackTrace();
        }
    }



}
