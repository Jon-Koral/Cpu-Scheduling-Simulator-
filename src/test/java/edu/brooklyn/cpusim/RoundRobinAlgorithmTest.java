package edu.brooklyn.cpusim;

import edu.brooklyn.cpusim.algorithm.RRAlgorithm;
import edu.brooklyn.cpusim.dto.ScheduleResult;
import edu.brooklyn.cpusim.model.Process;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * These are the tests for the Round Robin scheduling algorithm.
 *
 * Round Robin gives each process a fixed time quantum. If a process does not
 * finish within its quantum it goes to the back of the ready queue and the next
 * process runs. Processes that arrive during a running slice are enqueued BEFORE
 * the preempted process is re-enqueued, so new arrivals always join ahead of it.
 *
 * All input is validated in the frontend so all numbers are between 0 and 99.
 */
public class RoundRobinAlgorithmTest {

    // Helper to create a process (priority not used by RR, set to 0)
    private Process p(String id, int arrival, int burst) {
        return new Process(id, arrival, burst, 0);
    }

    /**
     * Classic three-process example with quantum = 2.
     *
     * Processes: P1(0,5), P2(1,3), P3(2,1)
     *
     * Gantt Chart:
     *   [0–2]  P1   [2–4]  P2   [4–5]  P3   [5–7]  P1   [7–8]  P2   [8–9]  P1
     *
     * Completion: P3=5, P2=8, P1=9
     * Turnaround: P1=9, P2=7, P3=3
     * Waiting:    P1=4, P2=4, P3=2   → avg = 10/3 = 3.33
     */
    @Test
    public void testClassicThreeProcesses() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 5));
        processes.add(p("P2", 1, 3));
        processes.add(p("P3", 2, 1));

        ScheduleResult result = new RRAlgorithm(2).runAlgorithm(processes);

        // Gantt chart order
        assertEquals("P1", result.getGanttChart().get(0).getProcessId(), "1st slice: P1");
        assertEquals("P2", result.getGanttChart().get(1).getProcessId(), "2nd slice: P2");
        assertEquals("P3", result.getGanttChart().get(2).getProcessId(), "3rd slice: P3");
        assertEquals("P1", result.getGanttChart().get(3).getProcessId(), "4th slice: P1");
        assertEquals("P2", result.getGanttChart().get(4).getProcessId(), "5th slice: P2");
        assertEquals("P1", result.getGanttChart().get(5).getProcessId(), "6th slice: P1 (last 1 unit)");

        // Gantt chart start / end times
        assertEquals(0, result.getGanttChart().get(0).getStart());
        assertEquals(2, result.getGanttChart().get(0).getEnd());

        assertEquals(2, result.getGanttChart().get(1).getStart());
        assertEquals(4, result.getGanttChart().get(1).getEnd());

        assertEquals(4, result.getGanttChart().get(2).getStart());
        assertEquals(5, result.getGanttChart().get(2).getEnd());

        assertEquals(5, result.getGanttChart().get(3).getStart());
        assertEquals(7, result.getGanttChart().get(3).getEnd());

        assertEquals(7, result.getGanttChart().get(4).getStart());
        assertEquals(8, result.getGanttChart().get(4).getEnd());

        assertEquals(8, result.getGanttChart().get(5).getStart());
        assertEquals(9, result.getGanttChart().get(5).getEnd());

        // Averages
        assertEquals(3.33, result.getAvgWaitingTime(),    0.01, "Average waiting time");
        assertEquals(6.33, result.getAvgTurnaroundTime(), 0.01, "Average turnaround time");
        // Throughput: 3 / 9 = 0.333
        assertEquals(0.333, result.getThroughput(), 0.01, "Throughput");
    }

    /**
     * All processes arrive at time 0 with quantum = 3.
     *
     * Processes: P1(0,4), P2(0,4), P3(0,4)
     *
     * Gantt Chart:
     *   [0–3] P1   [3–6] P2   [6–9] P3   [9–10] P1   [10–11] P2   [11–12] P3
     *
     * Completion: P1=10, P2=11, P3=12
     * Turnaround: P1=10, P2=11, P3=12  → avg = 11.0
     * Waiting:    P1=6,  P2=7,  P3=8   → avg = 7.0
     */
    @Test
    public void testAllArriveAtZero() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 4));
        processes.add(p("P2", 0, 4));
        processes.add(p("P3", 0, 4));

        ScheduleResult result = new RRAlgorithm(3).runAlgorithm(processes);

        assertEquals("P1", result.getGanttChart().get(0).getProcessId());
        assertEquals("P2", result.getGanttChart().get(1).getProcessId());
        assertEquals("P3", result.getGanttChart().get(2).getProcessId());
        assertEquals("P1", result.getGanttChart().get(3).getProcessId());
        assertEquals("P2", result.getGanttChart().get(4).getProcessId());
        assertEquals("P3", result.getGanttChart().get(5).getProcessId());

        // Start/end
        assertEquals(0,  result.getGanttChart().get(0).getStart());
        assertEquals(3,  result.getGanttChart().get(0).getEnd());
        assertEquals(6,  result.getGanttChart().get(2).getStart());
        assertEquals(9,  result.getGanttChart().get(2).getEnd());
        assertEquals(10, result.getGanttChart().get(3).getEnd(),  "P1 finishes with 1 remaining unit");
        assertEquals(11, result.getGanttChart().get(4).getEnd(),  "P2 finishes with 1 remaining unit");
        assertEquals(12, result.getGanttChart().get(5).getEnd(),  "P3 finishes with 1 remaining unit");

        assertEquals(7.0,  result.getAvgWaitingTime(),    0.01);
        assertEquals(11.0, result.getAvgTurnaroundTime(), 0.01);
        assertEquals(0.25, result.getThroughput(),         0.01);
    }

    /**
     * Quantum is larger than every burst time — behaves like FCFS.
     *
     * Processes: P1(0,3), P2(0,2), P3(0,1)  with quantum = 10
     *
     * Gantt Chart:
     *   [0–3] P1   [3–5] P2   [5–6] P3
     *
     * No process is ever preempted.
     */
    @Test
    public void testQuantumLargerThanAllBursts() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 3));
        processes.add(p("P2", 0, 2));
        processes.add(p("P3", 0, 1));

        ScheduleResult result = new RRAlgorithm(10).runAlgorithm(processes);

        // Each process runs exactly once — no preemption
        assertEquals(3, result.getGanttChart().size(), "Each process should have exactly one Gantt entry");

        assertEquals("P1", result.getGanttChart().get(0).getProcessId());
        assertEquals("P2", result.getGanttChart().get(1).getProcessId());
        assertEquals("P3", result.getGanttChart().get(2).getProcessId());

        assertEquals(0, result.getGanttChart().get(0).getStart());
        assertEquals(3, result.getGanttChart().get(0).getEnd());
        assertEquals(3, result.getGanttChart().get(1).getStart());
        assertEquals(5, result.getGanttChart().get(1).getEnd());
        assertEquals(5, result.getGanttChart().get(2).getStart());
        assertEquals(6, result.getGanttChart().get(2).getEnd());
    }

    /**
     * Quantum equals every burst time exactly — each process runs once and finishes.
     *
     * Processes: P1(0,4), P2(0,4), P3(0,4)  with quantum = 4
     *
     * Gantt Chart:
     *   [0–4] P1   [4–8] P2   [8–12] P3
     */
    @Test
    public void testQuantumEqualsEachBurst() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 4));
        processes.add(p("P2", 0, 4));
        processes.add(p("P3", 0, 4));

        ScheduleResult result = new RRAlgorithm(4).runAlgorithm(processes);

        assertEquals(3, result.getGanttChart().size(), "Each process runs exactly once");

        assertEquals(0,  result.getGanttChart().get(0).getStart());
        assertEquals(4,  result.getGanttChart().get(0).getEnd());
        assertEquals(4,  result.getGanttChart().get(1).getStart());
        assertEquals(8,  result.getGanttChart().get(1).getEnd());
        assertEquals(8,  result.getGanttChart().get(2).getStart());
        assertEquals(12, result.getGanttChart().get(2).getEnd());

        // Waiting times: P1=0, P2=4, P3=8  → avg = 4.0
        assertEquals(4.0, result.getAvgWaitingTime(),    0.01);
        // Turnaround: P1=4, P2=8, P3=12   → avg = 8.0
        assertEquals(8.0, result.getAvgTurnaroundTime(), 0.01);
    }

    /**
     * A single process — runs once straight through regardless of quantum.
     *
     * Gantt Chart:
     *   [0–5] P1
     */
    @Test
    public void testSingleProcess() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 5));

        ScheduleResult result = new RRAlgorithm(2).runAlgorithm(processes);

        // P1 runs in multiple slices: [0-2], [2-4], [4-5]
        assertEquals("P1", result.getGanttChart().get(0).getProcessId());
        assertEquals(0, result.getGanttChart().get(0).getStart());

        // Last slice ends at 5
        int lastIndex = result.getGanttChart().size() - 1;
        assertEquals(5, result.getGanttChart().get(lastIndex).getEnd());

        // Waiting time and turnaround for a single process
        assertEquals(0.0, result.getAvgWaitingTime(),    0.01, "Single process never waits");
        assertEquals(5.0, result.getAvgTurnaroundTime(), 0.01);
        assertEquals(0.2, result.getThroughput(),         0.01, "1 / 5 = 0.2");
    }

    /**
     * Processes arrive at staggered times; CPU idles between P1 finishing and P2 arriving.
     *
     * Processes: P1(0,3), P2(10,4)  with quantum = 2
     *
     * Gantt Chart:
     *   [0–2] P1   [2–3] P1   [idle 3–10]   [10–12] P2   [12–14] P2
     *
     * Completion: P1=3, P2=14
     * Waiting:    P1=0, P2=0   → avg = 0.0  (both start at their arrival time)
     */
    @Test
    public void testIdleGapBetweenProcesses() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0,  3));
        processes.add(p("P2", 10, 4));

        ScheduleResult result = new RRAlgorithm(2).runAlgorithm(processes);

        // P1 finishes at 3, then P2 starts at its arrival time 10
        assertEquals("P1", result.getGanttChart().get(0).getProcessId());
        assertEquals(0, result.getGanttChart().get(0).getStart());

        // Find the first P2 entry
        long p2Count = result.getGanttChart().stream()
                .filter(e -> e.getProcessId().equals("P2"))
                .count();
        assertEquals(2, p2Count, "P2 with burst 4 and quantum 2 should have exactly 2 slices");

        assertEquals(0.0, result.getAvgWaitingTime(), 0.01, "No waiting; each process starts at its own arrival time");
    }

    /**
     * Per-process waiting and turnaround times are stored correctly.
     *
     * Processes: P1(0,6), P2(0,4)  with quantum = 2
     *
     * Gantt Chart:
     *   [0–2] P1   [2–4] P2   [4–6] P1   [6–8] P2   [8–10] P1
     *
     * Completion: P2=8, P1=10
     * Turnaround: P1=10, P2=8
     * Waiting:    P1=4, P2=4   → avg = 4.0
     */
    @Test
    public void testPerProcessWaitingAndTurnaroundTimes() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 0, 6));
        processes.add(p("P2", 0, 4));

        ScheduleResult result = new RRAlgorithm(2).runAlgorithm(processes);

        // Find P1 and P2 in the process table (order matches input order)
        int p1Wait       = result.getProcessTable().get(0).getWaitingTime();
        int p1Turnaround = result.getProcessTable().get(0).getTurnaroundTime();
        int p2Wait       = result.getProcessTable().get(1).getWaitingTime();
        int p2Turnaround = result.getProcessTable().get(1).getTurnaroundTime();

        assertEquals(4,  p1Wait,       "P1 waiting time");
        assertEquals(10, p1Turnaround, "P1 turnaround time");
        assertEquals(4,  p2Wait,       "P2 waiting time");
        assertEquals(8,  p2Turnaround, "P2 turnaround time");

        assertEquals(4.0, result.getAvgWaitingTime(),    0.01);
        assertEquals(9.0, result.getAvgTurnaroundTime(), 0.01);
    }
    /**
     * Test case where the first process arrives after time 0.
     * CPU should idle from 0 to 5.
     */
    @Test
    public void testFirstArrivalDelayed() {
        List<Process> processes = new ArrayList<>();
        processes.add(p("P1", 5, 2));

        ScheduleResult result = new RRAlgorithm(2).runAlgorithm(processes);

        assertEquals(1, result.getGanttChart().size());
        assertEquals(5, result.getGanttChart().get(0).getStart(), "P1 should start at its arrival time 5");
        assertEquals(7, result.getGanttChart().get(0).getEnd());
        assertEquals(0.0, result.getAvgWaitingTime(), 0.01);
    }

    @Test
    public void testEmptyProcessList() {
        List<Process> processes = new ArrayList<>();
        ScheduleResult result = new RRAlgorithm(2).runAlgorithm(processes);
        assertEquals(0, result.getGanttChart().size());
        assertEquals(0.0, result.getThroughput(), 0.01);
    }
}
