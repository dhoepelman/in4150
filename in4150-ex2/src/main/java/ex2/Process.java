package ex2;

import java.rmi.*;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Process extends UnicastRemoteObject implements Process_RMI {
	private static final long serialVersionUID = 8114972165327988417L;

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
	 * Map of all processes and their RMI strings
	 */
	private final Map<Integer, String> processes;
	/**
	 * RMI registry
	 */
	private final Registry rmireg;


    /**
     * Make a process with a random ID between 0 and 2^32-1
     * @throws RemoteException
     */
	public Process(Map<Integer, String> processmap, Registry r) throws RemoteException {
		this(new Random().nextInt(), processmap, r);
	}

    /**
     * Make a process with a specific process id
     */
	public Process(int process_id, Map<Integer, String> processmap, Registry r) throws RemoteException {
		this.process_id = process_id;
		this.processes = processmap;
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
	 * Broadcast a message to all processes, with a [0,3]s random delay for each process
	 * @param m the message
	 */
	public void send(Message m) {
		send(m, true);
	}
	
	/**
	 * Broadcast a message to all processes
	 * @param m the message
	 * @param randomdelay Whether to introduce a random delay in [0,3]s for each process
	 */
	public void send(final Message m, final boolean randomdelay) {
		loginfo("Broadcasting " + m.toString());
		// Broadcast the message to every process (including this process)
		for(final String p_rmi : processes.values()) {
			new Thread() {
				public void run() {
					try {
						if(randomdelay) {
							randomDelay();
						}
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
