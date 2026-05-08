package edu.brooklyn.cpusim.dto;

import edu.brooklyn.cpusim.model.GanttEntry;
import edu.brooklyn.cpusim.model.ProcessResult;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ScheduleResult {
    private List<GanttEntry> ganttChart;
    private List<ProcessResult> processTable;
    private double avgWaitingTime;
    private double avgTurnaroundTime;
    private double throughput;
}
