package app_kvServer;

import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

import logger.LogSetup;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.*;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import KVCache.*;

import client_connection.ClientConnection; 

public class KVServer extends Thread implements IKVServer {

    class kvContainer {
        String key;
        String value;
        kvContainer(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    protected ArrayList<Thread> clientThreads;
    protected IKVCache kvCache;
    protected int port;
    protected int cacheSize;
    protected String strategy;
    protected CacheStrategy cacheStrategy;
    protected boolean running;
    protected ServerSocket serverSocket;


    private final ReentrantReadWriteLock readWriteLock =
        new ReentrantReadWriteLock();
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
        this.clientThreads = new ArrayList<Thread>();
        this.port = port;
        this.cacheSize = cacheSize;
        this.strategy = strategy;
        if(strategy.equals("LRU")) {
            System.out.println("LRU CACHE");
            cacheStrategy = CacheStrategy.LRU;
            kvCache = new KVLRUCache(cacheSize);
        } else if (strategy.equals("LFU")) {
            System.out.println("LFU CACHE");
            cacheStrategy = CacheStrategy.LFU;
            kvCache = new KVLFUCache(cacheSize);
        } else {
            System.out.println("FIFO CACHE");
            cacheStrategy = CacheStrategy.FIFO;
            kvCache = new KVFIFOCache(cacheSize);
        }
    }

    @Override
    public int getPort(){
        return serverSocket.getLocalPort();
    }

    @Override
    public String getHostname(){
        return serverSocket.getInetAddress().getCanonicalHostName();
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
            logger.info("Key in storage: " + key + " with value: " + inStoreString);
            return (true);
        } else {
            return (false);
        }
    }

    @Override
    public boolean inCache(String key){
        String ret = kvCache.Get(key);
        if(ret != null && !ret.isEmpty()) {
            logger.info("Key in cache: " + key + " with value: " + ret);
            return(true);
        }
        return(false);
    }

    public boolean inCacheNon(String key){
        String ret = kvCache.GetNonUpdate(key);
        if(ret != null && !ret.isEmpty()) {
            return(true);
        }
        return(false);
    }

    private String[] splitByUniqueByte(byte delim, String in_str) {
        StringBuilder keySb = new StringBuilder();
        StringBuilder valueSb = new StringBuilder();
        char[] in = in_str.toCharArray();
        String[] out = new String[2];
        boolean inKey = true;
        System.out.println("Line input: " + in_str);
        int last_limiter = in_str.lastIndexOf('"');
        for (int i = 0; i < last_limiter; i++) {
            if((byte) in[i] == 0x1e) {
                inKey = false;
                System.out.println("Found byte");
            } else {
                if(inKey) {
                    keySb.append(in[i]);                    
                } else {
                    valueSb.append(in[i]);
                }
            }
        }
        out[0] = keySb.toString();
        out[1] = valueSb.toString();
        return(out);
    }

    @Override
    public String getKV(String key) throws Exception{
        String myString = null;
        myString = kvCache.Get(key);
        if(myString != null && !myString.isEmpty()) {
            return(myString);
        } else {
            myString = searchStorage(key);
            if(myString != null && !myString.isEmpty()) {
                return(myString);
            }
        }
        throw new Exception("Failed to find key: " + key);
    }

    public String searchStorage(String key) {
        readWriteLock.readLock().lock();
        try {
            try {
                BufferedReader br = new BufferedReader(new FileReader(fileName));
                String line = "";
                while((line = br.readLine()) != null) {
                    String[] lineSplit = 
                        splitByUniqueByte((byte) 0x1e, line);
                        //line.split(new String(new byte[] {0x1e}));
                    if(lineSplit[0].equals(key)) {
                        br.close();
                        return(lineSplit[1]);
                    }
                }
            } catch (Exception e) {
                
            }
        } finally { readWriteLock.readLock().unlock(); }
        return(null);
    }

    @Override
    public synchronized void putKV(final String key,final String value) throws Exception {
        logger.info("Put KV: " + key + " with value: " + value);
        Thread cacheThread = new Thread() {
            public void run() {
                putCache(key, value);
            }
        };
        cacheThread.run();
        putKVSyn(key, value);
        cacheThread.join();
        return;
    }

    private void putCache(
            String key,
            String value
            ) {
        if(value.equals("") || value.isEmpty() || value.equals("null")) {
            logger.info("Deleting key: " + key);
            kvCache.Delete(key);
        } else {
            logger.info("Inserting into cache: " + key + " with value: " + value);
            kvCache.Insert(key, value);
        }
    }

    private String getCache(
            String key
            ) {
        return(kvCache.Get(key));
    }

    public void CacheStatus() {
        kvCache.CacheStatus();
    }

    private void putKVSyn(
            String key,
            String value
            ) throws Exception {
        logger.info("Putting " + key + " with value: " + value + " into file " + fileName);
        boolean completedRead = false;
        ArrayList<kvContainer> keyValues = new ArrayList<kvContainer>();
        boolean foundKey = false;
        readWriteLock.readLock().lock();
        try {
            try {
                File file  = new File(
                    fileName
                    );
                file.createNewFile();
                FileReader mainFile = new FileReader(fileName);
                BufferedReader br = new BufferedReader(mainFile);
                String line;
                while( (line = br.readLine()) != null) {
                    logger.info("Read line in " + fileName + "'" + line + "'");
                    //String[] keyValue = line.split(new String(new byte[] {0x1e}));
                    String[] keyValue = splitByUniqueByte((byte) 0x1e, line);
                    if(!foundKey && keyValue[0].equals(key)) {
                        foundKey = true;
                        if(value.equals("") || value == null || value.isEmpty() || value.equals("null")) {
                            logger.info("Clearing key: " + key + " from storage");
                            continue;
                        } else {
                            keyValue[1] = value;
                        }
                    }
                    keyValues.add(new kvContainer(
                        keyValue[0],
                        keyValue[1]
                    ));
                }
            } catch (FileNotFoundException fnfe) {
                logger.error("Failed to read file " + fileName);
                fnfe.printStackTrace();
                throw new Exception("Failed file");
            } catch (IOException ioe) {
                System.err.println(ioe);
                logger.error("IOE: " + fileName);
                ioe.printStackTrace();
                throw new Exception("Failed file");
            }
        } finally { readWriteLock.readLock().unlock(); }

        if(!foundKey) {
            keyValues.add(new kvContainer(
                        key,
                        value
                        )
                    );
        }

        
        Path fstream = Paths.get(
                "temp" + fileName
                );
        StringBuilder sb = new StringBuilder();
        for(kvContainer kvPair : keyValues) {
            sb.append(
                    (kvPair.key
                    + new String(new byte[] {0x1e}) 
                    + kvPair.value + '"'
                    + " \n")
                    );
        }
        Files.write(fstream,
                sb.toString().getBytes()
                );
        File renameTemp = new File("temp" + fileName);
        readWriteLock.writeLock().lock();
        try {
            File newFile = new File(renameTemp.getParent(), fileName);
            newFile.delete();
            Files.move(renameTemp.toPath(), newFile.toPath());
        } finally { readWriteLock.writeLock().unlock(); }
        return;
    }


    @Override
    public void clearCache(){
        kvCache.Clear();
        return;
    }

    @Override
    public void clearStorage(){
        readWriteLock.writeLock().lock();
        try {
            kvCache.Clear();
            File file = new File(fileName);
            file.delete();
        } finally { readWriteLock.writeLock().unlock(); }
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

    public boolean initializeServer() {
     logger.info("initialize server ...");
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            logger.info("Server listening on port: "
                    + serverSocket.getLocalPort());
            return (true);
        } catch (IOException e) {
            logger.error("Error! Cannt open server socket:");
            if(e instanceof BindException) {
                logger.error("Port " + port + " is already bound!");
            }
            return (false);
        }
    }

    public void run() {
        running = initializeServer();

        if(serverSocket != null) {
            while(isRunning()) {
                try {
                    Socket client = serverSocket.accept();
                    ClientConnection conn =
                        new ClientConnection(client, this);
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
                KVServer ourServer = new KVServer(port, cacheSize, strategy);
                ourServer.start();
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
