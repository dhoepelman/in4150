package ex2;

import java.rmi.*;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import ex2.Message.TYPE;

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
	private final Map<Integer, String> processrmimap;
	/**
	 * RMI registry
	 */
	private final Registry rmireg;
	
	/**
	 * Processes to request to
	 */
	private final Collection<Integer> requestSet;

    /**
     * Make a process with a specific process id
     * @param processrmimap 
     */
	public Process(int process_id, Map<Integer, String> processrmimap, Registry r, Collection<Integer> requestSet) throws RemoteException {
		this.process_id = process_id;
		this.processrmimap = processrmimap;
		this.rmireg = r;
		if(requestSet == null) {
			throw new NullPointerException();
		}
		this.requestSet = requestSet;
	}

    /**
     * Multicasts a new message from this process
     */
	public void multicastNewMessage(Message.TYPE t) {
		multicast(newMessage(t));
	}
	/**
	 * Create a new message
	 */
	private Message newMessage(Message.TYPE t) {
		return new Message(process_id, ++clock, t);
	}

	private AtomicBoolean granted = new AtomicBoolean(false);
	private AtomicReference<Message> current_grant = new AtomicReference<>();
	private Queue<Message> requestQueue = new PriorityBlockingQueue<>(); 
	private AtomicBoolean inquiring = new AtomicBoolean(false);
	private AtomicBoolean postponed = new AtomicBoolean(false);
	private AtomicInteger no_grants = new AtomicInteger(0);
	
	private void handleRequest(Message m) {
		if(!granted.get()) {
			current_grant.set(m);
			send(newMessage(TYPE.GRANT), m.process);
			granted.set(true);
		} else {
			requestQueue.add(m);
			Message topRequest = requestQueue.peek();
			if(current_grant.get().compareTo(m) < 0 || topRequest.compareTo(m) < 0) {
				send(newMessage(TYPE.POSTPONED), m.process);
			} else {
				if(!inquiring.get()) {
					inquiring.set(true);
					send(newMessage(TYPE.INQUIRE), current_grant.get().process);
				}
			}
		}
	}
	
	@Override
	public void receive(Message m) {
		loginfo("Received " + m.toString());
		// When receiving a message, set the clock to the max of the current
		// clock and the message time and increase
		clock = Math.max(clock, m.time) + 1;
		switch(m.type) {
		case REQUEST:
			handleRequest(m);
			break;
		case GRANT:
			if(no_grants.incrementAndGet() == requestSet.size()) {
				postponed.set(false);
				executeCriticalSection();
				multicastNewMessage(TYPE.RELEASE);
			}
			break;
		case INQUIRE:
			while(!postponed.get() && no_grants.get() <= requestSet.size()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					logerr("Was interrupted while handling INQUIRE");
					break;
				}
			}
			if(postponed.get()) {
				no_grants.decrementAndGet();
				send(newMessage(TYPE.RELINQUISH), m.process);
			}
			break;
		case RELINQUISH:
			inquiring.set(false);
			granted.set(false);
			requestQueue.add(current_grant.get());
			current_grant.set(requestQueue.poll());
			granted.set(true);
			send(newMessage(TYPE.GRANT), current_grant.get().process);
			break;
		case RELEASE:
			granted.set(false);
			inquiring.set(false);
			if(!requestQueue.isEmpty()) {
				current_grant.set(requestQueue.poll());
				send(newMessage(TYPE.GRANT), current_grant.get().process);
				granted.set(true);
			}
			break;
		case POSTPONED:
			postponed.set(true);
			break;
		default:
			logerr(String.format("Unhandled message %s", m.toString()));
		}
	}
	
	public void sendRequestForCS() {
		loginfo("I would like to enter my CS");
		no_grants.set(0);
		multicastNewMessage(TYPE.REQUEST);
	}
	
	/**
	 * Multicast a message to all processes in the request set
	 * @param m the message
	 */
	public void multicast(final Message m) {
		loginfo(String.format("Sending to request set %s message %s", Arrays.toString(requestSet.toArray()) , m.toString()));
		// Broadcast the message to every process (including this process)
		for(final Integer i : requestSet) {
			send(m, i, false);
		}
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
					((Process_RMI)rmireg.lookup(processrmimap.get(proc_id))).receive(m);
				} catch (RemoteException | NotBoundException e) {
					logerr(String.format("Could not send %s to %d", m, proc_id));
					e.printStackTrace();
				}
			}
		}.start();
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
	
	private AtomicBoolean inCS = new AtomicBoolean(false);
	private void executeCriticalSection() {
		inCS.set(true);
		loginfo("Entering CS");
		randomDelay(500,5000);
		loginfo("Done with CS");
		inCS.set(false);
	}
	
	public String status() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(String.format("Status of Process %d:\n", this.process_id));
		sb.append(String.format("\tgranted: %b\n", this.granted.get()));
		sb.append(String.format("\tcurrent_grant: %s\n", this.current_grant.get()));
		sb.append(String.format("\tno_grants: %d\n", this.no_grants.get()));
		sb.append(String.format("\tin CS: %b\n", this.inCS.get()));
		sb.append(String.format("\tinquiring: %b\n", this.inquiring.get()));
		sb.append(String.format("\tpostponed: %b\n", this.postponed.get()));
		sb.append(String.format("\tRequest queue: %s\n", this.requestQueue.toString()));
		
		return sb.toString();
	}
	
	public String toString() {
		return String.format("P_%d[%d]", process_id, clock);
	}
}
