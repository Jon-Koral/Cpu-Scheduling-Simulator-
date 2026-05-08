package edu.brooklyn.cpusim.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class GanttEntry {
    private String processId;
    private int start;
    private int end;
}
