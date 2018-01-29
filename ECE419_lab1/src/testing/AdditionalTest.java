package testing;

import org.junit.Test;

import junit.framework.TestCase;

//Client
import app_kvClient.KVClient;
import app_kvClient.KVClient.SocketStatus;
import app_kvClient.IKVClient;

//Server
import app_kvServer.KVServer;

//For catching printouts from handleCommand() 
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.IOException;

public class AdditionalTest extends TestCase {
    //Skeleton commands, call them and add the
    //final argument to them with string concat
    private String testConnect;
    private String testDisconnect;
    private String testGet;
    private String testPut;
    private String testLogLevel;
    private String testHelp;
    private String testQuit;
    private StringBuilder helpMsg;
    private String PROMPT = "KVClient> ";
    private String ERROR_PROMPT = PROMPT+"Error! ";
    //Client
    private KVClient client;
    //Server
    private KVServer ctx;
    //Catch handleCommand()'s printouts
    private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    private final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
    
    public void setUp() {
        //Set up skeleton commands
        testConnect = "connect localhost ";
        testDisconnect = "disconnect";
        testGet = "get ";
        testPut = "put putKeyTest ";
        testLogLevel = "logLevel ";
        testHelp = "help";
        testQuit = "quit";
        StringBuilder helpMsg = new StringBuilder();
        helpMsg.append(PROMPT).append("KVClient HELP (Usage):\n");
        helpMsg.append(PROMPT);
        helpMsg.append("::::::::::::::::::::::::::::::::");
        helpMsg.append("::::::::::::::::::::::::::::::::\n");
        helpMsg.append(PROMPT).append("connect <host> <port>");
        helpMsg.append("\t\t establishes a connection to a server\n");
        helpMsg.append(PROMPT).append("put <key> <value>");
        helpMsg.append("\t\t Inserts, updates or deletes entry. To delete use 'null' for <value>.\n");
        helpMsg.append(PROMPT).append("get <key>");
        helpMsg.append("\t\t\t get the value for the key specified from the server storage if it exists or return error if not.\n");
        helpMsg.append(PROMPT).append("disconnect");
        helpMsg.append("\t\t\t disconnects from the server \n");
        helpMsg.append(PROMPT).append("logLevel");
        helpMsg.append("\t\t\t changes the logLevel: ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF \n");
        helpMsg.append(PROMPT).append("quit ");
        helpMsg.append("\t\t\t\t exits the program");
        //Set up client
        KVClient client = new KVClient();
        //Set up server
        ctx = new KVServer(5000, 100, "FIFO");
        //Redirect output of handleCommand()
        //to stdout and stderr for output and 
        //errors respectively.
        System.setOut(new PrintStream(stdout));
        System.setErr(new PrintStream(stderr));
    }

    public void tearDown() {
        //Server clear
        clearCacheAndStorage();
        //Restore back to normal in case
        //any tests extend from this one
        System.setOut(System.out);
        System.setErr(System.err);
    }

    public void clearCacheAndStorage() {
        ctx.clearCache();
        ctx.clearStorage();
    }

    @Test
    public void testAllCmd(){
        //Test all supported CLI commands for basic functionality.
        KVClient client = new KVClient();
        //Invoke the help menu
        client.handleCommand(testHelp);
        StringBuilder helpMsg = new StringBuilder();
        helpMsg.append(PROMPT).append("KVClient HELP (Usage):\n");
        helpMsg.append(PROMPT);
        helpMsg.append("::::::::::::::::::::::::::::::::");
        helpMsg.append("::::::::::::::::::::::::::::::::\n");
        helpMsg.append(PROMPT).append("connect <host> <port>");
        helpMsg.append("\t\t establishes a connection to a server\n");                                                               
        helpMsg.append(PROMPT).append("put <key> <value>");                                                                          
        helpMsg.append("\t\t Inserts, updates or deletes entry. To delete use 'null' for <value>.\n");                               
        helpMsg.append(PROMPT).append("get <key>");                                                                                  
        helpMsg.append("\t\t\t get the value for the key specified from the server storage if it exists or return error if not.\n"); 
        helpMsg.append(PROMPT).append("disconnect");                                                                                 
        helpMsg.append("\t\t\t disconnects from the server \n");                                                                     
        helpMsg.append(PROMPT).append("logLevel");                                                                                   
        helpMsg.append("\t\t\t changes the logLevel: ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF \n");                           
        helpMsg.append(PROMPT).append("quit ");                                                                                      
        helpMsg.append("\t\t\t\t exits the program");                                                                                
        assertEquals(helpMsg+"\n",stdout.toString());
        stdout.reset();
        //Change logLevel to a valid level.
        client.handleCommand(testLogLevel+"ALL");
        assertEquals(PROMPT+"Log level changed to level ALL\n",stdout.toString());
        stdout.reset(); 
        //Connect correctly
        client.handleCommand(testConnect+"5000");
        assertEquals(PROMPT+"<localhost> <5000> CONNECTED\n",stdout.toString());
        stdout.reset();
        //Put a <key,null> pair. Clear <key> if already there.
        client.handleCommand(testPut+"''");
        assertEquals(PROMPT+"<putKeyTest> <null> DELETE_SUCCESS\n",stdout.toString());
        stdout.reset();
        //Put a <key,value> pair. Should be successful.
        client.handleCommand(testPut+"putValueTest");
        assertEquals(PROMPT+"<putKeyTest> <putValueTest> PUT_SUCCESS\n",stdout.toString());
        stdout.reset();
        //Get the <value> for <key> from above
        client.handleCommand(testGet+"putKeyTest");
        assertEquals(PROMPT+"<putKeyTest> <putValueTest> GET_SUCCESS\n",stdout.toString());
        stdout.reset();
        //Disconnect
        client.handleCommand(testDisconnect);
        assertEquals(PROMPT+"DISCONNECTED\n",stdout.toString());
        stdout.reset();
        //Quit
        client.handleCommand(testQuit);
        assertEquals(PROMPT+"Application exit!\n",stdout.toString());
        stdout.reset();
    }

    @Test
    public void testBasic(){
        //Mimics a basic, common, user CLI
        //session
        KVClient client = new KVClient();
        //Connect correctly
        client.handleCommand(testConnect+"5000");
        assertEquals(PROMPT+"<localhost> <5000> CONNECTED\n",stdout.toString());
        stdout.reset();
        //Put a <key,null> pair. Clear <key> if already there.
        client.handleCommand(testPut+"''");
        assertEquals(PROMPT+"<putKeyTest> <null> DELETE_SUCCESS\n",stdout.toString());
        stdout.reset();
        //Put a <key,value> pair. Should be successful.
        client.handleCommand(testPut+"putValueTest");
        assertEquals(PROMPT+"<putKeyTest> <putValueTest> PUT_SUCCESS\n",stdout.toString());
        stdout.reset();
        //Put a <key,value> pair. This should result in PUT_UPDATE.
        client.handleCommand(testPut+"putValueTest");
        assertEquals(PROMPT+"<putKeyTest> <putValueTest> PUT_UPDATE\n",stdout.toString());
        stdout.reset();
        //Get the <value> for <key> from above
        client.handleCommand(testGet+"putKeyTest");
        assertEquals(PROMPT+"<putKeyTest> <putValueTest> GET_SUCCESS\n",stdout.toString());
        stdout.reset();
        //Disconnect
        client.handleCommand(testDisconnect);
        assertEquals(PROMPT+"DISCONNECTED\n",stdout.toString());
        stdout.reset();
        //Quit
        client.handleCommand(testQuit);
        assertEquals(PROMPT+"Application exit!\n",stdout.toString());
        stdout.reset();
    }

    @Test
    public void testConnectDisconnect() {
        //Test basic connect and disconnect
        //session
        KVClient client = new KVClient();
        //So that the test doesn't require the user to
        //run server separately
        //KVServer ctx = new KVServer(5000,100,"FIFO");
        //clearCacheAndStorage();
        //Thread server = new Thread(ctx);
        //server.start();
        //Connect to wrong port. Should error out.
        client.handleCommand(testConnect+"1");
        assertEquals(ERROR_PROMPT+"Connection Failed! DISCONNECTED\n",stdout.toString());
        stdout.reset();//stdout keeps accumulating printouts from handleCommand
        //Connect. Should work since last connect would've failed.
        client.handleCommand(testConnect+"5000");
        assertEquals(PROMPT+"<localhost> <5000> CONNECTED\n",stdout.toString());
        stdout.reset();//stdout keeps accumulating printouts from handleCommand
        //Disconnect
        client.handleCommand(testDisconnect);
        assertEquals(PROMPT+"DISCONNECTED\n",stdout.toString());
        stdout.reset();
        //Connect again.
        client.handleCommand(testConnect+"5000");
        assertEquals(PROMPT+"<localhost> <5000> CONNECTED\n",stdout.toString());
        stdout.reset();
        //Connect twice. Should error out.
        client.handleCommand(testConnect+"5000");
        assertEquals(ERROR_PROMPT+"Client already connected!\n",stdout.toString());
        stdout.reset();
        //Disconnect.
        client.handleCommand(testDisconnect);
        assertEquals(PROMPT+"DISCONNECTED\n",stdout.toString());
        stdout.reset();
        //Disconnect twice. Should error out.
        client.handleCommand(testDisconnect);
        assertEquals(ERROR_PROMPT+"Need to connect first!\n",stdout.toString());
        stdout.reset();
        //Connect finally.
        client.handleCommand(testConnect+"5000");
        assertEquals(PROMPT+"<localhost> <5000> CONNECTED\n",stdout.toString());
        stdout.reset();
    }
    
    @Test
    public void testCmdDisconnected(){
        //Test commands like put and get when the connection
        //to a server does not exist.
        KVClient client = new KVClient();
        //Connect correctly first. Then disconnect and try put/get.
        //This tests two things, one that disconnect actually disconnects
        //the client and two that a put/get will error out when disconnected.
        client.handleCommand(testConnect+"5000");
        assertEquals(PROMPT+"<localhost> <5000> CONNECTED\n",stdout.toString());
        stdout.reset();
        //Disconnect
        client.handleCommand(testDisconnect);
        assertEquals(PROMPT+"DISCONNECTED\n",stdout.toString());
        stdout.reset();
        //Put while disconnected. Should error out.
        client.handleCommand(testPut+"putValueTest");
        assertEquals(ERROR_PROMPT+"DISCONNECTED\n",stdout.toString());
        stdout.reset();
        //Get while disconnected. Should error out.
        //Should not error out with key not found.
        client.handleCommand(testGet + "getKeyTest");
        assertEquals(ERROR_PROMPT+"DISCONNECTED\n",stdout.toString());
        stdout.reset();
    }
    
    @Test
    public void testCmdInvalidInput(){
        //Test all commands for cases of invalid inputs
        KVClient client = new KVClient();
        //Connect using letters for port 
        client.handleCommand(testConnect+"abcd");
        assertEquals(ERROR_PROMPT+"Port must be a number! DISCONNECTED\n",stdout.toString());
        stdout.reset();
        //Connect 
        client.handleCommand(testConnect+"5000");
        assertEquals(PROMPT+"<localhost> <5000> CONNECTED\n",stdout.toString());
        stdout.reset();
        //Put. Key with spaces should error out.
        client.handleCommand("put 'this key has spaces' 'putValueTest'");
        assertEquals(ERROR_PROMPT+"Keys cannot have spaces.\n",stdout.toString());
        stdout.reset();
        //Put. Unbalanced quotations end up regexed into more args.
        //This also tests invalid number of args.
        client.handleCommand("put 'this key has spaces' 'forgot the last single quote");
        assertEquals(ERROR_PROMPT+"Invalid number of arguments! 3 expected.\n",stdout.toString());
        stdout.reset();
        //Get. Incorrect number of args.
        //Default testGet doesn't contain the key to GET.
        client.handleCommand(testGet);
        assertEquals(ERROR_PROMPT+"Invalid number of arguments! 2 expected.\n",stdout.toString());
        stdout.reset();
        //logLevel. Invalid level.
        client.handleCommand(testLogLevel + "UNKNOWN");
        assertEquals(ERROR_PROMPT+"No valid log level!\n"+
                        PROMPT+"Possible log levels are:\n"+
                        PROMPT+"ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF\n",stdout.toString());
        stdout.reset();
        //logLevel. Invalid num args.
        client.handleCommand(testLogLevel);
        assertEquals(ERROR_PROMPT+"Invalid number of arguments! 2 expected.\n",stdout.toString());
        stdout.reset(); 
    }
}
