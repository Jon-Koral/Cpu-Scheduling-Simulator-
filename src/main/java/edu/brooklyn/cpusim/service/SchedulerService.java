package edu.brooklyn.cpusim.service;

import edu.brooklyn.cpusim.algorithm.AlgorithmStrategy;
import edu.brooklyn.cpusim.algorithm.RRAlgorithm;
import edu.brooklyn.cpusim.dto.ComparisonRequest;
import edu.brooklyn.cpusim.dto.ComparisonResult;
import edu.brooklyn.cpusim.dto.ScheduleRequest;
import edu.brooklyn.cpusim.dto.ScheduleResult;
import org.springframework.stereotype.Service;
import edu.brooklyn.cpusim.model.Process;


import java.util.*;

/**
 * This service runs the CPU scheduling simulations.
 * The SchedulerService stores all the scheduling algorithms you can use.
 *
 * Each algorithm is stored in a hashmap where it can easily be looked up
 * using its name as the key and that allows the program to select the
 * appropriate strategy to run.
 *
 * So far, the only version it supports is FCFS, but this class easily be
 * updated to support the rest when the algorithms are implemented.
 */
@Service
public class SchedulerService {
    private final Map<String, AlgorithmStrategy> algorithms = new HashMap<>();

    /**
     * This is the constructor for the SchedulerService.
     * All the algorithm strategies will be assigned a key and can be looked up
     * when it's needed.
     *
     * @param strategies a list of all the scheduling algorithm implementations
     */
    public SchedulerService(List<AlgorithmStrategy> strategies) {
        for(AlgorithmStrategy strategy : strategies) {
            String key = getAlgorithmKey(strategy);
            algorithms.put(key, strategy);
        }
    }

    /**
     * Creates the appropriate string key for the algorithm passed.
     *
     * Right now, it only supports FCFS because it's the only algorithm implemented,
     * but it will be updated to support the SJF, SRTF, RR, and Priority algorithms.
     *
     * @param strategy the class of the algorithm implementation
     * @return the string key that will be used to store the algorithm strategy
     * in the hashmap.
     */
    private String getAlgorithmKey(AlgorithmStrategy strategy) {
        if (strategy.getClass().getSimpleName().contains("FCFS")) {
            return "FCFS";
        } if(strategy.getClass().getSimpleName().contains("SJF")) {
            return "SJF";
        } if(strategy.getClass().getSimpleName().contains("SRTF")) {
            return "SRTF";
        } if(strategy.getClass().getSimpleName().contains("RR")) {
            return "RR";
        } if (strategy.getClass().getSimpleName().contains("Priority")) {
            return "Priority";
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Runs the scheduling simulation using the algorithm selected by the user along
     * with the list of processes.
     *
     * This method looks up the correct AlgorithmStrategy from the hashmap, runs the
     * algorithm, and returns the ScheduleResult object that has all the data to
     * create the Gantt chart and process table.
     *
     * @param request the simulation request that contains the processes with their
     *                arrival and burst times with the selected algorithm.
     * @return a ScheduleResult object that has the full simulation output.
     */
    public ScheduleResult runSimulation(ScheduleRequest request) {
        AlgorithmStrategy strategy = resolveStrategy(request.getAlgorithm(), request.getQuantum());
        return strategy.runAlgorithm(deepCopy(request.getProcesses()));
    }

    /**
     * Runs multiple scheduling algorithms on the same set of processes and
     * returns all results together so the frontend can display them side by side.
     *
     * Each algorithm receives its own deep copy of the process list so that
     * algorithms that sort or mutate the list in place cannot affect the others.
     *
     * @param request the comparison request containing a list of algorithm names,
     *                an optional quantum (for RR), and the shared process list.
     * @return a ComparisonResult containing each algorithm's ScheduleResult
     *         keyed by its name, in the same order they were requested.
     */
    public ComparisonResult runComparison(ComparisonRequest request) {
        // LinkedHashMap preserves the order the algorithms were requested in
        Map<String, ScheduleResult> results = new LinkedHashMap<>();

        for (String algorithmName : request.getAlgorithms()) {
            AlgorithmStrategy strategy = resolveStrategy(algorithmName, request.getQuantum());
            // Deep-copy so each algorithm works on a fresh, unmodified list
            ScheduleResult result = strategy.runAlgorithm(deepCopy(request.getProcesses()));
            results.put(algorithmName, result);
        }

        return new ComparisonResult(results);
    }

    /**
     * Resolves an algorithm name to its strategy implementation.
     * Round Robin is handled specially because it needs a quantum value.
     *
     * @param name    the algorithm name, e.g. "FCFS", "RR"
     * @param quantum the time quantum — only used when name is "RR"
     * @return the AlgorithmStrategy that should be used
     */
    private AlgorithmStrategy resolveStrategy(String name, Integer quantum) {
        if ("RR".equals(name)) {
            int q = (quantum != null && quantum > 0) ? quantum : 2;
            return new RRAlgorithm(q);
        }
        AlgorithmStrategy strategy = algorithms.get(name);
        if (strategy == null) {
            throw new IllegalArgumentException("Unknown algorithm: " + name);
        }
        return strategy;
    }

    /**
     * Creates a deep copy of the process list so that each algorithm run
     * starts with the original, unmodified data.
     *
     * @param processes the original list of processes
     * @return a new list containing new Process objects with identical field values
     */
    private List<Process> deepCopy(List<Process> processes) {
        List<Process> copy = new ArrayList<>();
        for (Process p : processes) {
            copy.add(new Process(p.getProcessId(), p.getArrivalTime(), p.getBurstTime(), p.getPriority()));
        }
        return copy;
    }
}