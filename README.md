[![codebeat badge](https://codebeat.co/badges/4a4ef002-23a1-432c-b2dc-efe7093c2cd6)](https://codebeat.co/projects/github-com-stumash-distributedsystemsproject-master)

# COMP 512 Distributed Systems Project, Fall 2018

## Project Layout

This repository contains all the instructions/specifications and code for the semester-long project.

The `instructions` folder contains all the instructions/specifications of the project as provided by the professor, Bettina Kimme. The instructions are split into three 'milestones' as there were three deliverables for this project over the course of the semester.

The `src` folder contains all project code. `src/main/java/` contains all the java code for the project, and `src/scripts/` contains all bash convenience scripts for running the project. Note that the `run.sh` file can be used instead of the using the code in `src/scripts/` directly.

## How to use it

### Milestones and Final Version

There are three commits tagged `milestone01`, `milestone02`, and `milestone03`. These commits snapshot the state of the project at the due date of each milestone, respectively.

The final version of the project is the most recent commit to branch `master`.

### Building and Running

`mvn clean install`

Then, `./run.sh <args>`. Use `./run.sh --help` or `./run.sh` with no args for more info.
