package group3;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;

public class Server {
	private static File logFile = new File("log.txt");
	private static File employeeFile = new File("employees.txt");
	private static File proFile = new File("profiles.txt"); 
	// for access to one account at a time by the ATM
	private static File accountFile = new File("accounts.txt");
	
	public static void main(String args[]) throws IOException {

		// make a server socket on the port number
		try (ServerSocket ss = new ServerSocket(777)) {

			// confirmation message
			System.out.println("Server is running ");

			// variable pool is a thread pool of 20 TODO: need more?
			var pool = Executors.newFixedThreadPool(20);

			// listening loop
			while (true) {

				// accept a connection on the socket
				var socket = ss.accept();

				// when there is a connection, the pool creates a new handler that uses socket
				// (current connection)
				pool.execute(new ClientHandler(socket, employeeFile, logFile, proFile, accountFile ));
				

			}
		}

	}
}

class ClientHandler implements Runnable {

	// has a socket attribute for the current connection
	private Socket socket;
	private static File logFile; 
	private static File employeeFile;
	private static File proFile;
	private static File accountFile;
	
	// Stream references for sending responses (added for ATM)
	private ObjectOutputStream outputStream;
	private ObjectInputStream inputStream;
	
	// ATM session tracking (added for ATM)
	private boolean atmLoggedIn = false;
	private String currentAccountNum = null;
	private double currentBalance = 0.0;

	// constructor takes the socket connection
	public ClientHandler(Socket s, File employees, File log, File profiles, File accounts) {
		socket = s;
		logFile = log;
		employeeFile = employees;
		proFile = profiles;
		accountFile = accounts;
	}

	@Override
	public void run() {

		try ( // create the object input and output streams on socket
				var s = socket;
				ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream in = new ObjectInputStream(socket.getInputStream());) {

			// Store streams as instance variables for use in handler methods
			this.outputStream = out;
			this.inputStream = in;

			// create variables outside of loop
			Message msg = null;
			MessageType type = MessageType.undefined;
			Application sender = Application.undefined;
			Boolean loggedIn = false;

			System.out.println("New Connection"); // TODO maybe remove
			
			// loop that listens for messages
			while (true) {
				
				// read new message
				msg = (Message) inputStream.readObject();

				// message status must be a request
				if (msg.getStatus() != MessageStatus.request) {
					// loop again
					continue;
				}
				// if message status is request
				else {
					// if type is undefined restart loop
					if(msg.getType() == MessageType.undefined) {
						continue;
					}
					
					// if message status is request and type is !undefined
					else {
						
						// check if it is from the ATM or Teller
						sender = msg.getSender();
						
						// if the ATM sent the message call ATM()
						if (sender == Application.ATM ) {
							System.out.println("ATM Message");
							handleATM(msg);
							continue;
						}
						
						// if the Teller sent the message call Teller()
						else if (sender == Application.teller) {
							Teller(msg);
							continue;
						}
						
						// if sender is undefined continue
						else {
							continue;
						}						
					}		
				}
			}
		}

		catch(Exception e) {}

		}

	
	
	// handles ATM related messages (IMPLEMENTED)
	private void handleATM(Message msg) throws Exception {
		switch (msg.getType()) {
			case customerLogin:
				String[] creds = msg.getText().split(",");
				java.util.Scanner scanner = new java.util.Scanner(accountFile);
				while (scanner.hasNextLine()) {
					String[] data = scanner.nextLine().split(",");
					if (data[0].equals(msg.getNum()) && data[1].equals(creds[1])) {
						atmLoggedIn = true;
						currentAccountNum = msg.getNum();
						currentBalance = Double.parseDouble(data[3]);
						sendResponse(MessageStatus.confirmation, "Login successful");
						scanner.close();
						return;
					}
				}
				scanner.close();
				sendResponse(MessageStatus.denial, "Invalid credentials");
				break;
				
			case withdrawal:
				double withdrawAmt = Double.parseDouble(msg.getText());
				if (withdrawAmt <= currentBalance) {
					currentBalance -= withdrawAmt;
					sendResponse(MessageStatus.confirmation, "Withdrawn. Balance: $" + currentBalance);
				} else {
					sendResponse(MessageStatus.denial, "Insufficient funds");
				}
				break;
				
			case deposit:
				currentBalance += Double.parseDouble(msg.getText());
				sendResponse(MessageStatus.confirmation, "Deposited. Balance: $" + currentBalance);
				break;
				
			case logout:
				atmLoggedIn = false;
				currentBalance = 0;
				sendResponse(MessageStatus.confirmation, "Logged out");
				break;
				
			default:
				if (msg.getText().equals("balance")) {
					sendResponse(MessageStatus.confirmation, String.valueOf(currentBalance));
				}
				break;
		}
	}
	
	// Helper to send ATM response (added for ATM)
	private void sendResponse(MessageStatus status, String text) throws Exception {
		outputStream.writeObject(new Message(status, null, Application.ATM, currentAccountNum, text));
		outputStream.flush();
	}
	
	// handles Teller related messages
	private void Teller(Message msg) {
		
		// store type
		MessageType type = msg.getType();
		
		// employee login happens first
		// if type is not login ignore
		// check employees.txt for credentials
		
		
		
		/*
		switch (type) {
	    case MessageType.login:
	        // code
	        break;
	    case MessageType.logout:
	        // code
	        break;
	    case MessageType.updateAccount:
	    	
	    case MessageType.updateProfile:
	    	
	    case MessageType.withdrawal:
	    	
	    case MessageType.deposit:
	    	
	    default:
	        // code
		*/
	}
	

}
