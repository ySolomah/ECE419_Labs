package app_kvClient;

import client.KVCommInterface;
import client.KVStore;
import common.messages.KVMessage;
import common.messages.KVMessage.StatusType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;

//For parsing user input
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;

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
    public void newConnection(String hostname, int port) throws Exception {
        try{
            //Tear down old connection and 
            //make a new one
            if (kvStore != null) kvStore.disconnect();
            kvStore = null;
            running = false;
            status = SocketStatus.DISCONNECTED;
            serverAddress = hostname;
            serverPort = port;
            kvStore = getStore();
            System.out.print(PROMPT + "Connecting to "+hostname+"/"+port+"\n");
            kvStore.connect();
            running = true;
            status = SocketStatus.CONNECTED;
            System.out.print(PROMPT + "Connected\n" );
        } catch (Exception e) {
            printError("Connection Failed!");
            logger.error("Connection Failed!", e);
            throw e;
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

    private List<String> parseCmd(String cmdLine){
        //From: 
        //https://stackoverflow.com/questions/366202/regex-for-splitting- \
        //a-string-using-space-when-not-surrounded-by-single-or-double
        List<String> tokens = new ArrayList<String>();
        Matcher match = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'").matcher(cmdLine);
        while (match.find()) {
            if (match.group(1) != null) {
                // Add double-quoted string without the quotes
                tokens.add(match.group(1));
            } else if (match.group(2) != null) {
                // Add single-quoted string without the quotes
                tokens.add(match.group(2));
            } else {
                // Add unquoted word
                tokens.add(match.group());
            }
        }
        if (tokens.contains(null) || tokens.contains("")){
            tokens.set(2,"null");
        }
        return tokens;
    }
    private void handleCommand(String cmdLine) {
        List<String> listTokens = parseCmd(cmdLine);
        String[] tokens = new String[listTokens.size()]; 
        tokens = listTokens.toArray(tokens);
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
                        if (status == SocketStatus.CONNECTION_LOST){
                            System.out.println(PROMPT+"Attempting to reconnect...");
                        }
                        //Start a new connection to the server
                        newConnection(tokens[1], Integer.parseInt(tokens[2]));
                    } else{
                        printError("Client already connected!");
                        logger.info("Client already connected!");
                    }
                } catch(NumberFormatException nfe) {
                    printError("No valid address. Port must be a number!");
                    logger.error("Unable to parse argument <port>", nfe);
                    status = SocketStatus.DISCONNECTED;
                    running = false;
                } catch (Exception e){
                    printError("Failed to establish new connection.");
                    logger.error("Failed to establish new connection.",e);
                    status = SocketStatus.DISCONNECTED;
                    running = false;
                }
            } else {
                printError("Invalid number of parameters! 3 expected.");
                logger.error("Invalid number of parameters! 3 expected.");
            }
                        
        } else if (tokens[0].equals("get")){ 
            if(tokens.length == 2){
                if (tokens[1].contains(" ")){ 
                    printError("Keys cannot have spaces.");
                    logger.error("Keys cannot have spaces.");
                }
                if (tokens[1].isEmpty()){
                    printError("Key cannot be empty.");
                    logger.error("Key cannot be empty.");
                }
                if(running){
                    try{
                        KVMessage retMsg = kvStore.get(tokens[1]);
                        if (retMsg.getStatus() == StatusType.GET_ERROR){
                            printError("GET_ERROR received!");
                            logger.error("GET_ERROR received!");
                        } else{
                            System.out.println(PROMPT+"<"+retMsg.getKey()+
                                            "> <" + retMsg.getValue() + ">");
                        }
                    } catch (Exception e){
                        printError("get failed!");
                        logger.error("get failed!",e);
                    }
                }
                else{
                    if (status == SocketStatus.CONNECTED){
                        status = SocketStatus.CONNECTION_LOST;
                        printError("Connection lost!");
                        logger.error("Connection lost!");
                    } else {
                        status = SocketStatus.DISCONNECTED;
                        printError("Not connected!");
                        logger.error("Not connected!");
                    }
                    running = false;
                } 
            } else {
                printError("Invalid number of arguments! 2 expected.");
                logger.error("Invalid number of arguments! 2 expected.");
            }  
        } else  if (tokens[0].equals("put")) {
            if(tokens.length == 3) {
                if (tokens[1].contains(" ")){ 
                    printError("Keys cannot have spaces.");
                    logger.error("Keys cannot have spaces.");
                }
                if (tokens[1].isEmpty()){
                    printError("Key cannot be empty.");
                    logger.error("Key cannot be empty.");
                }
                if(running){
                    //running = true only if kvStore is
                    //initialized with a port and address.
                    try{
                        KVMessage retMsg = kvStore.put(tokens[1], tokens[2]);
                        //Check the return msg from the server
                        if (retMsg.getStatus() == StatusType.PUT_ERROR){
                            printError("PUT_ERROR received!");
                            logger.error("PUT_ERROR received!");
                        } else if (retMsg.getStatus() == StatusType.PUT_SUCCESS){
                            if (tokens[2].equals("null"))
                                logger.info("PUT_SUCCESS received. Value deleted.");
                            else
                                logger.info("PUT_SUCCESS received. Value inserted.");
                            System.out.println(PROMPT+"Value inserted.");
                        } else if (retMsg.getStatus() == StatusType.PUT_UPDATE){
                            System.out.println(PROMPT+"Value updated.");
                            logger.info("PUT_UPDATE received. Value updated.");
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
                        printError("Not connected!");
                        logger.error("Not connected!");
                    }
                    running = false;
                }
            } else {
                printError("Invalid number of arguments! 3 expected.");
                logger.error("Invalid number of arguments! 3 expected.");
            }
            
        } else if(tokens[0].equals("disconnect")) {
            if (running) {
                if (status == SocketStatus.CONNECTED ||
                    status == SocketStatus.CONNECTION_LOST){
                    kvStore.disconnect();
                    //Get rid of store connected on old port and addr
                    kvStore = null;
                    serverAddress = "";
                    serverPort = -1;
                    System.out.println(PROMPT+"Client disconnected.");
                }
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
                printError("Invalid number of arguments! 2 expected.");
                logger.error("Invalid number of arguments! 2 expected.");
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
        sb.append("\t\t establishes a connection to a server\n");
        sb.append(PROMPT).append("put <key> <value>");
        sb.append("\t\t Inserts, updates or deletes entry. To delete use 'null' for <value>.\n");
        sb.append(PROMPT).append("get <key>");
        sb.append("\t\t\t get the value for the key specified from the server storage if it exists or return error if not.\n");
        sb.append(PROMPT).append("disconnect");
        sb.append("\t\t\t disconnects from the server \n");
        sb.append(PROMPT).append("logLevel");
        sb.append("\t\t\t changes the logLevel: ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF \n");
        sb.append(PROMPT).append("quit ");
        sb.append("\t\t\t\t exits the program");
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
            return LogSetup.UNKNOWN_LEVEL.toString();
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
