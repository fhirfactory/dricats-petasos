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
import net.fhirfactory.dricats.petasos.tasking.observations.synchronisation.worker.common.AgentWorkerBase;
import net.fhirfactory.pegacorn.core.interfaces.oam.notifications.PetasosITOpsNotificationAgentInterface;
import net.fhirfactory.pegacorn.core.interfaces.oam.notifications.PetasosITOpsNotificationBrokerInterface;
import net.fhirfactory.pegacorn.core.interfaces.topology.ProcessingPlantInterface;
import net.fhirfactory.pegacorn.core.interfaces.capabilities.CapabilityUtilisationBrokerInterface;
import net.fhirfactory.pegacorn.core.model.petasos.oam.metrics.reporting.factories.PetasosComponentMetricSetFactory;
import net.fhirfactory.pegacorn.core.model.petasos.oam.notifications.PetasosComponentITOpsNotification;
import net.fhirfactory.dricats.model.configuration.names.subsystems.SubsystemNames;
import net.fhirfactory.dricats.petasos.tasking.observations.metrics.cache.PetasosLocalMetricsDM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@ApplicationScoped
public class PetasosNotificationsAgentWorker extends AgentWorkerBase implements PetasosITOpsNotificationAgentInterface {
    private static final Logger LOG = LoggerFactory.getLogger(PetasosNotificationsAgentWorker.class);

    Queue<PetasosComponentITOpsNotification> notificationQueue;
    private Object notificationQueueLock;

    private static long SYNCHRONIZATION_CHECK_PERIOD = 15000L;
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
    private PetasosITOpsNotificationBrokerInterface notificationBroker;

    @Inject
    private SubsystemNames subsystemNames;

    @Inject
    private PetasosLocalMetricsDM metricsDM;

    @Inject
    private PetasosComponentMetricSetFactory componentMetricSetFactory;

    //
    // Constructor(s)
    //

    public PetasosNotificationsAgentWorker(){
        super();
        this.notificationQueueLock = new Object();
        this.initialised = false;
        this.notificationQueue = new ConcurrentLinkedQueue<>();
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
            scheduleNotificationsSynchronisation();
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
    public void sendNotification(PetasosComponentITOpsNotification notification) {
        addNotification(notification);
    }

    //
    // Actual Daemon Tasks
    //

    protected void forwardLocalNotificationsToServer(){
        LOG.debug(".forwardLocalNotificationsToServer(): Entry");
        while(hasMoreNotifications()) {
            PetasosComponentITOpsNotification nextNotification = null;
            synchronized (notificationQueueLock) {
                nextNotification = getNextNotification();
            }
            if(nextNotification != null) {
                LOG.debug(".forwardLocalNotificationsToServer(): Loaded metrics form local cache, forwarding");
                LOG.debug(".forwardLocalMetricsToServer(): Sending notification for Participant->{}", nextNotification.getParticipantName());
                notificationBroker.sendNotification(nextNotification);
            }
        }
        LOG.debug(".forwardLocalNotificationsToServer(): Exit");
    }

    //
    // Schedule Period Synchronisation Check/Update Activity
    //

    protected void scheduleNotificationsSynchronisation() {

        getLogger().debug(".scheduleNotificationSynchronisation(): Entry");
        TimerTask NotificationForwarderDaemon = new TimerTask() {
            public void run() {
                getLogger().debug(".NotificationForwarderDaemon(): Entry");
                if(!isDaemonIsStillRunning()){
                    notificationSynchronisationDaemon();
                } else {
                    Long ageSinceRun = Instant.now().getEpochSecond() - getDaemonLastRunTime().getEpochSecond();
                    if (ageSinceRun > getNotificationSynchronisationResetPeriod()) {
                        notificationSynchronisationDaemon();
                    }
                }
                notificationSynchronisationDaemon();
                getLogger().debug(".NotificationForwarderDaemon(): Exit");
            }
        };
        String timerName = "NotificationForwarderDaemonTimer";
        Timer timer = new Timer(timerName);
        timer.schedule(NotificationForwarderDaemon, getInitialCheckDelayPeriod(), getSynchronizationCheckPeriod());
        getLogger().debug(".scheduleNotificationSynchronisation(): Exit");
    }

    protected void notificationSynchronisationDaemon(){
        getLogger().debug(".notificationSynchronisationDaemon(): Entry");
        this.daemonIsStillRunning = true;
        forwardLocalNotificationsToServer();
        this.daemonIsStillRunning = false;
        this.daemonLastRunTime = Instant.now();
        getLogger().debug(".notificationSynchronisationDaemon(): Exit");
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

    public Queue<PetasosComponentITOpsNotification> getNotificationQueue() {
        return notificationQueue;
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

    public void addNotification(PetasosComponentITOpsNotification notification){
        getLogger().debug(".addNotification(): Entry, notification->{}", notification);
        if(notification == null){
            return;
        }
        getNotificationQueue().add(notification);
    }

    public PetasosComponentITOpsNotification getNextNotification(){
        getLogger().debug(".getNextNotification(): Entry");
        if(getNotificationQueue().isEmpty()){
            return(null);
        }
        PetasosComponentITOpsNotification nextNotification = getNotificationQueue().poll();
        return(nextNotification);
    }

    public boolean hasMoreNotifications(){
        boolean hasMore = (getNotificationQueue().isEmpty() != true);
        return(hasMore);
    }
}
