package ex2;

import java.io.Serializable;

/**
 * Message. Comparable on total ordering time.
 */
public class Message implements Serializable, Comparable<Message> {
	private static final long serialVersionUID = 5695487132387898197L;
	
	/**
	 * Sending process id
	 */
	public final int process;
	/**
	 * Value of clock in sender process
	 */
	public final int time;
	
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
		this.process = sender_process;
		this.time = time;
		this.type = t;
	}

	@Override
	public int compareTo(Message arg0) {
		int d = time - arg0.time;

		if (d == 0) {
			// Equal time, use process id as tie-breaker
			d = process - arg0.process;
		}

		return d;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + process;
		result = prime * result + time;
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
		if (process != other.process)
			return false;
		if (time != other.time)
			return false;
		return true;
	}
	
	public String toString() {
		return String.format("%s[%d,%d]", type.toString(), process, time);
	}
	
	public enum TYPE {
		REQUEST,
		GRANT,
		RELEASE,
		RELINQUISH,
		INQUIRE,
		POSTPONED
	}
}
