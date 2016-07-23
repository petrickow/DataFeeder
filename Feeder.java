
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
	public boolean setFileName(String fName) {
		if (fName.length() == 0) {
			return false;
		}
		fileName = fName;
		return true;
	}
	
	
	@Override
	public void run() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
	        @Override
            public void run() {
        		System.out.println("Feeder is done"); 
            }
        });
		
		System.out.println("--Feeder-> init run, read file: \t" + fileName);
		cal = Calendar.getInstance();
        sdf = new SimpleDateFormat("HH:mm:ss.SSSZ");
        
        sequenceNumber = 0;
        try {
        	System.out.println("--Feeder-> init run, read file: \t" + fileName);
        	fileContent = readFile(fileName);
        	fileLength = fileContent.size();
        	System.out.println("--Feeder-> signal length: "  + fileLength);
        	
        	timer = new Timer();
        	timer.schedule(new TimerPusher(), 0, 1); //task, delay, subsequent rate in ms
        } catch (IOException e) {
        	if (e instanceof FileNotFoundException) {
        		
        		try {
        			System.out.println("No file there sir");
        			sendError("File not found,400");
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					System.out.println("Lost connection as well?");
					e1.printStackTrace();
				}
        		//e.printStackTrace();
        	}
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
    		Runtime.getRuntime().addShutdownHook(new Thread() {
    	        @Override
                public void run() {
            		System.out.println("TimerPusher is done"); 
                }
            });
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
	 * @throws IOException 
	 */
	private void sendError(String toSend) throws IOException {
    	CharBuffer errorBuffer = CharBuffer.wrap(toSend);
    	while (errorBuffer.hasRemaining()) {
			clientCh.write(Charset.defaultCharset()
			        .encode(errorBuffer));
		}
    	clientCh.close();
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
