package edu.brooklyn.cpusim.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Process {
    private String processId;
    private int arrivalTime;
    private int burstTime;
    private int priority;

}
