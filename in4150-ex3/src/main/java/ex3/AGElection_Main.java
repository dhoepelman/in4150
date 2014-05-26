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
    private final Boolean even_nodes;
    private final String remote_host;
    private Registry reg;
    private Map<Integer, Node> nodemap = new HashMap<Integer, Node>();
    private Map<Integer, String> nodermimap = new HashMap<>();
    private boolean running = false;

    public AGElection_Main(int num_nodes, Boolean even_nodes, String remote_host) {
        this.num_nodes = num_nodes;
        this.even_nodes = even_nodes;
        this.remote_host = remote_host;
    }

    public static void main(String... args) throws InterruptedException {
        if (args.length > 2) {
            System.out.println("AGELection_Main [num_nodes=3] [even remote_host]");
            return;
        }
        int num_node = default_num_node;
        try {
            num_node = Integer.parseInt(args[0]);
        } catch (Exception e) {
            System.err.println("Invalid number of nodes, using the default of " + default_num_node);
        }
        Boolean even_nodes = null;
        String remote_host = null;
        if (args.length > 1) {
            try {
                even_nodes = Boolean.parseBoolean(args[1]);
                remote_host = args[2];
            } catch (Exception e) {
                System.err.println("Invalid boolean even, defaulting to all nodes");
            }
        }


        new AGElection_Main(num_node, even_nodes, remote_host).run();
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
            if (remote_host == null || i % 2 == (even_nodes ? 0 : 1)) {
                createLocalNode(i);
            } else {
                bindRemoteProcess(i);
            }
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
                        letProcessStartElection(1, Arrays.asList(new Integer[]{1, 2, 3}));
                        letProcessStartElection(2, Arrays.asList(new Integer[]{1, 2, 3}));
                        break;
                    case "concurrent-2":
                        letProcessStartElection(2, Arrays.asList(new Integer[]{1, 2, 3}));
                        Thread.sleep(50);
                        letProcessStartElection(1, Arrays.asList(new Integer[]{1, 2, 3}));
                        break;
                    case "clash":
                        letProcessStartElection(1, Arrays.asList(new Integer[]{1, 2, 3}));
                        letProcessStartElection(2, Arrays.asList(new Integer[]{3, 2, 1}));
                        break;
                    case "slow-6":
                        letProcessStartElection(5);
                        Thread.sleep(2000);
                        letProcessStartElection(6);
                        break;
                    case "concurrent-10":
                        letProcessStartElection(1);
                        letProcessStartElection(2);
                        letProcessStartElection(3);
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
                        System.out.println("status: show the status of all nodes and processes\n"
                                + "X: make node X start election\n"
                                + "{concurrent-1,concurrent-2,clash,slow-6,concurrent-10}: do this testcase"
                                + "reset: Reset all nodes"
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

    private void bindRemoteProcess(int pid) {
        nodermimap.put(pid, "rmi://" + remote_host + ":" + RMI_PORT + "/p_" + pid);
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
