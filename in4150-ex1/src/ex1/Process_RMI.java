package ex1;

import java.rmi.Remote;

public interface Process_RMI extends Remote {
	public void receive(Message m) throws java.rmi.RemoteException;
	public void receive(Ack ack) throws java.rmi.RemoteException;
}