package no.ifi.uio.catod;

import java.util.ArrayList;
//import java.util.Date;

public class Feeder implements Runnable {

	String fileName = "default.txt";
	
	@Override
	public void run() {
		
		/* Setup */
		ArrayList<String> fileContent = readFile(fileName);
		String toSend;
		int index = 0;
		
		
		
		while (true) {
			toSend = "";
			//TODO feed the file to recipient
			if (index > (fileContent.size()-1)) {
				index = 0;
			}
			
			toSend = stampTime();
			toSend += fileContent.get(index++);
			
			// TODO send line with time-stamp
		}
		
	}
	
	private ArrayList<String> readFile(String fName) {
		
		
		return null; 
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
