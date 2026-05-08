package edu.brooklyn.cpusim.algorithm;

import edu.brooklyn.cpusim.dto.ScheduleResult;
import edu.brooklyn.cpusim.model.Process;

import java.util.List;

/**
 * This interface is a strategy design pattern for running a CPU scheduling algorithm.
 * All the scheduling algorithms (FCFS, SJF, SRTF, RR, Priority) will be implemented
 * by this interface.
 */
public interface AlgorithmStrategy {
    /**
     * Runs the scheduling algorithm on the given list of processes.
     * Each algorithm decides how to order the processes and how to use the processes.
     *
     * @param processes the list of processes to be scheduled
     * @return a ScheduleResult object that has the Gantt chart, process tables, and calculations
     */
    ScheduleResult runAlgorithm(List<Process> processes);
}
