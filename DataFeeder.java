// TODO - refactor into smaller parts for exception handling

import java.io.IOException;
import java.io.WriteAbortedException;
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
 * Small application for simulating sensor node delivering arbitrary data to
 * main application
 * 
 * @author Cato Danielsen
 *
 */
public class DataFeeder {
	private static final String clientChannel = "clientChannel";
	private static final String serverChannel = "serverChannel";
	private static final String channelType = "channelType";

	private static final String delimiter = ",";
	private static final String OKCODE = "200";
	private static final String ABORTCODE = "400";
	private static final String REQCODE = "REQ";
	
	
	private static int bufferSize = 30;

	private static ArrayList<Thread> threadList;
	private static Map<String, Feeder> feederMap;

	public static void main(String[] args) throws IOException {
		
		int port = 4444;
		String localhost = "localhost";
		
		System.out.println("===DataFeeder--->\tRunning DataFeeder on port " + port);
		
		ServerSocketChannel channel = ServerSocketChannel.open();
		channel.bind(new InetSocketAddress(localhost, port));
		channel.configureBlocking(false);
		
		Selector selector = Selector.open();
		
		SelectionKey socketServerSelectionKey = channel.register(selector,
                SelectionKey.OP_ACCEPT);
        // set property in the key that identifies the channel
		
		/* Maps and lists of running threads, properties and of 
		 * feeder instances TODO: Remove all references to feeder and thread
		 * on connection close
		 * Weak hash map? 
		 */
		threadList = new ArrayList<Thread>();
		feederMap = new HashMap<String, Feeder>();
		Map<String, String> properties = new HashMap<String, String>();
        
        properties.put(channelType, serverChannel);
        socketServerSelectionKey.attach(properties);
        Iterator<SelectionKey> iterator;
        Set<SelectionKey> selectedKeys;
        
        do {
            if (selector.selectNow() == 0) { // Blocking select with timeout for debug
                continue;
            }
            
            // the select method returns with a list of selected keys
            selectedKeys = selector.selectedKeys();
            iterator = selectedKeys.iterator();
            
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                // the selection key could either by the socketserver meaning a new connection has been made, 
                // or
                // a socket client that is ready for read/write
                // we use the properties object attached to the channel to find the type of channel.
                Map<String, String> propertiesMap = (Map<String, String>)  key.attachment();
                
                // INIT
                if ((propertiesMap.get(channelType).equals(
                        serverChannel))) {
                	// MOVE TO initalConnection method
                    ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key
                            .channel(); // new channel
                    SocketChannel clientSocketChannel = serverSocketChannel
                            .accept();

                    // DONE after setup, spawn thread and map channel to it
                    if (clientSocketChannel != null) {
                        clientSocketChannel.configureBlocking(false);
                        SelectionKey clientKey = clientSocketChannel.register(
                                selector, 
                                SelectionKey.OP_READ,
                                SelectionKey.OP_WRITE);
                        
                        Map<String, String> clientproperties = new HashMap<String, String>(); // attached on the key to be able to find stuff 
                        
//                    	System.out.println("New conneciton..."); //TODO: implement logging
                        
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
 
//                      ACK to client
                        CharBuffer buffer = CharBuffer.wrap("OK," + OKCODE);

                        while (buffer.hasRemaining()) {
                            clientSocketChannel.write(Charset.defaultCharset()
                                    .encode(buffer));
                        }

                        buffer.clear();
                    }
                }     
//				INIT COMPLETE, expect ACK or ABORT from client
//				Otherwise it is probably connection lost. Other cases are not considered?
                else {
                    if (key.isReadable()) {
                    	readFromClient(key, propertiesMap);
                    }
                }
//              once a key is handled, it needs to be removed
            iterator.remove();
            }
        } while (true);
    }
	
	/**
	 * Read the response from client, called when activity is detected on channel.
	 * Uses IOException for sudden closing of 
	 * @param key
	 * @param propertiesMap
	 * @return
	 */
//	http://www.studytrails.com/java-io/non-blocking-io-multiplexing.jsp
	private static int readFromClient(SelectionKey key, Map<String, String> propertiesMap) {
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        SocketChannel clientCh = (SocketChannel) key.channel();
        int bytesRead = 0;;
        
        try {
        	if ((bytesRead = clientCh.read(buffer)) > 0) {
	            buffer.flip();
	            String resp = Charset.defaultCharset().decode(
	            		buffer).toString().trim();

//	            TODO: refactor out of this method --> handleAck() 
		        if (resp.endsWith(REQCODE)) { // had to remove \n with trim()
//		        	System.out.print("Got 200\t");
		
		        	Feeder f = feederMap.get(propertiesMap.get("thread"));
		        	Thread thf = threadList.get(Integer.valueOf(propertiesMap.get("thread"))); 
		        	
		        	String[] r = resp.split(delimiter);
		        	
		        	switch (r.length) {
		        		case 1: System.out.println("--DataFeeder->\tReturn file list"); writeToClient(getFiles(), clientCh); break;			        		
		        		case 2: System.out.println("--DataFeeder->\tOK, requested file: " + r[0]); f.setFileName(r[0]); thf.start();  break;
		        		case 3: System.out.println("--DataFeeder->\tTODO: send " + r[2] + " times, instead of infinite");
		        		default: System.out.println("--DataFeeder->\tERROR: unknown request size?\t " + resp); break;
		        	}
		        } else if (resp.endsWith(ABORTCODE)) {
		        	System.out.format("--DataFeeder->\tABORT: Got abort code:\t" + resp);
		        	clientCh.close();
		        	key.cancel(); // ??? 
		        } else {
		        	System.out.format("--DataFeeder->\tERROR: Got unknown code\t %s\n", resp);
		        	//TODO: write back on close
		        }
	        
		        buffer.clear();
			} 
    	} catch (IOException e) {
    		try {
    			System.out.println("===EXCEPTION--DataFeeder->\tClient has disconnected. Forceful shutdown");
    			clientCh.close();
				key.cancel();
			} catch (IOException e1) {
//				TODO Auto-generated catch block
				System.out.println("===EXCEPTION--DataFeeder->\tAttempt to close the socket/key failed!");
				e1.printStackTrace();
				return -1;
			}
        }
        return bytesRead;
	}
	private static void writeToClient(String toSend, SocketChannel ch) {
    	CharBuffer buffer = CharBuffer.wrap(toSend);
        while (buffer.hasRemaining()) {
            try {
            	ch.write(Charset.defaultCharset()
                    .encode(buffer));
            } catch (IOException e) {
            	e.printStackTrace();
            }
        }
        buffer.clear();
	}
	
	/**
	 * Mocked method.
	 * @return
	 */
	private static String getFiles() {
		return "signal.txt"; 
	}
}
