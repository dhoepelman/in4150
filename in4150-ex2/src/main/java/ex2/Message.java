package ex2;

import java.io.Serializable;

public class Message implements Serializable {
	private static final long serialVersionUID = -6731341631469577605L;
	
	/**
	 * Sending process id
	 */
	public final int sender_process;
	/**
	 * Value of clock in sender process
	 */
	public final int sender_time;
	/**
	 * Type of the message
	 */
	public final TYPE type;
	
	/**
	 * Create a new message
	 * 
	 * @param sender_process
	 *            sending process
	 * @param time
	 *            clock value of sending process
	 */
	public Message(int sender_process, int time, TYPE t) {
		this.sender_process = sender_process;
		this.sender_time = time;
		this.type = t;
	}
	
	public String toString() {
		return String.format("%s[%d,%d]", type.toString() , sender_process, sender_time);
	}
	
	public enum TYPE {
		REQUEST,
		GRANT,
		POSTPONED,
		INQUIRE,
		RELINQUISH
	}
}
