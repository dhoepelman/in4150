package ex2;

import java.rmi.*;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Process extends UnicastRemoteObject implements Process_RMI {
	private static final long serialVersionUID = -2731859865280472117L;

	private final Logger log = Logger.getLogger("Process");
	
    /**
     *  Scalar clock for this process
     */
	private int clock = 0;
    /**
     * Process id
     */
	public final int process_id;
	/**
	 * Map of all processes in the request set and their RMI strings
	 */
	private final Map<Integer, String> requestSet;
	/**
	 * RMI registry
	 */
	private final Registry rmireg;

    /**
     * Make a process with a specific process id
     */
	public Process(int process_id, Map<Integer, String> requestSet, Registry r) throws RemoteException {
		this.process_id = process_id;
		this.requestSet = requestSet;
		this.rmireg = r;
	}

    /**
     * Broadcast a new message from this process
     */
	public void sendNewMessage() {
		send(new Message(process_id, ++clock));
	}

	@Override
	public void receive(Message m) {
		loginfo("Received " + m.toString());
		// When receiving a message, set the clock to the max of the current
		// clock and the message time and increase
		clock = Math.max(clock, m.sender_time) + 1;
		

	}
	
	/**
	 * Broadcast a message to all processes in the request set
	 * @param m the message
	 */
	public void send(final Message m) {
		loginfo("Sending to request set: " + m.toString());
		// Broadcast the message to every process (including this process)
		for(final String p_rmi : requestSet.values()) {
			new Thread() {
				public void run() {
					try {
						((Process_RMI)rmireg.lookup(p_rmi)).receive(m);
					} catch (RemoteException | NotBoundException e) {
						logerr(String.format("Could not send %s to %s", m, p_rmi));
						e.printStackTrace();
					}
					
				}
			}.start();
		}
	}

	private void randomDelay() {
		// Random delay before sending [0.5,3]s
		try {
			Thread.sleep(new Random().nextInt(2500)+500);
		} catch (InterruptedException e) {
		}
	}

	private synchronized void loginfo(String msg) {
		log(Level.INFO, msg);
	}

	private void logwarn(String msg) {
		log(Level.WARNING, msg);
	}

	private void logerr(String msg) {
		log(Level.SEVERE, msg);
	}
	
	private synchronized void log(Level lvl, String msg) {
		log.log(lvl, String.format("P_%d[%d]: %s", process_id, clock, msg));
	}
	
	public void stop() {
		try {
			unexportObject(this, true);
		} catch (NoSuchObjectException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void setClock(int c){
		this.clock = c;
	}
}
