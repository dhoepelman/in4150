package ex1;

import java.io.Serializable;

public class Message implements Serializable, Comparable<Message> {
	private static final long serialVersionUID = 5695487132387898197L;
	
	/**
	 * Sending process id
	 */
	public final int sender_process;
	/**
	 * Value of clock in sender process
	 */
	public final int sender_time;

	/**
	 * Create a new message
	 * 
	 * @param sender_process
	 *            sending process
	 * @param time
	 *            clock value of sending process
	 */
	public Message(int sender_process, int time) {
		this.sender_process = sender_process;
		this.sender_time = time;
	}

	@Override
	public int compareTo(Message arg0) {
		int d = sender_time - arg0.sender_time;

		if (d == 0) {
			// Equal time, use process id as tie-breaker
			d = sender_process - arg0.sender_process;
		}

		return d;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + sender_process;
		result = prime * result + sender_time;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Message other = (Message) obj;
		if (sender_process != other.sender_process)
			return false;
		if (sender_time != other.sender_time)
			return false;
		return true;
	}
	
	public String toString() {
		return String.format("MSG[%d,%d]", sender_process, sender_time);
	}
}
