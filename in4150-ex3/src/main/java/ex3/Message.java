package ex3;

import java.io.Serializable;

/**
 * Message. Comparable on total ordering time.
 */
public class Message implements Serializable {
	private static final long serialVersionUID = 569548713387898197L;
	
	/**
	 * Sending process id
	 */
	public final int process;

    /**
     * Level of (level, id) content of message
     */
    public final int level;
    /**
     * ID of (level, id) content of message
     */
    public final int id;

	/**
	 * Create a new message
	 * 
	 * @param sender_process
	 *            sending process
	 */
	public Message(int sender_process, int level, int id) {
		this.process = sender_process;
		this.level = level;
        this.id = id;
	}
	
	public String toString() {
		return String.format("(%d,%d)[p:%d]", level, id, process);
	}
}
