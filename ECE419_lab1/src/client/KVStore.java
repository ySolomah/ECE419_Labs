package client;

import common.messages.KVMessage;
import common.messages.KVClientServerMessage;
import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.lang.IllegalArgumentException;

import org.apache.log4j.Logger;

public class KVStore implements KVCommInterface {
    /**
     * Initialize KVStore with address and port of KVServer
     * @param address the address of the KVServer
     * @param port the port of the KVServer
     */
    private Socket _clientSocket;
    private String _address;
    private int _port;
    private OutputStream _output;
    private InputStream _input;
    
    private static Logger logger = Logger.getRootLogger();

    public KVStore(String address, int port) {
        // TODO Auto-generated method stub
        _address = address;
        _port = port;
        _input = null;
        _output = null;
        _clientSocket = null;
    }

    @Override
    public void connect() throws Exception {
        // TODO Auto-generated method stub
        try {
            if(_clientSocket == null) {
                _clientSocket = new Socket(_address, _port);
                _clientSocket.setSoTimeout(5000);
                _output = _clientSocket.getOutputStream();
                _input = _clientSocket.getInputStream();                  
                logger.info("Created client socket to " + _address + ":" + String.valueOf(_port) + " successfully");
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Attempted to connect when socket was already open");
            disconnect();
            throw e;
        }
    }

    @Override
    public void disconnect() {
        // TODO Auto-generated method stub
        try {
            if (_clientSocket != null) {
                _clientSocket.close();
                _clientSocket = null;
            }
            if(_output != null) {
                _output.close();
                _output = null;
            }
            if(_input != null) {
                _input.close();
                _input = null;
            }
        } catch (IOException e) {
            logger.error("Failed to disconnect session: " + e.getCause().getMessage());
        } 
    }

   
    private KVMessage sendRequestAndReponse(KVClientServerMessage to_send) throws Exception {
        // send the request

        // check for illegal keys
        if(to_send.getKey().contains(" ") || to_send.getKey().length() == 0 || to_send.getKey().length() > 20) {
            logger.error("Key contains a space or empty - not legal");
            throw new IllegalArgumentException("Illegal key");
        }

        // check for illegal values
        if(to_send.getStatus() == KVMessage.StatusType.PUT && to_send.getValue() != null && to_send.getValue().length() > 120000) {
            logger.error("Value is over 120kB long - not legal");
            throw new IllegalArgumentException("Illegal value");
        }

        // check for open socket and message being present
        if (to_send != null && _output != null) {
            KVClientServerMessage.sendMessage(to_send, _output);
        } else {
            logger.error("Could not serialize KVMessage: " + to_send);
            if(to_send == null)
                throw new IOException("Could not serialize KVMessage - no message");
            else
                throw new IOException("Could not serialize KVMessage - no stream");
        }

        logger.info("Serialize and wrote KVMessage - waiting for response");
        KVMessage k; 
        // wait for response now
        if(_input != null) {
            k = KVClientServerMessage.receiveMessage(_input);
        } else {
            throw new IOException("Could not receive response: _input stream not open!");
        }
        logger.info("Received response: " + k.getStatus().name() + "<" + k.getKey() + "," + String.valueOf(k.getValue()) + ">");
        return k;
    }

    @Override
    public KVMessage put(String key, String value) throws Exception {
        // TODO Auto-generated method stub
        // generate the put request
        logger.debug("Sending PUT request");
        KVClientServerMessage to_send = new KVClientServerMessage(key, value);
        logger.debug("Received response");
        KVMessage k;
        try {
            k = sendRequestAndReponse(to_send);
            return k;
        } catch (IllegalArgumentException e) {
            if (value == null || value.equals("") || value.equals("null")) {
                k = new KVClientServerMessage(KVMessage.StatusType.DELETE_ERROR, key, value);
            } else {
                k = new KVClientServerMessage(KVMessage.StatusType.PUT_ERROR, key, value);
            }
            return k;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public KVMessage get(String key) throws Exception {
        // TODO Auto-generated method stub
        // generate the put request
        logger.debug("Sending PUT request");
        KVClientServerMessage to_send = new KVClientServerMessage(key);
        logger.debug("Receive response");
        KVMessage k;
        try {
            return sendRequestAndReponse(to_send);
        } catch (IllegalArgumentException e) {
            k = new KVClientServerMessage( KVMessage.StatusType.GET_ERROR, key, "");
            return k;
        } catch (Exception e) {
            throw e;
        }
    }
}
