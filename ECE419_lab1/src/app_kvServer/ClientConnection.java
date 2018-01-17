package client_connection;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import common.messages.KVMessage;
import common.messages.KVClientServerMessage;
import app_kvServer.KVServer;

import org.apache.log4j.*;


/**
 * Represents a connection end point for a particular client that is 
 * connected to the server. This class is responsible for message reception 
 * and sending. 
 * The class also implements the echo functionality. Thus whenever a message 
 * is received it is going to be echoed back to the client.
 */
public class ClientConnection implements Runnable {

	private static Logger logger = Logger.getRootLogger();
	
	private boolean isOpen;
	
	private Socket clientSocket;
	private InputStream input;
	private OutputStream output;
    private KVServer server;
	
	/**
	 * Constructs a new CientConnection object for a given TCP socket.
	 * @param clientSocket the Socket object for the client connection.
	 */
	public ClientConnection(Socket clientSocket, KVServer server) {
		this.clientSocket = clientSocket;
		this.isOpen = true;
        this.server = server;
	}
	
	/**
	 * Initializes and starts the client connection. 
	 * Loops until the connection is closed or aborted by the client.
	 */
	public void run() {
		try {
			output = clientSocket.getOutputStream();
			input = clientSocket.getInputStream();
		
			while(isOpen) {
				try {
					KVMessage latestMsg = KVClientServerMessage.receiveMessage(input);

                    KVClientServerMessage reply = handleMessage(latestMsg);
                    if (reply == null) {
                        throw new IOException("Invalid state sent to server over client");
                    }

                    // send the reply
                    byte[] msg = reply.toBytes(); 
                    if (msg != null) {
                        output.write(msg);
                        output.flush();
                    } else {
                        throw new IOException("Could not serialize KVMessage");
                    }
					
				/* connection either terminated by the client or lost due to 
				 * network problems*/	
				} catch (IOException ioe) {
					logger.error("Error! Connection lost!");
					isOpen = false;
				}				
			}
			
		} catch (IOException ioe) {
			logger.error("Error! Connection could not be established!", ioe);
			
		} finally {
			
			try {
				if (clientSocket != null) {
					input.close();
					output.close();
					clientSocket.close();
				}
			} catch (IOException ioe) {
				logger.error("Error! Unable to tear down connection!", ioe);
			}
		}
	}

    private KVClientServerMessage handleMessage(KVMessage k) {
        KVMessage.StatusType failure_mode = KVMessage.StatusType.GET_ERROR;
        KVMessage.StatusType success_mode = KVMessage.StatusType.GET_SUCCESS;
        KVClientServerMessage reply = null;

        // determine failure status
        if (k.getStatus() == KVMessage.StatusType.GET) {
            failure_mode = KVMessage.StatusType.GET_ERROR;
            success_mode = KVMessage.StatusType.GET_SUCCESS; 
        } else if (k.getStatus() == KVMessage.StatusType.PUT) {
            // check if delete
            if (k.getValue() == null) {
               failure_mode = KVMessage.StatusType.DELETE_ERROR;
               success_mode = KVMessage.StatusType.DELETE_SUCCESS;
            } else {
                failure_mode = KVMessage.StatusType.PUT_ERROR;
                // assume put update - key already exists
                success_mode = KVMessage.StatusType.PUT_UPDATE;
                // see if key already exists
                try {
                    server.getKV(k.getKey());
                } catch (Exception e) {
                    // this will be an update
                    success_mode = KVMessage.StatusType.PUT_SUCCESS;
                }
            }
        } else {
            return null; // shit hit the fan
        }
        try {
            if (k.getStatus() == KVMessage.StatusType.GET){
                String result = server.getKV(k.getKey());
                reply = new KVClientServerMessage(success_mode, k.getKey(), result);
            }
            else if (k.getStatus() == KVMessage.StatusType.PUT) {
                // could be a delete, update, or put
                server.putKV(k.getKey(), k.getValue());
                reply = new KVClientServerMessage(success_mode, k.getKey(), k.getValue()); 
            }
        } catch (Exception e) {
            reply = new KVClientServerMessage(failure_mode, k.getKey(), k.getValue());
        }
        return reply;
    }
}
