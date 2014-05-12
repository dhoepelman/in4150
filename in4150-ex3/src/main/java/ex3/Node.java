package ex3;

import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Node extends UnicastRemoteObject implements Node_RMI {
	private static final long serialVersionUID = -2731859365280472117L;
    /////////////////////////
    // Utility methods
    /////////////////////////
    private static final int mindelay = 100;
    private static final int maxdelay = 200; // TODO: Better delays
    /**
     * Node owner_id
     */
	public final int node_id;
    private final Logger log = Logger.getLogger("Node");
    /**
	 * Map of all processes in the request set and their RMI strings
	 */
	private final Map<Integer, String> nodeRMIMap;
	/**
     * RMI registry
     */
    private final Registry rmireg;
    /**
     * Candidate Process
     */
    private Candidate_Process cp = null;
    /**
     * Ordinary Process
     */
    private Process op = null;

    /**
     * Make a process with a specific process owner_id
     *
     * @param nodeRMIMap Map with RMI strings of all nodes
     */
    public Node(int node_id, Map<Integer, String> nodeRMIMap, Registry r) throws RemoteException {
        this.node_id = node_id;
        this.nodeRMIMap = nodeRMIMap;
        this.rmireg = r;
    }

    /////////////////////////
    // Algorithm related methods
    ////////////////////////

    /**
     * Create a new message
     */
    private Message newMessage(int level, int id) {
        return new Message(node_id, level, id);
    }

    @Override
    public void receive(Message m) {
        loginfo("Received " + m.toString());
        if (op == null) {
            op = new Process();
        }
        op.process(m);
        if (cp != null) {
            cp.process(m);
        }
    }

    public void send(final Message m, final int proc_id) {
        send(m, proc_id, true);
    }

    /**
     * Send a message to a process
     *
     * @param m The message to send
     */
    public void send(final Message m, final int proc_id, boolean log) {
        if (log) {
            loginfo(String.format("Sending to process %d message %s", proc_id, m.toString()));
        }
        new Thread() {
            public void run() {
                try {
                    randomDelay(mindelay, maxdelay);
                    ((Node_RMI) rmireg.lookup(nodeRMIMap.get(proc_id))).receive(m);
                } catch (RemoteException | NotBoundException e) {
                    logerr(String.format("Could not send %s to %d", m, proc_id));
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Make this node start a election
     */
    public void startElection() {
        if (cp != null) {
            logerr("Cannot start election, there is already a candidate process");
        }
        cp = new Candidate_Process(nodeRMIMap.keySet(), this.node_id);
        cp.elect();
        if (!cp.done()) {
            logwarn("CP stopped before it was done, eh...?");
        } else {
            if (cp.elected()) {
                loginfo("CP was elected");
            } else {
                loginfo("CP was not elected");
            }
        }
    }

    private void randomDelay(int min, int max) {
        try {
            Thread.sleep(new Random().nextInt(max - min) + min);
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
        log.log(lvl, String.format("P_%d: %s", node_id, msg));
    }

    public void stop() {
        try {
            unexportObject(this, true);
        } catch (NoSuchObjectException e) {
            e.printStackTrace();
        }
    }

    public String status() {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("Status of Node %d:\n", this.node_id));

        // TODO: Status

        return sb.toString();
    }

    public String toString() {
        return String.format("P_%d", node_id);
    }

    /**
     * A class for the ordinary processes
     */
    public class Process {
        // Algorithm status variables
        private int level = 0;
        /**
         * owner_id of the owner of this Ordinary process
         */
        private int owner_id = 0;
        /**
         * RMI string of the owning link/node
         */
        private Integer owner = null;
        private Integer potential_owner = null;

        public synchronized void process(Message m) {
            int level_ = m.level;
            int id_ = m.id;
            int link_ = m.link;
            // Check if message (level,id) is smaller than (level,owner_id)
            if (level_ < level || (level_ == level && id_ < owner_id)) {
                // New message (level,owner_id) is smaller, ignore
                // CHeck if message (level,id) is larger than (level,owner_id)
            } else if (level_ > level || (level_ == level && id_ > owner_id)) {
                loginfo(String.format("Captured by %d", id_));
                // We have been captured
                // Take potential new owner
                potential_owner = link_;
                level = level_;
                owner_id = id_;
                if (owner == null) {
                    owner = potential_owner;
                } else {
                    loginfo(String.format("Killing %d", owner));
                }
                // Kill previous owner, or ack new owner
                send(newMessage(level_, id_), owner);
            } else {
                // (level,id) == (level,owner_id), ack from previous father
                assert level_ == level && id_ == owner_id;
                // change father
                owner = potential_owner;
                // ack new father
                send(newMessage(level_, id_), owner);
            }
        }
    }

    public class Candidate_Process {
        private final int id;
        private final Set<Integer> untraversed_links;
        private int level = 0;
        private boolean killed = false;
        private Lock messageLock = new ReentrantLock();

        // while !untraved_linkes.isEmpty() && !killed
        private Condition messageArrival = messageLock.newCondition();

        /**
         * Create a new candidate process
         *
         * @param all_nodes The collection of all nodes/links in the system
         */
        public Candidate_Process(Set<Integer> all_nodes, int node_id) {
            this.untraversed_links = new HashSet<>(all_nodes);
            this.id = node_id;
        }

        public boolean elected() {
            return untraversed_links.isEmpty() && !killed;
        }

        public boolean done() {
            return killed || untraversed_links.isEmpty();
        }

        public void elect() {
            while (!done()) {
                Iterator<Integer> it = untraversed_links.iterator();
                int link = it.next();
                loginfo(String.format("Trying to capture %d", link));
                send(newMessage(level, id), link);
                try {
                    messageLock.lock();
                    messageArrival.await();
                } catch (InterruptedException e) {
                    logwarn("Was interrupted while candidate process was waiting for response");
                } finally {
                    messageLock.unlock();
                }
            }
        }

        public synchronized void process(Message m) {
            int level_ = m.level;
            int id_ = m.id;
            int link_ = m.link;
            if (id == id_ && !killed) {
                loginfo(String.format("Captured %d", link_));
                level++;
                untraversed_links.remove(link_);
                messageLock.lock();
                messageArrival.signal();
            } else {
                if (level_ < level || (level_ == level && id_ < id)) { // (level',owner_id') <  (level,owner_id)
                    // ignore
                } else {
                    loginfo(String.format("Killed by %d, which is now owned by %d", link_, id_));
                    killed = true;
                    send(newMessage(level_, id_), link_);
                    messageLock.lock();
                    messageArrival.signal();
                }
            }
        }
    }


}
