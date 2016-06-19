package no.ifi.uio.catod;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
//import java.util.Date;

public class Feeder implements Runnable {

	/* To avoid null reference */
	private String fileName = "default.txt";
	private int hz = 0; // number of samples pr second

	
	public Feeder() {
		System.out.println("constructing the Feeder:\t" + Thread.currentThread().getId());
	}
	
	@Override
	public void run() {
		System.out.println("in run: \t" + Thread.currentThread().getId());


	}
	
	/**
	 * Loops through the file and passes discrete data points
	 * from the time series to the client
	 * @param fileContent
	 */
	public void sendLoop(ArrayList<String> fileContent) {
		String toSend;
		int index = 0;
		
		
		while (true) {
			toSend = "";
			//TODO feed the file to recipient
			if (index > (fileContent.size()-1)) {
				index = 0;
				return; // TEMP: remove this when not testing
			}
			
			toSend = stampTime();
			toSend += fileContent.get(index++);
			
			// TODO send line with time-stamp
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

			    InputStream fis = new FileInputStream("the_file_name");
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
		
		
		return "TODO; ";
	}
	
}
