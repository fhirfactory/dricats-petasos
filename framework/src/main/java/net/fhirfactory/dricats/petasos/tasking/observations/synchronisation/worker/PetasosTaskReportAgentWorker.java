/*
 * Copyright (c) 2021 Mark A. Hunter (ACT Health)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.fhirfactory.dricats.petasos.tasking.observations.synchronisation.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import net.fhirfactory.dricats.model.configuration.names.subsystems.SubsystemNames;
import net.fhirfactory.dricats.petasos.tasking.observations.synchronisation.worker.common.AgentWorkerBase;
import net.fhirfactory.pegacorn.core.interfaces.oam.tasks.PetasosITOpsTaskReportingAgentInterface;
import net.fhirfactory.pegacorn.core.interfaces.oam.tasks.PetasosITOpsTaskReportingBrokerInterface;
import net.fhirfactory.pegacorn.core.interfaces.topology.ProcessingPlantInterface;
import net.fhirfactory.pegacorn.core.interfaces.capabilities.CapabilityUtilisationBrokerInterface;
import net.fhirfactory.pegacorn.core.model.petasos.oam.notifications.PetasosComponentITOpsNotification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

@ApplicationScoped
public class PetasosTaskReportAgentWorker extends AgentWorkerBase implements PetasosITOpsTaskReportingAgentInterface {
    private static final Logger LOG = LoggerFactory.getLogger(PetasosTaskReportAgentWorker.class);

    Queue<PetasosComponentITOpsNotification> taskReportQueue;
    private Object notificationQueueLock;

    private static long SYNCHRONIZATION_CHECK_PERIOD = 10000L;
    private static long INITIAL_CHECK_DELAY_PERIOD= 60000L;
    private Long NOTIFICATION_SYNCHRONISATION_WATCHDOG_RESET_PERIOD = 300L;

    private boolean backgroundCheckInitiated;
    private ObjectMapper jsonMapper;
    private boolean initialised;

    private boolean daemonIsStillRunning;
    private Instant daemonLastRunTime;

    @Inject
    private CapabilityUtilisationBrokerInterface capabilityUtilisationBroker;

    @Inject
    private ProcessingPlantInterface processingPlant;

    @Inject
    private SubsystemNames subsystemNames;

    @Inject
    private PetasosITOpsTaskReportingBrokerInterface brokerInterface;

    //
    // Constructor(s)
    //

    public PetasosTaskReportAgentWorker(){
        super();
        this.notificationQueueLock = new Object();
        this.initialised = false;
        this.taskReportQueue = new ConcurrentLinkedQueue<>();
        this.daemonLastRunTime = Instant.now();
        this.daemonIsStillRunning = true;
        this.jsonMapper = new ObjectMapper();
        JavaTimeModule module = new JavaTimeModule();
        this.jsonMapper.registerModule(module);
        this.jsonMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    //
    // Post Construct
    //

    @PostConstruct
    public void initialise(){
        getLogger().debug(".initialise(): Entry");
        if(!initialised){
            getLogger().info(".initialise(): Initialising....");
            scheduleTaskReportSynchronisation();
            this.initialised = true;
            getLogger().info(".initialise(): Done.");
        } else {
            getLogger().debug(".initialise(): Already initialised, nothing to do!");
        }
        getLogger().debug(".initialise(): Exit");
    }

    //
    // Business Methods
    //

    @Override
    public void sendTaskReport(PetasosComponentITOpsNotification notification) {
        addTaskReport(notification);
    }

    //
    // Actual Daemon Tasks
    //

    protected void forwardLocalTaskReportsToServer(){
        LOG.debug(".forwardLocalTaskReportsToServer(): Entry");
        while(hasMoreNotifications()) {
            PetasosComponentITOpsNotification nextTaskReport = null;
            synchronized (notificationQueueLock) {
                nextTaskReport = getNextNotification();
            }
            if(nextTaskReport != null) {
                LOG.debug(".forwardLocalTaskReportsToServer(): Loaded TaskReports form local cache, forwarding");
                LOG.debug(".forwardLocalTaskReportsToServer(): Sending TaskReports for Participant->{}", nextTaskReport.getParticipantName());
                brokerInterface.sendTaskReport(nextTaskReport);
            }
        }
        LOG.debug(".forwardLocalTaskReportsToServer(): Exit");
    }

    //
    // Schedule Period Synchronisation Check/Update Activity
    //

    protected void scheduleTaskReportSynchronisation() {

        getLogger().debug(".scheduleTaskReportSynchronisation(): Entry");
        TimerTask TaskReportForwarderDaemon = new TimerTask() {
            public void run() {
                getLogger().debug(".TaskReportForwarderDaemon(): Entry");
                if(!isDaemonIsStillRunning()){
                    taskReportSynchronisationDaemon();
                } else {
                    Long ageSinceRun = Instant.now().getEpochSecond() - getDaemonLastRunTime().getEpochSecond();
                    if (ageSinceRun > getNotificationSynchronisationResetPeriod()) {
                        taskReportSynchronisationDaemon();
                    }
                }
                taskReportSynchronisationDaemon();
                getLogger().debug(".TaskReportForwarderDaemon(): Exit");
            }
        };
        String timerName = "TaskReportForwarderDaemonTimer";
        Timer timer = new Timer(timerName);
        timer.schedule(TaskReportForwarderDaemon, getInitialCheckDelayPeriod(), getSynchronizationCheckPeriod());
        getLogger().debug(".scheduleTaskReportSynchronisation(): Exit");
    }

    protected void taskReportSynchronisationDaemon(){
        getLogger().debug(".taskReportSynchronisationDaemon(): Entry");
        this.daemonIsStillRunning = true;
        forwardLocalTaskReportsToServer();
        this.daemonIsStillRunning = false;
        this.daemonLastRunTime = Instant.now();
        getLogger().debug(".taskReportSynchronisationDaemon(): Exit");
    }

    //
    // Getters (and Setters)
    //

    @Override
    protected String getFriendlyName() {
        return ("PetasosNotificationForwarderAgent");
    }

    //
    // Getters and Setters
    //

    protected Logger getLogger(){
        return(LOG);
    }


    protected static long getSynchronizationCheckPeriod() {
        return SYNCHRONIZATION_CHECK_PERIOD;
    }

    protected static long getInitialCheckDelayPeriod() {
        return INITIAL_CHECK_DELAY_PERIOD;
    }

    protected CapabilityUtilisationBrokerInterface getCapabilityUtilisationBroker() {
        return capabilityUtilisationBroker;
    }

    protected ObjectMapper getJsonMapper() {
        return jsonMapper;
    }

    protected ProcessingPlantInterface getProcessingPlant() {
        return processingPlant;
    }

    protected Object getNotificationQueueLock() {
        return notificationQueueLock;
    }

    public Queue<PetasosComponentITOpsNotification> getTaskReportQueue() {
        return taskReportQueue;
    }

    protected boolean isDaemonIsStillRunning() {
        return daemonIsStillRunning;
    }

    protected Instant getDaemonLastRunTime() {
        return daemonLastRunTime;
    }

    protected Long getNotificationSynchronisationResetPeriod(){
        return(this.NOTIFICATION_SYNCHRONISATION_WATCHDOG_RESET_PERIOD);
    }

    //
    // Helpers
    //

    public void addTaskReport(PetasosComponentITOpsNotification notification){
        getLogger().debug(".addTaskReport(): Entry, notification->{}", notification);
        if(notification == null){
            return;
        }
        getTaskReportQueue().add(notification);
    }

    public PetasosComponentITOpsNotification getNextNotification(){
        getLogger().debug(".getNextNotification(): Entry");
        if(getTaskReportQueue().isEmpty()){
            return(null);
        }
        PetasosComponentITOpsNotification nextNotification = getTaskReportQueue().poll();
        return(nextNotification);
    }

    public boolean hasMoreNotifications(){
        boolean hasMore = (getTaskReportQueue().isEmpty() != true);
        return(hasMore);
    }
}
