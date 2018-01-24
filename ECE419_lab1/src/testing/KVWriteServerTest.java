package testing;

import org.junit.Test;
import app_kvServer.KVServer;

import junit.framework.TestCase;
import logger.LogSetup;
import org.apache.log4j.Logger;

public class KVWriteServerTest extends TestCase {

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
        try{
            ctx.putKV("Hello", "1");
            ctx.putKV("Jum", "3");
            ctx.inStorage("Hello");
            ctx.inCache("Hello");
        } catch (Exception e) {
            logger.error("Failed Simple test");
            e.printStackTrace();
        }
    }



}
