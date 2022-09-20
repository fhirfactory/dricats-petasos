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
import net.fhirfactory.pegacorn.core.interfaces.oam.subscriptions.PetasosSubscriptionReportBrokerInterface;
import net.fhirfactory.pegacorn.core.interfaces.oam.subscriptions.PetasosSubscriptionReportingServiceProviderNameInterface;
import net.fhirfactory.pegacorn.core.interfaces.topology.ProcessingPlantInterface;
import net.fhirfactory.pegacorn.core.model.petasos.oam.subscriptions.reporting.PetasosSubscriptionSummaryReport;
import net.fhirfactory.pegacorn.petasos.endpoints.services.subscriptions.PetasosParticipantSubscriptionServicesEndpointBase;
import net.fhirfactory.dricats.petasos.ipc.services.observations.configuration.subscriptions.PetasosSubscriptionReportingAgent;
import net.fhirfactory.dricats.petasos.ipc.services.observations.configuration.subscriptions.cache.PetasosLocalSubscriptionReportingDM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;

@ApplicationScoped
public class PetasosSubscriptionReportWorker extends AgentWorkerBase {
    private static final Logger LOG = LoggerFactory.getLogger(PetasosSubscriptionReportWorker.class);
    private boolean initialised;
    private static long SYNCHRONIZATION_CHECK_PERIOD = 30000;
    private static long INITIAL_CHECK_DELAY_PERIOD=60000;
    private boolean backgroundCheckInitiated;
    private ObjectMapper jsonMapper;

    @Inject
    private PetasosLocalSubscriptionReportingDM pubsubMapDM;

    @Inject
    private PetasosSubscriptionReportingAgent pubSubCollectionAgent;

    @Inject
    private PetasosRoutingClusterConnection subscriptionServicesBroker;

    @Inject
    private PetasosSubscriptionReportingServiceProviderNameInterface serviceProviderName;

    @Inject
    private PetasosSubscriptionReportBrokerInterface subscriptionReportBroker;

    @Inject
    private ProcessingPlantInterface processingPlant;

    public PetasosSubscriptionReportWorker() {
        super();
        this.initialised = false;
        this.backgroundCheckInitiated = false;
        this.jsonMapper = new ObjectMapper();
        JavaTimeModule module = new JavaTimeModule();
        this.jsonMapper.registerModule(module);
        this.jsonMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @PostConstruct
    public void initialise(){
        if(!initialised){
            if(!getProcessingPlant().isITOpsNode()) {
                scheduleSubscriptionMapForwarding();
            }
            this.initialised = true;
        }
    }

    protected void forwardPubSubReports() {
        LOG.debug(".forwardPubSubReports(): Entry");
        //
        // Build Map
        //
        LOG.trace(".forwardPubSubReports(): [Build PubSub Summaries] Start");
        pubSubCollectionAgent.refreshLocalPubSubReportingMap();
        LOG.trace(".forwardPubSubReports(): [Build PubSub Summaries] Finish");
        //
        // Get Data to be Reported ON
        //
        LOG.trace(".forwardPubSubReports(): [Build WorkUnitProcessor PubSub Report] Start");
        PetasosSubscriptionSummaryReport pubSubReport = pubsubMapDM.getPubSubReport();
        pubSubReport.setProcessingPlantComponentID(getProcessingPlant().getMeAsASoftwareComponent().getComponentID());
        LOG.trace(".forwardPubSubReports(): [Build WorkUnitProcessor PubSub Report] Start");
        //
        // Send via Broker
        //
        String subscriptionReportingServerName = serviceProviderName.getPetasosSubscriptionReportingServiceProviderName();
        Instant submissionInstant = subscriptionReportBroker.replicationSubscriptionSummaryReportToServer(subscriptionReportingServerName, pubSubReport);

        LOG.debug(".forwardPubSubReports(): Finished, sent time->{}", submissionInstant);
    }

    //
    // Schedule Period Synchronisation Check/Update Activity
    //

    public void scheduleSubscriptionMapForwarding() {

        getLogger().debug(".scheduleSubscriptionMapForwarding(): Entry");
        if(isBackgroundCheckInitiated()){
            // do nothing
        } else {
            TimerTask ITOpsPubSubCacheSynchronisationDaemon = new TimerTask() {
                public void run() {
                    getLogger().debug(".ITOpsPubSubCacheSynchronisationDaemon(): Entry");
                    forwardPubSubReports();
                    getLogger().debug(".ITOpsPubSubCacheSynchronisationDaemon(): Exit");
                }
            };
            String timerName = "ITOpsPubSubCacheSynchronisationDaemon";
            Timer timer = new Timer(timerName);
            timer.schedule(ITOpsPubSubCacheSynchronisationDaemon, getInitialCheckDelayPeriod(), getSynchronizationCheckPeriod());
            setBackgroundCheckInitiated(true);
        }
        getLogger().debug(".scheduleSubscriptionMapForwarding(): Exit");
    }

    //
    //
    //

    @Override
    protected String getFriendlyName() {
        return ("PetasosSubscriptionReportingAgent");
    }

    //
    // Getters (and Setters)
    //

    public ProcessingPlantInterface getProcessingPlant() {
        return processingPlant;
    }

    protected Logger getLogger() {
        return (LOG);
    }

    public boolean isInitialised() {
        return initialised;
    }

    public static long getSynchronizationCheckPeriod() {
        return SYNCHRONIZATION_CHECK_PERIOD;
    }

    public static long getInitialCheckDelayPeriod() {
        return INITIAL_CHECK_DELAY_PERIOD;
    }

    public boolean isBackgroundCheckInitiated() {
        return backgroundCheckInitiated;
    }

    public void setBackgroundCheckInitiated(boolean value){
        this.backgroundCheckInitiated = value;
    }

    public ObjectMapper getJsonMapper() {
        return jsonMapper;
    }
}
