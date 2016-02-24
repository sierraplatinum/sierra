/**
 *****************************************************************************
 * Copyright (c) 2015 Daniel Gerighausen, Lydia Mueller, and Dirk Zeckzer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************
 */
package biovis.sierra.server.Commander;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import com.google.gson.Gson;

import biovis.sierra.data.DataMapper;
import biovis.sierra.data.IO.Exporter;
import biovis.sierra.data.peakcaller.PeakList;
import biovis.sierra.server.PeakFactory;
import biovis.sierra.server.SuperDuperPeakCaller;
import biovislib.parallel4.ParallelizationFactory;
import de.kl.vis.lib.remoteControl.CommandDispatcherInterface;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

/**
 *
 * @author Daniel Gerighausen, Dirk Zeckzer
 */
public class PeakDispatcher
  implements CommandDispatcherInterface {

    private ServerMapper server;

    private boolean busy = false;

    private PeakCommander pc = new PeakCommander();
    public PeakList narrowPeakList;
    public PeakList broadPeakList;
    public PeakFactory pf;
    private Calculation calc;
    private Future<?> future;
    private ExecutorService executor;
    private Recalculation recalc;

	private boolean batchMode;

    /**
     * Constructor-
     */
    public PeakDispatcher() {

    }

    @Override
    public void dispatchCommand(Object[] command) {
        String scommand = (String) command[0];

        Logger log = Logger.getLogger("Command received");
        log.info("Command received");
		//		log.info("=================================================");
        //		log.info("start dispatching");

        switch (scommand) {
            case "setClient":
                log.info("set client");
                setClient(command);
                break;
            case "setDataMapper":
                log.info("set DataMapper");
                if (busy) {
                    isBusy(false, command);
                } else {
                    setDataMapper(command);
                }
                break;
            case "startCalc":
                log.info("start calculation");
                if (busy) {
                    isBusy(false, command);
                } else {
                    startCalc();
                }
                break;
            case "pullDataMapper":
                log.info("pull dataMapper");
                if (busy) {
                    isBusy(true, command);
                } else {
                    reloadDataMapper();
                }
                break;
            case "pullBroad":
                log.info("pull broads");
                if (busy) {
                    isBusy(true, command);
                } else {
                    sendBroad(command);
                }
                break;
            case "pullNarrow":
                log.info("pull narrow");
                if (busy) {
                    isBusy(true, command);
                } else {
                    sendNarrow(command);
                }
                break;
            case "saveState":
                log.info("save state");
                if (busy) {
                    isBusy(true, command);
                } else {
                    busy = true;
                	exportState(log, server.getSpc());
                    busy = false;
                }
                break;
            case "killJob":
                log.info("kill running job");
                if (busy) {
                    killJob();
                }
                break;
            case "QUIT":
                log.info("quit");
                dispatchQuit(command);
                break;
            case "credentials":
                log.info("received credentials");
                setCredentials(command);
                break;
            case "getProgress":
                log.info("getProgress");
                getProgress(command);
                break;
            case "recalc":
                log.info("Recalculation");
                recalculate();
                break;
            default:
                log.log(Level.INFO, "Received unknown command: {0}", scommand);
                break;
            // log.info("=================================================");
        }

    }

    /**
     * Get peak commander.
     *
     * @return peak commander
     */
    public PeakCommander getPc() {
        return pc;
    }

    /**
     * Set server.
     *
     * @param server server
     */
    public void setServer(ServerMapper server) {
        this.server = server;
    }

    /**
     * Recalculate.
     *
     */
    private void recalculate() {
        busy = true;

        recalc = new Recalculation();
        executor = Executors.newSingleThreadExecutor();
        try {
            future = executor.submit(recalc);
            Thread delorean = new Thread(new backToTheFuture());
            delorean.start();

            System.err.println("got it");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

	//I know this is deprecated but otherwise i would have to mess up lydia's code to kill the thread in a nice way.
    //Since the data produced by this thread cannot be used anyway and the data mapper will restored, do not care..
    /**
     * Kill useless jobs.
     */
    private void killJob() {
        busy = false;
//        future.cancel(true);
//        executor.shutdownNow();
//        Parallel.shutdownNow();
        System.err.println("Shutdown before!");
        ParallelizationFactory.shutdownNow();
        System.err.println("Shutdown done!");
        Object[] c = new Object[2];
        c[0] = "killedJob";
		//		Gson gson = new Gson();
        //		String dataString = gson.toJson(server.getMapper());
        c[1] = "killedJob";
        pc.sendCommand(c);
    }

    /**
     * Send lists of peaks.
     *
     * @param command command
     */
    private void sendBroad(Object[] command) {
        Gson gson = new Gson();
        String bedfile;	
//		if(server.getMapper().getPeakmode()){
        bedfile = gson.toJson(pf.getPeakListBroad());
//		}
//		else
//		{
//			bedfile = gson.toJson(pf.getPeakListNarrow());
//		}
        String hash = (String) command[1];
        Object[] c = new Object[2];
        c[0] = "sendBroad";
        c[1] = bedfile;
        pc.sendCommand(c, hash);
    }
    private void sendNarrow(Object[] command) {
        Gson gson = new Gson();
        String bedfile;
//		if(server.getMapper().getPeakmode()){
        bedfile = gson.toJson(pf.getPeakListNarrow());
//		}
//		else
//		{
//			bedfile = gson.toJson(pf.getPeakListNarrow());
//		}
        String hash = (String) command[1];
        Object[] c = new Object[2];
        c[0] = "sendNarrow";
        c[1] = bedfile;
        pc.sendCommand(c, hash);
    }

    /**
     * Set new credentials.
     *
     * @param command command
     */
    private void setCredentials(Object[] command) {
        Object[] payload = (Object[]) command[1];
        String passwd = (String) payload[0];
        String hash = (String) payload[1];
        if (server.isLocked()) {
            //			System.err.println("check password");

            if (server.getPasswd().equals(passwd)) {
				//				System.err.println("password matched!");

                //				pc.sendCommand(command);
            } else {
                Logger log = Logger.getLogger("Auth Failure");
                log.info("Auth Failure");
                Object[] c = {"QUIT", "AUTH_FAILURE"};
                pc.sendCommand(c, hash);
            }
        } else {
            //			System.err.println("Set password");
            server.setPasswd(passwd);
            server.setLocked(true);
        }
    }

    /**
     * Quit.
     *
     * @param command command
     */
    private void dispatchQuit(Object[] command) {
		//Client terminate or connection done?
        //		String command0  = (String) command[0];
        String command1 = (String) command[1];
        if (command1.equals("QUIT")) {
            //			server.setLocked(true);
            pc.setActive();
        }
    }

    /**
     * Set client.
     *
     * @param command command
     */
    private void setClient(Object[] command) {
        Object[] url = (Object[]) command[1];

        String host = (String) url[0];
        int port = (int) url[1];
        String hash = (String) url[2];

        pc.setServer(host, port, hash);
    }

    /**
     * Set data mapper.
     *
     * @param command command
     */
    private void setDataMapper(Object[] command) {
        Gson gson = new Gson();
        DataMapper obj = gson.fromJson((String) command[1], DataMapper.class);
        
        obj.setIOThreads(server.getIOThreads());
//        System.err.println("size of dispatcher: " + MemoryMeasurer.measureBytes(this));
        server.setMapper(obj);
    }

    /**
     * Start calculation.
     *
     */
    public void startCalc() {
        busy = true;
        System.err.println("start startCalc");
        System.err.println(server.getIOThreads());
        server.setSpc(new SuperDuperPeakCaller(server.getMapper(), pc, server.getChunksize()));
//        System.err.println("size of server: " + MemoryMeasurer.measureBytes(server));
        calc = new Calculation();
        executor = Executors.newSingleThreadExecutor();
        System.err.println("executor");

        try {
            future = executor.submit(calc);
            System.err.println("submitted");

            Thread delorean = new Thread(new backToTheFuture());
            delorean.start();

            System.err.println("got it");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    /**
     * Get progress.
     *
     * @param command command
     */
    private void getProgress(Object[] command) {

    }

    /**
     * Reload data mapper.
     */
    private void reloadDataMapper() {
        Object[] c = new Object[2];
        c[0] = "sendDataMapper";
        Gson gson = new Gson();
        String dataString = gson.toJson(server.getMapper());
        c[1] = dataString;
        System.out.println(dataString);
        pc.sendCommand(c);
    }

    /**
     * Is busy?
     *
     * @param send true iff answer should be sent in addition to logging it
     * @param command
     */
    private void isBusy(boolean send, Object[] command) {
        if (send) {
            String hash = (String) command[1];
            Logger log = Logger.getLogger("Busy Message");
            log.log(Level.INFO, "Sending Busy Message to {0}", hash);

            Object[] c = {"BUSY", "BUSY"};
            pc.sendCommand(c, hash);
        }

        Logger log = Logger.getLogger("Busy");
        log.info("Busy Lock");
    }

    /**
     *
     * @param message 
     */
    private void timestamp(String message) {
        System.err.println("TimeStamp: " + message
                           + "," + LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME)
        );
    }

    /**
     * Export peaks and mapper.
     * @param log logger
     * @param sdp SuperDuperPeakCaller
     */
    private boolean exportPeaksAndMapper(Logger log, SuperDuperPeakCaller sdp) {
        log.info("Before busy");
        if (!busy) {
            return false;
        }
        log.info("Get narrow and broad peak lists");
        narrowPeakList = pf.getPeakListNarrow();
        broadPeakList = pf.getPeakListBroad();
        log.info("Narrow and broad peak lists ready");
        timestamp("Get Narrow and Broad Peaks");
        
        if (!busy) {
            return false;
        }

        /*
        log.info("Clear unused information in data mapper");
        server.getMapper().clearDuplicates();
        log.info("Unused information in data mapper cleared");
        timestamp("Clear unused information in Data Mapper");
        */

        log.info("Start export peaks and mapper");
        String prefix = server.getMapper().getJobName() + "-" + server.getMapper().getCurrentStep() + "-";
        Exporter.exportBED(prefix + "narrow.bed", narrowPeakList, "narrow");
        Exporter.exportCSV(prefix + "narrow.csv", narrowPeakList);
        Exporter.exportBED(prefix + "broad.bed", broadPeakList, "broad");
        Exporter.exportCSV(prefix + "broad.csv", broadPeakList);
        Exporter.exportMapper(prefix + "mapper.dsierra", server.getMapper());
        log.info("Peaks and mapper exported");
        timestamp("Export Peak Lists and Data Mapper");
        return true;
    }

    /**
     * Export state
     * @param log logger
     * @param sdp SuperDuperPeakCaller
     */
    private void exportState(Logger log, SuperDuperPeakCaller sdp) {
        /*
        log.info("Start export state");
        log.info("Clear unused information in windows");
        for (Window w : sdp.getWl().getWindows()) {
            w.installLinux();
        }
        */
        log.info("Export state");
        String prefix = server.getMapper().getJobName() + "-" + server.getMapper().getCurrentStep() + "-";
        Exporter.exportWindows(prefix + "state.sierra", sdp.getWl(), server.getMapper());
        
        
//        String hash = (String) command[1];
        log = Logger.getLogger("State Message");
        log.log(Level.INFO, "Sending Busy Message");

        Object[] c = {"STATE DONE", "STATE DONE"};
        pc.sendCommand(c);
        log.info("State export done");
    }

    private void exportParameters(Logger log)
    {
    	   log.info("Start export parameters");
           String prefix = server.getMapper().getJobName() + "-" + server.getMapper().getCurrentStep();
           Exporter.exportCalcParameters(prefix, server.getMapper());
           log.info("Parameter export done");
    }

    private void exportServerMapper(Logger log)
    {
    	   log.info("Start server mapper");
           String prefix = server.getMapper().getJobName()+".sconf";
//           Exporter.exportCalcParameters(prefix, server.getMapper());
           Exporter.exportServerConfig(prefix, server);
           log.info("Server mapper export done");
    }

    /**
     * Private class for calculation thread.
     */
    private class Calculation implements Runnable {

        @Override
        public void run() {
            Logger log = Logger.getLogger("Calculation");
            exportServerMapper(log);
            SuperDuperPeakCaller sdp = server.getSpc();

            try {
                pf = sdp.start();
                server.getMapper().setResults(true);
                boolean export = exportPeaksAndMapper(log, sdp);
                if (!export) {
                    return;
                }
                Exporter.printHTML("sierra.htm", 0.98);

//                exportState(log, sdp);
                exportParameters(log);
                Exporter.printHTML("sierra.htm", 0.99);

                busy = false;
                log.info("Calculation done");
                if(batchMode)
                {
                	System.exit(0);
                }
                
                sendDataMapper();
            } catch (Exception e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
                busy = false;
                pc.resetProgress();
                //				System.err.println(e.toString());
                log.severe("boom");
            }
        }
    }

    /**
     * Private class for recalculation thread.
     */
    private class Recalculation implements Runnable {

        @Override
        public void run() {
            Logger log = Logger.getLogger("Recalculation");
            SuperDuperPeakCaller sdp = server.getSpc();

            try {
                pf = sdp.restart(server.getMapper());
                boolean export = exportPeaksAndMapper(log, sdp);
                if (!export) {
                    return;
                }
                Exporter.printHTML("sierra.htm", 0.98);
                exportParameters(log);
//                exportState(log, sdp);
                Exporter.printHTML("sierra.htm", 0.99);

                busy = false;
                log.info("Recalculation done");

                sendDataMapper();
            } catch (Exception e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
                busy = false;
                pc.resetProgress();
                //				System.err.println(e.toString());
                log.severe("boom");
            }
        }
    }

    /**
     * Private class for recalculation thread.
     */
    private class backToTheFuture implements Runnable {

        @Override
        public void run() {
            try {
                future.get();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ExecutionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * Send data mapper to clients.
     */
    private void sendDataMapper() {
        Exporter.printHTML("sierra.htm", 1.0);
        if (pc.isActive() > 0) {
            Object[] c = new Object[2];
            c[0] = "sendDataMapper";
            Gson gson = new Gson();
            String dataString = gson.toJson(server.getMapper());
            c[1] = dataString;
            pc.sendCommand(c);
            pc.resetProgress();
        }
    }

	public void setBatch(boolean batchMode) {
		this.batchMode = batchMode;
	}
}
