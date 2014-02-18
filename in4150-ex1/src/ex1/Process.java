package ex1;

import java.util.*;

public class Process {
	// Scalar clock for this process
	int clock = 0;
	// Process id
	final int process_id;
	
	// Queue of messages that have been received but not yet delivered
	Queue<Message> messq = new PriorityQueue<>();
	
	Process() {
		this.process_id = new Random().nextInt();
		clock = new Random().nextInt(100);
	}
	
	private Message createMessage() {
		return new Message(process_id, ++clock);
	}
	
	public void receive(Message m){
		clock = Math.max(clock,m.time)+1;
	}
	
	private void deliver() {
		// Deliver the message
	}
}
