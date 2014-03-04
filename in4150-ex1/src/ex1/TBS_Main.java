package ex1;

import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

/**
 * Main class for Distributed algorithms ex. 1. Initiates processes and
 * maintains Process registery
 */
public class TBS_Main extends UnicastRemoteObject implements TBS_Main_RMI {
	private static final int RMI_PORT = 1099;
	private static final String RMI_HOST = "localhost";
	private Registry reg;
	
	private int param_numproc = 3;
    /**
     * All hosts of processes
     */
    private Collection<String> param_process_hosts = new ArrayList<>();
    private String param_logger_host = "localhost";
	
	private Map<Integer, Process> localprocessmap = new HashMap<>();
	private Map<Integer, String> processrmimap = new HashMap<>();
	
	private boolean running = false;

    protected TBS_Main() throws RemoteException {
    }

    public static void main(String... args) throws InterruptedException {
		if(args.length == 0) {
			System.out.println("TBS_Main #proc [host-logger [host-1 [host-2 [...]]]]");
			return;
		}
        TBS_Main prog;
        try {
		    prog = new TBS_Main();
            Registry reg = createRegistery();
            reg.bind("rmi://" + RMI_HOST + ":" + RMI_PORT + "/host", prog);
        }
        catch(RemoteException|AlreadyBoundException e) {
            System.err.println("We couldn't create ourselves...");
            return;
        }
		try {
			prog.param_numproc = Integer.parseInt(args[0]);
		} catch (NumberFormatException e) {
			System.err.println("Param #proc must be an integer");
			return;
		}
        // Add all other hosts from the cmd arguments
        if(args.length > 1) {
            prog.param_logger_host = args[1];
            for(int i=1;i<args.length;i++) {
                prog.param_process_hosts.add(args[i]);
            }
        }
		prog.run();
	}

	public void run() throws InterruptedException {
		if ((reg = createRegistery()) == null) {
			System.exit(1);
		}

        createProcesses();
        lookupProcesses();

        Thread.sleep(1000);

		localprocessmap.get(1).sendNewMessage();
		
		Thread.sleep(2000);
		
		localprocessmap.get(2).sendNewMessage();
		
		stop();
	}

    /**
     * Create the processes of this host
     */
    private void createProcesses() {
        // Create all processes
        // Read only view for the processes
        Map<Integer, String> ro_pmap = Collections.unmodifiableMap(processrmimap);
        int pid = 0;
        for(int i=0;i<param_numproc;i++) {
            // 2^12 = 8096 is enough for a sample exercise like this
            // In the real-world you'd do some resolution to enforce unique ID's
            pid = new Random().nextInt(1<<12);
            //pid++;
            try {
                String rmiid = "rmi://" + RMI_HOST + ":" + RMI_PORT + "/p_" + pid;
                processrmimap.put(pid, rmiid);
                Process p = new Process(pid, 0);
                p.setProcesses(ro_pmap);
                p.setRMI(reg);
                localprocessmap.put(pid, p);
                reg.bind(rmiid, p);
            } catch (RemoteException |AlreadyBoundException e) {
                System.err.println("Error registering process " + pid + " to RMI registery");
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    /**
     * Lookup processes from other hosts
     */
    private void lookupProcesses() {
        for(String host : param_process_hosts) {
            // Ignore Localhost
            if(!host.equals(RMI_HOST)) {
                try {
                    processrmimap.putAll(((TBS_Main_RMI) reg.lookup("rmi://" + host + ":1099/host")).getProcesses(host));
                } catch (RemoteException e) {
                    System.err.println("Could not get processes from host " + host);
                } catch (NotBoundException e) {
                    System.err.println("Could not find host " + host);
                }
            }
        }
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
        Registry reg;
        // Create registry if neccesary
		try {
			LocateRegistry.createRegistry(RMI_PORT);
        } catch(Exception e) {
            // Registry already exists
        }
        try {
            reg = LocateRegistry.getRegistry(RMI_PORT);
		} catch (RemoteException e) {
			System.err.println("Could not obtain or create RMI registry");
			e.printStackTrace();
			return null;
		}

        // Create and install a security manager if neccesary
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }
        return reg;
	}	
	
	@Override
	protected void finalize() {
		if(running) {
			stop();
		}
	}

    @Override
    public Map<Integer, String> getProcesses(String me) throws RemoteException {
        Map<Integer, String> copy = new HashMap<>();
        for(Map.Entry<Integer, String> e : processrmimap.entrySet()) {
            // Change localhost to whatever the other host calls us, e.g. "rmi://localhost" => "rmi://192.168.0.1"
            copy.put(e.getKey(), e.getValue().replace(RMI_HOST, me));
        }
        return copy;
    }
}
