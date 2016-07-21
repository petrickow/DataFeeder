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
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Feeder implements Runnable {

	/* To avoid null reference */
	private String fileName = "default.txt";
	private int hz = 0; // number of samples pr second
	private SocketChannel clientCh;
	
	private Timer timer;
	private int currentLine, fileLength;
	private List<String> fileContent;
	
	/* Timestamping */
	private Calendar cal;
	private SimpleDateFormat sdf;
	private String s;

	private int sequenceNumber;
	/***************************
	 * Constructor need the channel we use for serving 
	 * signal
	 * 
	 * @param clientChannel
	 */
	public Feeder(SocketChannel clientChannel) {
		clientCh = clientChannel;
	}

	/***************************
	 * Set the file name we want to read
	 * the signals from
	 * **/
	public boolean setName(String fName) {
		if (fName.length() == 0) {
			return false;
		}
		fileName = fName;
		return true;
	}
	
	
	@Override
	public void run() {
		//System.out.println("--feeder-> in run: \t" + fileName);
		cal = Calendar.getInstance();
        sdf = new SimpleDateFormat("HH:mm:ss.SSSZ");
        
        sequenceNumber = 0;
        try {
        	fileContent = readFile(fileName);
        	fileLength = fileContent.size();
        	timer = new Timer();
            
        	timer.schedule(new TimerPusher(), 0, //initial delay
                    1 * 1); //subsequent rate
        	
        	
        } catch (IOException e) {
        	e.printStackTrace();
        	//TODO: give error message based on cause:
        	sendError("File not found, 400");
        }
        //System.out.println("--feeder-> Feeder thread signing off"); 
	}
	
	/***************************
	 * Timed task to run the serving
	 * @author Cato Danielsen
	 *
	 */
    class TimerPusher extends TimerTask {
    	
    	
        public void run()  {
        	
        	long startTime = System.nanoTime();
        	
        	try {
				send();
			} catch (IOException e) {
				try {
					clientCh.close(); // i verste fall lukkes den to ganger
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				timer.cancel();
				timer.purge();
				
				//System.out.println("--feeder-> Got this, closed timer and ch====> x"+e.getMessage() + " " + clientCh.isRegistered() + " " + clientCh.isConnected());
			}
        	
        	long endTime = System.nanoTime();
        	long duration = (endTime - startTime);  //divide by 1000000 to get milliseconds.
        	float msDuration = (float) duration/1000000;
        	//System.out.println(endTime + " - " + startTime + "\t=\tDuration ms: " + msDuration ); // TODO: log instead of print.
        }
      }

	/**
	 * Loops through the file and passes discrete data points
	 * from the time series to the client
	 * @param fileContent
	 * @throws IOException 
	 */
    private synchronized void send() throws IOException {
    	
    	if (currentLine >= fileLength) {
    		currentLine = 0;
    	}
    	
    	String toSend = fileContent.get(currentLine++);
    	CharBuffer buffer = CharBuffer.wrap(toSend  + ", " + ++sequenceNumber);
        while (buffer.hasRemaining()) {
            clientCh.write(Charset.defaultCharset()
                    .encode(buffer));
        }
        buffer.clear();
    }
	
	/**
	 * Takes a string with name for file containing
	 * time series of respiration signal.
	 * @param fName
	 * @return Each line as a String of the file stored in an ArrayList
	 * @throws IOException
	 */
	private List<String> readFile(String fName) throws IOException {
		List<String> content = Collections.synchronizedList(new ArrayList<String>()); 
		try (
			    InputStream fis = new FileInputStream(System.getProperty("user.dir") + "/" + fName);
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
	 * Util-method for sending error
	 * @param toSend contains information we want to send to client
	 */
	private void sendError(String toSend) {
    	CharBuffer errorBuffer = CharBuffer.wrap(toSend);
    	while (errorBuffer.hasRemaining()) {
            try {
				clientCh.write(Charset.defaultCharset()
				        .encode(errorBuffer));
			} catch (IOException sendError) {
				// TODO Auto-generated catch block
				sendError.printStackTrace();
			}
        }
	}
	
	/** 
	 * Appends a simulated time-stamp based on the recording frequency of the
	 * signal being sent
	 * @return String with theoretical time
	 */
	private String stampTime() {
		
		return Long.toString(System.currentTimeMillis());
	}
	
	
	/**
	 * Test method used in development to pass some 
	 * gibberish to client
	 */
    private synchronized  void beepTest() {
    	String toSend = "Beep! "; 
    	CharBuffer buffer = CharBuffer.wrap(toSend);
        while (buffer.hasRemaining()) {
            try {
				clientCh.write(Charset.defaultCharset()
				        .encode(buffer));
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        
    	try {
			clientCh.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block, might already be closed? 
			e.printStackTrace();
		}
        buffer.clear();
    }
    
	
}
