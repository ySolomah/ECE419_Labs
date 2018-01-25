package testing;

import client.KVStore;
import client.KVCommInterface;
import common.messages.KVMessage;

public class TesterThread implements Runnable {
    private int _id;
    private int _num_gets;
    private int _num_puts;
    private String _address;
    private int _port;
    private String prompt;

    private static String key = "foo";
    private static String value = "bar";

    public TesterThread(int id, int num_gets, int num_puts, String address, int port) {
        _id = id;
        _num_gets = num_gets;
        _num_puts = num_puts;
        _address = address;
        _port = port;
        prompt = "[THREAD "+ _id + "]: ";
    }
 
    public void run() 
    {
        System.out.println(prompt + "Starting thread " );
        
        // initialize interface
        KVCommInterface k = new KVStore(_address, _port);
        try {
            k.connect();
        } catch (Exception e) {
            System.out.println(prompt + "Could not connect to server");
            return;
        }
        
        // time the following, and average
        // determine total bytes sent in this time as well
        // catch any errors, reconnect if necessary - count number of errors
        int get_errors = 0;
        int put_errors = 0;
        int num_disconnects = 0;
        
        // send gets
        long startTime = System.nanoTime();
        for(int i = 0; i < _num_gets; ++i) {
            try {
                KVMessage ret = k.get(key);
                if(ret.getStatus() == KVMessage.StatusType.GET_ERROR) {
                    get_errors++;
                }
            } catch (Exception e) {
                get_errors++;
                num_disconnects++;
                // need to reconnect
                try {
                    k.connect(); 
                } catch (Exception j){
                    j.printStackTrace();
                    return;
                }
            }
        }

        // send puts
        for(int i = 0; i < _num_puts; ++i) {
            try {
                KVMessage ret = k.put(key, value);
                if(ret.getStatus() == KVMessage.StatusType.PUT_ERROR) {
                    put_errors++;
                }
            } catch (Exception e) {
                put_errors++;
                num_disconnects++;
                // need to reconnect
                try {
                    k.connect(); 
                } catch (Exception j){
                    j.printStackTrace();
                    return;
                }
            }
        }

        // close connection
        k.disconnect();
        long endTime = System.nanoTime();

        // print results
        System.out.println(prompt + "Number of disconnects: " + num_disconnects);
        System.out.println(prompt + "Number of get_errors: " + get_errors);
        System.out.println(prompt + "Number of put_errors: " + put_errors);
        System.out.println(prompt + "Ratio of disconnects: " + (float)num_disconnects/(_num_gets + _num_puts));
        System.out.println(prompt + "Ratio of get_errors: " + (float)get_errors/_num_gets);
        System.out.println(prompt + "Ratio of put_errors: " + (float)put_errors/_num_puts);
        System.out.println(prompt + "Time elapsed (ms): " + (float)(endTime - startTime)/1000000);
    }
}
