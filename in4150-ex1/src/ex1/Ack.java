package ex1;


/**
 * Acknowlegdement of a message
 */
// Ack could extends Message, but I prefer to keep Ack's strictly seperated so there's no change an incorrect check will *** 
public class Ack extends Message {
	private static final long serialVersionUID = -8689824778751383790L;
	
	/**
	 * Acknowledging message sender
	 */
	public final int ackmsg_process;
	/**
	 * Acknowledging message time
	 */
	public final int ackmsg_time;

	
	public Ack(int sender_process, int time, int ackmsg_process, int ackmsg_time) {
		super(sender_process, time);
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
