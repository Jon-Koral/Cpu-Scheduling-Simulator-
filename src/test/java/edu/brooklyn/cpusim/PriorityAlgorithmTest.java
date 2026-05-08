package edu.brooklyn.cpusim;

import edu.brooklyn.cpusim.algorithm.PriorityAlgorithm;
import edu.brooklyn.cpusim.dto.ScheduleResult;
import edu.brooklyn.cpusim.model.Process;
import edu.brooklyn.cpusim.model.ProcessResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * These are the tests for the Priority scheduling algorithm.
 *
 * Priority is non-preemptive: once a process starts, it runs to completion.
 * When the CPU is free, it picks the process with the lowest priority number
 * from the ready queue (lower number = higher priority).
 * Ties in priority are broken by arrival time (earlier arrival runs first).
 * If no process has arrived yet, the CPU idles until the next one does.
 *
 * All input is validated in the frontend so all numbers are between 0 and 99.
 */
public class PriorityAlgorithmTest {

    // Creates a Process with a specific priority
    private Process p(String id, int arrival, int burst, int priority) {
        return new Process(id, arrival, burst, priority);
    }

    // Looks up a process result by ID from the process table
    private ProcessResult find(ScheduleResult result, String id) {
        return result.getProcessTable().stream()
                .filter(p -> p.getProcessId().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Process " + id + " not found in process table"));
    }

    /**
     * P1 starts immediately at time 0. While it runs, P2 and P3 arrive.
     * When P1 finishes, Priority picks P3 (priority 1) before P2 (priority 3).
     *
     * Gantt Chart:
     *   [0–7] P1   [7–8] P3   [8–12] P2
     *
     * P1: arrived 0, starts 0,  ends 7  → wait=0, turnaround=7
     * P3: arrived 4, starts 7,  ends 8  → wait=3, turnaround=4
     * P2: arrived 2, starts 8,  ends 12 → wait=6, turnaround=10
     */
    @Test
    public void testHigherPriorityProcessJumpsAheadOfLower() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 7, 2));
        processes.add(p("P2", 2, 4, 3));
        processes.add(p("P3", 4, 1, 1));

        ScheduleResult result = new PriorityAlgorithm().runAlgorithm(processes);

        assertEquals(3, result.getGanttChart().size());

        assertEquals("P1", result.getGanttChart().get(0).getProcessId(), "P1 is the only process at time 0");
        assertEquals(0, result.getGanttChart().get(0).getStart());
        assertEquals(7, result.getGanttChart().get(0).getEnd());

        assertEquals("P3", result.getGanttChart().get(1).getProcessId(), "P3 has higher priority (1) than P2 (3)");
        assertEquals(7, result.getGanttChart().get(1).getStart());
        assertEquals(8, result.getGanttChart().get(1).getEnd());

        assertEquals("P2", result.getGanttChart().get(2).getProcessId(), "P2 runs last (lowest priority)");
        assertEquals(8,  result.getGanttChart().get(2).getStart());
        assertEquals(12, result.getGanttChart().get(2).getEnd());

        assertEquals(0, find(result, "P1").getWaitingTime(), "P1 starts immediately");
        assertEquals(3, find(result, "P3").getWaitingTime(), "P3 waits from arrival 4 until start 7");
        assertEquals(6, find(result, "P2").getWaitingTime(), "P2 waits from arrival 2 until start 8");
    }

    /**
     * P1 runs from 0–10 and four processes queue up behind it.
     * When P1 finishes, Priority runs them in priority order: P4(1) → P2(2) → P5(3) → P3(4).
     *
     * Gantt Chart:
     *   [0–10] P1  [10–14] P4  [14–18] P2  [18–23] P5  [23–27] P3
     */
    @Test
    public void testFourProcessesQueuedWhileLongJobRuns() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 10, 5));
        processes.add(p("P2", 1,  4, 2));
        processes.add(p("P3", 2,  4, 4));
        processes.add(p("P4", 3,  4, 1));
        processes.add(p("P5", 4,  5, 3));

        ScheduleResult result = new PriorityAlgorithm().runAlgorithm(processes);

        assertEquals("P1", result.getGanttChart().get(0).getProcessId());
        assertEquals("P4", result.getGanttChart().get(1).getProcessId(), "P4 has highest priority (1)");
        assertEquals("P2", result.getGanttChart().get(2).getProcessId(), "P2 next (2)");
        assertEquals("P5", result.getGanttChart().get(3).getProcessId(), "P5 next (3)");
        assertEquals("P3", result.getGanttChart().get(4).getProcessId(), "P3 last (4)");

        assertEquals(0,  result.getGanttChart().get(0).getStart());
        assertEquals(10, result.getGanttChart().get(0).getEnd());
        assertEquals(10, result.getGanttChart().get(1).getStart());
        assertEquals(14, result.getGanttChart().get(1).getEnd());
        assertEquals(14, result.getGanttChart().get(2).getStart());
        assertEquals(18, result.getGanttChart().get(2).getEnd());
        assertEquals(18, result.getGanttChart().get(3).getStart());
        assertEquals(23, result.getGanttChart().get(3).getEnd());
        assertEquals(23, result.getGanttChart().get(4).getStart());
        assertEquals(27, result.getGanttChart().get(4).getEnd());

        assertEquals(0,  find(result, "P1").getWaitingTime(), "P1 starts immediately");
        assertEquals(7,  find(result, "P4").getWaitingTime(), "P4 waited from arrival 3 to start 10");
        assertEquals(13, find(result, "P2").getWaitingTime(), "P2 waited from arrival 1 to start 14");
        assertEquals(14, find(result, "P5").getWaitingTime(), "P5 waited from arrival 4 to start 18");
        assertEquals(21, find(result, "P3").getWaitingTime(), "P3 waited from arrival 2 to start 23");
    }

    /**
     * All processes arrive at time 0 — Priority runs them in ascending priority order.
     *
     * Gantt Chart:
     *   [0–3] P3   [3–8] P1   [8–10] P2
     *
     * Waiting: P3=0, P1=3, P2=8  → avg = 11/3 = 3.67
     */
    @Test
    public void testAllArriveAtZeroRunInPriorityOrder() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 5, 2));
        processes.add(p("P2", 0, 2, 3));
        processes.add(p("P3", 0, 3, 1));

        ScheduleResult result = new PriorityAlgorithm().runAlgorithm(processes);

        assertEquals("P3", result.getGanttChart().get(0).getProcessId(), "Highest priority (1) runs first");
        assertEquals("P1", result.getGanttChart().get(1).getProcessId(), "Next priority (2)");
        assertEquals("P2", result.getGanttChart().get(2).getProcessId(), "Lowest priority (3) runs last");

        assertEquals(0,  result.getGanttChart().get(0).getStart());
        assertEquals(3,  result.getGanttChart().get(0).getEnd());
        assertEquals(3,  result.getGanttChart().get(1).getStart());
        assertEquals(8,  result.getGanttChart().get(1).getEnd());
        assertEquals(8,  result.getGanttChart().get(2).getStart());
        assertEquals(10, result.getGanttChart().get(2).getEnd());

        // Waiting times: P3=0, P1=3, P2=8  → avg = 11/3 = 3.67
        assertEquals(3.67, result.getAvgWaitingTime(),    0.01);
        // Turnaround: P3=3, P1=8, P2=10   → avg = 21/3 = 7.0
        assertEquals(7.0,  result.getAvgTurnaroundTime(), 0.01);
        // Throughput: 3 / 10 = 0.30
        assertEquals(0.30, result.getThroughput(), 0.01);
    }

    /**
     * When two processes have the same priority, the one that arrived earlier runs first.
     *
     * Processes: P1(0,5,2), P2(1,4,2), P3(2,3,2) — all priority 2.
     * P1 runs first (arrived 0), then P2 (arrived 1), then P3 (arrived 2).
     *
     * Gantt Chart:
     *   [0–5] P1   [5–9] P2   [9–12] P3
     */
    @Test
    public void testTieInPriorityBrokenByArrivalTime() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 5, 2));
        processes.add(p("P2", 1, 4, 2));
        processes.add(p("P3", 2, 3, 2));

        ScheduleResult result = new PriorityAlgorithm().runAlgorithm(processes);

        assertEquals("P1", result.getGanttChart().get(0).getProcessId(), "P1 arrived first (time 0)");
        assertEquals("P2", result.getGanttChart().get(1).getProcessId(), "P2 arrived second (time 1)");
        assertEquals("P3", result.getGanttChart().get(2).getProcessId(), "P3 arrived last (time 2)");

        assertEquals(0,  result.getGanttChart().get(0).getStart());
        assertEquals(5,  result.getGanttChart().get(0).getEnd());
        assertEquals(5,  result.getGanttChart().get(1).getStart());
        assertEquals(9,  result.getGanttChart().get(1).getEnd());
        assertEquals(9,  result.getGanttChart().get(2).getStart());
        assertEquals(12, result.getGanttChart().get(2).getEnd());

        // Waiting times: P1=0, P2=4, P3=7  → avg = 11/3 = 3.67
        assertEquals(3.67, result.getAvgWaitingTime(), 0.01);
    }

    /**
     * A single process runs with no competition.
     *
     * Gantt Chart:
     *   [0–5] P1
     */
    @Test
    public void testSingleProcess() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 5, 1));

        ScheduleResult result = new PriorityAlgorithm().runAlgorithm(processes);

        assertEquals(1, result.getGanttChart().size());
        assertEquals("P1", result.getGanttChart().get(0).getProcessId());
        assertEquals(0, result.getGanttChart().get(0).getStart());
        assertEquals(5, result.getGanttChart().get(0).getEnd());

        assertEquals(0.0, result.getAvgWaitingTime(),    0.01, "Single process never waits");
        assertEquals(5.0, result.getAvgTurnaroundTime(), 0.01);
        assertEquals(0.2, result.getThroughput(),         0.01, "1 / 5 = 0.2");
    }

    /**
     * CPU idles between P1 finishing and P2 arriving.
     * Both processes start at their own arrival times — no waiting.
     *
     * Gantt Chart:
     *   [0–4] P1   [idle 4–10]   [10–15] P2
     */
    @Test
    public void testIdleGapBetweenProcesses() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0,  4, 2));
        processes.add(p("P2", 10, 5, 1));

        ScheduleResult result = new PriorityAlgorithm().runAlgorithm(processes);

        assertEquals("P1", result.getGanttChart().get(0).getProcessId());
        assertEquals(0,  result.getGanttChart().get(0).getStart());
        assertEquals(4,  result.getGanttChart().get(0).getEnd());

        assertEquals("P2", result.getGanttChart().get(1).getProcessId());
        assertEquals(10, result.getGanttChart().get(1).getStart(), "P2 starts at its arrival time, not at 4");
        assertEquals(15, result.getGanttChart().get(1).getEnd());

        assertEquals(0.0, result.getAvgWaitingTime(), 0.01, "No waiting when a gap exists before each process");
    }

    /**
     * Lowest priority number (0) should be treated as highest priority
     * and run before processes with priority 1 or higher.
     *
     * Gantt Chart:
     *   [0–3] P2   [3–8] P1
     */
    @Test
    public void testPriorityZeroIsHighestPriority() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 5, 1));
        processes.add(p("P2", 0, 3, 0));

        ScheduleResult result = new PriorityAlgorithm().runAlgorithm(processes);

        assertEquals("P2", result.getGanttChart().get(0).getProcessId(), "Priority 0 is higher than priority 1");
        assertEquals(0,  result.getGanttChart().get(0).getStart());
        assertEquals(3,  result.getGanttChart().get(0).getEnd());

        assertEquals("P1", result.getGanttChart().get(1).getProcessId());
        assertEquals(3,  result.getGanttChart().get(1).getStart());
        assertEquals(8,  result.getGanttChart().get(1).getEnd());

        // Waiting times: P2=0, P1=3  → avg = 1.5
        assertEquals(1.5, result.getAvgWaitingTime(), 0.01);
    }

    /**
     * Processes arrive at staggered times with mixed priorities.
     *
     * P1(0,6,3): runs immediately at 0 (only process available)
     * At time 6: P2(2,4,1) and P3(4,5,2) are both waiting
     * Priority picks P2 (priority 1), then P3 (priority 2)
     *
     * Gantt Chart:
     *   [0–6] P1   [6–10] P2   [10–15] P3
     *
     * Waiting: P1=0, P2=4, P3=6  → avg = 10/3 = 3.33
     */
    @Test
    public void testStaggeredArrivalsWithMixedPriorities() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 6, 3));
        processes.add(p("P2", 2, 4, 1));
        processes.add(p("P3", 4, 5, 2));

        ScheduleResult result = new PriorityAlgorithm().runAlgorithm(processes);

        assertEquals("P1", result.getGanttChart().get(0).getProcessId(), "P1 is only process at time 0");
        assertEquals("P2", result.getGanttChart().get(1).getProcessId(), "P2 has higher priority (1) than P3 (2)");
        assertEquals("P3", result.getGanttChart().get(2).getProcessId(), "P3 runs last");

        assertEquals(0,  result.getGanttChart().get(0).getStart());
        assertEquals(6,  result.getGanttChart().get(0).getEnd());
        assertEquals(6,  result.getGanttChart().get(1).getStart());
        assertEquals(10, result.getGanttChart().get(1).getEnd());
        assertEquals(10, result.getGanttChart().get(2).getStart());
        assertEquals(15, result.getGanttChart().get(2).getEnd());

        assertEquals(0,  find(result, "P1").getWaitingTime(), "P1 starts immediately");
        assertEquals(4,  find(result, "P2").getWaitingTime(), "P2 waited from arrival 2 to start 6");
        assertEquals(6,  find(result, "P3").getWaitingTime(), "P3 waited from arrival 4 to start 10");

        assertEquals(3.33, result.getAvgWaitingTime(),    0.01);
        assertEquals(8.33, result.getAvgTurnaroundTime(), 0.01); // Updated from 8.0
        assertEquals(0.2,  result.getThroughput(),         0.01);
    }

    /**
     * Checks that per-process waiting and turnaround times are stored correctly.
     *
     * Processes: P1(0,5,3), P2(0,5,1), P3(0,5,2)
     *
     * Priority order: P2(1) → P3(2) → P1(3)
     * Gantt Chart:
     *   [0–5] P2   [5–10] P3   [10–15] P1
     *
     * Waiting:    P2=0, P3=5, P1=10
     * Turnaround: P2=5, P3=10, P1=15
     */
    @Test
    public void testPerProcessWaitingAndTurnaroundTimes() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 5, 3));
        processes.add(p("P2", 0, 5, 1));
        processes.add(p("P3", 0, 5, 2));

        ScheduleResult result = new PriorityAlgorithm().runAlgorithm(processes);

        assertEquals(0,  find(result, "P2").getWaitingTime(),    "P2 starts immediately");
        assertEquals(5,  find(result, "P2").getTurnaroundTime(), "P2 turnaround");

        assertEquals(5,  find(result, "P3").getWaitingTime(),    "P3 waits for P2");
        assertEquals(10, find(result, "P3").getTurnaroundTime(), "P3 turnaround");

        assertEquals(10, find(result, "P1").getWaitingTime(),    "P1 waits for P2 and P3");
        assertEquals(15, find(result, "P1").getTurnaroundTime(), "P1 turnaround");

        assertEquals(5.0,  result.getAvgWaitingTime(),    0.01);
        assertEquals(10.0, result.getAvgTurnaroundTime(), 0.01);
        assertEquals(0.2,  result.getThroughput(),         0.01, "3 / 15 = 0.2");
    }
}
