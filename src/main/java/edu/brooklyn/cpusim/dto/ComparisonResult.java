package edu.brooklyn.cpusim.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * This is the response body for the /api/compare endpoint.
 * It is a wrapper around a map so the frontend receives each
 * algorithm's full ScheduleResult keyed by its name.
 *
 * Example JSON shape:
 * {
 *   "results": {
 *     "FCFS":     { "ganttChart": [...], "processTable": [...], ... },
 *     "SJF":      { ... },
 *     "RR":       { ... }
 *   }
 * }
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ComparisonResult {
    private Map<String, ScheduleResult> results;
}