package no.ifi.uio.catod;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
//import java.util.Date;

public class Feeder implements Runnable {

	/* To avoid null reference */
	private String fileName = "default.txt";
	private int hz = 0; // number of samples pr second
	private SocketChannel clientCh;
	
	public Feeder() {
		System.out.println("constructing the Feeder:\t" + Thread.currentThread().getId());
	}
	
	public Feeder(SocketChannel clientChannel) {
		clientCh = clientChannel;
		
	}
	
	@Override
	public void run() {
		System.out.println("in run: \t" + Thread.currentThread().getId());
		try {
			sendLoop(readFile(fileName));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	/**
	 * Loops through the file and passes discrete data points
	 * from the time series to the client
	 * @param fileContent
	 * @throws IOException 
	 */
	public void sendLoop(ArrayList<String> fileContent) throws IOException {
		String toSend;
		int index = 0;
		
		ByteBuffer bf;

		
		while (true) {
			toSend = "";
			
			if (index > (fileContent.size()-1)) {
				index = 0;
				return; // TEMP: remove this when not testing
			}
			// TODO send line with time-stamp			
			toSend = stampTime();
			
			toSend += fileContent.get(index++) + "";
			CharBuffer buffer = CharBuffer.wrap(toSend);
            while (buffer.hasRemaining()) {
                clientCh.write(Charset.defaultCharset()
                        .encode(buffer));
            }
            
            buffer.clear();
		}
	}
	
	
	/**
	 * Takes a string with name for file containing
	 * time series of respiration signal.
	 * @param fName
	 * @return Each line as a String of the file stored in an ArrayList
	 * @throws IOException
	 */
	private ArrayList<String> readFile(String fName) throws IOException {
		
		ArrayList<String> content = new ArrayList<String>();
		
		try (
				
			    InputStream fis = new FileInputStream("c:\\" + fName);
			    InputStreamReader inputStream = new InputStreamReader(fis, Charset.forName("UTF-8"));
			    BufferedReader br = new BufferedReader(inputStream);
			) {
				String currentLine;
			    while ((currentLine = br.readLine()) != null) {
			        content.add(currentLine);
			    }
			} 
		
		return content; 
	}
	
	/** 
	 * Appends a simulated time-stamp based on the recording frequency of the
	 * signal being sent
	 * @return String with theoretical time
	 */
	private String stampTime() {
		return Long.toString(System.currentTimeMillis());
	}
	
}
