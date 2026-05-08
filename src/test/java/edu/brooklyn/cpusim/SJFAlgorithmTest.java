package edu.brooklyn.cpusim;

import edu.brooklyn.cpusim.algorithm.SJFAlgorithm;
import edu.brooklyn.cpusim.dto.ScheduleResult;
import edu.brooklyn.cpusim.model.Process;
import edu.brooklyn.cpusim.model.ProcessResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * These are the tests for the SJF scheduling algorithm.
 *
 * SJF is non-preemptive: once a process starts, it runs to completion.
 * When the CPU is free, it picks the shortest available process.
 * If no process has arrived yet, the CPU idles until one does.
 * Ties in burst time are broken by arrival time (earlier arrival runs first).
 */
public class SJFAlgorithmTest {

    // Creates a Process with priority 0
    private Process p(String id, int arrival, int burst) {
        return new Process(id, arrival, burst, 0);
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
     * When P1 finishes, SJF picks P3 (burst 1) before P2 (burst 4).
     *
     * Gantt Chart:
     *   [0–7] P1   [7–8] P3   [8–12] P2
     *
     * P1: arrived 0, starts 0, ends 7 → wait=0, turnaround=7
     * P3: arrived 4, starts 7, ends 8 → wait=3, turnaround=4
     * P2: arrived 2, starts 8, ends 12 → wait=6, turnaround=10
     */
    @Test
    public void testShortJobJumpsAheadOfLongerWaitingJob() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 7));
        processes.add(p("P2", 2, 4));
        processes.add(p("P3", 4, 1));

        ScheduleResult result = new SJFAlgorithm().runAlgorithm(processes);

        assertEquals(3, result.getGanttChart().size());

        assertEquals("P1", result.getGanttChart().get(0).getProcessId(), "P1 is the only process at time 0");
        assertEquals(0,  result.getGanttChart().get(0).getStart());
        assertEquals(7,  result.getGanttChart().get(0).getEnd());

        assertEquals("P3", result.getGanttChart().get(1).getProcessId(), "P3 has shorter burst (1) than P2 (4)");
        assertEquals(7,  result.getGanttChart().get(1).getStart());
        assertEquals(8,  result.getGanttChart().get(1).getEnd());

        assertEquals("P2", result.getGanttChart().get(2).getProcessId(), "P2 runs last");
        assertEquals(8,  result.getGanttChart().get(2).getStart());
        assertEquals(12, result.getGanttChart().get(2).getEnd());

        assertEquals(0, find(result, "P1").getWaitingTime(), "P1 starts immediately");
        assertEquals(3, find(result, "P3").getWaitingTime(), "P3 waits from arrival 4 until start 7");
        assertEquals(6, find(result, "P2").getWaitingTime(), "P2 waits from arrival 2 until start 8");
    }

    /**
     * P1 runs from 0–10. By time 10, four processes are queued.
     * SJF picks them in order of burst time: P3(2) → P5(3) → P2(6) → P4(8).
     *
     * Gantt Chart:
     *   [0–10] P1  [10–12] P3  [12–15] P5  [15–21] P2  [21–29] P4
     */
    @Test
    public void testFourProcessesQueuedWhileLongJobRuns() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 10));
        processes.add(p("P2", 1, 6));
        processes.add(p("P3", 2, 2));
        processes.add(p("P4", 3, 8));
        processes.add(p("P5", 4, 3));

        ScheduleResult result = new SJFAlgorithm().runAlgorithm(processes);

        assertEquals("P1", result.getGanttChart().get(0).getProcessId());
        assertEquals("P3", result.getGanttChart().get(1).getProcessId(), "P3 has shortest burst (2) in the ready queue");
        assertEquals("P5", result.getGanttChart().get(2).getProcessId(), "P5 next shortest (3)");
        assertEquals("P2", result.getGanttChart().get(3).getProcessId(), "P2 next (6)");
        assertEquals("P4", result.getGanttChart().get(4).getProcessId(), "P4 longest (8) runs last");

        assertEquals(0,  result.getGanttChart().get(0).getStart());
        assertEquals(10, result.getGanttChart().get(0).getEnd());
        assertEquals(10, result.getGanttChart().get(1).getStart());
        assertEquals(12, result.getGanttChart().get(1).getEnd());
        assertEquals(12, result.getGanttChart().get(2).getStart());
        assertEquals(15, result.getGanttChart().get(2).getEnd());
        assertEquals(15, result.getGanttChart().get(3).getStart());
        assertEquals(21, result.getGanttChart().get(3).getEnd());
        assertEquals(21, result.getGanttChart().get(4).getStart());
        assertEquals(29, result.getGanttChart().get(4).getEnd());

        // Waiting times
        assertEquals(0,  find(result, "P1").getWaitingTime(), "P1 starts immediately");
        assertEquals(8,  find(result, "P3").getWaitingTime(), "P3 waits from arrival 2 to start 10");
        assertEquals(8,  find(result, "P5").getWaitingTime(), "P5 waits from arrival 4 to start 12");
        assertEquals(14, find(result, "P2").getWaitingTime(), "P2 waits from arrival 1 to start 15");
        assertEquals(18, find(result, "P4").getWaitingTime(), "P4 waits from arrival 3 to start 21");
    }

    /**
     * Input is in reverse arrival order. SJF should still sort correctly
     * and pick the shortest available job regardless of list order.
     *
     * P3 arrives first (time 0), then P2 (time 1), then P1 (time 2).
     * At time 9, both P2(3) and P1(5) are waiting — SJF picks P2 first.
     *
     * Gantt Chart:
     *   [0–9] P3   [9–12] P2   [12–17] P1
     */
    @Test
    public void testInputListOrderDoesNotAffectSJFSelection() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 2, 5));
        processes.add(p("P2", 1, 3));
        processes.add(p("P3", 0, 9));

        ScheduleResult result = new SJFAlgorithm().runAlgorithm(processes);

        assertEquals("P3", result.getGanttChart().get(0).getProcessId(), "P3 arrives first at time 0");
        assertEquals("P2", result.getGanttChart().get(1).getProcessId(), "P2 has shorter burst (3) than P1 (5)");
        assertEquals("P1", result.getGanttChart().get(2).getProcessId(), "P1 runs last");

        assertEquals(0,  result.getGanttChart().get(0).getStart());
        assertEquals(9,  result.getGanttChart().get(0).getEnd());
        assertEquals(9,  result.getGanttChart().get(1).getStart());
        assertEquals(12, result.getGanttChart().get(1).getEnd());
        assertEquals(12, result.getGanttChart().get(2).getStart());
        assertEquals(17, result.getGanttChart().get(2).getEnd());
    }

    /**
     * All arrive at time 0 — SJF runs them in ascending burst-time order.
     *
     * Gantt Chart:
     *   [0–1] P3   [1–3] P2   [3–8] P1
     */
    @Test
    public void testAllArriveAtZeroRunInBurstOrder() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 5));
        processes.add(p("P2", 0, 2));
        processes.add(p("P3", 0, 1));

        ScheduleResult result = new SJFAlgorithm().runAlgorithm(processes);

        assertEquals("P3", result.getGanttChart().get(0).getProcessId(), "Shortest burst (1) runs first");
        assertEquals("P2", result.getGanttChart().get(1).getProcessId(), "Next shortest (2)");
        assertEquals("P1", result.getGanttChart().get(2).getProcessId(), "Longest burst (5) runs last");

        assertEquals(0, result.getGanttChart().get(0).getStart());
        assertEquals(1, result.getGanttChart().get(0).getEnd());
        assertEquals(1, result.getGanttChart().get(1).getStart());
        assertEquals(3, result.getGanttChart().get(1).getEnd());
        assertEquals(3, result.getGanttChart().get(2).getStart());
        assertEquals(8, result.getGanttChart().get(2).getEnd());

        // Waiting times: P3=0, P2=1, P1=3 → avg = 4/3 = 1.33
        assertEquals(1.33, result.getAvgWaitingTime(), 0.01);
        // Turnaround: P3=1, P2=3, P1=8 → avg = 12/3 = 4.0
        assertEquals(4.0,  result.getAvgTurnaroundTime(), 0.01);
        // Throughput: 3 / 8 = 0.375
        assertEquals(0.375, result.getThroughput(), 0.01);
    }

    /**
     * All processes arrive at the same non-zero time. CPU idles first, then applies SJF.
     *
     * All arrive at time 10: P1(burst=8), P2(burst=3), P3(burst=5)
     *
     * Gantt Chart:
     *   [idle 0–10]   [10–13] P2   [13–18] P3   [18–26] P1
     */
    @Test
    public void testAllArriveSameNonZeroTimeSJFApplied() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 10, 8));
        processes.add(p("P2", 10, 3));
        processes.add(p("P3", 10, 5));

        ScheduleResult result = new SJFAlgorithm().runAlgorithm(processes);

        assertEquals("P2", result.getGanttChart().get(0).getProcessId(), "Shortest burst (3)");
        assertEquals("P3", result.getGanttChart().get(1).getProcessId(), "Next shortest (5)");
        assertEquals("P1", result.getGanttChart().get(2).getProcessId(), "Longest burst (8)");

        assertEquals(10, result.getGanttChart().get(0).getStart(), "CPU jumps to arrival time 10");
        assertEquals(13, result.getGanttChart().get(0).getEnd());
        assertEquals(13, result.getGanttChart().get(1).getStart());
        assertEquals(18, result.getGanttChart().get(1).getEnd());
        assertEquals(18, result.getGanttChart().get(2).getStart());
        assertEquals(26, result.getGanttChart().get(2).getEnd());

        // Waiting times: P2=0, P3=3, P1=8 → avg = 11/3 = 3.67
        assertEquals(3.67, result.getAvgWaitingTime(), 0.01);
    }

    /**
     * All three processes have the same burst time and arrival time.
     * With no tiebreaker, they run in list order.
     *
     * Gantt Chart:
     *   [0–5] P1   [5–10] P2   [10–15] P3
     */
    @Test
    public void testEqualBurstAndArrivalTimeListOrderPreserved() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 5));
        processes.add(p("P2", 0, 5));
        processes.add(p("P3", 0, 5));

        ScheduleResult result = new SJFAlgorithm().runAlgorithm(processes);

        assertEquals("P1", result.getGanttChart().get(0).getProcessId());
        assertEquals("P2", result.getGanttChart().get(1).getProcessId());
        assertEquals("P3", result.getGanttChart().get(2).getProcessId());

        assertEquals(0,  result.getGanttChart().get(0).getStart());
        assertEquals(5,  result.getGanttChart().get(0).getEnd());
        assertEquals(5,  result.getGanttChart().get(1).getStart());
        assertEquals(10, result.getGanttChart().get(1).getEnd());
        assertEquals(10, result.getGanttChart().get(2).getStart());
        assertEquals(15, result.getGanttChart().get(2).getEnd());
    }

    /**
     * P2 and P3 have the same burst time but P2 arrived earlier.
     * When P1 finishes, both are in the queue so P2 should run first.
     *
     * Gantt Chart:
     *   [0–6] P1   [6–10] P2   [10–14] P3
     */
    @Test
    public void testEqualBurstDifferentArrivalEarlierArrivalRunsFirst() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 6));
        processes.add(p("P2", 1, 4));
        processes.add(p("P3", 3, 4));

        ScheduleResult result = new SJFAlgorithm().runAlgorithm(processes);

        assertEquals("P1", result.getGanttChart().get(0).getProcessId());
        assertEquals("P2", result.getGanttChart().get(1).getProcessId(), "P2 breaks tie — arrived earlier than P3");
        assertEquals("P3", result.getGanttChart().get(2).getProcessId());

        assertEquals(6,  result.getGanttChart().get(1).getStart());
        assertEquals(10, result.getGanttChart().get(1).getEnd());
        assertEquals(10, result.getGanttChart().get(2).getStart());
        assertEquals(14, result.getGanttChart().get(2).getEnd());
    }

    /**
     * The first process arrives late so the CPU idles from 0 until time 5.
     * After P1, both P2 and P3 are in the queue so SJF picks P2 (burst 2) first.
     *
     * Gantt Chart:
     *   [idle 0–5]   [5–8] P1   [8–10] P2   [10–14] P3
     */
    @Test
    public void testCPUIdlesUntilFirstProcessArrives() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 5, 3));
        processes.add(p("P2", 6, 2));
        processes.add(p("P3", 7, 4));

        ScheduleResult result = new SJFAlgorithm().runAlgorithm(processes);

        assertEquals("P1", result.getGanttChart().get(0).getProcessId(), "P1 is the only process at time 5");
        assertEquals(5,  result.getGanttChart().get(0).getStart(), "CPU jumps to arrival time 5");
        assertEquals(8,  result.getGanttChart().get(0).getEnd());

        assertEquals("P2", result.getGanttChart().get(1).getProcessId(), "P2 (burst 2) is shorter than P3 (burst 4)");
        assertEquals(8,  result.getGanttChart().get(1).getStart());
        assertEquals(10, result.getGanttChart().get(1).getEnd());

        assertEquals("P3", result.getGanttChart().get(2).getProcessId());
        assertEquals(10, result.getGanttChart().get(2).getStart());
        assertEquals(14, result.getGanttChart().get(2).getEnd());

        assertEquals(0, find(result, "P1").getWaitingTime());
        assertEquals(2, find(result, "P2").getWaitingTime());
        assertEquals(3, find(result, "P3").getWaitingTime(), "P3 arrived at 7, starts at 10, wait = 10-7 = 3");
    }

    /**
     * Three separate idle gaps. Each process starts exactly at its arrival time,
     * so no process waits.
     *
     * Gantt Chart:
     *   [idle 0–5]   [5–8] P1   [idle 8–20]   [20–23] P2   [idle 23–50]   [50–55] P3
     */
    @Test
    public void testMultipleIdleGapsNoWaiting() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 5,  3));
        processes.add(p("P2", 20, 3));
        processes.add(p("P3", 50, 5));

        ScheduleResult result = new SJFAlgorithm().runAlgorithm(processes);

        assertEquals(5,  result.getGanttChart().get(0).getStart());
        assertEquals(8,  result.getGanttChart().get(0).getEnd());
        assertEquals(20, result.getGanttChart().get(1).getStart(), "CPU jumps to P2 arrival 20");
        assertEquals(23, result.getGanttChart().get(1).getEnd());
        assertEquals(50, result.getGanttChart().get(2).getStart(), "CPU jumps to P3 arrival 50");
        assertEquals(55, result.getGanttChart().get(2).getEnd());

        assertEquals(0.0, result.getAvgWaitingTime(), 0.01, "No process ever waits — each starts on arrival");
    }

    /**
     * P1 finishes before P2 arrives, causing a mid-schedule idle gap.
     * Once both P2 and P3 are available at time 10, SJF picks P3 (burst 3) first.
     *
     * Gantt Chart:
     *   [0–4] P1   [idle 4–10]   [10–13] P3   [13–19] P2
     */
    @Test
    public void testIdleGapMidScheduleThenSJFApplied() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0,  4));
        processes.add(p("P2", 8,  6));
        processes.add(p("P3", 10, 3));

        ScheduleResult result = new SJFAlgorithm().runAlgorithm(processes);

        assertEquals("P1", result.getGanttChart().get(0).getProcessId());
        assertEquals(0,  result.getGanttChart().get(0).getStart());
        assertEquals(4,  result.getGanttChart().get(0).getEnd());

        assertEquals("P2", result.getGanttChart().get(1).getProcessId());
        assertEquals(8, result.getGanttChart().get(1).getStart());
        assertEquals(14, result.getGanttChart().get(1).getEnd());

        assertEquals("P3", result.getGanttChart().get(2).getProcessId());
        assertEquals(14, result.getGanttChart().get(2).getStart());
        assertEquals(17, result.getGanttChart().get(2).getEnd());
    }

    /**
     * P2 arrives while P1 is running but cannot interrupt it.
     * P2 must wait in the queue until P1 finishes.
     *
     * Gantt Chart:
     *   [0–10] P1   [10–11] P2   [11–16] P3
     */
    @Test
    public void testShortProcessCannotPreemptRunningProcess() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 10));
        processes.add(p("P2", 1, 1));
        processes.add(p("P3", 2, 5));

        ScheduleResult result = new SJFAlgorithm().runAlgorithm(processes);

        assertEquals("P1", result.getGanttChart().get(0).getProcessId());
        assertEquals(0,  result.getGanttChart().get(0).getStart());
        assertEquals(10, result.getGanttChart().get(0).getEnd());

        assertEquals("P2", result.getGanttChart().get(1).getProcessId());
        assertEquals(10, result.getGanttChart().get(1).getStart());
        assertEquals(11, result.getGanttChart().get(1).getEnd());

        // P2 waited from arrival 1 to start 10 → waiting time = 9
        assertEquals(9, find(result, "P2").getWaitingTime());
    }

    /**
     * P3 arrives at exactly the moment P1 finishes.
     * P3 and P2 are both in the queue so SJF picks P3 (burst 2) first.
     *
     * Gantt Chart:
     *   [0–5] P1   [5–7] P3   [7–12] P2
     */
    @Test
    public void testProcessArrivesExactlyWhenCurrentProcessEnds() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 5));
        processes.add(p("P2", 3, 5));
        processes.add(p("P3", 5, 2));

        ScheduleResult result = new SJFAlgorithm().runAlgorithm(processes);

        assertEquals("P3", result.getGanttChart().get(1).getProcessId(), "P3 arrives right as P1 ends and has shorter burst so its picked immediately");
        assertEquals(5, result.getGanttChart().get(1).getStart());
        assertEquals(7, result.getGanttChart().get(1).getEnd());

        assertEquals("P2", result.getGanttChart().get(2).getProcessId());
        assertEquals(7,  result.getGanttChart().get(2).getStart());
        assertEquals(12, result.getGanttChart().get(2).getEnd());
    }

    /**
     * Only one process arriving at time 0. It runs immediately with no waiting.
     */
    @Test
    public void testSingleProcessArrivesAtZero() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 8));

        ScheduleResult result = new SJFAlgorithm().runAlgorithm(processes);

        assertEquals(1, result.getGanttChart().size());
        assertEquals("P1", result.getGanttChart().get(0).getProcessId());
        assertEquals(0, result.getGanttChart().get(0).getStart());
        assertEquals(8, result.getGanttChart().get(0).getEnd());

        assertEquals(0.0, result.getAvgWaitingTime(),    0.01, "No waiting for a single process");
        assertEquals(8.0, result.getAvgTurnaroundTime(), 0.01, "Turnaround equals burst time");
        assertEquals(0.125, result.getThroughput(), 0.01, "1 / 8 = 0.125");
    }

    /**
     * Only one process, arriving late. CPU idles until it arrives.
     * Waiting time is 0 because the process starts the moment it arrives.
     */
    @Test
    public void testSingleProcessArrivesLate() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 30, 5));

        ScheduleResult result = new SJFAlgorithm().runAlgorithm(processes);

        assertEquals(30, result.getGanttChart().get(0).getStart(), "CPU jumps to arrival time 30");
        assertEquals(35, result.getGanttChart().get(0).getEnd());

        assertEquals(0.0, result.getAvgWaitingTime(),    0.01, "No queue so process starts on arrival");
        assertEquals(5.0, result.getAvgTurnaroundTime(), 0.01);
        // Throughput: 1 / 35 ≈ 0.0286
        assertEquals(0.0286, result.getThroughput(), 0.001);
    }

    /**
     * Four processes each with burst=1, arriving one unit apart.
     * Since all bursts are equal, SJF order matches arrival order.
     *
     * Gantt Chart:
     *   [0–1] P1   [1–2] P2   [2–3] P3   [3–4] P4
     */
    @Test
    public void testAllMinimumBurstTime() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 1));
        processes.add(p("P2", 1, 1));
        processes.add(p("P3", 2, 1));
        processes.add(p("P4", 3, 1));

        ScheduleResult result = new SJFAlgorithm().runAlgorithm(processes);

        for (int i = 0; i < 4; i++) {
            assertEquals("P" + (i + 1), result.getGanttChart().get(i).getProcessId());
            assertEquals(i,     result.getGanttChart().get(i).getStart());
            assertEquals(i + 1, result.getGanttChart().get(i).getEnd());
        }

        // Every process starts exactly on arrival → avg waiting = 0
        assertEquals(0.0, result.getAvgWaitingTime(), 0.01);
        // Throughput: 4 / 4 = 1.0
        assertEquals(1.0, result.getThroughput(), 0.01);
    }

    /**
     * P3 has burst=1 and arrives last, but should still be scheduled
     * before the longer waiting jobs once P1 finishes.
     *
     * Gantt Chart:
     *   [0–8] P1   [8–9] P3   [9–14] P2
     */
    @Test
    public void testOneMinimumBurstProcessScheduledFirst() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 8));
        processes.add(p("P2", 1, 5));
        processes.add(p("P3", 2, 1));

        ScheduleResult result = new SJFAlgorithm().runAlgorithm(processes);

        assertEquals("P1", result.getGanttChart().get(0).getProcessId());
        assertEquals("P3", result.getGanttChart().get(1).getProcessId(), "P3 (burst 1) is shortest in the queue");
        assertEquals("P2", result.getGanttChart().get(2).getProcessId());

        assertEquals(8, result.getGanttChart().get(1).getStart());
        assertEquals(9, result.getGanttChart().get(1).getEnd());

        assertEquals(0, find(result, "P3").getWaitingTime() == 6 ? 0 : 1);
        assertEquals(6, find(result, "P3").getWaitingTime(), "P3 waited from arrival 2 until start 8");
    }

    /**
     * Two processes with maximum burst time (99), both arriving at time 0.
     * Equal burst, so they run in list order.
     *
     * Gantt Chart:
     *   [0–99] P1   [99–198] P2
     */
    @Test
    public void testMaximumBurstTimes() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 99));
        processes.add(p("P2", 0, 99));

        ScheduleResult result = new SJFAlgorithm().runAlgorithm(processes);

        assertEquals(0,   result.getGanttChart().get(0).getStart());
        assertEquals(99,  result.getGanttChart().get(0).getEnd());
        assertEquals(99,  result.getGanttChart().get(1).getStart());
        assertEquals(198, result.getGanttChart().get(1).getEnd());

        // Waiting times: P1=0, P2=99 → avg = 49.5
        assertEquals(49.5,  result.getAvgWaitingTime(), 0.01);
        // Turnaround: P1=99, P2=198 → avg = 148.5
        assertEquals(148.5, result.getAvgTurnaroundTime(), 0.01);
    }

    /**
     * A short job (burst=1) and a max-burst job (burst=99) both arrive at time 0.
     * SJF picks the short one first.
     *
     * Gantt Chart:
     *   [0–1] P2   [1–100] P1
     */
    @Test
    public void testShortJobBeforeMaxBurstJob() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 99));
        processes.add(p("P2", 0, 1));

        ScheduleResult result = new SJFAlgorithm().runAlgorithm(processes);

        assertEquals("P2", result.getGanttChart().get(0).getProcessId(), "Shortest (1) runs before longest (99)");
        assertEquals(0,   result.getGanttChart().get(0).getStart());
        assertEquals(1,   result.getGanttChart().get(0).getEnd());

        assertEquals("P1", result.getGanttChart().get(1).getProcessId());
        assertEquals(1,   result.getGanttChart().get(1).getStart());
        assertEquals(100, result.getGanttChart().get(1).getEnd());

        assertEquals(0.5, result.getAvgWaitingTime(), 0.01);
    }

    /**
     * Single process with the maximum allowed arrival time of 99.
     * CPU idles from 0 to 99, then runs the process.
     */
    @Test
    public void testMaximumArrivalTime() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 99, 1));

        ScheduleResult result = new SJFAlgorithm().runAlgorithm(processes);

        assertEquals(99,  result.getGanttChart().get(0).getStart(), "CPU waits until arrival 99");
        assertEquals(100, result.getGanttChart().get(0).getEnd());

        assertEquals(0.0, result.getAvgWaitingTime(),    0.01);
        assertEquals(1.0, result.getAvgTurnaroundTime(), 0.01);
    }

    /**
     * P1: arrival=0, burst=6  → runs 0–6,   wait=0,  turnaround=6
     * P2: arrival=2, burst=2  → runs 6–8,   wait=4,  turnaround=6
     * P3: arrival=4, burst=8  → runs 8–16,  wait=4,  turnaround=12
     * P4: arrival=6, burst=3  → runs 16-19, wait=10, turnaround=13
     *
     * At time 6: queue = [P2(2), P3(8), P4(3)] → picks P2
     * At time 8: queue = [P3(8), P4(3)] → picks P4
     *
     * Wait: avg = (0 + 4 + 4 + 10) / 4 = 4.5
     * Turnaround: avg = (6 + 6 + 12 + 13) / 4 = 9.25
     * Throughput: 4 / 19
     */
    @Test
    public void testAveragesAndThroughputMixedScenario() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 6));
        processes.add(p("P2", 2, 2));
        processes.add(p("P3", 4, 8));
        processes.add(p("P4", 6, 3));

        ScheduleResult result = new SJFAlgorithm().runAlgorithm(processes);

        assertEquals("P1", result.getGanttChart().get(0).getProcessId());
        assertEquals("P2", result.getGanttChart().get(1).getProcessId(), "P2 shortest (2) at time 6");
        assertEquals("P4", result.getGanttChart().get(2).getProcessId(), "P4 next shortest (3) at time 8");
        assertEquals("P3", result.getGanttChart().get(3).getProcessId(), "P3 longest (8) runs last");

        assertEquals(0,  result.getGanttChart().get(0).getStart());
        assertEquals(6,  result.getGanttChart().get(0).getEnd());
        assertEquals(6,  result.getGanttChart().get(1).getStart());
        assertEquals(8,  result.getGanttChart().get(1).getEnd());
        assertEquals(8,  result.getGanttChart().get(2).getStart());
        assertEquals(11, result.getGanttChart().get(2).getEnd());
        assertEquals(11, result.getGanttChart().get(3).getStart());
        assertEquals(19, result.getGanttChart().get(3).getEnd());

        assertEquals(0, find(result, "P1").getWaitingTime());
        assertEquals(4, find(result, "P2").getWaitingTime());
        assertEquals(2, find(result, "P4").getWaitingTime(), "P4 arrived 6, starts 8 → wait=2");
        assertEquals(7, find(result, "P3").getWaitingTime(), "P3 arrived 4, starts 11 → wait=7");

        assertEquals(3.25, result.getAvgWaitingTime(), 0.01);

        assertEquals(8.0, result.getAvgTurnaroundTime(), 0.01);

        assertEquals(0.210, result.getThroughput(), 0.01);
    }

    /**
     * Verifies that throughput uses the end time of the last process,
     * not the sum of burst times.
     *
     * Gantt Chart:
     *   [0–2] P2   [2–99] P1 (last end = 99)
     * Throughput = 2 / 99 = 0.0202
     */
    @Test
    public void testThroughputUsesLastEndTime() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 97));
        processes.add(p("P2", 0, 2));

        ScheduleResult result = new SJFAlgorithm().runAlgorithm(processes);

        assertEquals("P2", result.getGanttChart().get(0).getProcessId());
        assertEquals(0,  result.getGanttChart().get(0).getStart());
        assertEquals(2,  result.getGanttChart().get(0).getEnd());
        assertEquals(2,  result.getGanttChart().get(1).getStart());
        assertEquals(99, result.getGanttChart().get(1).getEnd());

        assertEquals(0.0202, result.getThroughput(), 0.001);
    }
}