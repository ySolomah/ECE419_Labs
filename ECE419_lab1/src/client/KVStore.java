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
    private static int BUFFER_SIZE = 1024;

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

    private byte[] move_buffers(byte[] msgBytes, byte[] bufferBytes, int length) {
        byte[] to_return = new byte[msgBytes.length + length];
        System.arraycopy(msgBytes, 0, to_return, 0, msgBytes.length);
        System.arraycopy(bufferBytes, 0, to_return, msgBytes.length, msgBytes.length + length);
        return to_return;
    }

    private KVMessage receiveMessage() throws IOException{
        int index = 0;
        byte[] msgBytes = null, tmp = null;
        byte[] bufferBytes = new byte[BUFFER_SIZE]; 

        // first 4 bytes will be the length of the message
        byte read = (byte) _input.read();
        int size = 0x0000;
        for(int i = 0; i < 4; ++i) {
            size += ((int) read) << (8 * (3 - i));
            read = (byte) _input.read();
        }

        // TODO Logger for size
        
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
        KVMessage k = KVClientServerMessage.deserialize(msgBytes);

        return k;  
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
        KVMessage k = receiveMessage();
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
