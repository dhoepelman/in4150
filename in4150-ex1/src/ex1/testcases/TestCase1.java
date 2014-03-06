package ex1.testcases;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import ex1.*;
import ex1.Process;

public class TestCase1 extends Thread implements Target {

	private AtomicBoolean change = new AtomicBoolean(false);
	
	private final Map<Integer, Process> localprocessmap;
	
	private Map<Integer, Deque<Message>> messagestack = new HashMap<>();
	
	public TestCase1(Map<Integer, Process> localprocessmap) {
		this.localprocessmap = localprocessmap;
		for(Process p : localprocessmap.values()) {
			messagestack.put(p.process_id, new ArrayDeque<Message>());
		}
	}

	@Override
	public synchronized void deliver(int deliveringPID, Message m) {
		messagestack.get(deliveringPID).push(m);
		change.set(true);
	}
	
	public void run() {
		if(localprocessmap.size() < 3) {
			System.err.println("Test1 requires 3 processes");
			return;
		}
		// Send a message a first and second process at "the same time"
		Process p0 = localprocessmap.get(0);
		Process p1 = localprocessmap.get(1);
		Process p2 = localprocessmap.get(2);
		p0.sendNewMessage();
		p1.sendNewMessage();
		p2.sendNewMessage();
		try {
			Thread.sleep(1500);
		} catch (InterruptedException e) {}
		p2.sendNewMessage();
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {}
		p0.sendNewMessage();
		
		// Wait for both messages to be delivered
		boolean completed = false;
		while(!completed) {
			while(change.get() == false) {}
			if(messagestack.get(0).size() >= 5 &&
					messagestack.get(1).size() >= 5 &&
					messagestack.get(2).size() >= 5) {
				completed = true;
			} else {
				change.set(false);
			}
		}
		while(change.get()==false) {
			for(Process p: localprocessmap.values()) {
				if(messagestack.get(p.process_id).size() < 5) {
					change.set(false);
					break;
				}
			}
		}
		
		// Check that all process queues are the same
		Deque<?> q = messagestack.get(0);
		boolean success = true;
		for(int i=0;i<3;i++) {
			success = success && messagestack.get(i).toString().equals(q.toString());
			if(!success) {
				System.out.println("Delivery queue of p" + i + " is different: " + messagestack.get(i) + " v.s. " + q);
			}
		}
		if(!success) {
			System.out.println("Testcase1 failed, not all messages were received in the same order by all processes");
		} else {
			System.out.println("Delivery order: " + q.toString());
		}

		System.out.println("TestCase 1 completed " + (success?"succesfully":"unsuccessfully"));
	}

}
