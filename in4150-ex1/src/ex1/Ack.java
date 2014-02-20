package ex1;

import java.io.Serializable;

/**
 * Acknowlegdement of a message
 */
// Ack could extends Message, but I prefer to keep Ack's strictly seperated so there's no change an incorrect check will *** 
public class Ack implements Serializable {
	private static final long serialVersionUID = -8689824778751383790L;
	
	/**
	 * Sending process id
	 */
	public final int sender_process;
	/**
	 * Value of clock in sender process
	 */
	public final int sender_time;
	/**
	 * Acknowledging message sender
	 */
	public final int ackmsg_process;
	/**
	 * Acknowledging message time
	 */
	public final int ackmsg_time;

	
	public Ack(int sender_process, int time, int ackmsg_process, int ackmsg_time) {
		this.sender_process = sender_process;
		this.sender_time = time;
		this.ackmsg_process = ackmsg_process;
		this.ackmsg_time = ackmsg_time;
	}
	
	public String toString() {
		return String.format("ACK[%d,%d,%s]", sender_process, sender_time, getAckedMsg().toString());
	}
	
	public Message getAckedMsg() {
		return new Message(ackmsg_process, ackmsg_time);
	}
}
