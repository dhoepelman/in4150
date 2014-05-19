package ex3;

import java.io.Serializable;

/**
 * Message. Comparable on total ordering time.
 */
public class Message implements Serializable, Comparable<Message> {
    private static final long serialVersionUID = 569548713387898198L;

    /**
     * Sender node/link of the message
     */
    public final int link;

    public final int level;
    public final int id;

    /**
	 * Create a new message
	 *
	 */
	public Message(int link, int level, int id) {
        this.link = link;
        this.level = level;
        this.id = id;
	}
	
	public String toString() {
        return String.format("(level=%d,id=%d)", level, id);
    }

    @Override
    public int compareTo(Message o) {
        int res = Integer.compare(level, o.level);
        if(res == 0) {
            res = Integer.compare(id, o.id);
        }
        return res;
    }
}
