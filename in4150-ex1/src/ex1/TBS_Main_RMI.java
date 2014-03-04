package ex1;

import java.util.Map;

/**
 * Created by David on 4-3-14.
 */
public interface TBS_Main_RMI {
    /**
     * Get the processes from this host in the form they should be called with RMI
     * E.g. if this host is "192.168.0.1" and has process 4, it will return {(4, "rmi://192.168.0.1:1099/p_4")}
     * @param me the adress you use to call this host
     */
    public Map<Integer, String> getProcesses(String me) throws java.rmi.RemoteException;
}
