/*
 * Copyright (c) 2020 Mark A. Hunter
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
package net.fhirfactory.dricats.petasos.participant.wup.base;

import net.fhirfactory.dricats.configuration.defaults.petasos.PetasosPropertyConstants;
import net.fhirfactory.dricats.interfaces.petasos.participant.capabilities.ProcessingPlantRoleSupportInterface;
import net.fhirfactory.dricats.interfaces.petasos.participant.topology.ProcessingPlantConfigurationServiceInterface;
import net.fhirfactory.dricats.interfaces.petasos.participant.topology.WorkshopInterface;
import net.fhirfactory.dricats.internals.fhir.r4.internal.topics.FHIRElementTopicFactory;
import net.fhirfactory.dricats.model.component.datatypes.ComponentId;
import net.fhirfactory.dricats.model.component.valuesets.ComponentStatusEnum;
import net.fhirfactory.dricats.model.petasos.participant.capabilities.PetasosParticipantRegistration;
import net.fhirfactory.dricats.model.petasos.participant.components.common.PetasosParticipant;
import net.fhirfactory.dricats.model.petasos.participant.components.wup.valuesets.WUPArchetypeEnum;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.dataparcel.DataParcelManifest;
import net.fhirfactory.dricats.model.petasos.tasking.routing.identification.WUPComponentNames;
import net.fhirfactory.dricats.model.petasos.tasking.routing.subscriptions.datatypes.DataParcelManifestSubscriptionMask;
import net.fhirfactory.dricats.petasos.observations.metrics.PetasosMetricAgentFactory;
import net.fhirfactory.dricats.petasos.observations.metrics.agents.ProcessingPlantMetricsAgent;
import net.fhirfactory.dricats.petasos.observations.metrics.agents.ProcessingPlantMetricsAgentAccessor;
import net.fhirfactory.dricats.petasos.observations.metrics.agents.WorkUnitProcessorMetricsAgent;
import net.fhirfactory.dricats.petasos.participant.manager.LocalPetasosParticipantCacheIM;
import net.fhirfactory.dricats.petasos.tasking.fulfilment.wupcontainer.manager.WorkUnitProcessorFrameworkManager;
import net.fhirfactory.dricats.petasos.tasking.observations.reporting.tasks.PetasosTaskReportAgentFactory;
import net.fhirfactory.dricats.petasos.tasking.observations.reporting.tasks.agents.WorkUnitProcessorTaskReportAgent;
import net.fhirfactory.dricats.util.FHIRContextUtility;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.model.RouteDefinition;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generic Message Orientated Architecture (MOA) Work Unit Processor (WUP) Template
 * 
 * @author Mark A. Hunter
 * @since 2020-07-01
 */

public abstract class  GenericMessageBasedWUPTemplate extends UnmonitoredWorkUnitProcessorBase {

    public static final Integer IPC_PACKET_MAXIMUM_FRAME_SIZE = 25 * 1024 * 1024; // 25 MB
    private boolean initialised;
    private PetasosParticipant petasosParticipant;
    private WUPComponentNames nameSet;
    private WUPArchetypeEnum wupArchetype;
    private Set<DataParcelManifestSubscriptionMask> topicSubscriptionSet;
    private WorkUnitProcessorMetricsAgent metricsAgent;
    private WorkUnitProcessorTaskReportAgent taskReportAgent;

    @Inject
    private WorkUnitProcessorFrameworkManager wupFrameworkManager;

    @Inject
    private FHIRElementTopicFactory fhirTopicIDBuilder;

    @Inject
    private ProcessingPlantConfigurationServiceInterface processingPlantServices;

    @Inject
    private CamelContext camelContext;

    @Inject
    private FHIRContextUtility fhirContextUtility;

    @Inject
    private LocalPetasosParticipantCacheIM participantCacheIM;

    @Inject
    private PetasosMetricAgentFactory metricAgentFactory;

    @Inject
    private ProcessingPlantMetricsAgentAccessor processingPlantMetricsAgentAccessor;

    @Inject
    private PetasosTaskReportAgentFactory taskReportAgentFactory;

    @Inject
    private ProcessingPlantRoleSupportInterface processingPlantCapabilityStatement;

    //
    // Constructor(s)
    //

    public GenericMessageBasedWUPTemplate() {
        super();
        this.initialised = false;
    }

    /**
     * This function essentially establishes the WUP itself, by first calling all the (abstract classes realised within subclasses)
     * and setting the core attributes of the WUP. Then, it executes the buildWUPFramework() function, which invokes the Petasos
     * framework around this WUP.
     *
     * It is automatically called by the CDI framework following Constructor invocation (see @PostConstruct tag).
     */
    @PostConstruct
    protected void initialise(){
        getLogger().debug(".initialise(): Entry");
        if(!isInitialised()) {
            getLogger().info(".initialise(): Initialising...");

            getLogger().info(".initialise(): Metadata: Component Name --> {}", specifyComponentName());
            getLogger().info(".initialise(): Metadata: Component Version --> {}", specifyComponentVersion());

            getLogger().info(".initialise(): [Initialise Containing Processing Plant (if required)] Start");
            this.getProcessingPlant().initialisePlant();
            getLogger().info(".initialise(): [Initialise Containing Processing Plant (if required)] Finish");

            getLogger().info(".initialise(): [Initialise PetasosParticipant] Start");
            this.petasosParticipant = buildPetasosParticipant();
            getLogger().info(".initialise(): [Initialise PetasosParticipant] Finish");

            getLogger().info(".initialise(): [Initialise Route NameSet for this WUP] Start");
            this.nameSet = new WUPComponentNames(specifyParticipantName(), specifyParticipantVersion());
            getLogger().info(".initialise(): [Initialise Route NameSet for this WUP] Finish");

            getLogger().info(".initialise(): [Setting the WUP EgressEndpoint] Start");
            setEgressEndpoint(specifyEgressEndpoint());
            getLogger().info(".initialise(): [Setting the WUP EgressEndpoint] Finish");

            getLogger().info(".initialise(): [Setting the WUP IngresEndpoint] Start");
            setIngressEndpoint(specifyIngresEndpoint());
            getLogger().info(".initialise(): [Setting the WUP IngresEndpoint] Finish");

            getLogger().info(".initialise(): [Setting the WUP Archetype] Start");
            this.wupArchetype = specifyWUPArchetype();
            getLogger().info(".initialise(): [Setting the WUP Archetype] Finish");

            getLogger().info(".initialise(): [Invoking subclass initialising function(s)] Start");
            executePostInitialisationActivities();
            getLogger().info(".initialise(): [Invoking subclass initialising function(s)] Finish");

            getLogger().info(".initialise(): [Setting the Topic Subscription Set] Start");
            this.topicSubscriptionSet = specifySubscriptionTopics();
            getLogger().info(".initialise(): [Setting the Topic Subscription Set] Finish");

            getLogger().info(".initialise(): [Establish the WorkUnitProcessor Metrics Agent] Start");
            ComponentId componentId = getComponentId();
            String participantName = getPetasosParticipant().getName();
            this.metricsAgent = metricAgentFactory.newWorkUnitProcessingMetricsAgent(processingPlantCapabilityStatement, componentId, participantName);
            getLogger().info(".initialise(): [Establish the WorkUnitProcessor Metrics Agent] Finish");

            getLogger().info(".initialise(): [Establish the WorkUnitProcessor Metrics Agent] Start");
            this.taskReportAgent = taskReportAgentFactory.newWorkUnitProcessorTaskReportingAgent(processingPlantCapabilityStatement, componentId, participantName);
            getLogger().info(".initialise(): [Establish the WorkUnitProcessor Metrics Agent] Start");

            getLogger().info(".initialise(): [Establish (if any) Endpoint Metric Agents] Start");
            establishEndpointMetricAgents();
            getLogger().info(".initialise(): [Establish (if any) Endpoint Metric Agents] Finish");

            getLogger().info(".initialise(): [Build Surrounding WUP Framework] Start");
            buildWUPFramework(this.getContext());
            getLogger().info(".initialise(): [Build Surrounding WUP Framework] Finish");

            getLogger().info(".initialise(): [Register any Capabilities this Work Unit Processor supports] Start");
            registerCapabilities();
            getLogger().info(".initialise(): [Register any Capabilities this Work Unit Processor supports] Finish");

            getLogger().info(".initialise(): [Set my component status!] Start");
            this.setOperationalStatus(ComponentStatusEnum.SOFTWARE_COMPONENT_OPERATIONAL);
            getLogger().info(".initialise(): [Set my component status!] Finish");

            this.initialised = true;

            getLogger().info(".initialise(): Initialising... Done...");
        } else {
            getLogger().debug(".initialise(): Already initialised");
        }
        getLogger().debug(".initialise(): Exit");
    }

    //
    // To be implemented methods (in Specialisations)
    //

    protected abstract Set<DataParcelManifestSubscriptionMask> specifySubscriptionTopics();
    protected abstract List<DataParcelManifest> declarePublishedTopics();
    protected abstract WUPArchetypeEnum specifyWUPArchetype();
    protected abstract String specifyParticipantName();
    protected abstract String specifyParticipantDisplayName();
    protected abstract String specifyParticipantVersion();

    protected void establishEndpointMetricAgents(){
        // no nothing
    }

    protected void registerCapabilities(){
        // do nothing
    }

    protected abstract WorkshopInterface specifyWorkshop();

    abstract protected Logger specifyLogger();

    //
    // Getters (and Setters)
    //

    private boolean isInitialised(){
        return(this.initialised);
    }

    protected WorkshopInterface getWorkshop(){
        return(specifyWorkshop());
    }

    protected ProcessingPlantConfigurationServiceInterface getProcessingPlant(){
        return(processingPlantServices);
    }

    protected Logger getLogger(){
        return(specifyLogger());
    }

    protected boolean getUsesWUPFrameworkGeneratedIngresEndpoint(){
        return(getIngressEndpoint().isUsingStandardCamelRoute());
    }
    protected boolean getUsesWUPFrameworkGeneratedEgressEndpoint(){
        return(getEgressEndpoint().isUsingStandardCamelRoute());
    }

    protected void executePostInitialisationActivities(){
        // Subclasses can optionally override
    }

    protected PetasosMetricAgentFactory getMetricAgentFactory(){
        return(this.metricAgentFactory);
    }

    protected String getEndpointHostName(){
        String dnsName = getProcessingPlant().getMeAsASoftwareComponent().getAssignedDNSName();
        return(dnsName);
    }

    protected WUPComponentNames getNameSet() {
        return nameSet;
    }

    protected WUPArchetypeEnum getWupArchetype() {
        return wupArchetype;
    }

    protected List<DataParcelManifest> getTopicSubscriptionSet() {
        return topicSubscriptionSet;
    }

    protected void setTopicSubscriptionSet(List<DataParcelManifest> topicSubscriptionSet) {
        this.topicSubscriptionSet = topicSubscriptionSet;
    }

    protected FHIRElementTopicFactory getFHIRTopicIDBuilder(){
        return(this.fhirTopicIDBuilder);
    }

    protected FHIRContextUtility getFHIRContextUtility(){
        return(this.fhirContextUtility);
    }


    protected WorkUnitProcessorMetricsAgent getMetricsAgent(){
        return(this.metricsAgent);
    }

    protected ProcessingPlantMetricsAgent getProcessingPlantMetricsAgent(){
        return(this.processingPlantMetricsAgentAccessor.getMetricsAgent());
    }

    protected WorkUnitProcessorTaskReportAgent getTaskReportAgent(){
        return(this.taskReportAgent);
    }

    public PetasosParticipant getPetasosParticipant() {
        return petasosParticipant;
    }

    //
    // Routing Support Functions
    //

    protected String ingresFeed(){
        return(getIngresEndpoint().getEndpointSpecification());
    }

    protected String egressFeed(){
        return(getEgressEndpoint().getEndpointSpecification());
    }

    protected RouteDefinition fromIncludingPetasosServices(String uri) {
        NodeDetailInjector nodeDetailInjector = new NodeDetailInjector();
        AuditAgentInjector auditAgentInjector = new AuditAgentInjector();
        TaskReportAgentInjector taskReportAgentInjector = new TaskReportAgentInjector();
        RouteDefinition route = fromWithStandardExceptionHandling(uri);
        route
                .process(nodeDetailInjector)
                .process(auditAgentInjector)
                .process(taskReportAgentInjector)
        ;
        return route;
    }

    protected RouteDefinition fromIncludingPetasosServicesNoExceptionHandling(String uri) {
        NodeDetailInjector nodeDetailInjector = new NodeDetailInjector();
        AuditAgentInjector auditAgentInjector = new AuditAgentInjector();
        TaskReportAgentInjector taskReportAgentInjector = new TaskReportAgentInjector();
        RouteDefinition route = from(uri);
        route
                .process(nodeDetailInjector)
                .process(auditAgentInjector)
                .process(taskReportAgentInjector)
        ;
        return route;
    }

    public class NodeDetailInjector implements Processor {
        @Override
        public void process(Exchange exchange) throws Exception {
            getLogger().debug("NodeDetailInjector.process(): Entry");
            boolean alreadyInPlace = false;
            if(exchange.hasProperties()) {
                WorkUnitProcessorSoftwareComponent wupTN = exchange.getProperty(PetasosPropertyConstants.WUP_PETASOS_PARTICIPANT_EXCHANGE_PROPERTY_NAME, WorkUnitProcessorSoftwareComponent.class);
                if (wupTN != null) {
                    alreadyInPlace = true;
                }
            }
            if(!alreadyInPlace) {
                exchange.setProperty(PetasosPropertyConstants.WUP_PETASOS_PARTICIPANT_EXCHANGE_PROPERTY_NAME, getMeAsATopologyComponent());
            }
        }
    }

    public class AuditAgentInjector implements Processor{
        @Override
        public void process(Exchange camelExchange) throws Exception{
            getLogger().debug("AuditAgentInjector.process(): Entry");
            camelExchange.setProperty(PetasosPropertyConstants.WUP_METRICS_AGENT_EXCHANGE_PROPERTY, getMetricsAgent());
        }
    }

    public class TaskReportAgentInjector implements Processor{
        @Override
        public void process(Exchange camelExchange) throws Exception{
            getLogger().debug("TaskReportAgentInjector.process(): Entry");
            camelExchange.setProperty(PetasosPropertyConstants.ENDPOINT_TASK_REPORT_AGENT_EXCHANGE_PROPERTY, getTaskReportAgent());
        }
    }

    //
    // PetasosParticipant Functions
    //

    public void buildWUPFramework(CamelContext routeContext) {
        getLogger().debug(".buildWUPFramework(): Entry");
        wupFrameworkManager.buildWUPFramework(getPetasosParticipant(), this.getTopicSubscriptionSet(), this.getWupArchetype(), getMetricsAgent());
        getLogger().debug(".buildWUPFramework(): Exit");
    }

    private PetasosParticipant buildPetasosParticipant(){
        getLogger().debug(".buildPetasosParticipant(): Entry");
        Set<DataParcelManifestSubscriptionMask> subscribedTopicSet = new HashSet<>();
        if (!specifySubscriptionTopics().isEmpty()) {
            for (DataParcelManifestSubscriptionMask currentTopicID : specifySubscriptionTopics()) {
                DataParcelManifestSubscriptionMask taskWorkItem = new DataParcelManifestSubscriptionMask(currentTopicID);
                if (subscribedTopicSet.contains(taskWorkItem)) {
                    // Do nothing
                } else {
                    subscribedTopicSet.add(taskWorkItem);
                }
            }
        }

        PetasosParticipantRegistration participantRegistration = participantCacheIM.registerPetasosParticipant(getPetasosParticipant(), declarePublishedTopics(), subscribedTopicSet);
        PetasosParticipant participant = null;
        if(participantRegistration != null){
            participant = participantRegistration.getParticipant();
        }
        return(participant);
    }
}
