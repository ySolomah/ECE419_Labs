package app_kvClient;

import client.KVCommInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;

import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class KVClient implements IKVClient {

    @Override
    public void newConnection(String hostname, int port) throws Exception{
		try{
			//Connect using KVCommInterface API
			connect(hostname, port);

		} catch (UnknownHostException e) {
			printError("Unknown Host!");
			logger.info("Unknown Host!", e);
		} catch (IOException e) {
			printError("Could not establish connection!");
			logger.warn("Could not establish connection!", e);
		}
    }

    @Override
    public KVCommInterface getStore(){
        // TODO Haven't figured out what this wants yet.
        return null;
    }
	private static Logger logger = Logger.getRootLogger();
	private static final String PROMPT = "KVClient> ";
	private BufferedReader stdin;
	private Client client = null;
	private boolean stop = false;
	
	private String serverAddress;
	private int serverPort;
	
	public void run() {
		while(!stop) {
			stdin = new BufferedReader(new InputStreamReader(System.in));
			System.out.print(PROMPT);
			
			try {
				String cmdLine = stdin.readLine();
				this.handleCommand(cmdLine);
			} catch (IOException e) {
				stop = true;
				printError("CLI does not respond - KVClient terminated ");
			}
		}
	}
	
	private void handleCommand(String cmdLine) {
		String[] tokens = cmdLine.split("\\s+");

		if(tokens[0].equals("quit")) {	
			stop = true;
			disconnect();
			System.out.println(PROMPT + "KVClient exit!");
		
		} else if (tokens[0].equals("connect")){
			if(tokens.length == 3) {
				try{
					serverAddress = tokens[1];
					serverPort = Integer.parseInt(tokens[2]);
					newConnection(serverAddress, serverPort);
				} catch(NumberFormatException nfe) {
					printError("No valid address. Port must be a number!");
					logger.info("Unable to parse argument <port>", nfe);
				//} catch (UnknownHostException e) {
				//	printError("Unknown Host!");
				//	logger.info("Unknown Host!", e);
				//} catch (IOException e) {
				//	printError("Could not establish connection!");
				//	logger.warn("Could not establish connection!", e);
				} catch (Exception e){
					printError("Failed to establish new connection.");
					logger.error("Failed to establish new connection.",e);
				}
			} else {
				printError("Invalid number of parameters!");
			}
			
		} else  if (tokens[0].equals("put")) {
			if(tokens.length == 3) {
				if(client != null && client.isRunning()){
					//StringBuilder msg = new StringBuilder();
					//for(int i = 1; i < tokens.length; i++) {
					//	msg.append(tokens[i]);
					//	if (i != tokens.length -1 ) {
					//		msg.append(" ");
					//	}
					//}	
					//sendMessage(msg.toString());
					logger.info("Placeholder: putMsg() to be called here to handle the <key> <value> pair.");
					putMsg(tokens[1], tokens[2]);
				} else {
					printError("Not connected!");
				}
			} else {
				printError("No message passed!");
			}
			
		} else if(tokens[0].equals("disconnect")) {
			disconnect();
			
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
	
	private void putMsg(String key, String value){
		try {
			//client.sendMessage(new TextMessage(msg));
			logger.info("Placeholder: Client needs to implement putMsg().\n");	
		} catch (Exception e) {
			printError("Unable to send message!");
			logger.error("putMsg() failed.",e);
			disconnect();
		}
	}

	private String getMsg(String key){
		String placeholder = "";
		try{
			//TODO: Implement getMsg() to get a real value from Server or error out.
			logger.info("Placeholder: Client needs to implement getMsg().\n");
			placeholder = "Placeholder";
		} catch (Exception e){
			printError("Unable to retrieve value for key " + key);
			disconnect();
		}
		return placeholder;
	}

	private void connect(String address, int port) 
			throws UnknownHostException, IOException {
		client = new Client(address, port);
		client.addListener(this);
		client.start();
	}
	
	private void disconnect() {
		if(client != null) {
			client.closeConnection();
			client = null;
		}
	}
	
	private void printHelp() {
		StringBuilder sb = new StringBuilder();
		sb.append(PROMPT).append("KV_CLIENT HELP (Usage):\n");
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
	
	//@Override
	//public void handleNewMessage(TextMessage msg) {
	//	if(!stop) {
	//		System.out.println(msg.getMsg());
	//		System.out.print(PROMPT);
	//	}
	//}
	
	@Override
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
