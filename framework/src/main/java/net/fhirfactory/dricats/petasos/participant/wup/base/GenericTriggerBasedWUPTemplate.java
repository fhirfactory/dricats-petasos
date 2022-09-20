/*
 * Copyright (c) 2021 Mark A. Hunter
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
import net.fhirfactory.dricats.interfaces.petasos.ipc.endpoints.PetasosEndpointContainerInterface;
import net.fhirfactory.dricats.interfaces.petasos.participant.capabilities.ProcessingPlantRoleSupportInterface;
import net.fhirfactory.dricats.interfaces.petasos.participant.topology.WorkshopInterface;
import net.fhirfactory.dricats.internals.fhir.r4.internal.topics.FHIRElementTopicFactory;
import net.fhirfactory.dricats.model.component.datatypes.ComponentId;
import net.fhirfactory.dricats.model.petasos.participant.components.common.PetasosParticipant;
import net.fhirfactory.dricats.model.petasos.participant.archetypes.wup.valuesets.WUPArchetypeEnum;
import net.fhirfactory.dricats.model.petasos.participant.capabilities.PetasosParticipantRegistration;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.dataparcel.DataParcelManifest;
import net.fhirfactory.dricats.model.petasos.tasking.fulfillment.execctrl.PetasosTaskJobCard;
import net.fhirfactory.dricats.model.petasos.tasking.routing.subscriptions.datatypes.DataParcelManifestSubscriptionMask;
import net.fhirfactory.dricats.petasos.observations.metrics.PetasosMetricAgentFactory;
import net.fhirfactory.dricats.petasos.observations.metrics.agents.WorkUnitProcessorMetricsAgent;
import net.fhirfactory.dricats.petasos.participant.manager.LocalPetasosParticipantCacheIM;
import net.fhirfactory.dricats.model.petasos.tasking.routing.identification.WUPComponentNames;
import net.fhirfactory.dricats.petasos.tasking.fulfilment.wupcontainer.manager.WorkUnitProcessorFrameworkManager;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.model.RouteDefinition;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generic Trigger Initiated Message Architecture Work Unit Processor (WUP) Template
 *
 */

public abstract class GenericTriggerBasedWUPTemplate extends UnmonitoredWorkUnitProcessorBase {

    abstract protected Logger specifyLogger();

    protected Logger getLogger(){
        return(specifyLogger());
    }

    private PetasosParticipant petasosParticipantInformation;
    private PetasosTaskJobCard wupJobCard;
    private WUPComponentNames nameSet;
    private WUPArchetypeEnum wupArchetype;
    private List<DataParcelManifestSubscriptionMask> topicSubscriptionSet;
    private PetasosEndpointContainerInterface egressEndpoint;
    private PetasosEndpointContainerInterface ingresEndpoint;
    private String wupInstanceName;
    private WorkUnitProcessorMetricsAgent metricsAgent;
    private boolean workUnitProcessorTemplateInitialised;

    @Inject
    private WorkUnitProcessorFrameworkManager frameworkManager;

    @Inject
    private FHIRElementTopicFactory fhirTopicIDBuilder;

    @Inject
    private CamelContext camelContext;

    @Inject
    private PetasosMetricAgentFactory metricAgentFactory;

    @Inject
    private LocalPetasosParticipantCacheIM participantCacheIM;

    @Inject
    private ProcessingPlantRoleSupportInterface processingPlantCapabilityStatement;

    //
    // Constructor(s)
    //

    public GenericTriggerBasedWUPTemplate() {
        super();
        this.workUnitProcessorTemplateInitialised = false;
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
        if(isWorkUnitProcessorBaseInitialised()){
            getLogger().debug(".initialise(): Already initialised, nothing to do");
            return;
        }
        getLogger().info(".initialise(): [WUP Base] Initialising.... ");
        getLogger().info(".initialise(): WUP Instance Name --> {}", getWUPInstanceName());
        getLogger().info(".initialise(): WUP Instance Version --> {}", specifyWUPInstanceVersion());

        getLogger().info(".initialise(): [Build WUP Topology Node Element] Start");

        getLogger().info(".initialise(): [Build WUP Topology Node Element] Finish");

        getLogger().info(".initialise(): [Build PetasosParticipant] Start");
        setPetasosParticipantInformation(buildPetasosParticipant());
        getLogger().info(".initialise(): [Build PetasosParticipant] Finish");

        getLogger().info(".initialise(): [Build WUP Component Name-set] Start");
        this.nameSet = new WUPComponentNames(getPetasosParticipantInformation().getParticipantId().getParticipantName(), getPetasosParticipantInformation().getParticipantId().getParticipantVersion());
        getLogger().info(".initialise(): [Build WUP Component Name-set] Finish");

        getLogger().info(".initialise(): Setting the WUP EgressEndpoint");
        this.egressEndpoint = specifyEgressEndpoint();
        getLogger().info(".initialise(): Setting the WUP IngresEndpoint");
        this.ingresEndpoint = specifyIngresEndpoint();
        getLogger().info(".initialise(): Setting the WUP Archetype - which is used by the WUP Framework to ascertain what wrapping this WUP needs");
        this.wupArchetype =  specifyWUPArchetype();
        getLogger().info(".initialise(): Now invoking subclass initialising function(s)");
        executePostInitialisationActivities();
        getLogger().info(".initialise(): Setting the Topic Subscription Set (i.e. the list of Data Sets we will process)");
        this.topicSubscriptionSet = specifySubscriptionTopics();

        getLogger().info(".initialise(): [Build PetasosParticipant] Start");
        setPetasosParticipantInformation(buildPetasosParticipant());
        getLogger().info(".initialise(): [Build PetasosParticipant] Finish");

        getLogger().info(".initialise(): [Establish the metrics agent] Start");
        ComponentId componentId = SerializationUtils.clone(getComponentId());
        String participantName = getPetasosParticipantInformation().getParticipantId().getParticipantName();
        this.metricsAgent = metricAgentFactory.newWorkUnitProcessingMetricsAgent(processingPlantCapabilityStatement, componentId, participantName);
        getLogger().info(".initialise(): [Establish the metrics agent] Finish");

        getLogger().info(".initialise(): [Build the Petasos framework around this WUP] Start");
        buildWUPFramework(this.getContext());
        getLogger().info(".initialise(): [Build the Petasos framework around this WUP] Finish");

        setWorkUnitProcessorBaseInitialised(true);
        getLogger().info(".initialise(): [WUP Base] Initialising.... Done...");

        getLogger().debug(".initialise(): Exit");
    }

    //
    // To be implemented methods (in Specialisations)
    //

    protected abstract List<DataParcelManifestSubscriptionMask> specifySubscriptionTopics();
    protected abstract List<DataParcelManifest> declarePublishedTopics();
    protected abstract WUPArchetypeEnum specifyWUPArchetype();
    protected abstract String specifyWUPInstanceVersion();

    protected abstract WorkshopInterface specifyWorkshop();
    protected abstract PetasosEndpointContainerInterface specifyIngresEndpoint();
    protected abstract PetasosEndpointContainerInterface specifyEgressEndpoint();

    //
    // PostConstruct
    //

    protected void executePostInitialisationActivities(){
        // Subclasses can optionally override
    }

    //
    // Getters (and Setters)
    //

    protected WorkshopInterface getWorkshop(){
        return(specifyWorkshop());
    }

    public PetasosParticipant getPetasosParticipantInformation() {
        return petasosParticipantInformation;
    }

    public void setPetasosParticipantInformation(PetasosParticipant petasosParticipantInformation) {
        this.petasosParticipantInformation = petasosParticipantInformation;
    }

    public PetasosEndpointContainerInterface getEgressEndpoint() {
        return egressEndpoint;
    }

    public PetasosEndpointContainerInterface getIngresEndpoint() {
        return ingresEndpoint;
    }

    protected boolean getUsesWUPFrameworkGeneratedIngresEndpoint(){
        return(getIngresEndpoint().isFrameworkEnabled());
    }
    protected boolean getUsesWUPFrameworkGeneratedEgressEndpoint(){
        return(getEgressEndpoint().isFrameworkEnabled());
    }


    public WorkUnitProcessorFrameworkManager getFrameworkManager(){
        return(this.frameworkManager);
    }

    public WUPComponentNames getNameSet() {
        return nameSet;
    }

    public WUPArchetypeEnum getWupArchetype() {
        return wupArchetype;
    }

    public List<DataParcelManifestSubscriptionMask> getTopicSubscriptionSet() {
        return topicSubscriptionSet;
    }

    public void setTopicSubscriptionSet(List<DataParcelManifestSubscriptionMask> topicSubscriptionSet) {
        this.topicSubscriptionSet = topicSubscriptionSet;
    }

    public FHIRElementTopicFactory getFHIRTopicIDBuilder(){
        return(this.fhirTopicIDBuilder);
    }

    public CamelContext getCamelContext() {
        return camelContext;
    }

    public String getWUPInstanceName(){
        return(this.wupInstanceName);
    }

    protected WorkUnitProcessorMetricsAgent getMetricsAgent(){
        return(this.metricsAgent);
    }

    public ProcessingPlantRoleSupportInterface getProcessingPlantServiceProviderFunction() {
        return (processingPlantCapabilityStatement);
    }

    //
    // Helper Methods
    //

    public void buildWUPFramework(CamelContext routeContext) {
        getLogger().debug(".buildWUPFramework(): Entry");
        frameworkManager.buildWUPFramework(getPetasosParticipantInformation(), this.getTopicSubscriptionSet(), this.getWupArchetype(), getMetricsAgent());
        getLogger().debug(".buildWUPFramework(): Exit");
    }

    public String getEndpointHostName(){
        String dnsName = "not defined";
        return(dnsName);
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

    public class NodeDetailInjector implements Processor{
        @Override
        public void process(Exchange exchange) throws Exception {
            getLogger().debug("NodeDetailInjector.process(): Entry");
            boolean alreadyInPlace = false;
            if(exchange.hasProperties()) {
                PetasosParticipant wupTN = exchange.getProperty(PetasosPropertyConstants.WUP_PETASOS_PARTICIPANT_EXCHANGE_PROPERTY_NAME, PetasosParticipant.class);
                if (wupTN != null) {
                    alreadyInPlace = true;
                }
            }
            if(!alreadyInPlace) {
                exchange.setProperty(PetasosPropertyConstants.WUP_PETASOS_PARTICIPANT_EXCHANGE_PROPERTY_NAME, getPetasosParticipantInformation());
            }
        }
    }

    /**
     * @param uri
     * @return the RouteBuilder.from(uri) with all exceptions logged but not handled
     */
    protected RouteDefinition fromIncludingPetasosServices(String uri) {
        NodeDetailInjector nodeDetailInjector = new NodeDetailInjector();
        RouteDefinition route = fromWithStandardExceptionHandling(uri);
        route
                .process(nodeDetailInjector)
        ;
        return route;
    }

    /*
    protected IPCServerTopologyEndpoint deriveAssociatedTopologyEndpoint(String interfaceName, IPCAdapterDefinition interfaceDefinition){
        getLogger().debug(".deriveServerTopologyEndpoint(): Entry, interfaceName->{}, interfaceDefinition->{}", interfaceName, interfaceDefinition);
        ProcessingPlantSoftwareComponent processingPlantSoftwareComponent = processingPlantServices.getMeAsASoftwareComponent();
        getLogger().trace(".deriveServerTopologyEndpoint(): Parse through all endpoints and their IPC Definitions");
        for(TopologyNodeFDN endpointFDN: processingPlantSoftwareComponent.getEndpoints()){
            IPCServerTopologyEndpoint endpoint = (IPCServerTopologyEndpoint)topologyIM.getNode(endpointFDN);
            getLogger().trace(".deriveServerTopologyEndpoint(): endpoint->{}", endpoint);
            if(endpoint.getEndpointConfigurationName().equalsIgnoreCase(interfaceName)) {
                getLogger().trace(".deriveServerTopologyEndpoint(): names ({}) match, now confirming supported InterfaceDefinition", interfaceName);
                for (IPCAdapter currentInterface : endpoint.getAdapterList()) {
                    getLogger().trace(".deriveServerTopologyEndpoint(): currentInterface->{}", currentInterface);
                    for (IPCAdapterDefinition currentInterfaceDef : currentInterface.getSupportedInterfaceDefinitions()) {
                        getLogger().trace(".deriveServerTopologyEndpoint(): currentInterfaceDef->{}", currentInterfaceDef);
                        if (currentInterfaceDef.equals(interfaceDefinition)) {
                            getLogger().debug(".deriveServerTopologyEndpoint(): Exit, match found, currentInterfaceDef->{}, endpoint->{}", currentInterfaceDef, endpoint);
                            return (endpoint);
                        }
                    }
                }
            }
        }
        getLogger().debug(".deriveServerTopologyEndpoint(): Exit, nothing found!");
        return(null);
    }

    protected IPCTopologyEndpoint getTopologyEndpoint(String topologyEndpointName){
        getLogger().debug(".getTopologyEndpoint(): Entry, topologyEndpointName->{}", topologyEndpointName);
        ArrayList<TopologyNodeFDN> endpointFDNs = getProcessingPlant().getMeAsASoftwareComponent().getEndpoints();
        for(TopologyNodeFDN currentEndpointFDN: endpointFDNs){
            IPCTopologyEndpoint endpointTopologyNode = (IPCTopologyEndpoint)getTopologyIM().getNode(currentEndpointFDN);
            if(endpointTopologyNode.getEndpointConfigurationName().contentEquals(topologyEndpointName)){
                getLogger().debug(".getTopologyEndpoint(): Exit, node found -->{}", endpointTopologyNode);
                return(endpointTopologyNode);
            }
        }
        getLogger().debug(".getTopologyEndpoint(): Exit, Could not find node!");
        return(null);
    }
     */

    //
    // PetasosParticipant Functions
    //

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

        Set<DataParcelManifest> publishedTopicSet = new HashSet<>();
        if (!declarePublishedTopics().isEmpty()) {
            for (DataParcelManifest currentTopicID : declarePublishedTopics()) {
                DataParcelManifest taskWorkItem = new DataParcelManifest(currentTopicID);
                if (publishedTopicSet.contains(taskWorkItem)) {
                    // Do nothing
                } else {
                    publishedTopicSet.add(taskWorkItem);
                }
            }
        }
        String participantName = getPetasosParticipantInformation().getParticipantId().getParticipantName();
        PetasosParticipantRegistration participantRegistration = participantCacheIM.registerPetasosParticipant(getPetasosParticipantInformation(),  publishedTopicSet, subscribedTopicSet);
        PetasosParticipant participant = null;
        if(participantRegistration != null){
            participant = participantRegistration.getParticipant();
        }
        return(participant);
    }
}
