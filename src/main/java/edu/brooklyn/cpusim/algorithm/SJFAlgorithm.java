package edu.brooklyn.cpusim.algorithm;

import edu.brooklyn.cpusim.dto.ScheduleResult;
import edu.brooklyn.cpusim.model.GanttEntry;
import edu.brooklyn.cpusim.model.Process;
import edu.brooklyn.cpusim.model.ProcessResult;
import edu.brooklyn.cpusim.sorter.ProcessSorter;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * This class is the implementation of the SJF CPU scheduling algorithm.
 * The algorithm creates a Gantt chart showing where the processes start
 * and end, a table of results for each process, and average waiting/turnaround
 * times and throughput.
 */
@Service
public class SJFAlgorithm implements AlgorithmStrategy {

    /**
     * Runs the SJF scheduling algorithm on the list of processes.
     *
     * Steps:
     * 1.   Sort the processes by arrival time
     * 2.   Add all processes that have arrived by the current time to the ready queue
     * 3.   If the ready queue is empty, jump to the next arrival time
     * 4.   Sort the ready queue by burst time and pick the shortest job
     * 5.   Build the Gantt chart entry for the selected process
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
        while(i < processes.size() || !ready.isEmpty()) {
            while(i < processes.size() && processes.get(i).getArrivalTime() <= curr) {
                ready.add(processes.get(i));
                i++;
            }
            //if the current time has nothing in the ready queue, then set the current time to the time when the next process arrives
            if(ready.isEmpty()) {
                curr = processes.get(i).getArrivalTime();
                continue;
            }

            // we need to sort the ready queue by shortest time first
            ProcessSorter.sortByBurstTime(ready);

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

        double averageTurnaroundTime = processTable.stream()
                .mapToInt(process -> process.getTurnaroundTime())
                .average()
                .orElse(0);

        double throughput = (double) processes.size() / end;

        return new ScheduleResult(ganttChart, processTable, avgWaitingTime, averageTurnaroundTime, throughput);
    }
}
