package ex2;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Component_RMI extends Remote {
	public void receive(Message m) throws RemoteException;
}
