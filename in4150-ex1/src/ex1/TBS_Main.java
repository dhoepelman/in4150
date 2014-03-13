package ex1;

import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

import ex1.testcases.TestCase1;

/**
 * Main class for Distributed algorithms ex. 1. Initiates processes and
 * maintains Process registry
 */
public class TBS_Main {
	private static final int RMI_PORT = 1099;
	private Registry reg;

	private Map<Integer, Process> localprocessmap = new HashMap<>();
	private Map<Integer, String> processrmimap = new HashMap<>();
	
	private boolean running = false;

	private final String remotehost;
	private final boolean evenprocessnumbers;
	
	private final static int num_proc = 3;
	
    public static void main(String... args) throws InterruptedException {
    	if(args.length != 2) {
    		System.out.println("TBM_Main remote even_process_numbers={0,1}");
    		return;
    	}
		new TBS_Main(args[0], Integer.parseInt(args[1])==1).run();
	}

    public TBS_Main(String remote, boolean even) {
    	this.remotehost = remote;
    	this.evenprocessnumbers = even;
    }
    
	public void run() throws InterruptedException {
		if ((reg = createRegistery()) == null) {
			System.exit(1);
		}
    
		int startpid;
		int startremotepid;
		if(evenprocessnumbers){
			startpid = 0;
			startremotepid = 1;
		} else {
			startpid = 1;
			startremotepid = 0;
		}
		
		// create the local processes
		for(int i=0;i<num_proc;i++) {
			// Create processes {0,2,4,...} or {1,3,5,...}
			createLocalProcess(startpid+i);
			//createLocalProcess(startpid+2*i);
			// bind the other (remote) processes
			//bindRemoteProcess(startremotepid+2*i);
		}
		
		System.out.println("Processes started, now accepting commands:");
		Scanner in = new Scanner(System.in);
		console: while(true) {
			String line = in.nextLine();
			switch(line) {
			case "exit":
				break console;
			case "reset":
				for(Process p : localprocessmap.values()) {
					p.setClock(0);
				}
				break;
			case "test1":
				TestCase1 tc1 = new TestCase1(localprocessmap);
				setTarget(tc1);
				tc1.start();
				break;
			default:
				try {
					localprocessmap.get(Integer.parseInt(line)).sendNewMessage();
				}
				catch(NumberFormatException e) {
					System.out.println("Invalid number");
				}
				catch(NullPointerException e) {
					System.out.println("Not a local process");
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
		in.close();
		stop();
	}

	private void setTarget(Target t) {
		for(Process p: localprocessmap.values()) {
			p.setTarget(t);
		}
	}
	
	private void createLocalProcess(int pid) {
		String rmiid = "rmi://localhost:" + RMI_PORT + "/p_" + pid;
		try {
            processrmimap.put(pid, rmiid);
            Process p = new Process(pid,  Collections.unmodifiableMap(processrmimap), reg);
            localprocessmap.put(pid, p);
            reg.bind(rmiid, p);
        } catch (RemoteException |AlreadyBoundException e) {
            System.err.println("Error registering process " + pid + " to RMI registery");
            e.printStackTrace();
            System.exit(1);
        }
	}
	
	private void bindRemoteProcess(int pid) {
		processrmimap.put(pid, "rmi://" + remotehost + ":" + RMI_PORT + "/p_" + pid);
	}

    private void stop() {
		running = false;
		// Stop processes
		for(Process p : localprocessmap.values()) {
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
