package ex3;

import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

/**
 * Main class for Distributed algorithms ex. 1. Initiates processes and
 * maintains Node registry
 */
public class AGElection_Main {
	private static final int RMI_PORT = 1099;
	private Registry reg;

	private Map<Integer, Node> nodemap = new HashMap<>();
	private Map<Integer, String> nodermimap = new HashMap<>();
	
	private boolean running = false;

	private final static int default_num_node = 3;
	
	private final int num_nodes;
	
    public static void main(String... args) throws InterruptedException {
    	if(args.length > 1) {
    		System.out.println("AGELection_Main [num_nodes=3]");
    		return;
    	}
    	int num_node = default_num_node;
    	try{
    		num_node = Integer.parseInt(args[0]);
    	}catch(Exception e) {
    		System.err.println("Invalid number of nodes, using the default of " + default_num_node);
    	}

    	new AGElection_Main(num_node).run();
	}

    public AGElection_Main(int num_nodes) {
    	this.num_nodes = num_nodes;
    }
    
	public void run() throws InterruptedException {
		if ((reg = createRegistry()) == null) {
			System.exit(1);
		}
		
		// create the local processes
		for(int i=1;i<= num_nodes;i++) {
			createLocalNode(i);
		}
		
		System.out.println("Press help to show commands\nProcesses started, now accepting commands:");
		Scanner in = new Scanner(System.in);
		console: while(true) {
			String line = in.nextLine();
			switch(line.split(" ")[0]) {
			case "exit":
				break console;
			case "random":
				Random r = new Random();
				while(true) {
					letProcessStartElection(r.nextInt(num_nodes) + 1);
					Thread.sleep(1000);
				}
			case "status":
				String second = line.split(" ")[1];
				if(second.equals("all")){
					for(Node p : nodemap.values()) {
						System.out.println(p.status());
					}
				} else {
					System.out.println(nodemap.get(Integer.parseInt(second)).status());
				}
				break;
			case "help":
				System.out.println("[proc_id]: Make proc_id request CS access\n"
						+ "status ([proc_id]|all): get status of proc_id or all processes\n"
						+ "test[n]: execute test n\n"
						+ "random: Make a random process request CS access every second\n"
						+ "exit: stop");
				break;
			default:
				if(!nodemap.containsKey(Integer.parseInt(line))) {
					System.out.println("Not a local process");
					continue;
				}
				try {
					letProcessStartElection(Integer.parseInt(line));
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
	private void letProcessStartElection(final int i) {
        new Thread(() -> { nodemap.get(i).startElection(); }).start();
	}
	
	private void createLocalNode(int pid) {
		String rmiid = "rmi://localhost:" + RMI_PORT + "/p_" + pid;
		try {
            nodermimap.put(pid, rmiid);
            Map<Integer, String> reqSet = new HashMap<>();
            Node p = new Node(pid, nodermimap, reg);
            nodemap.put(pid, p);
            reg.bind(rmiid, p);
        } catch (RemoteException |AlreadyBoundException e) {
            System.err.println("Error registering process " + pid + " to RMI registry");
            e.printStackTrace();
            System.exit(1);
        }
	}

    private void stop() {
		running = false;
		// Stop processes
		for(Node p : nodemap.values()) {
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
	
	private static Registry createRegistry() {
        // Create and install a security manager if necessary
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
