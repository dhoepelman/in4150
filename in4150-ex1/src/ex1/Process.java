package ex1;

import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Logger;

public class Process implements Process_RMI {
	private static final Logger log = Logger.getLogger("Process");
	
	// Scalar clock for this process
	private int clock = 0;
	// Process id
	public final int process_id;
	/**
	 * Map of all processes and their RMI strings
	 */
	private Map<Integer, String> processes;
	
	// Sorted queue of messages that have been received but not yet delivered
	//Queue<Message> messq = new PriorityQueue<>();
	// Threadsafe priorityqueue
	Queue<Message> messq = new PriorityBlockingQueue<>();
	
	public Process() {
		this(new Random().nextInt());
	}
	public Process(int process_id) {
		this(process_id, new Random().nextInt(100));
	}
	public Process(int process_id, int clock) {
		this.process_id = process_id;
		this.clock = clock;
	}
	
	private Message createMessage() {
		return createMessage(Message.TYPE.MSG);
	}
	
	private Message createAck() {
		return createMessage(Message.TYPE.ACK);
	}
	
	private Message createMessage(Message.TYPE t) {
		// When creating a message, first increase the clock
		loginfo("Creating " + t.toString() + " message");
		return new Message(process_id, ++clock, t);
	}
	
	public void receive(Message m){
		loginfo("Received " + m.toString());
		// When receiving a message, set the clock to the max of the current clock and the message time and increase
		clock = Math.max(clock,m.time)+1;
	}
	
	private void deliver() {
		// Deliver the message
	}
	
	private void send(Message m) {
		
	}
	
	private void loginfo(String msg) {
		log.info(String.format("P_%d[%d]: %m", process_id, clock, msg));
	}
	
	/**
	 * Sets the map of processes, from process id to RMI string
	 */
	public void setProcesses(Map<Integer, String> processmap) {
		this.processes = processmap;
	}
}
