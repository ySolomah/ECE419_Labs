package app_kvServer;

import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

import logging.LogSetup;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.*;
import java.io.*;

public class KVServer implements IKVServer extends Thread {

    class kvContainer {
        String key;
        String value;
        kvContainer(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    protected ArrayList<Thread> clientThreads;
    protected KV_Server_Cache kvCache;
    protected port;
    protected int cacheSize;
    protected String Strategy;
    protected CacheStrategy cacheStrategy;
    protected KVServer myServer;
    protected boolean running;
    protected ServerSocket serverSocket;

    private static Logger logger = Logger.getRootLogger();
    private static String fileName = "kv.txt";

	/**
	 * Start KV Server at given port
	 * @param port given port for storage server to operate
	 * @param cacheSize specifies how many key-value pairs the server is allowed
	 *           to keep in-memory
	 * @param strategy specifies the cache replacement strategy in case the cache
	 *           is full and there is a GET- or PUT-request on a key that is
	 *           currently not contained in the cache. Options are "FIFO", "LRU",
	 *           and "LFU".
	 */
	public KVServer(int port, int cacheSize, String strategy) {
		// TODO Auto-generated method stub
	}

	@Override
	public int getPort(){
		return this.port;
	}

	@Override
    public String getHostname(){
		return serverSocket.getLocalHost().getHostName();
	}

	@Override
    public CacheStrategy getCacheStrategy(){
		return this.cacheStrategy;
	}

	@Override
    public int getCacheSize(){
		return this.cacheSize;
	}

    private boolean isRunning() {
        return this.running;
    }

	@Override
    public boolean inStorage(String key){
		String inStoreString = searchStorage(key);
        if(inStoreString != null) {
            return (true);
        } else {
		    return (false);
        }
	}

	@Override
    public boolean inCache(String key){
        String ret = null;
        ret = getCache(key);
		if(ret != null && !ret.isEmpty()) {
            return(true);
        }
        return(false);
	}

	@Override
    public String getKV(String key) throws Exception{
        String myString = null;
        myString = kvCache.get(key);
        if(myString != null && !myString.isEmpty()) {
            return(myString);
        } else {
            myString = searchStorage(key);
            if(myString != null && !myString.isEmpty()) {
                return(myString);
            }
        }
		throw Exception("Failed to find key: " + key);
        return ("");
	}

    public String searchStorage(String key) {
        try {
                BufferedReader br = new BufferedReader(new FileReader(fileName));
                String line = "";
                while((line = br.readLine()) != null) {
                    String[] lineSplit = 
                        line.split(" ");
                    if(lineSplit[0].equals(key)) {
                        br.close();
                        return(lineSplit[1]);
                    }
                }
            }
        return(null);
    }

	@Override
    public void putKV(String key, String value) throws Exception {
        Thread cacheThread = new Thread() {
            public void run() {
                putCache(key, value);
            }
        }
        cacheThread.run();
        putKVSyn(key, value);
        cacheThread.join();
        return;
	}

    private synchronized void putCache(
            String key,
            String value
            ) {
       // TODO 
    }

    private void getCache(
            String key
            ) {
        // TODO
    }

    private synchronized void putKVSyn throws Exception(
            String key,
            String value
            ) {
        boolean completedRead = false;
        ArrayList<kvContainer> keyValues = new ArrayList<String>();

        try{
            RandomAccessFile file  = new RandomAccessFile(
                fileName,
                "rw"
                );
            boolean foundKey = false;
            String line;
            while( (line = file.readLine()) != null) {
                String[] keyValue = line.split(" ");
                if(keyValue.length >= 2) {
                    if(!foundKey && keyValue[0].equals(key)) {
                        if(value == null) {
                            continue;
                            foundKey = true;
                        } else {
                            keyValue[1] = value;
                            foundKey = true;
                        {
                    }
                    keyValues.add(new kvContainer(
                                keyValue[0],
                                keyValue[1]
                                )
                            );
                    
                }
                if(!foundKey) {
                    if(value == null) {
                        throw Exception("Cannot find and delete KV with key: " + key);
                    }
                    keyValues.add(new kvContainer(
                                key,
                                value
                                )
                            );
                    foundKey = true;
                }
                file.close()
                completedRead = true;
            }
        } catch (FileNotFoundException fnfe) {
        } catch (IOException ioe) {
            System.err.println(ioe);
        }
        
        if(completedRead) {
            FileWriter fstream = new FileWriter(
                    "temp" + fileName,
                    true
                    );
            BufferedWriter out = new BufferedWriter(fstream);
            for(kvContainer kvPair : keyValues) {
                out.write(kvPair.key 
                        + " " 
                        + kvPair.value
                        + " \n"
                        );
            }
            out.close();
            fstream.renameTo(fileName);
        }
        return;
    }


	@Override
    public void clearCache(){
        kvCache.clear();
        return;
	}

	@Override
    public void clearStorage(){
        FileWriter fstream = new FilterWriter(
                "temp" + fileName,
                true
                );
        BufferedWriter out = new BufferedWriter(fstream);
        out.write("");
        out.close();
        fstream.renameTo(fileName);
        return;
	}

	@Override
    public void kill() {
        logger.error("Killing Server");
        System.exit(2);
	}

	@Override
    public void close(){
        running = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            logger.error("Error! Unable to close socket on port: " + port, e);
        }
        for (Thread connThread : clientThreads) {
            try {
                connThread.stop();
            } catch (Exception e) {
                continue;
            }
        }
        return;
	}

    public void run() {
        running = initializeServer();

        if(serverSockey != null) {
            while(isRunning()) {
                try {
                    Socket client = serverSocket.accept();
                    KVClientConnection conn =
                        new ClientConnection(client, myServer);
                    Thread connThread = new Thread(conn);
                    clientThreads.add(connThread);
                    connThread.start();
                    logger.info("Connected to "
                            + client.getInetAddress().getHostName()
                            + " on port " + client.getPort());
                } catch (IOException e) {
                    logger.error("Error! " +
                            "Unable to establish connection. \n", e);
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            new LogSetup("logs/server.log", Level.ALL);
            if(args.length < 3) {
                System.out.println("Error! Invalid number of arguments!");
                System.out.println("Usage: KVServer port cacheSize cacheStrat");
            } else {
                int port = Integer.parseInt(args[0]);
                int cacheSize = Integer.parseInt(args[1]);
                String strategy = args[2];
                KVServer ourServer = KVServer(port, cacheSize, strategy);
                this.myServer = ourServer;
                this.myServer.start();
            }
        } catch (IOException e) {
            System.out.println("Error! Unable to initialize logger!");
            e.printStackTrace();
            System.exit(1);
        } catch (NumberFormatException nfe) {
            System.out.println("Error! Invalid argument <port> or <cacheSize>! Not a number!");
            nfe.printStackTrace();
            System.exit(1);
        }
    }
}












