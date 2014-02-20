package ex1;

import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
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

	public static void main(String... args) {
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

	public void run() {
		if (!createRegistery()) {
			System.exit(1);
		}
		// Create and install a security manager
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new RMISecurityManager());
		}
		// Create all processes
		Map<Integer, String> processrmimap = new HashMap<>();
		// Read only view for the processes
		Map<Integer, String> ropmap = Collections.unmodifiableMap(processrmimap);
		for(int i=0;i<param_numproc;i++) {
			// 2^12 = 8096 is enough for a sample exercise like this
			// In the real-world you'd do some resolution to enforce unique ID's
			int pid = new Random().nextInt(1<<12);	
			String rmiid = "rmi://" + RMI_HOST + ":" + RMI_PORT + "/p_" + pid;
			processrmimap.put(pid, rmiid);
			Process p = new Process(pid, 0);
			p.setProcesses(processrmimap);
			processmap.put(pid, p);
		}
	}

	private boolean createRegistery() {
		try {
			reg = java.rmi.registry.LocateRegistry.createRegistry(RMI_PORT);
			return true;
		} catch (RemoteException e) {
			System.err.println("Please run rmiregistery command first");
			e.printStackTrace();
			return false;
		}
	}	
}
