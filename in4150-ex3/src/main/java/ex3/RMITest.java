package ex3;

import java.rmi.AlreadyBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

public class RMITest {
    private static final int RMI_PORT = 1099;
    private static final String RMI_HOST = "localhost";

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
                System.err.println("A RMI Registery could not be created or obtained");
                e.printStackTrace();
                return null;
            }
        }

        return reg;
    }

    private static Registry reg;

    public static void main(String... args) {
        if ((reg = createRegistry()) == null) {
            System.exit(1);
        }

        createLocalNode(1);
    }

    private static void createLocalNode(int pid) {
        String rmiid = "rmi://" + RMI_HOST + ":" + RMI_PORT + "/p_" + pid;
        try {
            Map<Integer, String> reqSet = new HashMap<>();
            RMIClass p = new RMIClass();
            reg.bind(rmiid, p);
        } catch (RemoteException | AlreadyBoundException e) {
            System.err.println("Error registering process " + pid + " to RMI registry");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static class RMIClass extends UnicastRemoteObject implements RMIClass_RMI {
        protected RMIClass() throws RemoteException {
            super();
        }

        @Override
        public void doSomething() throws RemoteException {
            System.out.println("Yah we did something");
        }
    }

    private interface RMIClass_RMI extends Remote {
        public void doSomething() throws RemoteException;
    }
}
