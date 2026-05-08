package edu.brooklyn.cpusim.algorithm;

import edu.brooklyn.cpusim.dto.ScheduleResult;
import edu.brooklyn.cpusim.model.GanttEntry;
import edu.brooklyn.cpusim.model.Process;
import edu.brooklyn.cpusim.model.ProcessResult;
import edu.brooklyn.cpusim.sorter.ProcessSorter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * This class is the implementation of the Priority CPU scheduling algorithm.
 * The algorithm creates a Gantt chart showing where the processes start
 * and end, a table of results for each process, and average waiting/turnaround
 * times and throughput.
 *
 * Priority is a non-preemptive algorithm. When the CPU is free, it picks the
 * process with the lowest priority number from the ready queue (lower number =
 * higher priority). If no process has arrived yet, the CPU idles until the next
 * one does. Ties in priority are broken by arrival time; if arrival times are
 * also equal, the process appearing first in the input list runs first.
 */
@Service
public class PriorityAlgorithm implements AlgorithmStrategy {

    /**
     * Runs the Priority scheduling algorithm on the list of processes.
     *
     * Steps:
     * 1.   Sort the processes by arrival time
     * 2.   Add all processes that have arrived by the current time to the ready queue
     * 3.   If the ready queue is empty, jump to the next arrival time
     * 4.   Sort the ready queue by priority (lowest number = highest priority),
     *      breaking ties by arrival time
     * 5.   Pick the highest-priority process and build its Gantt chart entry
     * 6.   Calculate the waiting time and turnaround time for each process
     * 7.   Add the process to the process table when all calculations are finished
     * 8.   Calculate the throughput
     * 9.   Package all the information into a ScheduleResult object and return it
     *
     * @param processes the list of processes to be scheduled
     * @return a ScheduleResult containing the Gantt chart, process table, average
     * waiting/turnaround time, and throughput
     */
    @Override
    public ScheduleResult runAlgorithm(List<Process> processes) {

        ProcessSorter.sortByArrivalTime(processes);

        List<GanttEntry> ganttChart = new ArrayList<>();
        List<ProcessResult> processTable = new ArrayList<>();

        List<Process> ready = new ArrayList<>();

        int i = 0;
        int curr = 0;
        int end = 0;

        while (i < processes.size() || !ready.isEmpty()) {

            // Enqueue all processes that have arrived by the current time
            while (i < processes.size() && processes.get(i).getArrivalTime() <= curr) {
                ready.add(processes.get(i));
                i++;
            }

            // If nothing is ready yet, jump the clock to the next arrival time
            if (ready.isEmpty()) {
                curr = processes.get(i).getArrivalTime();
                continue;
            }

            // Pick the process with the lowest priority number;
            // break ties by arrival time (earlier arrival wins)
            ready.sort(Comparator.comparingInt(Process::getPriority)
                    .thenComparingInt(Process::getArrivalTime));

            Process p = ready.remove(0);
            String processId = p.getProcessId();
            int arrivalTime = p.getArrivalTime();
            int burstTime = p.getBurstTime();
            int priority = p.getPriority();

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

        double avgTurnaroundTime = processTable.stream()
                .mapToInt(process -> process.getTurnaroundTime())
                .average()
                .orElse(0);

        double throughput = (double) processes.size() / end;

        return new ScheduleResult(ganttChart, processTable, avgWaitingTime, avgTurnaroundTime, throughput);
    }
}