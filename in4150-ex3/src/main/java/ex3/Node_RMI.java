package ex3;

import java.rmi.Remote;

public interface Node_RMI extends Remote {
    /**
     * Send a message to this process
     * @param m the message to send to this process
     */
    public void receive(Message m, PROCESS_TYPE target) throws java.rmi.RemoteException;

    enum PROCESS_TYPE {
        CP,
        OP,
        BOTH
    }
}