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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * Small application for simulating sensor node delivering
 * arbitrary data to main application
 * @author catoda
 *
 */
public class DataFeeder {
    private static String clientChannel = "clientChannel";
    private static String serverChannel = "serverChannel";
    private static String channelType = "channelType";
	
	
	
	public static void main(String[] args) throws IOException {
		// TODO Create a non-blocking server socket:
		int port = 4444;
		String localhost = "localhost";
		
		ServerSocketChannel channel = ServerSocketChannel.open();

		channel.bind(new InetSocketAddress(localhost, port));
		
		channel.configureBlocking(false);
		
		Selector selector = Selector.open();
		
		SelectionKey socketServerSelectionKey = channel.register(selector,
                SelectionKey.OP_ACCEPT);
        // set property in the key that identifies the channel
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(channelType, serverChannel);
        socketServerSelectionKey.attach(properties);
		
        
        for (;;) {
        	 
            // the select method is a blocking method which returns when atleast
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
                System.out.println(key.attachment().toString());

                // the selection key could either by the socketserver informing
                // that a new connection has been made, or
                // a socket client that is ready for read/write
                // we use the properties object attached to the channel to find
                // out the type of channel.
                if (((Map<String, String>) key.attachment()).get(channelType).equals(
                        serverChannel)) {

                	// a new connection has been obtained. This channel is
                    // therefore a socket server.
                	
                    ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key
                            .channel();
                    
                    // accept the new connection on the server socket. Since the
                    // server socket channel is marked as non blocking
                    // this channel will return null if no client is connected.
                    
                    SocketChannel clientSocketChannel = serverSocketChannel
                            .accept();
 
                    // TODO after setup, spawn thread and map channel to it
                    if (clientSocketChannel != null) {
                        // set the client connection to be non blocking
                        clientSocketChannel.configureBlocking(false);
                        SelectionKey clientKey = clientSocketChannel.register(
                                selector, SelectionKey.OP_READ,
                                SelectionKey.OP_WRITE);
                        Map<String, String> clientproperties = new HashMap<String, String>();
                        clientproperties.put(channelType, clientChannel);
                        clientKey.attach(clientproperties);
 
                        // write something to the new created client
                        CharBuffer buffer = CharBuffer.wrap("Ready?");
                        while (buffer.hasRemaining()) {
                            clientSocketChannel.write(Charset.defaultCharset()
                                    .encode(buffer));
                        }
                        buffer.clear();
                    }
 
                } else {
                	// TODO, when ack i  received from client, start thread and push data

                	// data is available for read 
                    // buffer for reading
                    ByteBuffer buffer = ByteBuffer.allocate(20);
                    SocketChannel clientChannel = (SocketChannel) key.channel();
                    int bytesRead = 0;
                    if (key.isReadable()) {
                        // the channel is non blocking so keep it open till the
                        // count is >=0
                        if ((bytesRead = clientChannel.read(buffer)) > 0) {
                            buffer.flip();
                            String resp = Charset.defaultCharset().decode(
                                    buffer).toString().trim();
                            
                            /* 
                             * Create a simple text-based html inspired protocol for interaticon 
                             * {fName}, 200  - ok, postfixed with filename 
                             * 400 - abort 
                             * */
                            if (resp.endsWith("200")) { // had to remove \n with trim()
                            	System.out.print("Got 200\t");
                            	String[] r = resp.split(",");
                            	System.out.println(r.length);
                            	
                            	
                            } else if (resp.endsWith("400")) {
                            	clientChannel.close();
                            	
                            } else {
                            	System.out.format("Got unknown code\t %s\n", resp);
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

	
	private static boolean spawn() {
		
		return false;
	}
}
