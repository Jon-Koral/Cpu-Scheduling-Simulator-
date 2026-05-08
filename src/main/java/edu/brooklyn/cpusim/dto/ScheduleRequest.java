package edu.brooklyn.cpusim.dto;

import edu.brooklyn.cpusim.model.Process;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ScheduleRequest {
    private String algorithm;
    private Integer quantum;
    private List<Process> processes;
}
