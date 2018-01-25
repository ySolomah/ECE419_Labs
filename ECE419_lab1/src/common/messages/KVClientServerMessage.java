package common.messages;

import java.io.Serializable;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.io.PrintWriter;
import org.apache.log4j.Logger;

public class KVClientServerMessage implements KVMessage {
    private String key;
    private String value;
    private StatusType status;
    
    // do not serialize the logger
    private transient static Logger logger = Logger.getRootLogger();

    // constructor is basic and initializes each of these attributes
    public KVClientServerMessage(StatusType s, String k, String v) {
        status = s;
        key = k;
        value = v;
    }

    // constructs a GET request
    public KVClientServerMessage(String k) {
        status = KVMessage.StatusType.GET;
        key = k;
        value = ""; 
    }

    // constructs PUT request
    public KVClientServerMessage(String k, String v) {
        status = KVMessage.StatusType.PUT;
        key = k;
        value = v;
    }

    @Override
    public String getKey(){
        return key;
    }

    @Override
    public String getValue(){
        return value;
    }

    @Override
    public StatusType getStatus() {
        return status;
    }

    public static void sendMessage(KVMessage k, OutputStream _output) throws IOException {
        logger.info("Sending a message");
        BufferedOutputStream s = new BufferedOutputStream(_output);
        // write status, then 3 (EOT), key, 3 (EOT), value, and 4 (EOTransmission)
        byte [] b = k.getStatus().name().getBytes();
        byte [] key_bytes = k.getKey().getBytes();
        byte[] value_bytes = k.getValue().getBytes();
        for(int i = 0; i < b.length; ++i){
            s.write(b[i]); 
        }

        // put three
        s.write(0x3);
        
        // put key
        for(int i = 0; i < key_bytes.length; ++i) {
            s.write(key_bytes[i]);
        }
        // put three
        s.write(0x3);

        for(int i =0; i< value_bytes.length; ++i) {
            s.write(value_bytes[i]);
        }
        s.write(0x4);
        // show the bytes
        s.flush();
        logger.info("Sent successfully");
    }

    public static KVMessage receiveMessage(InputStream _input) throws IOException, ClassNotFoundException {
        logger.info("Receiving a message");
        ByteArrayOutputStream s = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[100];
        int i = 0;
        KVMessage.StatusType st;
        String key;
        String value;
        while(true) {
            nRead = _input.read();
            if(nRead == -1) continue;
            if(nRead != 0x3) {
                data[i] = (byte)nRead;
                i++;
            }
            else {
                // form the status
                byte[] tmp = new byte[i];
                for(int j = 0; j < i; j++) {
                    tmp[j] = data[j];
                }
                String status = new String(tmp);
                st = KVMessage.StatusType.valueOf(status);
                break;
            }
        }
        i = 0;
        while(true) {
            nRead = (byte)_input.read();
            if(nRead == -1) continue;
            if(nRead != 0x3) {
                data[i] = (byte)nRead;
                i++;
            }
            else {
                // form the status
                byte[] tmp = new byte[i];
                for(int j = 0; j < i; j++) {
                    tmp[j] = data[j];
                }
                key = new String(tmp);
                break;
            }
        }
        i = 0;
        while(true) {
            nRead = (byte)_input.read();
            if(nRead == -1) continue;
            if(nRead != 0x4) {
                data[i] = (byte)nRead;
                i++;
            }
            else {
                // form the status
                byte[] tmp = new byte[i];
                for(int j = 0; j < i; j++) {
                    tmp[j] = data[j];
                }
                value = new String(tmp);
                break;
            }
        }


        KVMessage k = (KVMessage) new KVClientServerMessage(st, key, value) ;
        logger.info("Successfully received message of type: " + k.getStatus().name());
        logger.info("Key: " + k.getKey());
        logger.info("Value: " + k.getValue());

        return k;  
    }

}
