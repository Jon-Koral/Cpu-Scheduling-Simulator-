package edu.brooklyn.cpusim.algorithm;

import edu.brooklyn.cpusim.dto.ScheduleResult;
import edu.brooklyn.cpusim.model.GanttEntry;
import edu.brooklyn.cpusim.model.Process;
import edu.brooklyn.cpusim.model.ProcessResult;
import edu.brooklyn.cpusim.sorter.ProcessSorter;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * This class is the implementation of the FCFS CPU scheduling algorithm
 * The algorithm creates a gantt chart showing where the processes start
 * and end, a table of results for each process, and average waiting/turnaround
 *  times and throughput.
 */
@Service
public class FCFSAlgorithm implements AlgorithmStrategy {

    /**
     * Runs the FCFS scheduling algorithm on the list of processes
     *
     * Steps:
     * 1.   Sort the processes by arrival time
     * 2.   Build the Gantt chart by tracking when each process starts and finishes
     * 3.   Calculate the waiting time and turnaround time for each process
     * 4.   Add the process to the process table when all calculations are finished
     * 5.   Calculate the throughput
     * 6.   Package all the information into a ScheduleResult object and return it
     *
     * @param processes the list of processes to be scheduled
     * @return a ScheduleResult containing the Gantt chart, process table,average
     * waiting/turnaround time, and throughput
     */
    @Override
    public ScheduleResult runAlgorithm(List<Process> processes) {

        // Sort the processes by arrival time so FCFS runs them in the correct order
        ProcessSorter.sortByArrivalTime(processes);

        List<GanttEntry> ganttChart = new ArrayList<>();
        List<ProcessResult> processTable = new ArrayList<>();

        // Keeps track of the current time on the CPU timeline
        int curr = 0;
        // Keeps track of the time when the process ends
        int end = 0;

        for(Process process : processes) {
            String processId = process.getProcessId();
            int arrivalTime = process.getArrivalTime();
            int burstTime = process.getBurstTime();
            int priority = process.getPriority();

            if(curr < arrivalTime) {
                curr = arrivalTime;
            }
            end = curr + burstTime;

            GanttEntry ganttEntry = new GanttEntry(processId, curr, end);
            ganttChart.add(ganttEntry);

            curr = end;

            int turnaroundTime = end - arrivalTime;
            int waitingTime = turnaroundTime - burstTime;

            ProcessResult processResult = new ProcessResult(processId, arrivalTime, burstTime, priority, waitingTime, turnaroundTime);
            processTable.add(processResult);
        }

        double avgWaitingTime = processTable.stream()
                .mapToInt(process -> process.getWaitingTime())
                .average()
                .orElse(0);

        double averageTurnaroundTime = processTable.stream()
                .mapToInt(process -> process.getTurnaroundTime())
                .average()
                .orElse(0);

        double throughput = (double) processes.size() / end;

        return new ScheduleResult(ganttChart, processTable, avgWaitingTime, averageTurnaroundTime, throughput);
    }
}
