package edu.brooklyn.cpusim.algorithm;

import edu.brooklyn.cpusim.dto.ScheduleResult;
import edu.brooklyn.cpusim.model.GanttEntry;
import edu.brooklyn.cpusim.model.Process;
import edu.brooklyn.cpusim.model.ProcessResult;
import edu.brooklyn.cpusim.sorter.ProcessSorter;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * This class is the implementation of the SRTF CPU scheduling algorithm.
 * The algorithm creates a Gantt chart showing where the processes start
 * and end, a table of results for each process, and average waiting/turnaround
 * times and throughput.
 */
@Service
public class SRTFAlgorithm implements AlgorithmStrategy {

    /**
     * Runs the SRTF scheduling algorithm on the list of processes.
     *
     * Steps:
     * 1.   Sort the processes by arrival time
     * 2.   Track the remaining burst time for each process in a map
     * 3.   At each time unit, build the ready queue from all arrived, unfinished processes
     * 4.   If the ready queue is empty, advance time by one unit
     * 5.   Pick the process with the shortest remaining time (tie-break by process ID)
     * 6.   If the running process changes, close the current Gantt entry and start a new one
     * 7.   Decrement the running process's remaining time by one unit
     * 8.   If the process finishes, record its completion time and close its Gantt entry
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

        ProcessSorter.sortByArrivalTime(processes);

        List<GanttEntry> ganttChart = new ArrayList<>();
        List<ProcessResult> processTable = new ArrayList<>();

        // We can store the processes in a hashmap, keeping track of them with their id, and keep track of their remaining burst time
        Map<String, Integer> remainingBurstTime = new HashMap<>();

        for(Process process : processes) {
            remainingBurstTime.put(process.getProcessId(), process.getBurstTime());
        }

        int completed = 0;
        int end = 0;
        String currentProcessId = "";
        int curr = 0;

        Map<String, Integer> completion = new HashMap<>();

        while(completed < processes.size()) {
            List<Process> ready = new ArrayList<>();
            for(Process process : processes) {
                if(process.getArrivalTime() <= end && remainingBurstTime.get(process.getProcessId()) > 0) {
                    ready.add(process);
                }
            }

            // if nothing is ready, then go to the next arrival time
            if(ready.isEmpty()) {
                end++;
                continue;
            }

            ProcessSorter.sortByProcessId(ready);
            Process shortestProcess = ready.stream()
                    .min(Comparator.comparingInt((Process p) -> remainingBurstTime.get(p.getProcessId()))
                            .thenComparing(Process::getProcessId)) // Tie-breaker: Smaller ID
                    .get();

            if(currentProcessId == "" || !currentProcessId.equals(shortestProcess.getProcessId())) {
                if(currentProcessId != "") {
                    GanttEntry ganttEntry = new GanttEntry(currentProcessId, curr, end);
                    ganttChart.add(ganttEntry);
                }
                currentProcessId = shortestProcess.getProcessId();
                curr = end;
            }

            remainingBurstTime.put(currentProcessId, remainingBurstTime.get(currentProcessId) - 1);
            end++;

            // if the process is finished
            if(remainingBurstTime.get(currentProcessId) == 0) {
                completion.put(currentProcessId, end);
                completed++;

                GanttEntry ganttEntry = new GanttEntry(currentProcessId, curr, end);
                ganttChart.add(ganttEntry);
                currentProcessId = "";
            }
        }

        for(Process process : processes) {
            String processId = process.getProcessId();
            int arrivalTime = process.getArrivalTime();
            int burstTime = process.getBurstTime();
            int priority = process.getPriority();
            int comp = completion.get(process.getProcessId());

            int turnaround = comp - arrivalTime;
            int waiting = turnaround - burstTime;

            ProcessResult processResult = new ProcessResult(processId, arrivalTime, burstTime, priority, waiting, turnaround);
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
