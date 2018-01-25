package client;

import common.messages.KVMessage;
import common.messages.KVClientServerMessage;
import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

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
                _output = _clientSocket.getOutputStream();
                _input = _clientSocket.getInputStream();                  
                logger.info("Created client socket to " + _address + ":" + String.valueOf(_port) + " successfully");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("There was an error connecting to a new port");
            logger.error("Attempted to connect when socket was already open");
            disconnect();
            throw new Exception("Tried connecting with client socket!");
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
        if (to_send != null && _output != null) {
            KVClientServerMessage.sendMessage(to_send, _output);
        } else {
            logger.error("Could not serialize KVMessage: " + to_send);
            throw new IOException("Could not serialize KVMessage");
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
        return sendRequestAndReponse(to_send);
    }

    @Override
    public KVMessage get(String key) throws Exception {
        // TODO Auto-generated method stub
        // generate the put request
        logger.debug("Sending PUT request");
        KVClientServerMessage to_send = new KVClientServerMessage(key);
        logger.debug("Receive response");
        return sendRequestAndReponse(to_send);
    }
}
