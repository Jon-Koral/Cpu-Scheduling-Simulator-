package edu.brooklyn.cpusim;

import edu.brooklyn.cpusim.algorithm.SRTFAlgorithm;
import edu.brooklyn.cpusim.dto.ScheduleResult;
import edu.brooklyn.cpusim.model.GanttEntry;
import edu.brooklyn.cpusim.model.Process;
import edu.brooklyn.cpusim.model.ProcessResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
/**
 * These are the tests for the SRTF scheduling algorithm.
 *
 * SRTF is preemptive: when a new process arrives, the CPU checks if it has less
 * remaining burst time than the currently running process. If so, it immediately
 * takes over and the interrupted process goes back to the ready queue.
 * If no process is available, the CPU idles until the next arrival.
 * Ties in remaining time are broken by process ID (smaller ID runs first).
 */
public class SRTFAlgorithmTest {

    // Creates a Process with priority 0 (SRTF ignores priority)
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

     * P1 starts at 0. At time 2, P2 arrives (remaining 4 < P1's remaining 5) -> P2 preempts P1.
     * At time 4, P3 arrives (remaining 1 < P2's remaining 2) -> P3 preempts P2.
     * P3 finishes at 5. P2 resumes (remaining 2), finishes at 7. P1 resumes, finishes at 12.
     *
     * Gantt Chart:
     *   [0-2] P1  [2-4] P2  [4-5] P3  [5-7] P2  [7-12] P1
     *
     * P1: arrived 0, completed 12 -> turnaround=12, wait=5
     * P2: arrived 2, completed 7 -> turnaround=5, wait=1
     * P3: arrived 4, completed 5 -> turnaround=1, wait=0
     */
    @Test
    public void testBasicPreemptionChain() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 7));
        processes.add(p("P2", 2, 4));
        processes.add(p("P3", 4, 1));

        ScheduleResult result = new SRTFAlgorithm().runAlgorithm(processes);

        List<GanttEntry> g = result.getGanttChart();
        assertEquals(5, g.size(), "Three preemptions produce 5 Gantt segments");

        assertEquals("P1", g.get(0).getProcessId());
        assertEquals(0,  g.get(0).getStart());
        assertEquals(2,  g.get(0).getEnd());

        assertEquals("P2", g.get(1).getProcessId());
        assertEquals(2,  g.get(1).getStart());
        assertEquals(4,  g.get(1).getEnd());

        assertEquals("P3", g.get(2).getProcessId());
        assertEquals(4,  g.get(2).getStart());
        assertEquals(5,  g.get(2).getEnd());

        assertEquals("P2", g.get(3).getProcessId());
        assertEquals(5,  g.get(3).getStart());
        assertEquals(7,  g.get(3).getEnd());

        assertEquals("P1", g.get(4).getProcessId());
        assertEquals(7,  g.get(4).getStart());
        assertEquals(12, g.get(4).getEnd());

        assertEquals(5, find(result, "P1").getWaitingTime(), "P1 turnaround 12 - burst 7");
        assertEquals(1, find(result, "P2").getWaitingTime(), "P2 turnaround 5 - burst 4");
        assertEquals(0, find(result, "P3").getWaitingTime(), "P3 ran immediately, never waited");
    }

    /**
     * Every new arrival is longer than the running process, so no preemption happens.
     * P1 runs uninterrupted from start to finish.
     *
     * Gantt Chart (no preemption):
     *   [0-2] P1   [2-7] P2   [7-15] P3
     */
    @Test
    public void testNoPreemptionWhenNewArrivalIsLonger() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 2));
        processes.add(p("P2", 1, 5));
        processes.add(p("P3", 2, 8));

        ScheduleResult result = new SRTFAlgorithm().runAlgorithm(processes);

        List<GanttEntry> g = result.getGanttChart();
        assertEquals(3, g.size(), "No preemptions — one segment per process");

        assertEquals("P1", g.get(0).getProcessId());
        assertEquals(0, g.get(0).getStart());
        assertEquals(2, g.get(0).getEnd());

        assertEquals("P2", g.get(1).getProcessId());
        assertEquals(2, g.get(1).getStart());
        assertEquals(7, g.get(1).getEnd());

        assertEquals("P3", g.get(2).getProcessId());
        assertEquals(7,  g.get(2).getStart());
        assertEquals(15, g.get(2).getEnd());

        assertEquals(0, find(result, "P1").getWaitingTime(), "P1 never waited");
        assertEquals(1, find(result, "P2").getWaitingTime(), "P2 arrived at 1, started at 2");
        assertEquals(5, find(result, "P3").getWaitingTime(), "P3 arrived at 2, started at 7");
    }

    /**
     * P1 gets preempted three times, once by each of P2, P3, and P4.
     * Each preemptor has burst=1 and arrives 1 unit apart, so P1 never gets
     * a chance to resume until all three are done.
     *
     * At t=1: P1 remaining=9, P2 remaining=1 -> P2 preempts
     * At t=2: P1 remaining=9, P3 remaining=1 -> P3 preempts
     * At t=3: P1 remaining=9, P4 remaining=1 -> P4 preempts
     * At t=4: only P1 left, runs 4-13
     *
     * Gantt Chart:
     *   [0-1] P1  [1-2] P2  [2-3] P3  [3-4] P4  [4-13] P1
     */
    @Test
    public void testProcessPreemptedMultipleTimes() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 10));
        processes.add(p("P2", 1, 1));
        processes.add(p("P3", 2, 1));
        processes.add(p("P4", 3, 1));

        ScheduleResult result = new SRTFAlgorithm().runAlgorithm(processes);

        List<GanttEntry> g = result.getGanttChart();
        assertEquals(5, g.size(), "P1 is interrupted 3 times, creating 2 P1 segments + 3 short segments");

        assertEquals("P1", g.get(0).getProcessId());
        assertEquals(0, g.get(0).getStart());
        assertEquals(1, g.get(0).getEnd());

        assertEquals("P2", g.get(1).getProcessId());
        assertEquals(1, g.get(1).getStart());
        assertEquals(2, g.get(1).getEnd());

        assertEquals("P3", g.get(2).getProcessId());
        assertEquals(2, g.get(2).getStart());
        assertEquals(3, g.get(2).getEnd());

        assertEquals("P4", g.get(3).getProcessId());
        assertEquals(3, g.get(3).getStart());
        assertEquals(4, g.get(3).getEnd());

        assertEquals("P1", g.get(4).getProcessId());
        assertEquals(4,  g.get(4).getStart());
        assertEquals(13, g.get(4).getEnd());

        // P1: completed at 13, arrived at 0 -> turnaround=13, wait=3
        assertEquals(3, find(result, "P1").getWaitingTime(), "P1 was pushed aside 3 times, each for 1 unit");
        assertEquals(0, find(result, "P2").getWaitingTime());
        assertEquals(0, find(result, "P3").getWaitingTime());
        assertEquals(0, find(result, "P4").getWaitingTime());
    }

    /**
     * P1 and P2 arrive at the same time with equal burst times.
     * P1 has a smaller ID so it runs first. No preemption occurs.
     *
     * Gantt Chart:
     *   [0-4] P1   [4-8] P2
     */
    @Test
    public void testEqualBurstSameArrivalSmallerIdRunsFirst() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 4));
        processes.add(p("P2", 0, 4));

        ScheduleResult result = new SRTFAlgorithm().runAlgorithm(processes);

        List<GanttEntry> g = result.getGanttChart();
        assertEquals(2, g.size());

        assertEquals("P1", g.get(0).getProcessId(), "P1 runs first — smaller ID wins the tie");
        assertEquals(0, g.get(0).getStart());
        assertEquals(4, g.get(0).getEnd());

        assertEquals("P2", g.get(1).getProcessId());
        assertEquals(4, g.get(1).getStart());
        assertEquals(8, g.get(1).getEnd());
    }

    /**
     * P2 arrives when P1 has exactly the same remaining time as P2's burst.
     * They are tied — P1 (smaller ID) keeps the CPU and finishes uninterrupted.
     *
     * P1: arrival=0, burst=5. At time 2, P1 has 3 remaining.
     * P2: arrival=2, burst=3. P2 remaining=3, P1 remaining=3 — tied. P1 keeps the CPU.
     *
     * Gantt Chart (no preemption):
     *   [0-5] P1   [5-8] P2
     */
    @Test
    public void testEqualRemainingTimeNoPreemptionSmallerIdKeepsCPU() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 5));
        processes.add(p("P2", 2, 3));

        ScheduleResult result = new SRTFAlgorithm().runAlgorithm(processes);

        List<GanttEntry> g = result.getGanttChart();
        assertEquals(2, g.size(), "No preemption — P1 keeps CPU on tie (smaller ID)");

        assertEquals("P1", g.get(0).getProcessId());
        assertEquals(0, g.get(0).getStart());
        assertEquals(5, g.get(0).getEnd());

        assertEquals("P2", g.get(1).getProcessId());
        assertEquals(5, g.get(1).getStart());
        assertEquals(8, g.get(1).getEnd());
    }

    /**
     * P3 arrives with strictly less remaining time than P1 — preemption happens.
     *
     * P1: arrival=0, burst=5. At time 2, P1 has 3 remaining.
     * P3: arrival=2, burst=2. P3 remaining=2 < P1 remaining=3 -> P3 preempts P1.
     *
     * Gantt Chart:
     *   [0-2] P1  [2-4] P3  [4-7] P1
     */
    @Test
    public void testPreemptionOccursWhenNewArrivalIsStrictlyShorter() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 5));
        processes.add(p("P3", 2, 2));

        ScheduleResult result = new SRTFAlgorithm().runAlgorithm(processes);

        List<GanttEntry> g = result.getGanttChart();
        assertEquals(3, g.size(), "P3 preempts P1 — should be 3 segments");

        assertEquals("P1", g.get(0).getProcessId());
        assertEquals(0, g.get(0).getStart());
        assertEquals(2, g.get(0).getEnd());

        assertEquals("P3", g.get(1).getProcessId());
        assertEquals(2, g.get(1).getStart());
        assertEquals(4, g.get(1).getEnd());

        assertEquals("P1", g.get(2).getProcessId());
        assertEquals(4, g.get(2).getStart());
        assertEquals(7, g.get(2).getEnd());
    }

    /**
     * All processes arrive late so the CPU idles from 0 until the first arrival.
     * P1 and P2 are tied on remaining time at t=6, so P1 (smaller ID) keeps the CPU.
     *
     * Gantt Chart:
     *   [idle 0-5]   [5-8] P1   [8-10] P2
     */
    @Test
    public void testCPUIdlesUntilFirstArrival() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 5, 3));
        processes.add(p("P2", 6, 2));

        ScheduleResult result = new SRTFAlgorithm().runAlgorithm(processes);

        List<GanttEntry> g = result.getGanttChart();

        // At t=5: only P1 in queue, starts. At t=6: P2 arrives with remaining=2, P1 has remaining=2.
        // Tie -> smaller ID (P1) keeps CPU. P1 finishes at 8. Then P2 runs 8-10.
        assertEquals("P1", g.get(0).getProcessId());
        assertEquals(5,  g.get(0).getStart());
        assertEquals(8,  g.get(0).getEnd());

        assertEquals("P2", g.get(1).getProcessId());
        assertEquals(8,  g.get(1).getStart());
        assertEquals(10, g.get(1).getEnd());

        assertEquals(0, find(result, "P1").getWaitingTime(), "P1 started on arrival, never preempted");
        assertEquals(2, find(result, "P2").getWaitingTime(), "P2 arrived at 6, started at 8 -> wait=2");
    }

    /**
     * P1 finishes before P2 arrives, leaving an idle gap mid-schedule.
     *
     * Gantt Chart:
     *   [0-3] P1   [idle 3-10]   [10-13] P2
     */
    @Test
    public void testIdleGapMidSchedule() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0,  3));
        processes.add(p("P2", 10, 3));

        ScheduleResult result = new SRTFAlgorithm().runAlgorithm(processes);

        List<GanttEntry> g = result.getGanttChart();
        assertEquals(2, g.size());

        assertEquals("P1", g.get(0).getProcessId());
        assertEquals(0,  g.get(0).getStart());
        assertEquals(3,  g.get(0).getEnd());

        assertEquals("P2", g.get(1).getProcessId());
        assertEquals(10, g.get(1).getStart(), "CPU jumps to P2 arrival time 10");
        assertEquals(13, g.get(1).getEnd());

        assertEquals(0.0, result.getAvgWaitingTime(), 0.01, "Neither process ever waited");
    }

    /**
     * Multiple separate idle gaps. Each process starts exactly when it arrives
     * and no preemption is possible since they never overlap.
     *
     * Gantt Chart:
     *   [idle 0-5]   [5-7] P1   [idle 7-20]   [20-23] P2   [idle 23-50]   [50-54] P3
     */
    @Test
    public void testMultipleIdleGapsNoOverlap() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 5,  2));
        processes.add(p("P2", 20, 3));
        processes.add(p("P3", 50, 4));

        ScheduleResult result = new SRTFAlgorithm().runAlgorithm(processes);

        List<GanttEntry> g = result.getGanttChart();
        assertEquals(3, g.size());

        assertEquals(5,  g.get(0).getStart());
        assertEquals(7,  g.get(0).getEnd());
        assertEquals(20, g.get(1).getStart());
        assertEquals(23, g.get(1).getEnd());
        assertEquals(50, g.get(2).getStart());
        assertEquals(54, g.get(2).getEnd());

        assertEquals(0.0, result.getAvgWaitingTime(), 0.01);
    }

    /**
     * All arrive at time 0 so SRTF behaves like SJF — no mid-run arrivals, no preemption.
     * Shortest burst runs first.
     *
     * Gantt Chart:
     *   [0-1] P3   [1-3] P2   [3-8] P1
     */
    @Test
    public void testAllArriveAtZeroActsLikeSJF() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 5));
        processes.add(p("P2", 0, 2));
        processes.add(p("P3", 0, 1));

        ScheduleResult result = new SRTFAlgorithm().runAlgorithm(processes);

        List<GanttEntry> g = result.getGanttChart();
        assertEquals(3, g.size(), "All arrive at once — no preemptions, one segment each");

        assertEquals("P3", g.get(0).getProcessId(), "Shortest burst (1) runs first");
        assertEquals(0, g.get(0).getStart());
        assertEquals(1, g.get(0).getEnd());

        assertEquals("P2", g.get(1).getProcessId(), "Next shortest (2)");
        assertEquals(1, g.get(1).getStart());
        assertEquals(3, g.get(1).getEnd());

        assertEquals("P1", g.get(2).getProcessId(), "Longest (5) runs last");
        assertEquals(3, g.get(2).getStart());
        assertEquals(8, g.get(2).getEnd());

        // Waiting times: P3=0, P2=1, P1=3  ->  avg = 4/3 = 1.33
        assertEquals(1.33, result.getAvgWaitingTime(),    0.01);
        // Turnaround: P3=1, P2=3, P1=8  ->  avg = 12/3 = 4.0
        assertEquals(4.0,  result.getAvgTurnaroundTime(), 0.01);
        // Throughput: 3 / 8 = 0.375
        assertEquals(0.375, result.getThroughput(), 0.01);
    }

    /**
     * All processes arrive at the same non-zero time. CPU idles first, then runs shortest first.
     *
     * All arrive at time 15: P1(burst=7), P2(burst=3), P3(burst=5)
     *
     * Gantt Chart:
     *   [idle 0-15]   [15-18] P2   [18-23] P3   [23-30] P1
     */
    @Test
    public void testAllArriveSameNonZeroTime() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 15, 7));
        processes.add(p("P2", 15, 3));
        processes.add(p("P3", 15, 5));

        ScheduleResult result = new SRTFAlgorithm().runAlgorithm(processes);

        List<GanttEntry> g = result.getGanttChart();
        assertEquals(3, g.size());

        assertEquals("P2", g.get(0).getProcessId(), "Shortest burst (3) first");
        assertEquals(15, g.get(0).getStart(), "CPU jumps to arrival time 15");
        assertEquals(18, g.get(0).getEnd());

        assertEquals("P3", g.get(1).getProcessId());
        assertEquals(18, g.get(1).getStart());
        assertEquals(23, g.get(1).getEnd());

        assertEquals("P1", g.get(2).getProcessId());
        assertEquals(23, g.get(2).getStart());
        assertEquals(30, g.get(2).getEnd());
    }

    /**
     * P1 is preempted once by P2, then resumes. The Gantt chart must show
     * two separate P1 entries — not one merged entry.
     *
     * P1: arrival=0, burst=7. At t=3, P1 has 4 remaining.
     * P2: arrival=3, burst=2. P2 remaining=2 < P1 remaining=4 -> preempts.
     *
     * Gantt Chart:
     *   [0-3] P1  [3-5] P2  [5-9] P1
     */
    @Test
    public void testGanttChartSplitsOnPreemption() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 7));
        processes.add(p("P2", 3, 2));

        ScheduleResult result = new SRTFAlgorithm().runAlgorithm(processes);

        List<GanttEntry> g = result.getGanttChart();
        assertEquals(3, g.size(), "P1 runs, P2 preempts, P1 resumes — exactly 3 segments");

        assertEquals("P1", g.get(0).getProcessId());
        assertEquals(0, g.get(0).getStart());
        assertEquals(3, g.get(0).getEnd());

        assertEquals("P2", g.get(1).getProcessId());
        assertEquals(3, g.get(1).getStart());
        assertEquals(5, g.get(1).getEnd());

        assertEquals("P1", g.get(2).getProcessId(), "P1 resumes — this is a separate Gantt segment, not merged");
        assertEquals(5, g.get(2).getStart());
        assertEquals(9, g.get(2).getEnd());
    }

    /**
     * No preemption at all — three processes each run once.
     * Gantt chart has exactly 3 entries.
     *
     * Gantt Chart:
     *   [0-2] P1   [2-5] P2   [5-9] P3
     */
    @Test
    public void testGanttChartHasOneEntryPerProcessWhenNoPreemption() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 2));
        processes.add(p("P2", 0, 3));
        processes.add(p("P3", 0, 4));

        ScheduleResult result = new SRTFAlgorithm().runAlgorithm(processes);

        assertEquals(3, result.getGanttChart().size(), "No preemptions — exactly 3 Gantt entries");
    }

    /**
     * Only one process arriving at time 0. Nothing to preempt it, so it runs straight through.
     */
    @Test
    public void testSingleProcessArrivesAtZero() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 6));

        ScheduleResult result = new SRTFAlgorithm().runAlgorithm(processes);

        assertEquals(1, result.getGanttChart().size());
        assertEquals("P1", result.getGanttChart().get(0).getProcessId());
        assertEquals(0, result.getGanttChart().get(0).getStart());
        assertEquals(6, result.getGanttChart().get(0).getEnd());

        assertEquals(0.0, result.getAvgWaitingTime(),    0.01);
        assertEquals(6.0, result.getAvgTurnaroundTime(), 0.01);
        // Throughput: 1 / 6 = 0.167
        assertEquals(0.167, result.getThroughput(), 0.001);
    }

    /**
     * Only one process, arriving late. CPU idles until arrival, then runs with no preemption.
     */
    @Test
    public void testSingleProcessArrivesLate() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 25, 4));

        ScheduleResult result = new SRTFAlgorithm().runAlgorithm(processes);

        assertEquals(1, result.getGanttChart().size());
        assertEquals(25, result.getGanttChart().get(0).getStart(), "CPU idles until arrival 25");
        assertEquals(29, result.getGanttChart().get(0).getEnd());

        assertEquals(0.0, result.getAvgWaitingTime(),    0.01);
        assertEquals(4.0, result.getAvgTurnaroundTime(), 0.01);
    }

    /**
     * Four processes each with burst=1, all arriving at time 0.
     * No preemption is possible since each finishes in 1 tick.
     * SRTF reduces to FCFS here — tie-breaking goes by process ID.
     *
     * Gantt Chart:
     *   [0-1] P1   [1-2] P2   [2-3] P3   [3-4] P4
     */
    @Test
    public void testAllBurstOneNoPreemptionPossible() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 1));
        processes.add(p("P2", 0, 1));
        processes.add(p("P3", 0, 1));
        processes.add(p("P4", 0, 1));

        ScheduleResult result = new SRTFAlgorithm().runAlgorithm(processes);

        List<GanttEntry> g = result.getGanttChart();
        assertEquals(4, g.size());

        for (int i = 0; i < 4; i++) {
            assertEquals("P" + (i + 1), g.get(i).getProcessId(), "Process order follows ID (all equal remaining)");
            assertEquals(i,     g.get(i).getStart());
            assertEquals(i + 1, g.get(i).getEnd());
        }

        assertEquals(1.5, result.getAvgWaitingTime(), 0.01, "With burst=1, each process barely waits");
    }

    /**
     * A burst=1 process arrives while P1 is running and immediately preempts it.
     * Since P2 finishes in 1 tick, P1 resumes right away.
     *
     * P1: burst=5, arrives 0. At t=3, P1 has 2 remaining.
     * P2: burst=1, arrives 3. P2 remaining=1 < P1 remaining=2 -> P2 preempts.
     * P2 done at t=4. P1 resumes at t=4 with 2 remaining, done at t=6.
     *
     * Gantt Chart:
     *   [0-3] P1  [3-4] P2  [4-6] P1
     */
    @Test
    public void testMinimumBurstProcessPreemptsAndFinishesInstantly() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 5));
        processes.add(p("P2", 3, 1));

        ScheduleResult result = new SRTFAlgorithm().runAlgorithm(processes);

        List<GanttEntry> g = result.getGanttChart();
        assertEquals(3, g.size());

        assertEquals("P1", g.get(0).getProcessId());
        assertEquals(0, g.get(0).getStart());
        assertEquals(3, g.get(0).getEnd());

        assertEquals("P2", g.get(1).getProcessId());
        assertEquals(3, g.get(1).getStart());
        assertEquals(4, g.get(1).getEnd());

        assertEquals("P1", g.get(2).getProcessId());
        assertEquals(4, g.get(2).getStart());
        assertEquals(6, g.get(2).getEnd());

        assertEquals(0, find(result, "P2").getWaitingTime(), "P2 ran the moment it arrived");
        assertEquals(1, find(result, "P1").getWaitingTime(), "P1 lost 1 unit to P2's preemption");
    }

    /**
     * Two processes at max burst (99), equal arrival -> P1 (smaller ID) goes first. No preemption.
     *
     * Gantt Chart:
     *   [0-99] P1   [99-198] P2
     */
    @Test
    public void testMaximumBurstTimesNoPreemption() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 99));
        processes.add(p("P2", 0, 99));

        ScheduleResult result = new SRTFAlgorithm().runAlgorithm(processes);

        List<GanttEntry> g = result.getGanttChart();
        assertEquals(2, g.size(), "Equal burst, equal arrival — no preemption");

        assertEquals("P1", g.get(0).getProcessId());
        assertEquals(0,   g.get(0).getStart());
        assertEquals(99,  g.get(0).getEnd());

        assertEquals("P2", g.get(1).getProcessId());
        assertEquals(99,  g.get(1).getStart());
        assertEquals(198, g.get(1).getEnd());

        assertEquals(49.5,  result.getAvgWaitingTime(),    0.01);
        assertEquals(148.5, result.getAvgTurnaroundTime(), 0.01);
    }

    /**
     * A burst=1 process arrives at t=50 while P1 (burst=99) is still running.
     * P1 has 49 remaining at that point, so P2 preempts immediately.
     *
     * At t=50: P1 has 49 remaining, P2 has 1 remaining -> P2 preempts.
     * P2 done at 51. P1 resumes with 49 remaining, finishes at 100.
     *
     * Gantt Chart:
     *   [0-50] P1  [50-51] P2  [51-100] P1
     */
    @Test
    public void testLateArrivalShortProcessPreemptsLongRunningProcess() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0,  99));
        processes.add(p("P2", 50, 1));

        ScheduleResult result = new SRTFAlgorithm().runAlgorithm(processes);

        List<GanttEntry> g = result.getGanttChart();
        assertEquals(3, g.size());

        assertEquals("P1", g.get(0).getProcessId());
        assertEquals(0,  g.get(0).getStart());
        assertEquals(50, g.get(0).getEnd());

        assertEquals("P2", g.get(1).getProcessId());
        assertEquals(50, g.get(1).getStart());
        assertEquals(51, g.get(1).getEnd());

        assertEquals("P1", g.get(2).getProcessId());
        assertEquals(51,  g.get(2).getStart());
        assertEquals(100, g.get(2).getEnd());

        assertEquals(0, find(result, "P2").getWaitingTime(), "P2 ran the moment it arrived");
        assertEquals(1, find(result, "P1").getWaitingTime(), "P1 lost exactly 1 unit to P2");
    }

    /**
     * P1: arrival=0, burst=8
     * P2: arrival=1, burst=4
     * P3: arrival=2, burst=2
     * P4: arrival=3, burst=1
     *
     * t=0:  only P1 -> P1 starts
     * t=1:  P2 arrives (remaining 4), P1 has 7 left -> P2 preempts P1
     * t=2:  P3 arrives (remaining 2), P2 has 3 left -> P3 preempts P2
     * t=3:  P4 arrives (remaining 1), P3 has 1 left -> tied, P3 (smaller ID) keeps CPU
     * t=4:  P3 finishes. Ready: P4(1), P2(3), P1(7). P4 runs.
     * t=5:  P4 finishes. Ready: P2(3), P1(7). P2 runs.
     * t=8:  P2 finishes. P1 runs.
     * t=15: P1 finishes.
     *
     * Gantt Chart:
     *   [0-1] P1  [1-2] P2  [2-4] P3  [4-5] P4  [5-8] P2  [8-15] P1
     *
     * Waiting: P1=15-0-8=7, P2=8-1-4=3, P3=4-2-2=0, P4=5-3-1=1 -> avg=11/4=2.75
     * Turnaround: P1=15, P2=7, P3=2, P4=2 -> avg=26/4=6.5
     * Throughput: 4/15 = 0.267
     */
    @Test
    public void testFullScenarioWithMultiplePreemptionsAndStats() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 8));
        processes.add(p("P2", 1, 4));
        processes.add(p("P3", 2, 2));
        processes.add(p("P4", 3, 1));

        ScheduleResult result = new SRTFAlgorithm().runAlgorithm(processes);

        List<GanttEntry> g = result.getGanttChart();
        assertEquals(6, g.size());

        assertEquals("P1", g.get(0).getProcessId()); assertEquals(0,  g.get(0).getStart()); assertEquals(1,  g.get(0).getEnd());
        assertEquals("P2", g.get(1).getProcessId()); assertEquals(1,  g.get(1).getStart()); assertEquals(2,  g.get(1).getEnd());
        assertEquals("P3", g.get(2).getProcessId()); assertEquals(2,  g.get(2).getStart()); assertEquals(4,  g.get(2).getEnd());
        assertEquals("P4", g.get(3).getProcessId()); assertEquals(4,  g.get(3).getStart()); assertEquals(5,  g.get(3).getEnd());
        assertEquals("P2", g.get(4).getProcessId()); assertEquals(5,  g.get(4).getStart()); assertEquals(8,  g.get(4).getEnd());
        assertEquals("P1", g.get(5).getProcessId()); assertEquals(8,  g.get(5).getStart()); assertEquals(15, g.get(5).getEnd());

        assertEquals(7, find(result, "P1").getWaitingTime());
        assertEquals(3, find(result, "P2").getWaitingTime());
        assertEquals(0, find(result, "P3").getWaitingTime());
        assertEquals(1, find(result, "P4").getWaitingTime());

        assertEquals(2.75, result.getAvgWaitingTime(),    0.01);
        assertEquals(6.5,  result.getAvgTurnaroundTime(), 0.01);
        assertEquals(0.267, result.getThroughput(),       0.01);
    }

    /**
     * Verifies that throughput uses the completion time of the last process,
     * not the sum of all burst times.
     *
     * P1: arrival=0, burst=10, completes at 13 (delayed by P2 and P3)
     * P2: arrival=1, burst=1,  completes at 2
     * P3: arrival=2, burst=2,  completes at 4
     *
     * Gantt Chart:
     *   [0-1] P1  [1-2] P2  [2-4] P3  [4-13] P1
     * Last end = 13 -> throughput = 3/13 = 0.231
     */
    @Test
    public void testThroughputUsesActualLastCompletionTime() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 10));
        processes.add(p("P2", 1, 1));
        processes.add(p("P3", 2, 2));

        ScheduleResult result = new SRTFAlgorithm().runAlgorithm(processes);

        assertEquals(0.231, result.getThroughput(), 0.01,
                "Throughput = 3 / 13, where 13 is the actual last completion time");
    }
}