package common.messages;

import java.io.Serializable;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;

public class KVClientServerMessage implements KVMessage, Serializable {
    private static final long serialVersionUID = 3735928559L;

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
        value = null; 
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

    private byte[] int_to_bytes(int i) {
        byte[] b = new byte[4];
        for(int j = 0; j < 4; j++) {
            b[j] = (byte)((i >>> (3-j)*8) & (0x000000FF));
        }
        logger.debug("Convert " + String.valueOf(i) + " to " +
                    String.valueOf(b[3]) + ", " +
                    String.valueOf(b[2]) + ", " +
                    String.valueOf(b[1]) + ", " +
                    String.valueOf(b[0]));
                    
        return b;
    }

    public byte[] toBytes() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = null;
        byte[] to_return = null;
        logger.info("Serializing the message...");
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(this);
            out.flush();
            byte[] tmp = bos.toByteArray();
            to_return = new byte[tmp.length + 4];
            byte[] size = int_to_bytes(tmp.length);
            for(int i = 0; i < 4; i++) {
                to_return[i] = size[i];
            }
            for(int i = 4; i < tmp.length + 4; i++) {
                to_return[i] = tmp[i-4];
            }
            logger.info("Serialization succeeded");
        } catch(IOException e) {
            logger.error("Serialization failed - will return null");
        }
        finally {
            try {
                bos.close();
            } catch (IOException ex) {
                logger.error("Failed to close the byte array output stream");
            }
        }
        return to_return;
    }
    private static byte[] move_buffers(byte[] msgBytes, byte[] bufferBytes, int length) {
        byte[] to_return = new byte[msgBytes.length + length];
        System.arraycopy(msgBytes, 0, to_return, 0, msgBytes.length);
        System.arraycopy(bufferBytes, 0, to_return, msgBytes.length, msgBytes.length + length);
        return to_return;
    }

    public static KVMessage receiveMessage(InputStream _input) throws IOException{
        int BUFFER_SIZE = 1024;
        int index = 0;
        byte[] msgBytes = null, tmp = null;
        byte[] bufferBytes = new byte[BUFFER_SIZE]; 

        logger.info("Receiving a message - will read size first");
        // first 4 bytes will be the length of the message
        byte read = (byte) _input.read();
        int size = 0x0000;
        for(int i = 0; i < 4; ++i) {
            size += ((int) read) << (8 * (3 - i));
            read = (byte) _input.read();
        }

        // TODO Logger for size
        logger.info("Size is: " + String.valueOf(size));
        
        // get the bytes now
        for(int i = 0; i < size; ++i) {
            if(index == BUFFER_SIZE) {
                // expand and copy
                msgBytes = move_buffers(msgBytes, bufferBytes, BUFFER_SIZE);
                index = 0;
            }
            bufferBytes[index] = read;
            read = (byte) _input.read();
            index++;
        }

        // copy the rest to the message
        msgBytes = move_buffers(msgBytes, bufferBytes, index);
        KVMessage k = deserialize(msgBytes);
        logger.info("Successfully received message");

        return k;  
    }

    public static KVClientServerMessage deserialize(byte[] bytes_to_obj) {
        ByteArrayInputStream bos = new ByteArrayInputStream(bytes_to_obj);
        ObjectInputStream in = null;
        KVClientServerMessage o = null;
        try {
            in = new ObjectInputStream(bos);
            o = (KVClientServerMessage) in.readObject();
            logger.info("Deserialized message successfully");
        } catch(IOException e) {
            logger.error("Failed to deserialize: " + e.getCause().getMessage());
        }
        catch(ClassNotFoundException e) {
            logger.error("Failed to find the class 'KVClientServerMessage'");
        }
        finally {
            try {
                if(in != null) {
                    in.close();
                }
                logger.info("Successfully closed input stream");
            } catch (IOException ex) {
                logger.error("Failed to close input stream: " + ex.getCause().getMessage());
            }
        }
        return o;
    }
}
