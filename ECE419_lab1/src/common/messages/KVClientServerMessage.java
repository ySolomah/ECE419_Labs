package common.messages;

import java.io.Serializable;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

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

    private byte[] int_to_bytes(int j) {
        byte[] bytes =  ByteBuffer.allocate(4).putInt(j).array();
        logger.debug("Bytes are:");
        for(int i = 0; i < bytes.length; ++i) {
            logger.debug(String.format("%02X", bytes[i]));
        }
        return bytes;
    }

    private static int from_byte_array(byte[] bytes) {
        logger.debug("Bytes are:");
        for(int i = 0; i < bytes.length; ++i) {
            logger.debug(String.format("%02X", bytes[i]));
        }
        return ByteBuffer.wrap(bytes).getInt();
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
        System.arraycopy(bufferBytes, 0, to_return, msgBytes.length, length);
        return to_return;
    }

    public static void sendMessage(KVMessage k, OutputStream _output) throws IOException {
        logger.info("Sending a message");
        ObjectOutputStream s = new ObjectOutputStream(_output);
        s.writeObject(k);
        s.flush();
        logger.info("Sent successfully");
    }

    public static KVMessage receiveMessage(InputStream _input) throws IOException, ClassNotFoundException {
        logger.info("Receiving a message");
        ObjectInputStream s = new ObjectInputStream(_input);
        KVMessage k = (KVMessage) ((KVClientServerMessage) s.readObject());
        logger.info("Successfully received message of type: " + k.getStatus().name());

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
            logger.error("Failed to deserialize: ObjectInputStream threw IOException");
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
                logger.error("Failed to close input stream");
            }
        }
        return o;
    }
}
