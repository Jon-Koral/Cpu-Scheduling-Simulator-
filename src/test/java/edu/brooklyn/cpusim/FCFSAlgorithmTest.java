package edu.brooklyn.cpusim;

import edu.brooklyn.cpusim.algorithm.FCFSAlgorithm;
import edu.brooklyn.cpusim.dto.ScheduleResult;
import edu.brooklyn.cpusim.model.Process;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * These are the tests for the FCFS scheduling algorithm
 *
 * FCFS runs each process in the order they arrive. If two processes arrive at
 * the same time, the one added to the list first is run first. If no process
 * has arrived yet, the CPU sits idle until the next one shows up.
 *
 * All the input is validated in the frontend so all tests have numbers in between 0 and 99.
 */
public class FCFSAlgorithmTest {

    // Helper to create a process
    private Process p(String id, int arrival, int burst) {
        return new Process(id, arrival, burst, 0);
    }

    /**
     * Four processes that arrive at different times.
     * Expected running order:
     * P1 → P3 → P2 → P4
     *
     * Gantt Chart:
     *   [0–5]  P1   [5–13] P3   [13–18] P2   [18–21] P4
     */
    @Test
    public void testBasicOrderByArrivalTime() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 5));
        processes.add(p("P2", 2, 5));
        processes.add(p("P3", 1, 8));
        processes.add(p("P4", 8, 3));

        ScheduleResult result = new FCFSAlgorithm().runAlgorithm(processes);

        // Gantt chart order
        assertEquals("P1", result.getGanttChart().get(0).getProcessId(), "1st process should be P1 (arrives at 0)");
        assertEquals("P3", result.getGanttChart().get(1).getProcessId(), "2nd process should be P3 (arrives at 1)");
        assertEquals("P2", result.getGanttChart().get(2).getProcessId(), "3rd process should be P2 (arrives at 2)");
        assertEquals("P4", result.getGanttChart().get(3).getProcessId(), "4th process should be P4 (arrives at 8)");

        // Gantt chart start/end times
        assertEquals(0,  result.getGanttChart().get(0).getStart(), "P1 starts at 0");
        assertEquals(5,  result.getGanttChart().get(0).getEnd(),   "P1 ends at 5");
        assertEquals(5,  result.getGanttChart().get(1).getStart(), "P3 starts at 5");
        assertEquals(13, result.getGanttChart().get(1).getEnd(),   "P3 ends at 13");
        assertEquals(13, result.getGanttChart().get(2).getStart(), "P2 starts at 13");
        assertEquals(18, result.getGanttChart().get(2).getEnd(),   "P2 ends at 18");
        assertEquals(18, result.getGanttChart().get(3).getStart(), "P4 starts at 18");
        assertEquals(21, result.getGanttChart().get(3).getEnd(),   "P4 ends at 21");

        // Summary
        // Waiting times:  P1=0, P3=4, P2=11, P4=10  →  avg = 25/4 = 6.25
        assertEquals(6.25, result.getAvgWaitingTime(),    0.01, "Average waiting time");
        // Turnaround times: P1=5, P3=12, P2=16, P4=13  →  avg = 46/4 = 11.5
        assertEquals(11.5, result.getAvgTurnaroundTime(), 0.01, "Average turnaround time");
        // Throughput: 4 processes / last end time 21
        assertEquals(0.190, result.getThroughput(), 0.01, "Throughput");
    }

    /**
     * Processes listed out of order in the input list.
     * Expected running order:
     * P3 → P1 → P2  (arrivals: 0, 3, 5)
     *
     * Gantt Chart:
     *   [0–4] P3   [4–9] P1   [9–14] P2
     */
    @Test
    public void testInputListOrderDoesNotMatter() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 3, 5));
        processes.add(p("P2", 5, 5));
        processes.add(p("P3", 0, 4));

        ScheduleResult result = new FCFSAlgorithm().runAlgorithm(processes);

        assertEquals("P3", result.getGanttChart().get(0).getProcessId(), "P3 should run first (arrives at 0)");
        assertEquals("P1", result.getGanttChart().get(1).getProcessId(), "P1 should run second (arrives at 3)");
        assertEquals("P2", result.getGanttChart().get(2).getProcessId(), "P2 should run third (arrives at 5)");

        assertEquals(0,  result.getGanttChart().get(0).getStart());
        assertEquals(4,  result.getGanttChart().get(0).getEnd());
        assertEquals(4,  result.getGanttChart().get(1).getStart());
        assertEquals(9,  result.getGanttChart().get(1).getEnd());
        assertEquals(9,  result.getGanttChart().get(2).getStart());
        assertEquals(14, result.getGanttChart().get(2).getEnd());
    }

    /**
     * All three processes have gaps between them and the CPU idles each time.
     *
     * Gantt Chart:
     *   [0–5] P1   [idle 5–10]   [10–30] P2   [idle 30–40]   [40–90] P3
     */
    @Test
    public void testIdleGapsBetweenAllProcesses() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0,  5));
        processes.add(p("P2", 10, 20));
        processes.add(p("P3", 40, 50));

        ScheduleResult result = new FCFSAlgorithm().runAlgorithm(processes);

        // Order
        assertEquals("P1", result.getGanttChart().get(0).getProcessId());
        assertEquals("P2", result.getGanttChart().get(1).getProcessId());
        assertEquals("P3", result.getGanttChart().get(2).getProcessId());

        // Start/end times
        assertEquals(0,  result.getGanttChart().get(0).getStart(), "P1 starts at 0");
        assertEquals(5,  result.getGanttChart().get(0).getEnd(),   "P1 ends at 5");
        assertEquals(10, result.getGanttChart().get(1).getStart(), "P2 starts at its arrival 10, not at 5");
        assertEquals(30, result.getGanttChart().get(1).getEnd(),   "P2 ends at 30");
        assertEquals(40, result.getGanttChart().get(2).getStart(), "P3 starts at its arrival 40, not at 30");
        assertEquals(90, result.getGanttChart().get(2).getEnd(),   "P3 ends at 90");

        // Waiting time: every process starts exactly at its arrival time → no waiting
        assertEquals(0.0, result.getAvgWaitingTime(), 0.01, "No process waits when gaps exist");
        // Turnaround times: P1=5, P2=20, P3=50  →  avg = 75/3 = 25
        assertEquals(25.0, result.getAvgTurnaroundTime(), 0.01);
        // Throughput: 3 / 90 = 0.0333
        assertEquals(0.0333, result.getThroughput(), 0.01);
    }

    /**
     * A single idle gap only between the first and second process.
     * The third process arrives while the second is still running, so no second gap.
     *
     * Gantt Chart:
     *   [0–3] P1   [idle 3–10]   [10–16] P2   [16–21] P3
     */
    @Test
    public void testIdleGapOnlyAtStart() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0,  3));
        processes.add(p("P2", 10, 6));
        processes.add(p("P3", 12, 5));

        ScheduleResult result = new FCFSAlgorithm().runAlgorithm(processes);

        assertEquals(0,  result.getGanttChart().get(0).getStart(), "P1 starts at 0");
        assertEquals(3,  result.getGanttChart().get(0).getEnd(),   "P1 ends at 3");
        assertEquals(10, result.getGanttChart().get(1).getStart(), "P2 waits for CPU to jump to arrival 10");
        assertEquals(16, result.getGanttChart().get(1).getEnd(),   "P2 ends at 16");
        assertEquals(16, result.getGanttChart().get(2).getStart(), "P3 starts immediately after P2 (arrived at 12, no gap)");
        assertEquals(21, result.getGanttChart().get(2).getEnd(),   "P3 ends at 21");

        // Waiting times: P1=0, P2=0 (starts at arrival), P3=4  →  avg = 4/3 = 1.33
        assertEquals(1.33, result.getAvgWaitingTime(), 0.01);
        // Turnaround: P1=3, P2=6, P3=9  →  avg = 18/3 = 6.0
        assertEquals(6.0,  result.getAvgTurnaroundTime(), 0.01);
    }

    /**
     * All processes arrive at time 0. They run in list order.
     *
     * Gantt Chart:
     *   [0–4] P1   [4–9] P2   [9–14] P3
     */
    @Test
    public void testAllProcessesArriveAtTimeZero() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 4));
        processes.add(p("P2", 0, 5));
        processes.add(p("P3", 0, 5));

        ScheduleResult result = new FCFSAlgorithm().runAlgorithm(processes);

        assertEquals("P1", result.getGanttChart().get(0).getProcessId());
        assertEquals("P2", result.getGanttChart().get(1).getProcessId());
        assertEquals("P3", result.getGanttChart().get(2).getProcessId());

        assertEquals(0,  result.getGanttChart().get(0).getStart());
        assertEquals(4,  result.getGanttChart().get(0).getEnd());
        assertEquals(4,  result.getGanttChart().get(1).getStart());
        assertEquals(9,  result.getGanttChart().get(1).getEnd());
        assertEquals(9,  result.getGanttChart().get(2).getStart());
        assertEquals(14, result.getGanttChart().get(2).getEnd());

        // Waiting times: P1=0, P2=4, P3=9  →  avg = 13/3 = 4.33
        assertEquals(4.33, result.getAvgWaitingTime(), 0.01);
        // Turnaround: P1=4, P2=9, P3=14  →  avg = 27/3 = 9.0
        assertEquals(9.0, result.getAvgTurnaroundTime(), 0.01);
        // Throughput: 3 / 14 = 0.214
        assertEquals(0.214, result.getThroughput(), 0.01);
    }

    /**
     * All processes arrive at a non-zero time.
     * The CPU idles from 0 to 5, then they run in list order because they all arrive at 5.
     *
     * Gantt Chart:
     *   [idle 0–5]   [5–8] P1   [8–13] P2   [13–16] P3
     */
    @Test
    public void testAllProcessesArriveSameNonZeroTime() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 5, 3));
        processes.add(p("P2", 5, 5));
        processes.add(p("P3", 5, 3));

        ScheduleResult result = new FCFSAlgorithm().runAlgorithm(processes);

        assertEquals(5,  result.getGanttChart().get(0).getStart(), "P1 starts at arrival 5, not 0");
        assertEquals(8,  result.getGanttChart().get(0).getEnd());
        assertEquals(8,  result.getGanttChart().get(1).getStart());
        assertEquals(13, result.getGanttChart().get(1).getEnd());
        assertEquals(13, result.getGanttChart().get(2).getStart());
        assertEquals(16, result.getGanttChart().get(2).getEnd());

        // Waiting times: P1=0, P2=3, P3=8  →  avg = 11/3 = 3.67
        assertEquals(3.67, result.getAvgWaitingTime(), 0.01);
        // Turnaround: P1=3, P2=8, P3=11  →  avg = 22/3 = 7.33
        assertEquals(7.33, result.getAvgTurnaroundTime(), 0.01);
    }

    /**
     * Only one process, arriving at time 0.
     * Waiting time must be 0.
     */
    @Test
    public void testSingleProcessArrivesAtZero() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 7));

        ScheduleResult result = new FCFSAlgorithm().runAlgorithm(processes);

        assertEquals(1, result.getGanttChart().size(), "Gantt chart should have exactly 1 entry");
        assertEquals("P1", result.getGanttChart().get(0).getProcessId());
        assertEquals(0, result.getGanttChart().get(0).getStart());
        assertEquals(7, result.getGanttChart().get(0).getEnd());

        assertEquals(0.0, result.getAvgWaitingTime(),    0.01, "No waiting for a single process");
        assertEquals(7.0, result.getAvgTurnaroundTime(), 0.01);
        assertEquals(0.143, result.getThroughput(), 0.01);
    }

    /**
     * Only one process, but it doesn't arrive at 0
     * The CPU idles until it arrives.
     * Waiting time is still 0 because the process starts the moment it arrives.
     */
    @Test
    public void testSingleProcessArrivesLate() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 20, 10));

        ScheduleResult result = new FCFSAlgorithm().runAlgorithm(processes);

        assertEquals(20, result.getGanttChart().get(0).getStart(), "CPU jumps to arrival time 20");
        assertEquals(30, result.getGanttChart().get(0).getEnd());

        assertEquals(0.0,  result.getAvgWaitingTime(),    0.01);
        assertEquals(10.0, result.getAvgTurnaroundTime(), 0.01);
        assertEquals(0.0333, result.getThroughput(), 0.01);
    }

    /**
     * Three processes each with a burst time of 1.
     *
     * Gantt Chart:
     *   [0–1] P1   [1–2] P2   [2–3] P3
     */
    @Test
    public void testMinimumBurstTimeOneUnit() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 1));
        processes.add(p("P2", 0, 1));
        processes.add(p("P3", 0, 1));

        ScheduleResult result = new FCFSAlgorithm().runAlgorithm(processes);

        assertEquals(0, result.getGanttChart().get(0).getStart());
        assertEquals(1, result.getGanttChart().get(0).getEnd());
        assertEquals(1, result.getGanttChart().get(1).getStart());
        assertEquals(2, result.getGanttChart().get(1).getEnd());
        assertEquals(2, result.getGanttChart().get(2).getStart());
        assertEquals(3, result.getGanttChart().get(2).getEnd());

        // Waiting times: P1=0, P2=1, P3=2  →  avg = 1.0
        assertEquals(1.0, result.getAvgWaitingTime(), 0.01);
        // Turnaround:   P1=1, P2=2, P3=3  →  avg = 2.0
        assertEquals(2.0, result.getAvgTurnaroundTime(), 0.01);
        // Throughput:   3 / 3 = 1.0
        assertEquals(1.0, result.getThroughput(), 0.01);
    }

    /**
     * Two processes both using the maximum allowed burst time of 99.
     *
     * Gantt Chart:
     *   [0–99] P1   [99–198] P2
     */
    @Test
    public void testMaximumBurstTimes() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0,  99));
        processes.add(p("P2", 0,  99));

        ScheduleResult result = new FCFSAlgorithm().runAlgorithm(processes);

        assertEquals(0,   result.getGanttChart().get(0).getStart());
        assertEquals(99,  result.getGanttChart().get(0).getEnd());
        assertEquals(99,  result.getGanttChart().get(1).getStart());
        assertEquals(198, result.getGanttChart().get(1).getEnd());

        // Waiting times: P1=0, P2=99  →  avg = 49.5
        assertEquals(49.5, result.getAvgWaitingTime(), 0.01);
        // Turnaround:   P1=99, P2=198  →  avg = 148.5
        assertEquals(148.5, result.getAvgTurnaroundTime(), 0.01);
    }

    /**
     * A process with the maximum allowed arrival time of 99.
     * The CPU idles until time 99, then runs the process.
     */
    @Test
    public void testMaximumArrivalTime() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 99, 1));

        ScheduleResult result = new FCFSAlgorithm().runAlgorithm(processes);

        assertEquals(99,  result.getGanttChart().get(0).getStart(), "CPU waits until arrival 99");
        assertEquals(100, result.getGanttChart().get(0).getEnd());

        assertEquals(0.0, result.getAvgWaitingTime(), 0.01, "Process starts on arrival");
        assertEquals(1.0, result.getAvgTurnaroundTime(), 0.01);
    }

    /**
     * Checks the waiting time and turnaround time stored for each process.
     *
     * Gantt Chart:
     *   [0–5] P1   [5–10] P2   [10–18] P3
     *
     * P1: arrived 0, starts 0,  ends 5  → wait=0,  turnaround=5
     * P2: arrived 0, starts 5,  ends 10 → wait=5,  turnaround=10
     * P3: arrived 2, starts 10, ends 18 → wait=8,  turnaround=16
     */
    @Test
    public void testPerProcessWaitingAndTurnaroundTimes() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 5));
        processes.add(p("P2", 0, 5));
        processes.add(p("P3", 2, 8));

        ScheduleResult result = new FCFSAlgorithm().runAlgorithm(processes);

        // P1
        assertEquals(0,  result.getProcessTable().get(0).getWaitingTime(),    "P1 waiting time");
        assertEquals(5,  result.getProcessTable().get(0).getTurnaroundTime(), "P1 turnaround time");

        // P2
        assertEquals(5,  result.getProcessTable().get(1).getWaitingTime(),    "P2 waiting time");
        assertEquals(10, result.getProcessTable().get(1).getTurnaroundTime(), "P2 turnaround time");

        // P3 (arrived at 2 but waits until P1 and P2 finish at 10)
        assertEquals(8,  result.getProcessTable().get(2).getWaitingTime(),    "P3 waiting time");
        assertEquals(16, result.getProcessTable().get(2).getTurnaroundTime(), "P3 turnaround time");
    }

    /**
     * A process that arrives exactly when the previous one finishes.
     *
     * Gantt Chart:
     *   [0–5] P1   [5–10] P2
     * P2 arrives at exactly 5 — the same moment P1 ends.
     */
    @Test
    public void testProcessArrivesExactlyWhenPreviousEnds() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 5));
        processes.add(p("P2", 5, 5));

        ScheduleResult result = new FCFSAlgorithm().runAlgorithm(processes);

        assertEquals(5,  result.getGanttChart().get(1).getStart());
        assertEquals(10, result.getGanttChart().get(1).getEnd());

        assertEquals(0, result.getProcessTable().get(1).getWaitingTime());
        assertEquals(5, result.getProcessTable().get(1).getTurnaroundTime());
    }

    /**
     * Two processes with very different burst times.
     *
     * Gantt Chart:
     *   [0–1] P1   [1–99] P2
     * Last end = 99  →  throughput = 2 / 99 = 0.0202
     */
    @Test
    public void testThroughputWithVaryingBurstTimes() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0,  1));
        processes.add(p("P2", 0,  98));

        ScheduleResult result = new FCFSAlgorithm().runAlgorithm(processes);

        assertEquals(0.0202, result.getThroughput(), 0.001);
    }

    /**
     * Five processes all with burst time 10, no gaps.
     * Total time = 50 → throughput = 5 / 50 = 0.10
     */
    @Test
    public void testThroughputFiveEqualProcesses() {
        List<Process> processes = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            processes.add(p("P" + i, 0, 10));
        }

        ScheduleResult result = new FCFSAlgorithm().runAlgorithm(processes);

        assertEquals(0.10, result.getThroughput(), 0.001);
    }

    /**
     * P1 arrives first and runs before P2.
     * P2 must wait for P1 to complete.
     *
     * Gantt Chart:
     *   [0–6] P1   [6–14] P2
     *
     * P1: wait=0, turnaround=6
     * P2: wait=2, turnaround=10
     */
    @Test
    public void testTwoProcessesSequential() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 6));
        processes.add(p("P2", 4, 8));

        ScheduleResult result = new FCFSAlgorithm().runAlgorithm(processes);

        assertEquals("P1", result.getGanttChart().get(0).getProcessId());
        assertEquals("P2", result.getGanttChart().get(1).getProcessId());

        assertEquals(0,  result.getGanttChart().get(0).getStart());
        assertEquals(6,  result.getGanttChart().get(0).getEnd());
        assertEquals(6,  result.getGanttChart().get(1).getStart());
        assertEquals(14, result.getGanttChart().get(1).getEnd());

        // avg waiting: (0 + 2) / 2 = 1.0
        assertEquals(1.0, result.getAvgWaitingTime(), 0.01);
        // avg turnaround: (6 + 10) / 2 = 8.0
        assertEquals(8.0, result.getAvgTurnaroundTime(), 0.01);
        // throughput: 2 / 14 = 0.143
        assertEquals(0.143, result.getThroughput(), 0.01);
    }

    /**
     * P2 arrives well after P1 finishes — CPU goes idle in between.
     * Both processes should have zero waiting time.
     *
     * Gantt Chart:
     *   [3–8] P1   [idle 8–15]   [15–20] P2
     */
    @Test
    public void testTwoProcessesWithIdleGap() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 3,  5));
        processes.add(p("P2", 15, 5));

        ScheduleResult result = new FCFSAlgorithm().runAlgorithm(processes);

        assertEquals(3,  result.getGanttChart().get(0).getStart());
        assertEquals(8,  result.getGanttChart().get(0).getEnd());
        assertEquals(15, result.getGanttChart().get(1).getStart());
        assertEquals(20, result.getGanttChart().get(1).getEnd());

        assertEquals(0.0, result.getAvgWaitingTime(), 0.01);
    }
}