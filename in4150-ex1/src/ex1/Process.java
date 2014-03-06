package ex1;

import java.rmi.*;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Process extends UnicastRemoteObject implements Process_RMI {
	private static final long serialVersionUID = -2731859865280472117L;

	private final Logger log = Logger.getLogger("Process");

	private Target target;
	
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
     * Sorted queue of messages that have been received but not yet delivered
     */
	// PriorityBlockingQueue is a synchronized priority queue
	Queue<Message> messq = new PriorityBlockingQueue<>();
    /**
     * For every received message, a list of processes that have yet to
     * acknowledge it
     */
	Map<Message, Set<Integer>> ackList = new HashMap<>();

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

    /**
     * Broadcast an acknowledgement
     * @param m the message to be ack'ed
     */
	public void sendNewAck(Message m) {
		send(new Ack(process_id, ++clock, m.sender_process, m.sender_time));
	}

	@Override
	public void receive(Message m) {
		loginfo("Received " + m.toString());
		// When receiving a message, set the clock to the max of the current
		// clock and the message time and increase
		clock = Math.max(clock, m.sender_time) + 1;
		
		if(m instanceof Ack) {
			Message ackedm = ((Ack)m).getAckedMsg();
			populateAckList(ackedm);
			Set<Integer> remaining_acks = ackList.get(ackedm);
			// We've gotten an ack for m from this process, remove it from the list
			remaining_acks.remove(m.sender_process);
			// If the acked message is at the top of the queue and there's no more
			// acks remaining, deliver!
			if (remaining_acks.isEmpty() && ackedm.equals(messq.peek())) {
				deliver();
			}
		} else {
			// Put the message in the queue
			messq.add(m);
			// Populate the remaing acknowledgements list
			populateAckList(m);
			// Send acknowledgements for this message
			sendNewAck(m);
		}
	}

    /**
     * Check to see if the message at the top of the queue can be delivered
     * @return true if the top message can be delivered, false if not or if there is no message
     */
	private boolean canDeliver() {
		Message m = messq.peek();
		if (m == null) {
			return false;
		}
		Set<Integer> remaining_acks = ackList.get(m);
		if (remaining_acks == null) {
			logwarn("The remaining ack list for the top message is null!");
			return false;
		}
		return remaining_acks.isEmpty();
	}

	/**
	 * Make the initial list of remaining acknowledgements for a message
	 */
	private void populateAckList(Message m) {
        // If there isn't an set of processes already
		if (!ackList.containsKey(m)) {
            // Copy all processes into the new set
			ackList.put(m, new HashSet<>(processes.keySet()));
		}
	}

    /**
     * "Deliver" the message
     * As there is no process behind this just delete the message
     */
	private void deliver() {
		// Deliver the message at the top of the queue
		Message m = messq.poll();
		if (m == null) {
			logwarn("Deliver was called but there was no message to deliver!");
			return;
		}
		// Get the remaining acknowledgments out of the ack map
		Set<Integer> remaing_acks = ackList.get(m);
		if (remaing_acks == null) {
			logwarn(String
					.format("Deliver was called for %s, but the acklist was null. Were acknowledgements ever administered?",
							m.toString()));
		} else if (!remaing_acks.isEmpty()) {
			logerr(String
					.format("Deliver was called for %s, but there were remaining acks! (was: %s)",
							m.toString(), remaing_acks.toString()));
		}
		// Remove the acklist for m
		ackList.remove(m);
		loginfo("Delivered " + m.toString());
		if(target != null) {
			target.deliver(process_id, m);
		}
		// Check if we can deliver the next message
		if (canDeliver()) {
			deliver();
		}
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

	public void setTarget(Target t) {
		this.target = t;
	}
}
