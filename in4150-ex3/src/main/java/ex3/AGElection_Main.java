package ex3;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

/**
 * Main class for Distributed algorithms ex. 1. Initiates processes and
 * maintains Node registry
 */
public class AGElection_Main {
    private static final int RMI_PORT = 1099;
    private static final String RMI_HOST = "localhost";
    private final static int default_num_node = 3;
    private final int num_nodes;
    private Registry reg;
    private Map<Integer, Node> nodemap = new HashMap<>();
    private Map<Integer, String> nodermimap = new HashMap<>();
    private boolean running = false;

    public AGElection_Main(int num_nodes) {
        this.num_nodes = num_nodes;
    }

    public static void main(String... args) throws InterruptedException {
        if (args.length > 1) {
            System.out.println("AGELection_Main [num_nodes=3]");
            return;
        }
        int num_node = default_num_node;
        try {
            num_node = Integer.parseInt(args[0]);
        } catch (Exception e) {
            System.err.println("Invalid number of nodes, using the default of " + default_num_node);
        }

        new AGElection_Main(num_node).run();
    }

    private static Registry createRegistry() {
        // Create and install a security manager if necessary
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        Registry reg = null;
        // Create registry if neccesary
        if (reg == null) {
            try {
                //reg = LocateRegistry.createRegistry(RMI_PORT);
                reg = LocateRegistry.createRegistry(RMI_PORT);
            } catch (Exception e) {
                // Registry already exists
                System.err.println("A RMI Registery could not be created");
                e.printStackTrace();
            }
        }
        if(reg == null) {
            try {
                //reg = LocateRegistry.getRegistry(RMI_PORT);
                reg = LocateRegistry.getRegistry();
            } catch (RemoteException e) {
                System.err.println("A RMI Registery could not be obtained or created");
                e.printStackTrace();
            }
        }

        return reg;
    }

    public void run() throws InterruptedException {
        if ((reg = createRegistry()) == null) {
            System.exit(1);
        }

        // create the local processes
        for (int i = 1; i <= num_nodes; i++) {
            createLocalNode(i);
        }

        System.out.println("Press help to show commands\nProcesses started, now accepting commands:");
        Scanner in = new Scanner(System.in);
        try {
            console:
            while (true) {
                String line = in.nextLine();
                switch (line.split(" ")[0]) {
                    case "exit":
                        break console;
                    case "random":
                        Random r = new Random();
                        while (true) {
                            letProcessStartElection(r.nextInt(num_nodes) + 1);
                            Thread.sleep(1000);
                        }
                    case "status":
                        String second = null;
                        try {
                            second = line.split(" ")[1];
                        } catch (ArrayIndexOutOfBoundsException e) {
                            System.err.println("status (all|process number)");
                            continue;
                        }
                        if (second.equals("all")) {
                            for (Node p : nodemap.values()) {
                                System.out.println(p.status());
                            }
                        } else {
                            System.out.println(nodemap.get(Integer.parseInt(second)).status());
                        }
                        break;
                    case "help":
                        System.out.println("You really need to put the commands here\n"
                                + "exit: stop");
                        break;
                    default:
                        if (!nodemap.containsKey(Integer.parseInt(line))) {
                            System.out.println("Not a local process");
                            continue;
                        }
                        try {
                            letProcessStartElection(Integer.parseInt(line));
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid number");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                }
            }
        } finally {
            in.close();
            stop();
        }
    }

    private void letProcessStartElection(final int i) {
        new Thread(() -> {
            nodemap.get(i).startElection();
        }).start();
    }

    private void createLocalNode(int pid) {
        String rmiid = "rmi://" + RMI_HOST + ":" + RMI_PORT + "/p_" + pid;
        try {
            nodermimap.put(pid, rmiid);
            Map<Integer, String> reqSet = new HashMap<>();
            Node p = new Node(pid, nodermimap, reg);
            nodemap.put(pid, p);
            reg.bind(rmiid, p);
        } catch (RemoteException | AlreadyBoundException e) {
            System.err.println("Error registering process " + pid + " to RMI registry");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void stop() {
        running = false;
        // Stop processes
        for (Node p : nodemap.values()) {
            p.stop();
        }
        // Unbind RMI
        try {
            for (String rmi_p : reg.list()) {
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

    @Override
    protected void finalize() {
        if (running) {
            stop();
        }
    }
}
