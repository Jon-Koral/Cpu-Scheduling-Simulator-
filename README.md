# CPU Scheduling Simulator
This project is a CPU Scheduling Algorithm Simulator web application that helps students visualize how different 
scheduling algorithms work. It will allow the user to input any number of processes with their desired arrival 
time and burst time. The user can then select an algorithm they wish to use such as FCFS, SJF, SRTF, RR, and Priority. 
The goal is to make an easy way for anyone interested to visualize and play around with different CPU scheduling 
algorithms so they can strengthen their understanding of the topic.
## Important Requirements

This project uses ***Java 25***, so you need to have it installed on your computer.

You can check your version of java by running:
java -version

## Project Structure
```
cpu-scheduling-simulator/
│
├── src/
│   ├── main/
│   │   ├── java/edu/brooklyn/cpusim/
│   │   │   ├── algorithm/
│   │   │   │   ├── AlgorithmStrategy.java
│   │   │   │   ├── FCFSAlgorithm.java
│   │   │   │   ├── SJFAlgorithm.java
│   │   │   │   └── SRTFAlgorithm.java
│   │   │   ├── configuration/
│   │   │   │   └── WebConfig.java
│   │   │   ├── controller/
│   │   │   │   └── SimulationController.java
│   │   │   ├── dto/
│   │   │   │   ├── ScheduleRequest.java
│   │   │   │   └── ScheduleResult.java
│   │   │   ├── model/
│   │   │   │   ├── GanttEntry.java
│   │   │   │   ├── Process.java
│   │   │   │   └── ProcessResult.java
│   │   │   ├── Notes/
│   │   │   │   └── Frontend_JSON_Example.txt
│   │   │   ├── service/
│   │   │   │   └── SchedulerService.java
│   │   │   ├── sorter/
│   │   │   │   └── ProcessSorter.java
│   │   │   └── CpuSchedulingSimulatorApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── SoftwareDocumentation/
│   │       │   ├── Koral CPU Scheduling Simulator Slides.pdf
│   │       │   ├── Koral Software Development Life Cycle.pdf
│   │       │   └── Koral, CISC4900 SRS Document.pdf
│   │       └── static/
│   │           ├── index.html
│   │           ├── main.js
│   │           └── styles.css
│   │
│   └── test/
│       └── java/edu/brooklyn/cpusim/
│           ├── CpuSchedulingSimulatorApplicationTests.java
│           ├── FCFSAlgorithmTest.java
│           ├── SJFAlgorithmTest.java
│           └── SRTFAlgorithmTest.java
```

## How to Run the Project
1. Open a terminal in the project folder. Go to the main project directory (cpu-scheduling-simulator).
2. Start the Spring Boot backend by running thre command: .\mvnw spring-boot:run
3. Open the frontend. Open the index.html file in your browser. It is located at: src/main/resources/static/index.html


You can now enter processes and run the FCFS scheduling simulation.

## How to Stop the Program
If the backend is running in your terminal, you can stop it by pressing: Ctrl + C

This will shut down the Spring Boot server.

## Technologies Used
- Java 25
- Spring Boot
- Maven
- HTML / CSS / JavaScript

## Notes
Right now the project supports FCFS scheduling. I plan to add more algorithms later like SJF, SRTF, Round Robin, and Priority.

## Software Documentation
https://github.com/J-Koral/cpu-scheduling-simulator/tree/master/src/main/resources/SoftwareDocumentation
Documentation can be found in this folder which includes the SRS document, SDLC, and Project Slides.
