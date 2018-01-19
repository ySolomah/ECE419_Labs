package testing;

import org.junit.Test;

import junit.framework.TestCase;
import java.nio.ByteBuffer;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import common.messages.KVClientServerMessage;
import common.messages.KVMessage;

public class MessageTest extends TestCase {
	
	// TODO add your test cases, at least 3
	
	@Test
	public void testConstructors() {
        KVClientServerMessage kv = new KVClientServerMessage(KVMessage.StatusType.PUT_SUCCESS, "key", "value");
		assertTrue(kv.getKey().equals("key"));
        assertTrue(kv.getValue().equals("value"));
        assertTrue(kv.getStatus() == KVMessage.StatusType.PUT_SUCCESS);

        kv = new KVClientServerMessage("key1");
		assertTrue(kv.getKey().equals("key1"));
        assertTrue(kv.getValue() == null);
        assertTrue(kv.getStatus() == KVMessage.StatusType.GET);
        
        kv = new KVClientServerMessage("key2", "another_value");
		assertTrue(kv.getKey().equals("key2"));
        assertTrue(kv.getValue().equals("another_value"));
        assertTrue(kv.getStatus() == KVMessage.StatusType.PUT);
	}

    @Test
    public void testToBytes() {
        KVClientServerMessage kv = new KVClientServerMessage("key", "value");
        byte[] b = kv.toBytes();

        // assert size is correct
        int size = 0x00000000;
        byte[] buf = new byte[4];
        for(int i = 0; i < 4; i++) {
            buf[i] = b[i];
        }
        size = ByteBuffer.wrap(buf).getInt();
        System.out.println("Size is: " + String.valueOf(size));
        System.out.println("Size of packet is: " + String.valueOf(b.length - 4));
        assertTrue(size == (b.length - 4));

        // convert back to message and confirm it
        byte[] to_convert = new byte[b.length - 4];
        System.arraycopy(b, 4, to_convert, 0, b.length - 4);
        KVClientServerMessage kv_new = KVClientServerMessage.deserialize(to_convert);
        
        assertTrue(kv_new.getStatus() == kv.getStatus());
        assertTrue(kv_new.getKey().equals(kv.getKey()));
        assertTrue(kv_new.getValue().equals(kv.getValue()));
    }

    @Test
    public void testReceiveMessage() {
        KVClientServerMessage kv = new KVClientServerMessage(KVMessage.StatusType.PUT_UPDATE, "Key", "VALUE");
        
        // serialize first
        byte[] b = kv.toBytes();

        // convert to InputStream
        InputStream is = new ByteArrayInputStream(b);
        try {
            KVMessage kv_new = KVClientServerMessage.receiveMessage(is);  
            assertTrue(kv_new.getStatus() == kv.getStatus());
            assertTrue(kv_new.getKey().equals(kv.getKey()));
            assertTrue(kv_new.getValue().equals(kv.getValue()));
        } catch(IOException e) {
            System.out.println("Error while trying to receiveMessage");
            assertTrue(false);
        }

        // make a large message (larger than 1024 bytes)
        kv = new KVClientServerMessage(KVMessage.StatusType.DELETE_ERROR, "qwert" 
       +"opasdfghjklzxcvbnm,.qwertyuiopasdfghjklzxcvbnm,qwertyuiopasdfghjklzxcvbnm,."
       +"opasdfghjklzxcvbnm,.qwertyuiopasdfghjklzxcvbnm,qwertyuiopasdfghjklzxcvbnm,."
       +"opasdfghjklzxcvbnm,.qwertyuiopasdfghjklzxcvbnm,qwertyuiopasdfghjklzxcvbnm,."
       +"opasdfghjklzxcvbnm,.qwertyuiopasdfghjklzxcvbnm,qwertyuiopasdfghjklzxcvbnm,."
       +"opasdfghjklzxcvbnm,.qwertyuiopasdfghjklzxcvbnm,qwertyuiopasdfghjklzxcvbnm,."
       +"opasdfghjklzxcvbnm,.qwertyuiopasdfghjklzxcvbnm,qwertyuiopasdfghjklzxcvbnm,."
       +"opasdfghjklzxcvbnm,.qwertyuiopasdfghjklzxcvbnm,qwertyuiopasdfghjklzxcvbnm,."
       +"opasdfghjklzxcvbnm,.qwertyuiopasdfghjklzxcvbnm,qwertyuiopasdfghjklzxcvbnm,."
       +"opasdfghjklzxcvbnm,.qwertyuiopasdfghjklzxcvbnm,qwertyuiopasdfghjklzxcvbnm,."
       +"opasdfghjklzxcvbnm,.qwertyuiopasdfghjklzxcvbnm,qwertyuiopasdfghjklzxcvbnm,."
       +"opasdfghjklzxcvbnm,.qwertyuiopasdfghjklzxcvbnm,qwertyuiopasdfghjklzxcvbnm,."
       +"opasdfghjklzxcvbnm,.qwertyuiopasdfghjklzxcvbnm,qwertyuiopasdfghjklzxcvbnm,."
       +"opasdfghjklzxcvbnm,.qwertyuiopasdfghjklzxcvbnm,qwertyuiopasdfghjklzxcvbnm,."
       +"opasdfghjklzxcvbnm,.qwertyuiopasdfghjklzxcvbnm,qwertyuiopasdfghjklzxcvbnm,."
       +"opasdfghjklzxcvbnm,.qwertyuiopasdfghjklzxcvbnm,qwertyuiopasdfghjklzxcvbnm,.",
        "value");
        
        // serialize first
        b = kv.toBytes();

        // convert to InputStream
        is = new ByteArrayInputStream(b);
        try {
            KVMessage kv_new = KVClientServerMessage.receiveMessage(is);  
            assertTrue(kv_new.getStatus() == kv.getStatus());
            assertTrue(kv_new.getKey().equals(kv.getKey()));
            assertTrue(kv_new.getValue().equals(kv.getValue()));
        } catch(IOException e) {
            System.out.println("Error while trying to receiveMessage");
            assertTrue(false);
        }
   }
}
