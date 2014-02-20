package ex1;

import java.rmi.*;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.logging.Logger;

public class Process extends UnicastRemoteObject implements Process_RMI {
	private static final long serialVersionUID = -2731859865280472117L;

	private final Logger log = Logger.getLogger("Process");

	// Scalar clock for this process
	private int clock = 0;
	// Process id
	public final int process_id;
	/**
	 * Map of all processes and their RMI strings
	 */
	private Map<Integer, String> processes;
	/**
	 * RMI registry
	 */
	private Registry rmireg;

	// Sorted queue of messages that have been received but not yet delivered
	Queue<Message> messq = new PriorityQueue<>();
	// For every received message, a list of processes that have yet to
	// acknowledge it
	Map<Message, Set<Integer>> ackList = new HashMap<>();

	public Process() throws RemoteException {
		this(new Random().nextInt());
	}

	public Process(int process_id) throws RemoteException {
		this(process_id, new Random().nextInt(100));
	}

	public Process(int process_id, int clock) throws RemoteException {
		this.process_id = process_id;
		this.clock = clock;
	}

	public void sendNewMessage() {
		send(new Message(process_id, ++clock));
	}

	public void sendNewAck(Message m) {
		send(new Ack(process_id, ++clock, m.sender_process, m.sender_time));
	}

	@Override
	public void receive(Message m) {
		loginfo("Received " + m.toString());
		// When receiving a message, set the clock to the max of the current
		// clock and the message time and increase
		clock = Math.max(clock, m.sender_time) + 1;
		// Put the message in the queue
		messq.add(m);
		// Populate the remaing acknowledgements list
		populateAckList(m);
		// Send acknowledgements for this message
		sendNewAck(m);
	}

	@Override
	public void receive(Ack ack) {
		loginfo("Received " + ack.toString());
		
		// When receiving a message, set the clock to the max of the current
		// clock and the message time and increase
		clock = Math.max(clock, ack.sender_time) + 1;
		
		Message m = ack.getAckedMsg();
		populateAckList(m);
		Set<Integer> remaining_acks = ackList.get(m);
		// We've gotten an ack for m from this process, remove it from the list
		remaining_acks.remove(ack.sender_process);
		// If the acked message is at the top of the queue and there's no more
		// acks remaining, deliver!
		if (remaining_acks.isEmpty() && m.equals(messq.peek())) {
			deliver();
		}
	}

	// Check to see if we can deliver
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
		if (!ackList.containsKey(m)) {
			ackList.put(m, new HashSet<>(processes.keySet()));
		}
	}

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

	public void send(Ack ack) {
		// Broadcast the message to every process (including this process)
		loginfo("Broadcasting " + ack.toString());
		for(String p_rmi : processes.values()) {
			try {
				((Process_RMI)rmireg.lookup(p_rmi)).receive(ack);
			} catch (RemoteException | NotBoundException e) {
				logerr(String.format("Could not send %s to %s", ack, p_rmi));
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
