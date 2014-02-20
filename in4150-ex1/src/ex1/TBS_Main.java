package ex1;

import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

/**
 * Main class for Distributed algorithms ex. 1. Initiates processes and
 * maintains Process registery
 */
public class TBS_Main {
	private static final int RMI_PORT = 1099;
	private static final String RMI_HOST = "localhost";
	private Registry reg;
	
	private int param_numproc = 3;
	
	private Map<Integer, Process> processmap = new HashMap<>();
	private Map<Integer, String> processrmimap = new HashMap<>();
	
	private boolean running = false;

	public static void main(String... args) throws InterruptedException {
		if(args.length == 0) {
			System.out.println("TBS_Main #proc");
			return;
		}
		TBS_Main prog = new TBS_Main();
		try {
			prog.param_numproc = Integer.parseInt(args[0]);
		} catch (NumberFormatException e) {
			System.err.println("Param #proc must be an integer");
			return;
		}
		prog.run();
	}

	public void run() throws InterruptedException {
		if (!createRegistery()) {
			System.exit(1);
		}
		// Create and install a security manager
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new RMISecurityManager());
		}
		// Create all processes
		// Read only view for the processes
		Map<Integer, String> ro_pmap = Collections.unmodifiableMap(processrmimap);
		int pid = 0;
		for(int i=0;i<param_numproc;i++) {
			// 2^12 = 8096 is enough for a sample exercise like this
			// In the real-world you'd do some resolution to enforce unique ID's
			//int pid = new Random().nextInt(1<<12);	
			pid++;
			try {
				String rmiid = "rmi://" + RMI_HOST + ":" + RMI_PORT + "/p_" + pid;
				processrmimap.put(pid, rmiid);
				Process p = new Process(pid, 0);
				p.setProcesses(ro_pmap);
				p.setRMI(reg);
				processmap.put(pid, p);
				reg.bind(rmiid, p);
			} catch (RemoteException|AlreadyBoundException e) {
				System.err.println("Error registering process " + pid + " to RMI registery");
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		Thread.sleep(1000);

		processmap.get(1).sendNewMessage();
		
		Thread.sleep(2000);
		
		processmap.get(2).sendNewMessage();
		
		stop();
	}

	private void stop() {
		running = false;
		// Stop processes
		for(Process p : processmap.values()) {
			p.stop();
		}
		// Unbind RMI
		try {
			for(String rmi_p : reg.list()) {
				try {
					reg.unbind(rmi_p);
				} catch (RemoteException | NotBoundException e) {
					System.err.println("Could not unbind " + rmi_p);
				}
			}
		} catch (RemoteException e) {
			System.err.println("Could not access RMI registry to stop processes");
			e.printStackTrace();
		}
	}
	
	private boolean createRegistery() {
		try {
			reg = LocateRegistry.createRegistry(RMI_PORT);
			return true;
		} catch (RemoteException e) {
			System.err.println("Please run rmiregistery command first");
			e.printStackTrace();
			return false;
		}
	}	
	
	@Override
	protected void finalize() {
		if(running) {
			stop();
		}
	}
}
