package ex2;

import java.rmi.Remote;

public interface Process_RMI extends Remote {
    /**
     * Send a message to this process
     * @param m the message to send to this process
     */
	public void receive(Message m) throws java.rmi.RemoteException;
}