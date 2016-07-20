package no.ifi.uio.catod;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Small application for simulating sensor node delivering
 * arbitrary data to main application
 * @author Cato Danielsen
 *
 */
public class DataFeeder {
    private static String clientChannel = "clientChannel";
    private static String serverChannel = "serverChannel";
    private static String channelType = "channelType";
	
    private static String delimiter = ",";
    private static String OKCODE = "200";
    private static String ABORTCODE = "400";
    private static int bufferSize = 100;
	
    
    private static ArrayList<Thread> threadList;
    private static Map<String, Feeder> feederMap;
	
	public static void main(String[] args) throws IOException {
		// TODO Create a non-blocking server socket:
		int port = 4444;
		String localhost = "localhost";
		threadList = new ArrayList<Thread>();
		
		ServerSocketChannel channel = ServerSocketChannel.open();

		channel.bind(new InetSocketAddress(localhost, port));
		
		channel.configureBlocking(false);
		
		Selector selector = Selector.open();
		
		SelectionKey socketServerSelectionKey = channel.register(selector,
                SelectionKey.OP_ACCEPT);
        // set property in the key that identifies the channel
        Map<String, String> properties = new HashMap<String, String>();
        feederMap = new HashMap<String, Feeder>();
        
        properties.put(channelType, serverChannel);
        socketServerSelectionKey.attach(properties);
		
        int i = 0;
		int count = 10000; // for debug output
        
        for (;;) {
        	
        	i = i++ % count; // for debugpurposes, count to n and restart i + 1 = iteration

            // the select method is a blocking method which returns when at least
            // one of the registered
            // channel is selected. In this example, when the socket accepts a
            // new connection, this method
            // will return. Once a socketclient is added to the list of
            // registered channels, then this method
            // would also return when one of the clients has data to be read or
            // written. It is also possible to perform a nonblocking select
            // using the selectNow() function.
            // We can also specify the maximum time for which a select function
            // can be blocked using the select(long timeout) function.
            if (selector.selectNow() == 0)
                continue;
            // the select method returns with a list of selected keys
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectedKeys.iterator();
            
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                System.out.println(key.toString());

                // the selection key could either by the socketserver meaning a new connection has been made, 
                // or
                // a socket client that is ready for read/write
                // we use the properties object attached to the channel to find the type of channel.
                Map<String, String> propertiesMap = (Map<String, String>)  key.attachment();

                // INIT
                if ((propertiesMap.get(channelType).equals(
                        serverChannel))) {
                	
                    ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key
                            .channel(); // new channel
                    
                    SocketChannel clientSocketChannel = serverSocketChannel
                            .accept();
 
                    // TODO after setup, spawn thread and map channel to it
                    if (clientSocketChannel != null) {
                        clientSocketChannel.configureBlocking(false);
                        SelectionKey clientKey = clientSocketChannel.register(
                                selector, 
                                SelectionKey.OP_READ,
                                SelectionKey.OP_WRITE);
                        
                        Map<String, String> clientproperties = new HashMap<String, String>(); // attached on the key to be able to find stuff 
                        
                        System.out.println("New conneciton..."); //TODO: implement logging
                        
                        /* TODO: rewrite open channel/running threads mapping
                         * Dette blir litt tullete, kan like så greit bare lage Feeder objektet og
                         * mellomlagre det. Må uansett finne det frem og legge til filnavn når vi
                         * får det, for så å starte tråden som serverer den filen.
                         * Her kan vi redusere antall maps og unødvendig kompleksitet.
                         */
                        
                        Feeder f = new Feeder(clientSocketChannel);
                        Thread feeder = new Thread(f);
                        
                        if (threadList.add(feeder)) {
                        	clientproperties.put("thread", Integer.toString(threadList.indexOf(feeder))); // map associated thread to index
                        	feederMap.put(Integer.toString(threadList.indexOf(feeder)), f); // thread contains key to feeder object in map
                        }

                        clientproperties.put(channelType, clientChannel); // map channel type to client channel
                        clientKey.attach(clientproperties);
 
                        // ACK to client
                        CharBuffer buffer = CharBuffer.wrap(OKCODE +", OK");
                        while (buffer.hasRemaining()) {
                            clientSocketChannel.write(Charset.defaultCharset()
                                    .encode(buffer));
                        }

                        buffer.clear();
                    }
                // INIT COMPLETE, client ACK to 200, OK
                } else {
        			//buffer for reading
                    ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
                    SocketChannel clientChannel = (SocketChannel) key.channel();
                    //SelectionKey k = clientChannel.keyFor(key.selector());
                    //HashMap<String, String> map = (HashMap<String, String>) k.attachment();
                    int bytesRead = 0;

                    if (key.isReadable()) {
                        if ((bytesRead = clientChannel.read(buffer)) > 0) {
                            buffer.flip();
                            String resp = Charset.defaultCharset().decode(
                                    buffer).toString().trim();
                            /* 
                             * Create a simple text-based http inspired protocol for interaticon 
                             * {fName}, 200  - ok, postfixed with filename 
                             * 400 - abort
                             * http://www.studytrails.com/java-io/non-blocking-io-multiplexing.jsp 
                             * */
                            
                            if (resp.endsWith(OKCODE)) { // had to remove \n with trim()
                            	//System.out.print("Got 200\t");

                            	Feeder f = feederMap.get(propertiesMap.get("thread"));
                            	Thread thf = threadList.get(Integer.valueOf(propertiesMap.get("thread"))); 
                            	
                            	String[] r = resp.split(delimiter);
                            	
                            	switch (r.length) {
                            		case 1: System.out.println("ERROR: got ok from client, but no file name"); break;
                            		case 2: System.out.println("OK, requested file: " + r[0]); f.setName(r[0]); thf.start();  break;
                            		default: System.out.println("ERROR: more than one file requested?\t " + resp); break;
                            	}
                            } else if (resp.endsWith(ABORTCODE)) {
                            	System.out.format("ABORT: Got abort code:\t" + resp);
                            	clientChannel.close();
                            } else {
                            	System.out.format("ERROR: Got unknown code\t %s\n", resp);
                            }
                            
                            buffer.clear();
                        }
                        if (bytesRead < 0) {
                            // the key is automatically invalidated once the
                            // channel is closed
                            clientChannel.close();
                        }
                    }
                }
                // once a key is handled, it needs to be removed
                iterator.remove();
            }
        }
	}
}
