package client;

import common.messages.KVMessage;
import common.messages.KVClientServerMessage;
import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

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

	public KVStore(String address, int port) {
		// TODO Auto-generated method stub
        _address = address;
        _port = port;
	}

	@Override
	public void connect() throws Exception {
		// TODO Auto-generated method stub
        if (_clientSocket != null) {
            _clientSocket = new Socket(_address, _port);
            _output = _clientSocket.getOutputStream();
            _input = _clientSocket.getInputStream();                  
        }
	}

	@Override
	public void disconnect() {
		// TODO Auto-generated method stub
        try {
            if (_clientSocket != null) {
                _output.close();
                _input.close();
                _clientSocket.close();
                _clientSocket = null;
            }
        } catch (IOException e) {
        } 
	}

   
    private KVMessage sendRequestAndReponse(KVClientServerMessage to_send) throws Exception {
        // send the request
        byte[] msg = to_send.toBytes();
        if (to_send != null) {
            _output.write(msg);
            _output.flush();
        } else {
            throw new IOException("Could not serialize KVMessage");
        }

        // TODO logger
        
        // wait for response now
        KVMessage k = KVClientServerMessage.receiveMessage(_input);
        return k;
    }

	@Override
	public KVMessage put(String key, String value) throws Exception {
		// TODO Auto-generated method stub
        // generate the put request
        KVClientServerMessage to_send = new KVClientServerMessage(key, value);
		return sendRequestAndReponse(to_send);
	}

	@Override
	public KVMessage get(String key) throws Exception {
		// TODO Auto-generated method stub
        // generate the put request
        KVClientServerMessage to_send = new KVClientServerMessage(key);
		return sendRequestAndReponse(to_send);
	}
}
