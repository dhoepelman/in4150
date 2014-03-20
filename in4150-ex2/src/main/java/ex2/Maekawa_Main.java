package ex2;

import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Main class for Distributed algorithms ex. 1. Initiates processes and
 * maintains Process registry
 */
public class Maekawa_Main {
	private static final int RMI_PORT = 1099;
	private Registry reg;

	private Map<Integer, Process> processmap = new HashMap<>();
	private Map<Integer, String> processrmimap = new HashMap<>();
	
	private boolean running = false;
	
	private final static int default_num_proc = 3;
	
	private final int num_proc;
	
    public static void main(String... args) throws InterruptedException {
    	if(args.length > 1) {
    		System.out.println("Maekawa_Main [num_processes=3]");
    		return;
    	}
    	int num_proc = default_num_proc;
    	try{
    		num_proc = Integer.parseInt(args[0]);
    	}catch(Exception e) {
    		System.err.println("Invalid number of processes, using the default of " + default_num_proc);
    	}
    	if(num_proc != 3 && num_proc != 7) {
    		System.err.println("Warning: optimal request sets only known for n=3 and n=7");
    	}
    	new Maekawa_Main(num_proc).run();
	}

    public Maekawa_Main(int num_processes) {
    	this.num_proc = num_processes;
    }
    
	public void run() throws InterruptedException {
		if ((reg = createRegistery()) == null) {
			System.exit(1);
		}
		
		// create the local processes
		for(int i=1;i<=num_proc;i++) {
			createLocalProcess(i);
		}
		
		System.out.println("Processes started, now accepting commands:");
		Scanner in = new Scanner(System.in);
		console: while(true) {
			String line = in.nextLine();
			switch(line.split(" ")[0]) {
			case "exit":
				break console;
			case "test1":
				// 2 processes enter CS at the same time
				letProcessEnterCS(1);
				letProcessEnterCS(2);
				break;
			case "status":
				String second = line.split(" ")[1];
				if(second.equals("all")){
					for(Process p : processmap.values()) {
						System.out.println(p.status());
					}
				} else {
					System.out.println(processmap.get(Integer.parseInt(second)).status());
				}
				break;
			default:
				if(!processmap.containsKey(Integer.parseInt(line))) {
					System.out.println("Not a local process");
					continue;
				}
				try {
					letProcessEnterCS(Integer.parseInt(line));
				}
				catch(NumberFormatException e) {
					System.out.println("Invalid number");
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
		in.close();
		stop();
	}
	private void letProcessEnterCS(final int i) {
		new Thread() {
			public void run() {
				processmap.get(i).sendRequestForCS();
			}
		}.start();
	}
	
	private void createLocalProcess(int pid) {
		String rmiid = "rmi://localhost:" + RMI_PORT + "/p_" + pid;
		try {
            processrmimap.put(pid, rmiid);
            Map<Integer, String> reqSet = new HashMap<>();
            Process p = new Process(pid, processrmimap, reg, createRequestSet(pid));
            processmap.put(pid, p);
            reg.bind(rmiid, p);
        } catch (RemoteException |AlreadyBoundException e) {
            System.err.println("Error registering process " + pid + " to RMI registery");
            e.printStackTrace();
            System.exit(1);
        }
	}
	
	// N -> (proc_i -> {a,b,c})
	private static final Map<Integer, Map<Integer, Collection<Integer>>> optReqSet = ImmutableMap.<Integer, Map<Integer, Collection<Integer>>>builder()
		.put(3, ImmutableMap.<Integer, Collection<Integer>>builder()
				.put(1, ImmutableList.of(1, 2))
				.put(2, ImmutableList.of(2, 3))
				.put(3, ImmutableList.of(1, 3))
				.build())
		.put(7, ImmutableMap.<Integer, Collection<Integer>>builder()
				.put(1, ImmutableList.of(1, 2, 3))
				.put(2, ImmutableList.of(2, 4, 6))
				.put(3, ImmutableList.of(3, 5, 6))
				.put(4, ImmutableList.of(1, 4, 5))
				.put(5, ImmutableList.of(2, 5, 7))
				.put(6, ImmutableList.of(1, 6, 7))
				.put(7, ImmutableList.of(3, 4, 7))
				.build()
	).build();
	
	private Collection<Integer> createRequestSet(int proc_i) {
		return optReqSet.get(num_proc).get(proc_i);
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
	
	private static Registry createRegistery() {
        // Create and install a security manager if neccesary
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
	    }
		
        Registry reg;
        // Create registry if neccesary
		try {
			LocateRegistry.createRegistry(RMI_PORT);
        } catch(Exception e) {
            // Registry already exists
        	System.out.println("A RMI Registery was already running");
        }
        try {
            reg = LocateRegistry.getRegistry(RMI_PORT);
		} catch (RemoteException e) {
			System.err.println("Could not obtain or create RMI registry");
			e.printStackTrace();
			return null;
		}
        
        return reg;
	}	
	
	@Override
	protected void finalize() {
		if(running) {
			stop();
		}
	}
}
