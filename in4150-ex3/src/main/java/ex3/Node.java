package ex3;

import java.rmi.*;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Node extends UnicastRemoteObject implements Node_RMI {
	private static final long serialVersionUID = -2731859365280472117L;

    private static final int mindelay = 100;
    private static final int maxdelay = 1000;

	private final Logger log = Logger.getLogger("Node");

    /**
     * Node id
     */
	public final int process_id;
	/**
	 * Map of all processes in the request set and their RMI strings
	 */
	private final Map<Integer, String> processrmimap;
	/**
	 * RMI registry
	 */
	private final Registry rmireg;

    /**
     * Make a process with a specific process id
     * @param processrmimap 
     */
	public Node(int process_id, Map<Integer, String> processrmimap, Registry r) throws RemoteException {
		this.process_id = process_id;
		this.processrmimap = processrmimap;
		this.rmireg = r;
	}

	/**
	 * Create a new message
	 */
	private Message newMessage(int level, int id) {
		return new Message(process_id, level, id);
	}
	
	@Override
	public void receive(Message m) {
		loginfo("Received " + m.toString());

	}
	
	
	public void send(final Message m, final int proc_id) {
		send(m,proc_id,true);
	}
	/**
	 * Send a message to a process
	 * @param m
	 */
	public void send(final Message m, final int proc_id, boolean log) {
		if(log) {
			loginfo(String.format("Sending to process %d message %s", proc_id , m.toString()));
		}
		new Thread() {
			public void run() {
				try {
                    randomDelay(mindelay, maxdelay);
					((Node_RMI)rmireg.lookup(processrmimap.get(proc_id))).receive(m);
				} catch (RemoteException | NotBoundException e) {
					logerr(String.format("Could not send %s to %d", m, proc_id));
					e.printStackTrace();
				}
			}
		}.start();
	}

    /**
     * Make this node start a election
     */
    public void startElection() {
        // TODO: Implement election
    }

	private void randomDelay(int min, int max) {
		try {
			Thread.sleep(new Random().nextInt(max)+min);
		} catch (InterruptedException e) {
		}
	}

	private void loginfo(String msg) {
		log(Level.INFO, msg);
	}

	private void logwarn(String msg) {
		log(Level.WARNING, msg);
	}

	private void logerr(String msg) {
		log(Level.SEVERE, msg);
	}
	
	private synchronized void log(Level lvl, String msg) {
		log.log(lvl, String.format("P_%d: %s", process_id, msg));
	}
	
	public void stop() {
		try {
			unexportObject(this, true);
		} catch (NoSuchObjectException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String status() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(String.format("Status of Node %d:\n", this.process_id));

        // TODO: Status
		
		return sb.toString();
	}
	
	public String toString() {
		return String.format("P_%d", process_id);
	}
}
