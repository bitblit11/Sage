# Sage Telemetry and Command System

The Sage telemetry and command system comes preconfigured to interface with NASA's [Core Flight Software](http://opensource.gsfc.nasa.gov/projects/cfe/index.php).  Core Flight Software (CFS) is an open source flight software framework.  Sage parses the binary telemetry messages and presents the data in a human readable format as well as builds and sends binary commands from user input.  The CFS Core Flight Executive and all the product line applications telemetry and commands are already defined in Sage, with a set of displays to view telemetry and interact with the flight software.  You are also able to edit existing and create new data definitions, scripts, and displays.  Sage is build on [YAMCS](http://www.yamcs.org/), with a custom Telemetry/Command library to receive and transmit the CFS telemetry and command stream, predefined telemetry and command definitions, and prebuilt displays and scripts.

## Platforms
*  Linux.  Tested with the following:
  *  Mint 17.1, 17.2, 17.3
*  Windows.  (IN WORK)

## Assumptions
*  You supply the CFS build.  Additional instructions in work.
*  Telemetry and commands are CCSDS, with the Software Bus defined "32_32" secondary header and sent via non-framed UDP messages.

## Build
-  Install [Oracle JDK 1.8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html).
-  Install [Tokyocabinet v. 1.4](http://fallabs.com/tokyocabinet/)
-  Install [libjtokyocabinet v 1.22](http://fallabs.com/tokyocabinet/javapkg/)
-  Run the "build.sh" script

## Run
-  Edit the "live/etc/cfs.yaml" file to set the IP address and UDP ports to match your CFS instantiation.  
-  Change directories to "live"
-  Run "bin/yamcs-server.sh"
-  In another shell, run "./YamcsStudio".
-  Select a location to create your new workspace.
-  Import the project at "template-project"
-  Click the button in the upper left corner to open the connection dialog.
-  Create a new connection:
  - Yamcs Instance:  cfs
  - Primary Server 
    - Host:  localhost
    - Port:  8090
  - Name: cfs
-  In the menu, click "Window->Show View->Navigator"
-  In the Navigator view on the right, expand the "CFS" project you just imported right click "Main.opi", select "Open With" and click "OPI Display (Workbench)".
-  Click the newly opened window by the tab and drag it to where you want it.
-  Navigate the displays to see various telemetry.  
