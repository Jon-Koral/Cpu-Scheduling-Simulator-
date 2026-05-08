package edu.brooklyn.cpusim.algorithm;

import edu.brooklyn.cpusim.dto.ScheduleResult;
import edu.brooklyn.cpusim.model.GanttEntry;
import edu.brooklyn.cpusim.model.Process;
import edu.brooklyn.cpusim.model.ProcessResult;
import edu.brooklyn.cpusim.sorter.ProcessSorter;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * This class is the implementation of the Round Robin CPU scheduling algorithm.
 * The algorithm creates a Gantt chart showing where the processes start
 * and end, a table of results for each process, and average waiting/turnaround
 * times and throughput.
 *
 * Round Robin is a preemptive algorithm. Each process is given a fixed time
 * slice called a quantum. If a process does not finish within its quantum,
 * it is placed back at the end of the ready queue and the next process runs.
 * This continues until all processes are finished.
 */
@Service
public class RRAlgorithm implements AlgorithmStrategy {

    // The time quantum used for this Round Robin instance
    private final int quantum;

    /**
     * Constructor for RoundRobinAlgorithm.
     * Defaults the quantum to 2 if called without arguments (required by Spring).
     */
    public RRAlgorithm() {
        this.quantum = 2;
    }

    /**
     * Constructor for RoundRobinAlgorithm with a specific quantum.
     *
     * @param quantum the time slice each process is allowed to run before being preempted
     */
    public RRAlgorithm(int quantum) {
        this.quantum = quantum;
    }

    /**
     * Runs the Round Robin scheduling algorithm on the list of processes.
     *
     * Steps:
     * 1.   Sort the processes by arrival time
     * 2.   Track the remaining burst time for each process in a map
     * 3.   Maintain a ready queue; enqueue processes as they arrive
     * 4.   Dequeue the first process and run it for min(quantum, remainingTime) units
     * 5.   Build a Gantt chart entry for each time slice
     * 6.   After the slice, enqueue any new arrivals before re-enqueuing the current process
     * 7.   If the process finishes, record its completion time
     * 8.   Repeat until all processes are complete
     * 9.   Calculate the waiting time and turnaround time for each process
     * 10.  Calculate the throughput
     * 11.  Package all the information into a ScheduleResult object and return it
     *
     * @param processes the list of processes to be scheduled
     * @return a ScheduleResult containing the Gantt chart, process table, average
     * waiting/turnaround time, and throughput
     */
    @Override
    public ScheduleResult runAlgorithm(List<Process> processes) {
        if (processes == null || processes.isEmpty()) {
            return new ScheduleResult(new ArrayList<>(), new ArrayList<>(), 0.0, 0.0, 0.0);
        }

        List<Process> sortedProcesses = new ArrayList<>(processes);
        ProcessSorter.sortByArrivalTime(sortedProcesses);

        List<GanttEntry> ganttChart = new ArrayList<>();
        List<ProcessResult> processTable = new ArrayList<>();
        Map<String, Integer> remainingBurstTime = new HashMap<>();
        Map<String, Integer> completion = new HashMap<>();
        Queue<Process> readyQueue = new ArrayDeque<>();

        for (Process process : sortedProcesses) {
            remainingBurstTime.put(process.getProcessId(), process.getBurstTime());
        }

        int currTime = 0;
        int nextProcessIndex = 0;

        // Continue as long as there are processes to arrive OR waiting in queue
        while (nextProcessIndex < sortedProcesses.size() || !readyQueue.isEmpty()) {

            // If CPU is idle, jump to the next arrival time
            if (readyQueue.isEmpty()) {
                currTime = Math.max(currTime, processes.get(nextProcessIndex).getArrivalTime());
                while (nextProcessIndex < processes.size() && processes.get(nextProcessIndex).getArrivalTime() <= currTime) {
                    readyQueue.add(processes.get(nextProcessIndex));
                    nextProcessIndex++;
                }
            }

            Process p = readyQueue.poll();
            String id = p.getProcessId();
            int remaining = remainingBurstTime.get(id);

            int runTime = Math.min(quantum, remaining);
            int endTime = currTime + runTime;

            ganttChart.add(new GanttEntry(id, currTime, endTime));

            currTime = endTime;
            remainingBurstTime.put(id, remaining - runTime);

            // 1. Load processes that arrived DURING the current execution slice
            while (nextProcessIndex < processes.size() && processes.get(nextProcessIndex).getArrivalTime() <= currTime) {
                readyQueue.add(processes.get(nextProcessIndex));
                nextProcessIndex++;
            }

            // 2. Re-enqueue the preempted process (New arrivals get priority in standard RR)
            if (remainingBurstTime.get(id) > 0) {
                readyQueue.add(p);
            } else {
                completion.put(id, currTime);
            }
        }for (Process process : processes) {
            String processId = process.getProcessId();
            int arrivalTime = process.getArrivalTime();
            int burstTime = process.getBurstTime();
            int priority = process.getPriority();
            int comp = completion.get(processId);

            int turnaroundTime = comp - arrivalTime;
            int waitingTime = turnaroundTime - burstTime;

            ProcessResult processResult = new ProcessResult(processId, arrivalTime, burstTime, priority, waitingTime, turnaroundTime);
            processTable.add(processResult);
        }

        // Calculate averages using consistent stream logic
        double avgWaitingTime = processTable.stream()
                .mapToInt(process -> process.getWaitingTime())
                .average()
                .orElse(0);

        double averageTurnaroundTime = processTable.stream()
                .mapToInt(process -> process.getTurnaroundTime())
                .average()
                .orElse(0);

        // Calculate throughput based on final 'curr' time
        double throughput = (currTime == 0) ? 0 : (double) processes.size() / currTime;

        return new ScheduleResult(ganttChart, processTable, avgWaitingTime, averageTurnaroundTime, throughput);
    }
}
