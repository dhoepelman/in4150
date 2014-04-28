package ex3;

import java.rmi.Remote;

public interface Node_RMI extends Remote {
    /**
     * Send a message to this process
     * @param m the message to send to this process
     */
	public void receive(Message m) throws java.rmi.RemoteException;
}