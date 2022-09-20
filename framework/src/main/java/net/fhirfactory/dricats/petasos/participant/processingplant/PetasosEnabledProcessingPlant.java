/*
 * Copyright (c) 2020 Mark A. Hunter (ACT Health)
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
package net.fhirfactory.dricats.petasos.participant.processingplant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.fhirfactory.dricats.configuration.api.services.processingplant.EdgeProcessingPlantConfigurationService;
import net.fhirfactory.dricats.configuration.defaults.dricats.systemwide.ReferenceProperties;
import net.fhirfactory.dricats.configuration.defaults.petasos.PetasosPropertyConstants;
import net.fhirfactory.dricats.interfaces.capabilities.CapabilityFulfillmentInterface;
import net.fhirfactory.dricats.interfaces.pathway.TaskPathwayManagementServiceInterface;
import net.fhirfactory.dricats.interfaces.petasos.participant.capabilities.ProcessingPlantRoleSupportInterface;
import net.fhirfactory.dricats.interfaces.petasos.participant.topology.ProcessingPlantConfigurationServiceInterface;
import net.fhirfactory.dricats.interfaces.petasos.tasking.audit.PetasosAuditEventGranularityLevelInterface;
import net.fhirfactory.dricats.internals.fhir.r4.internal.topics.FHIRElementTopicFactory;
import net.fhirfactory.dricats.model.capability.Capability;
import net.fhirfactory.dricats.model.configuration.filebased.archetypes.common.BaseSubsystemPropertyFile;
import net.fhirfactory.dricats.model.configuration.filebased.archetypes.common.WildflyBasedServerPropertyFile;
import net.fhirfactory.dricats.model.configuration.filebased.segments.datatypes.ParameterNameValuePairType;
import net.fhirfactory.dricats.model.petasos.audit.valuesets.PetasosAuditEventGranularityLevelEnum;
import net.fhirfactory.dricats.model.petasos.participant.components.common.PetasosParticipant;
import net.fhirfactory.dricats.model.petasos.participant.capabilities.PetasosParticipantRegistration;
import net.fhirfactory.dricats.model.petasos.participant.datatypes.PetasosParticipantId;
import net.fhirfactory.dricats.model.petasos.participant.valuesets.PetasosParticipantFulfillmentStatusEnum;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.work.TaskWorkItemManifestType;
import net.fhirfactory.dricats.model.petasos.tasking.fulfillment.replication.PetasosParticipantFulfillment;
import net.fhirfactory.dricats.model.petasos.tasking.routing.subscriptions.datatypes.DataDescriptorSubscriptionMask;
import net.fhirfactory.dricats.model.petasos.tasking.routing.subscriptions.datatypes.DataParcelBoundaryPointSubscriptionMaskType;
import net.fhirfactory.dricats.model.petasos.tasking.routing.subscriptions.datatypes.DataParcelManifestSubscriptionMask;
import net.fhirfactory.dricats.model.petasos.tasking.routing.subscriptions.datatypes.DataParcelQualitySubscriptionMaskType;
import net.fhirfactory.dricats.model.petasos.tasking.routing.subscriptions.valuesets.*;
import net.fhirfactory.dricats.petasos.observations.metrics.PetasosMetricAgentFactory;
import net.fhirfactory.dricats.petasos.observations.metrics.agents.ProcessingPlantMetricsAgent;
import net.fhirfactory.dricats.petasos.observations.metrics.agents.ProcessingPlantMetricsAgentAccessor;
import net.fhirfactory.dricats.petasos.participant.manager.LocalPetasosParticipantCacheIM;
import net.fhirfactory.dricats.petasos.participant.processingplant.physical.DeploymentSite;
import net.fhirfactory.dricats.petasos.participant.processingplant.physical.InfrastructureNode;
import net.fhirfactory.dricats.petasos.participant.processingplant.physical.PlatformNode;
import net.fhirfactory.dricats.petasos.participant.workshops.*;
import net.fhirfactory.dricats.petasos.tasking.oversight.watchdogs.GlobalPetasosTaskContinuityWatchdog;
import net.fhirfactory.dricats.petasos.tasking.oversight.watchdogs.GlobalPetasosTaskRecoveryWatchdog;
import net.fhirfactory.dricats.util.EnvironmentProperties;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class PetasosEnabledProcessingPlant extends ProcessingPlantBase implements ProcessingPlantRoleSupportInterface, ProcessingPlantConfigurationServiceInterface, PetasosAuditEventGranularityLevelInterface  {

    private PetasosParticipant petasosParticipantInformation;
    private boolean petasosEnabledProcessingPlantInitialised;
    private ProcessingPlantMetricsAgent metricsAgent;
    private PetasosAuditEventGranularityLevelEnum processingPlantAuditLevel;
    private ConcurrentHashMap<String, CapabilityFulfillmentInterface> capabilityDeliveryServices;

    //
    // The Workshops!

    private FrameworkSupportWorkshop frameworkSupportWorkshop;
    private EdgeWorkshop edgeWorkshop;
    private PolicyEnforcementWorkshop policyEnforcementWorkshop;
    private TransformWorkshop transformWorkshop;
    private InteractWorkshop interactWorkshop;

    //
    // Injected Resources

    @Inject
    private EdgeProcessingPlantConfigurationService processingPlantConfigurationService;

    @Inject
    private DeploymentSite deploymentSite;

    @Inject
    private InfrastructureNode infrastructureNode;

    @Inject
    private PlatformNode platformNode;

    @Inject
    private EnvironmentProperties environmentProperties;

    @Inject
    private GlobalPetasosTaskContinuityWatchdog petasosTaskContinuityWatchdog;

    @Inject
    private GlobalPetasosTaskRecoveryWatchdog petasosTaskRecoveryWatchdog;

    @Inject
    private PetasosEnabledProcessingPlantInformationAccessor processingPlantServicesAccessor;

    @Inject
    private FHIRElementTopicFactory fhirElementTopicFactory;

    @Inject
    private TaskPathwayManagementServiceInterface taskPathwayManagementService;

    @Inject
    private LocalPetasosParticipantCacheIM localPetasosParticipantCacheIM;

    @Inject
    private ReferenceProperties referenceProperties;

    @Inject
    private ProcessingPlantMetricsAgentAccessor metricsAgentAccessor;

    @Inject
    private PetasosMetricAgentFactory metricAgentFactory;
    

    //
    // Constructor(s)
    //

    public PetasosEnabledProcessingPlant() {
        super();
        this.capabilityDeliveryServices = new ConcurrentHashMap<>();
        this.petasosEnabledProcessingPlantInitialised = false;
        this.processingPlantAuditLevel = PetasosAuditEventGranularityLevelEnum.AUDIT_LEVEL_COARSE;
    }

    //
    // Abstract Methods
    //

    abstract protected WildflyBasedServerPropertyFile specifyPropertyFile();
    abstract protected Logger specifyLogger();
    abstract protected void executePostConstructActivities();

    abstract protected Set<DataParcelManifestSubscriptionMask> specifySubscriptionSet();
    abstract protected Set<Capability> specifyCapabilitySet();
    abstract protected Set<TaskWorkItemManifestType> specifyPublishedWorkItems();



    //
    // Post Construct
    //

    @Override
    protected void doPostConstructInitialisation(){
        petasosEnabledProcessingPlantInitialise();
    }

    public void petasosEnabledProcessingPlantInitialise() {
        getLogger().debug(".petasosEnabledProcessingPlantInitialise(): Entry");
        if (isPetasosEnabledProcessingPlantInitialised()) {
            getLogger().debug(".petasosEnabledProcessingPlantInitialise(): Already initialised, nothing to do");
        } else {
            getLogger().info("ProcessingPlant::initialise(): Initialising....");
            getLogger().info("ProcessingPlant::initialise(): [TopologyIM Initialisation] Start");
//            getLocalSolutionMap().initialise();
            getLogger().info("ProcessingPlant::initialise(): [TopologyIM Initialisation] Finish");

            getLogger().info("ProcessingPlant::initialise(): [Register ProcessingPlant] Start");
            processingPlantServicesAccessor.setProcessingPlant(this);
            getLogger().info("ProcessingPlant::initialise(): [Register processingPlant] Finish");

            getLogger().info("ProcessingPlant::initialise(): [ProcessingPlant Resolution] Start");
            establishedPetasosParticipantDetail();
            getLogger().info("ProcessingPlant::initialise(): [ProcessingPlant Resolution] getPetasosParticipantInformation()->{}", getPetasosParticipantInformation());
            getLogger().info("ProcessingPlant::initialise(): [ProcessingPlant Resolution] Finish");

            getLogger().info("ProcessingPlant::initialise(): [Capability Delivery Services Map Initialisation] Start");
            getLogger().info("ProcessingPlant::initialise(): [Executing other PostConstruct Activities] Start");
            executePostConstructActivities();
            getLogger().info("ProcessingPlant::initialise(): [Executing other PostConstruct Activities] Finish");

            getLogger().info("ProcessingPlant::initialise(): [Register for My PetasosParticipant] Start!");
            PetasosParticipantRegistration petasosParticipantRegistration = localPetasosParticipantCacheIM.registerPetasosParticipant(getPetasosParticipantInformation(), new HashSet<>(), new HashSet<>());
            getLogger().info("ProcessingPlant::initialise(): [Register for My PetasosParticipant] Finished!");

            getLogger().info("ProcessingPlant::initialise(): [Audit Event Level Derivation] Start");
            this.processingPlantAuditLevel = deriveAuditEventGranularityLevel();
            getLogger().info("ProcessingPlant::initialise(): [Audit Event Level Derivation] Finish");

            getLogger().info("ProcessingPlant::initialise(): [Initialise Task Continuity Watchdog] Start");
            this.petasosTaskContinuityWatchdog.initialise();
            getLogger().info("ProcessingPlant::initialise(): [Initialise Task Continuity Watchdog] Finish");

            getLogger().info("ProcessingPlant::initialise(): [Initialise Task Recovery Watchdog] Start");
            this.petasosTaskRecoveryWatchdog.initialise();
            getLogger().info("ProcessingPlant::initialise(): [Initialise Task Recovery Watchdog] Finish");

            getLogger().info("ProcessingPlant::initialise(): [Initialise Metrics Agent] Start");
            this.metricsAgent = metricAgentFactory.newProcessingPlantMetricsAgent(this, getComponentId(), getPetasosParticipantInformation().getParticipantId().getParticipantName());
            metricsAgentAccessor.setMetricsAgent(this.metricsAgent);
            getLogger().info("ProcessingPlant::initialise(): [Initialise Metrics Agent] Finish");

            setPetasosEnabledProcessingPlantInitialised(true);
            getLogger().info("StandardProcessingPlatform::initialise(): Done...");
        }
        getLogger().debug(".initialise(): Exit");
    }

    //
    // Overridden Business Methods
    //

    @Override
    public void initialisePlant() {
        establishedPetasosParticipantDetail();
    }

    //
    // Topology Detail
    //

    private void establishedPetasosParticipantDetail() {
        getLogger().debug(".establishedPetasosParticipantDetail(): Entry");

        //
        // Create
        PetasosParticipant petasosParticipant = new PetasosParticipant();

        //
        // Identification
        getLogger().debug(".establishedPetasosParticipantDetail(): [Establish ProcessingPlant Identity] Start");
        String processingPlantName = getPropertyFile().getSubsystemInstant().getSubsystemParticipantName();
        String processingPlantDisplayName = getPropertyFile().getSubsystemInstant().getSubsystemParticipantDisplayName();
        String processingPlantVersion = getPropertyFile().getSubsystemInstant().getSubsystemVersion();
        PetasosParticipantId participantId = new PetasosParticipantId();
        participantId.setParticipantName(processingPlantName);
        participantId.setSubsystemName(processingPlantName);
        participantId.setParticipantDisplayName(processingPlantDisplayName);
        participantId.setParticipantVersion(processingPlantVersion);
        petasosParticipant.setParticipantId(participantId);
        getLogger().debug(".establishedPetasosParticipantDetail(): [Establish ProcessingPlant Identity] Finish...");

        //
        // Participant Detail
        getLogger().debug(".establishedPetasosParticipantDetail(): [Establish ProcessingPlant PetasosDetail] Start");
        petasosParticipant.setSubscriptions(specifySubscriptionSet());
        petasosParticipant.setCapabilitySet(specifyCapabilitySet());
        petasosParticipant.setPublishedWorkItemManifests(specifyPublishedWorkItems());
        petasosParticipant.setFulfillmentState(new PetasosParticipantFulfillment());
        petasosParticipant.getFulfillmentState().setFulfillmentStatus(PetasosParticipantFulfillmentStatusEnum.PETASOS_PARTICIPANT_PARTIALLY_FULFILLED);
        petasosParticipant.getFulfillmentState().getFulfillerComponents().add(getComponentId());
        petasosParticipant.getFulfillmentState().setNumberOfActualFulfillers(1);
        petasosParticipant.getFulfillmentState().setNumberOfFulfillersExpected(getPropertyFile().getDeploymentMode().getProcessingPlantReplicationCount());
        getLogger().debug(".establishedPetasosParticipantDetail(): [Establish ProcessingPlant PetasosDetail] Finish");

        //
        // Assign it
        setPetasosParticipantInformation(petasosParticipant);

        //
        // Done
        getLogger().debug(".establishedPetasosParticipantDetail(): Exit, PetasosPlant->{}", getPetasosParticipantInformation());
    }

    //
    //
    //

    @Override
    public void registerCapabilityFulfillmentService(String capabilityName, CapabilityFulfillmentInterface fulfillmentInterface) {
        getLogger().debug(".registerCapabilityFulfillmentService(): Entry, capabilityName->{}", capabilityName);
        if(fulfillmentInterface == null){
            getLogger().debug(".registerCapabilityFulfillmentService(): Exit, Capability Fulfillment Interface is NULL");
            return;
        }
        this.capabilityDeliveryServices.put(capabilityName, fulfillmentInterface);
        getLogger().debug(".registerCapabilityFulfillmentService(): Exit, Capability Fulillment Interface registered");
    }

    @Override
    public PetasosAuditEventGranularityLevelEnum getAuditEventGranularityLevel() {
        return(this.processingPlantAuditLevel);
    }

    //
    // Get ProcessingPlant Audit Level
    //

    protected PetasosAuditEventGranularityLevelEnum deriveAuditEventGranularityLevel(){
        List<ParameterNameValuePairType> otherDeploymentParameters = getPropertyFile().getDeploymentMode().getSubsystemConfigurationParameters();
        if(otherDeploymentParameters != null){
            for(ParameterNameValuePairType currentNameValuePair: otherDeploymentParameters){
                if(currentNameValuePair.getParameterName().equalsIgnoreCase(PetasosPropertyConstants.AUDIT_LEVEL_PARAMETER_NAME)){
                    String parameterValue = currentNameValuePair.getParameterValue();
                    PetasosAuditEventGranularityLevelEnum petasosAuditEventGranularityLevelEnum = PetasosAuditEventGranularityLevelEnum.fromDisplayName(parameterValue);
                    if(petasosAuditEventGranularityLevelEnum == null){
                        getLogger().warn(".deriveAuditEventGranularityLevel(): Unable to derive PetasosAuditEventGranularityLevelEnum, setting to AUDIT_LEVEL_COARSE");
                        return(PetasosAuditEventGranularityLevelEnum.AUDIT_LEVEL_COARSE);
                    } else {
                        return (petasosAuditEventGranularityLevelEnum);
                    }
                }
            }
        }
        getLogger().warn(".deriveAuditEventGranularityLevel(): Unable to derive PetasosAuditEventGranularityLevelEnum, setting to AUDIT_LEVEL_COARSE");
        return (PetasosAuditEventGranularityLevelEnum.AUDIT_LEVEL_COARSE);
    }

    //
    // Remote Subscription Functions
    //

    protected Set<DataParcelManifestSubscriptionMask> specifyParticipantSubscriptions(List<DataDescriptorSubscriptionMask> triggerEventList, String sourceSystem){
        getLogger().info(".specifyParticipantSubscriptions(): Entry, sourceSystem->{}", sourceSystem);
        if(triggerEventList.isEmpty()){
            return(null);
        }
        getLogger().trace(".specifyParticipantSubscriptions(): We have entries in the subscription list, processing");
        Set<DataParcelManifestSubscriptionMask> subscriptionMaskTypeSet = new HashSet<>();
        for(DataDescriptorSubscriptionMask currentTriggerEvent: triggerEventList){
            getLogger().info(".specifyParticipantSubscriptions(): currentTriggerEvent->{}", currentTriggerEvent);
            DataParcelManifestSubscriptionMask parcelQuality = new DataParcelManifestSubscriptionMask();
            parcelQuality.setContentDescriptorMask(currentTriggerEvent);
            DataParcelQualitySubscriptionMaskType dataQualitySubscriptionMask = new DataParcelQualitySubscriptionMaskType();
            dataQualitySubscriptionMask.setContentNormalisationStatusMask(DataParcelNormalisationStatusSubscriptionMaskEnum.DATA_PARCEL_CONTENT_NORMALISATION_TRUE);
            dataQualitySubscriptionMask.setContentValidationStatusMask(DataParcelValidationStatusSubscriptionMaskEnum.DATA_PARCEL_CONTENT_VALIDATED_TRUE);
            dataQualitySubscriptionMask.setContentExternalDistributionStatusMask(DataParcelExternallyDistributableStatusSubscriptionMaskEnum.DATA_PARCEL_EXTERNALLY_DISTRIBUTABLE_FALSE);
            dataQualitySubscriptionMask.setInterSubsystemDistributableMask(DataParcelInternallyDistributableStatusSubscriptionMaskEnum.DATA_PARCEL_INTERNALLY_DISTRIBUTABLE_TRUE);
            parcelQuality.setContentQualityMask(dataQualitySubscriptionMask);
            parcelQuality.setDataParcelFlowDirectionMask(DataParcelDirectionSubscriptionMaskEnum.INFORMATION_FLOW_INBOUND_DATA_PARCEL);
            DataParcelBoundaryPointSubscriptionMaskType origin = new DataParcelBoundaryPointSubscriptionMaskType();
            origin.setBoundaryPointExternalSubsystemNameMask(sourceSystem);
            origin.setBoundaryPointProcessingPlantParticipantNameMask(new PetasosParticipantId(sourceSystem));
            parcelQuality.setOriginMask(origin);
            DataParcelBoundaryPointSubscriptionMaskType destination = new DataParcelBoundaryPointSubscriptionMaskType();
            destination.setAllowAll(true);
            parcelQuality.setDestinationMask(destination);
            subscriptionMaskTypeSet.add(parcelQuality);
        }
//        getLogger().info(".subscribeToRemoteDataParcels(): Registration Processing Plant Petasos Participant ... :)");
//        PetasosParticipantRegistration participantRegistration = getLocalPetasosParticipantCacheIM().registerPetasosParticipant(getTopologyNode(), new HashSet<>(), manifestList);
        getLogger().info(".specifyParticipantSubscriptions(): Exit, subscriptionMaskTypeSet->{}", subscriptionMaskTypeSet);
        return(subscriptionMaskTypeSet);
    }

    //
    // Getters (and Setters)
    //

    public FrameworkSupportWorkshop getOamWorkshop() {
        return frameworkSupportWorkshop;
    }

    public void setOamWorkshop(FrameworkSupportWorkshop frameworkSupportWorkshop) {
        this.frameworkSupportWorkshop = frameworkSupportWorkshop;
    }

    public EdgeWorkshop getEdgeWorkshop() {
        return edgeWorkshop;
    }

    public void setEdgeWorkshop(EdgeWorkshop edgeWorkshop) {
        this.edgeWorkshop = edgeWorkshop;
    }

    public PolicyEnforcementWorkshop getPolicyEnforcementWorkshop() {
        return policyEnforcementWorkshop;
    }

    public void setPolicyEnforcementWorkshop(PolicyEnforcementWorkshop policyEnforcementWorkshop) {
        this.policyEnforcementWorkshop = policyEnforcementWorkshop;
    }

    public TransformWorkshop getTransformWorkshop() {
        return transformWorkshop;
    }

    public void setTransformWorkshop(TransformWorkshop transformWorkshop) {
        this.transformWorkshop = transformWorkshop;
    }

    public InteractWorkshop getInteractWorkshop() {
        return interactWorkshop;
    }

    public void setInteractWorkshop(InteractWorkshop interactWorkshop) {
        this.interactWorkshop = interactWorkshop;
    }

    protected ReferenceProperties getReferenceProperties(){
        return(referenceProperties);
    }

    protected Logger getLogger(){
        return(specifyLogger());
    }

    protected BaseSubsystemPropertyFile getPropertyFile(){
        return(specifyPropertyFile());
    }

    protected TaskPathwayManagementServiceInterface getTaskPathwayManagementService(){
        return(taskPathwayManagementService);
    }

    protected LocalPetasosParticipantCacheIM getLocalPetasosParticipantCacheIM(){
        return(localPetasosParticipantCacheIM);
    }

    protected FHIRElementTopicFactory getFHIRElementTopicFactory(){
        return(fhirElementTopicFactory);
    }

    public PetasosAuditEventGranularityLevelEnum getProcessingPlantAuditLevel() {
        return processingPlantAuditLevel;
    }

    protected ProcessingPlantMetricsAgent getMetricsAgent(){
        return(metricsAgent);
    }

    @JsonIgnore
    protected DeploymentSite getDeploymentSite(){
        return(this.deploymentSite);
    }

    public PetasosParticipant getPetasosParticipantInformation() {
        return petasosParticipantInformation;
    }

    public void setPetasosParticipantInformation(PetasosParticipant petasosParticipantInformation) {
        this.petasosParticipantInformation = petasosParticipantInformation;
    }

    public InfrastructureNode getInfrastructureNode() {
        return infrastructureNode;
    }

    public PlatformNode getPlatformNode() {
        return platformNode;
    }

    public boolean isPetasosEnabledProcessingPlantInitialised() {
        return petasosEnabledProcessingPlantInitialised;
    }

    public void setPetasosEnabledProcessingPlantInitialised(boolean petasosEnabledProcessingPlantInitialised) {
        this.petasosEnabledProcessingPlantInitialised = petasosEnabledProcessingPlantInitialised;
    }
}
