package ex3;

import com.google.common.primitives.Ints;

import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

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
        try {
            // Create registry if neccesary
            reg = LocateRegistry.createRegistry(RMI_PORT);
        } catch (Exception e) {
            // Registry already exists
            //System.err.println("A RMI Registery could not be created");
            //e.printStackTrace();
            try {
                //reg = LocateRegistry.getRegistry(RMI_PORT);
                reg = LocateRegistry.getRegistry();
            } catch (RemoteException e2) {
                e.printStackTrace();
                System.err.println("A RMI registry could not be created");
                e2.printStackTrace();
                System.err.println("A RMI Registry could not be obtained");
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
        try (Scanner in = new Scanner(System.in)) {
            console:
            while (true) {
                String line = in.nextLine();
                switch (line.split(" ")[0]) {
                    case "exit":
                        break console;
                    case "random":
                        Random r = new Random();
                        for (int i = 120; i > 0; i--) {
                            letProcessStartElection(r.nextInt(num_nodes) + 1);
                            Thread.sleep(1000);
                        }
                        break;
                    case "concurrent-1":
                        letProcessStartElection(1, Arrays.asList(new Integer[]{1,2,3}));
                        letProcessStartElection(2, Arrays.asList(new Integer[]{1,2,3}));
                        break;
                    case "concurrent-2":
                        letProcessStartElection(2, Arrays.asList(new Integer[]{1,2,3}));
                        Thread.sleep(50);
                        letProcessStartElection(1, Arrays.asList(new Integer[]{1,2,3}));
                        break;
                    case "clash":
                        letProcessStartElection(1, Arrays.asList(new Integer[]{1,2,3}));
                        letProcessStartElection(2, Arrays.asList(new Integer[]{3,2,1}));
                        break;
                    case "slow-6":
                        letProcessStartElection(5);
                        Thread.sleep(2000);
                        letProcessStartElection(6);
                        break;
                    case "status":
                        String second;
                        try {
                            second = line.split(" ")[1];
                        } catch (ArrayIndexOutOfBoundsException e) {
                            System.err.println("status (all|process number)");
                            continue;
                        }
                        if (second.equals("all")) {
                            for (Node p : nodemap.values()) {
                                System.out.print(p.status());
                            }
                        } else {
                            System.out.println(nodemap.get(Integer.parseInt(second)).status());
                        }
                        break;
                    case "help":
                        System.out.println("You really need to put the commands here\n"
                                + "exit: stop");
                        break;
                    case "reset":
                        stop();
                        run();
                        break console;
                    default:
                        Integer i = Ints.tryParse(line);
                        if (i == null || !nodemap.containsKey(i)) {
                            System.out.println("Typo or not a local process");
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
            stop();
        }
    }

    private void letProcessStartElection(final int i, List<Integer> order) {
        new Thread(() -> nodemap.get(i).startElection(order)).start();
    }

    private void letProcessStartElection(final int i) {
        letProcessStartElection(i, null);
    }

    private void createLocalNode(int pid) {
        String rmiid = "rmi://" + RMI_HOST + ":" + RMI_PORT + "/p_" + pid;
        try {
            nodermimap.put(pid, rmiid);
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
                } catch (NoSuchObjectException e) {
                    // Already unbound
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
    protected void finalize() throws Throwable {
        super.finalize();
        if (running) {
            stop();
        }
    }
}
