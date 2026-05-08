package edu.brooklyn.cpusim.dto;

import edu.brooklyn.cpusim.model.Process;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * This is the request body for the /api/compare endpoint.
 * It carries the same process list as a normal simulation request,
 * but instead of a single algorithm name it holds a list of algorithm
 * names so that all of them can be run and compared in one call.
 *
 * algorithms – e.g. ["FCFS", "SJF", "RR"]
 * quantum    – only used when "RR" is in the list (ignored otherwise)
 * processes  – the shared process set every algorithm will receive
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ComparisonRequest {
    private List<String> algorithms;
    private Integer quantum;
    private List<Process> processes;
}
