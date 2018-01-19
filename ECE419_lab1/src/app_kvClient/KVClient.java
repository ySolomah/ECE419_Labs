package app_kvClient;

import client.KVCommInterface;
import client.KVStore;
import common.messages.KVMessage;
import common.messages.KVMessage.StatusType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;

import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class KVClient implements IKVClient {
	private boolean running;
	private static Logger logger = Logger.getRootLogger();
	private static final String PROMPT = "KVClient> ";
	private BufferedReader stdin;
	private boolean stop = false;
	private KVCommInterface kvStore = null;
	private String serverAddress;
	private int serverPort;
    //Initialize to disconnected just to be extra safe.
    private SocketStatus status = SocketStatus.DISCONNECTED;
    
    public enum SocketStatus{
        CONNECTED,
        DISCONNECTED,
        CONNECTION_LOST
    }

    @Override
    public KVCommInterface getStore(){
        //Prof said only one client needs to run in the shell.
        //We'll return the instance we have or make a new one
        //if this is the first time this method gets invoked.
        if (kvStore == null){
            KVCommInterface newStore = new KVStore(serverAddress,serverPort);
            return newStore;
        }
        else return kvStore;
    }

    @Override
    public void newConnection(String hostname, int port) throws Exception   {
		try{
            kvStore = getStore();
            kvStore.connect();
		} catch (Exception e) {
			printError("Connection Failed!");
			logger.info("Connection Failed!", e);
		}
    }
    
    public void run(){
    	while(!stop) {
			stdin = new BufferedReader(new InputStreamReader(System.in));
			System.out.print(PROMPT);
			try {
				String cmdLine = stdin.readLine();
				this.handleCommand(cmdLine);
			} catch (IOException e) {
				stop = true;
				printError("CLI does not respond - Application terminated ");
                logger.error("CLI does not respond - Application terminated ");
			}
		}
	}

	private void handleCommand(String cmdLine) {
		String[] tokens = cmdLine.split("\\s+");

		if(tokens[0].equals("quit")) {	
			stop = true;
            //Disconnect the client if it was connected to server
			if (running) kvStore.disconnect(); 
            status = SocketStatus.DISCONNECTED;
            running = false;
			System.out.println(PROMPT + "Application exit!");
		
		} else if (tokens[0].equals("connect")){
			if(tokens.length == 3) {
				try{
                   if (!running && status != SocketStatus.CONNECTED){
					    serverAddress = tokens[1];
					    serverPort = Integer.parseInt(tokens[2]);
                        //Start a new connection to the server
					    newConnection(serverAddress, serverPort);
				    } else{
                        printError("Client already connected!");
                        logger.error("Client already connected!");
                    }
                } catch(NumberFormatException nfe) {
					printError("No valid address. Port must be a number!");
					logger.error("Unable to parse argument <port>", nfe);
				} catch (Exception e){
					printError("Failed to establish new connection.");
					logger.error("Failed to establish new connection.",e);
				}
                running = true;
                status = SocketStatus.CONNECTED;
			} else {
				printError("Invalid number of parameters! Exactly 3 needed.");
                logger.error("Invalid number of parameters! Exactly 3 needed.");
			}
            			
		} else if (tokens[0].equals("get")){ 
            if(tokens.length == 2){
                if(running){
                    try{
                        KVMessage retMsg = kvStore.get(tokens[1]);
                        if (retMsg.getStatus() == StatusType.GET_ERROR){
                            printError("get failed!");
                            logger.error("get failed!");
                        }
                    } catch (Exception e){
                        printError("get failed!");
                        logger.error("get failed!",e);
                    }
                }
                else{
                    printError("Not connected!");
                    logger.error("Not connected!");
                } 
            } else {
                printError("Invalid number of arguments! Only 2 allowed.");
                logger.error("Invalid number of arguments! Only 2 allowed.");
            }  
        } else  if (tokens[0].equals("put")) {
			if(tokens.length == 3) {
				if(running){
                    //running = true only if kvStore is
                    //initialized with a port and address.
                    try{
                        KVMessage retMsg = kvStore.put(tokens[1], tokens[2]);
                        //Check the return msg from the server
                        if (retMsg.getStatus() == StatusType.PUT_ERROR){
                            printError("put failed!");
                            logger.error("put failed!");
                        } else if (retMsg.getStatus() == StatusType.PUT_SUCCESS){
                            logger.info("put successful! Value inserted.");
                        } else if (retMsg.getStatus() == StatusType.PUT_UPDATE){
                            logger.info("put successful! Value updated.");
                        }
                    } catch (Exception e){
                        printError("put failed!");
                        logger.error("put failed!",e);
                    }
				} else {
                    if (status == SocketStatus.CONNECTED){
                        status = SocketStatus.CONNECTION_LOST;
                        printError("Connection lost!");
                        logger.error("Connection lost!");
                    } else {
                        status = SocketStatus.DISCONNECTED;
                        logger.error("Not connected!");
                    }
					printError("Not connected!");
				}
			} else {
				printError("No message passed!");
			}
			
		} else if(tokens[0].equals("disconnect")) {
			if (running) {
                if (status == SocketStatus.CONNECTED)
                    kvStore.disconnect();
                status = SocketStatus.DISCONNECTED;
                running = false;
            }
            else printError("Need to connect first!");
			
		} else if(tokens[0].equals("logLevel")) {
			if(tokens.length == 2) {
				String level = setLevel(tokens[1]);
				if(level.equals(LogSetup.UNKNOWN_LEVEL)) {
					printError("No valid log level!");
					printPossibleLogLevels();
				} else {
					System.out.println(PROMPT + 
							"Log level changed to level " + level);
				}
			} else {
				printError("Invalid number of parameters!");
			}
			
		} else if(tokens[0].equals("help")) {
			printHelp();
		} else {
			printError("Unknown command");
			printHelp();
		}
	}

	private void printHelp() {
		StringBuilder sb = new StringBuilder();
		sb.append(PROMPT).append("KVClient HELP (Usage):\n");
		sb.append(PROMPT);
		sb.append("::::::::::::::::::::::::::::::::");
		sb.append("::::::::::::::::::::::::::::::::\n");
		sb.append(PROMPT).append("connect <host> <port>");
		sb.append("\t establishes a connection to a server\n");
		sb.append(PROMPT).append("put <key> <value>");
		sb.append("\n\t Inserts a key-value pair into the storage server data structures. \n\tUpdates (overwrites) the current value with the given value if the server already contains the specified key. \n\tDeletes the entry for the given key if <value> equals null.\n");
		sb.append(PROMPT).append("get <key>");
		sb.append("\t\t\t get the value for the key specified from the server storage if it exists or return error if not.\n");
		sb.append(PROMPT).append("disconnect");
		sb.append("\t\t\t disconnects from the server \n");
		
		sb.append(PROMPT).append("logLevel");
		sb.append("\t\t\t changes the logLevel \n");
		sb.append(PROMPT).append("\t\t\t\t ");
		sb.append("ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF \n");
		
		sb.append(PROMPT).append("quit ");
		sb.append("\t\t\t exits the program");
		System.out.println(sb.toString());
	}
	
	private void printPossibleLogLevels() {
		System.out.println(PROMPT 
				+ "Possible log levels are:");
		System.out.println(PROMPT 
				+ "ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF");
	}

	private String setLevel(String levelString) {
		
		if(levelString.equals(Level.ALL.toString())) {
			logger.setLevel(Level.ALL);
			return Level.ALL.toString();
		} else if(levelString.equals(Level.DEBUG.toString())) {
			logger.setLevel(Level.DEBUG);
			return Level.DEBUG.toString();
		} else if(levelString.equals(Level.INFO.toString())) {
			logger.setLevel(Level.INFO);
			return Level.INFO.toString();
		} else if(levelString.equals(Level.WARN.toString())) {
			logger.setLevel(Level.WARN);
			return Level.WARN.toString();
		} else if(levelString.equals(Level.ERROR.toString())) {
			logger.setLevel(Level.ERROR);
			return Level.ERROR.toString();
		} else if(levelString.equals(Level.FATAL.toString())) {
			logger.setLevel(Level.FATAL);
			return Level.FATAL.toString();
		} else if(levelString.equals(Level.OFF.toString())) {
			logger.setLevel(Level.OFF);
			return Level.OFF.toString();
		} else {
			return LogSetup.UNKNOWN_LEVEL;
		}
	}
	
	public void handleStatus(SocketStatus status) {
		if(status == SocketStatus.CONNECTED) {
		} else if (status == SocketStatus.DISCONNECTED) {
			System.out.print(PROMPT);
			System.out.println("Connection terminated: " 
					+ serverAddress + " / " + serverPort);
			
		} else if (status == SocketStatus.CONNECTION_LOST) {
			System.out.println("Connection lost: " 
					+ serverAddress + " / " + serverPort);
			System.out.print(PROMPT);
		}
		
	}

	private void printError(String error){
		System.out.println(PROMPT + "Error! " +  error);
	}
	
    /**
     * Main entry point for the echo server application. 
     * @param args contains the port number at args[0].
     */
    public static void main(String[] args) {
    	try {
			new LogSetup("logs/client.log", Level.OFF);
			KVClient app = new KVClient();
			app.run();
		} catch (IOException e) {
			System.out.println("Error! Unable to initialize logger!");
			e.printStackTrace();
			System.exit(1);
		}
    }

}
