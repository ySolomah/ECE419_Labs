package testing;


import testing.TesterThread;
import org.junit.Test;
import client.KVCommInterface;
import client.KVStore;
import common.messages.KVMessage;
import common.messages.KVMessage.StatusType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import junit.framework.TestCase;

public class StressTester extends TestCase {
    private static int parseConditionalArgument(int position, int default_val, String[] args) {
        if(args.length > position) {
            return Integer.parseInt(args[position]);
        } else {
            return default_val;
        }
    }
    /**
     * Main entry point for the stress tester
     * @param args contains the type of testing
     * args[0] - Server Address
     * args[1] - Server Port
     * args[1+ 1] - number of requests (total) - default 1000
     * args[1+ 2] - concurrency - default 10
     * args[1+ 3] - put ratio (0 to 1) - default 0.5
     */
    public static void main(String[] args) {
        // collect arguments
        int requests, concurrency, num_puts, num_gets;
        float puts = 0.5f;
        String serverAddress = args[0];
        int port = Integer.parseInt(args[1]);
        int args_count = 2;

        requests = parseConditionalArgument(args_count++, 1000, args);
        concurrency = parseConditionalArgument(args_count++, 10, args);
        if(args.length > args_count) {
            puts = Float.parseFloat(args[args_count++]);
        }

        num_puts = (int) (puts * requests);
        // sanitize inputs
        if(num_puts < 0) {
            num_puts = 0;
        }
        if(concurrency < 0) {
            concurrency = 10;
        }
        if(requests < 0) {
            requests = 1000;
        }
        num_gets = requests - num_puts;

        int gets_per = (int) num_gets/concurrency;
        int puts_per = (int) num_puts/concurrency;
        System.out.println("Number of gets: " + num_gets);
        System.out.println("Number of puts: " + num_puts);
        // make the requests
        TesterThread[] clients = new TesterThread[concurrency];
        Thread[] t = new Thread[concurrency];
        for(int i = 0; i < concurrency; ++i) {
            clients[i] = new TesterThread(i, gets_per, puts_per, serverAddress, port);
            t[i] = new Thread(clients[i]);
            t[i].start();
        }
        try {
            for(int i = 0; i < concurrency; ++i) {
                t[i].join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(true);
    }

}
