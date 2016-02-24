# Sierra Platinum

## Introduction
 We propose Sierra Platinum,
  a new, fast and robust optimal multiple-replicate peak-caller
  with visual quality-control and -steering.
Sierra Platinum uses a model based on the Poisson distribution together
  with the inverse normal method to combine the individual replicates.
Multiple quality measures are computed and visualized.
This allows to judge the replicate's quality.
A subset of replicates can be selected and weighted to
  obtain the combined peaks.


## Install
Clone the project and compile the sources with the Java IDE of your choice.

The main class for the client is located at: sierra/src/biovis/sierra/client/GUI/SierreClient.java

The main class for the server is located at: sierra/src/biovis/sierra/server/Commander/SierraServer.java

## Usage
Start the compiled jar files on the terminal as follows:

java -jar SierraClient.jar

java -jar SierraServer.jar

You should increase the maximum heapspace of the server to around 30 Gigabytes with the -Xmx java parameter.
