package edu.brooklyn.cpusim.sorter;

import edu.brooklyn.cpusim.model.Process;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * This is a helper class that helps sort processes for the algorithm that
 * implements them.
 */
public class ProcessSorter {
    /**
     * This method sorts a list of processes based on their arrival time.
     *
     * This method is used by the FCFSAlgorithm class to sort the processes
     * by arrival time instead of doing it inside the class.
     *
     * @param processes the list of Process objects that need to be sorted
     */
    public static void sortByArrivalTime(List<Process> processes) {
        Collections.sort(processes, Comparator.comparing(Process::getArrivalTime));
    }

    public static void sortByBurstTime(List<Process> processes) {
        Collections.sort(processes, Comparator.comparing(Process::getBurstTime));
    }

    public static void sortByProcessId(List<Process> processes) {
        Collections.sort(processes, Comparator.comparing(Process::getProcessId));
    }
}
