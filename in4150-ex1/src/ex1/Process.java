package ex1;

import java.rmi.*;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
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
	 * Map of all processes and their RMI strings
	 */
	private Map<Integer, String> processes;
	/**
	 * RMI registry
	 */
	private Registry rmireg;

    /**
     * Sorted queue of messages that have been received but not yet delivered
     */
	Queue<Message> messq = new PriorityQueue<>();
    /**
     * For every received message, a list of processes that have yet to
     * acknowledge it
     */
	Map<Message, Set<Integer>> ackList = new HashMap<>();

    /**
     * Make a process with a random ID between 0 and 2^32-1
     * @throws RemoteException
     */
	public Process() throws RemoteException {
		this(new Random().nextInt());
	}

    /**
     * Make a process with a specific process id
     */
	public Process(int process_id) throws RemoteException {
		this(process_id, new Random().nextInt(100));
	}

    /**
     * Make a process with a specific process id and initial clock
     */
	public Process(int process_id, int clock) throws RemoteException {
		this.process_id = process_id;
		this.clock = clock;
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
		// In a real situation we would actually do something here
		loginfo("Delivered " + m.toString());
		// Check if we can deliver the next message
		if (canDeliver()) {
			deliver();
		}
	}

	public void send(Message m) {
		//randomDelay();
		loginfo("Broadcasting " + m.toString());
		// Broadcast the message to every process (including this process)
		for(String p_rmi : processes.values()) {
			try {
				((Process_RMI)rmireg.lookup(p_rmi)).receive(m);
			} catch (RemoteException | NotBoundException e) {
				logerr(String.format("Could not send %s to %s", m, p_rmi));
				e.printStackTrace();
			}
		}
	}

	private void randomDelay() {
		// Random delay before sending [0,0.5]s
		try {
			Thread.sleep(new Random().nextInt(500));
		} catch (InterruptedException e) {
		}
	}

	private void loginfo(String msg) {
		log.info(String.format("P_%d[%d]: %s", process_id, clock, msg));
	}

	private void logwarn(String msg) {
		log.warning(String.format("P_%d[%d]: %m", process_id, clock, msg));
	}

	private void logerr(String msg) {
		log.severe(String.format("P_%d[%d]: %m", process_id, clock, msg));
	}

	/**
	 * Sets the map of processes, from process id to RMI string
	 */
	public void setProcesses(Map<Integer, String> processmap) {
		this.processes = processmap;
	}
	
	public void setRMI(Registry r) {
		this.rmireg = r;
	}
	
	public void stop() {
		try {
			unexportObject(this, true);
		} catch (NoSuchObjectException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
