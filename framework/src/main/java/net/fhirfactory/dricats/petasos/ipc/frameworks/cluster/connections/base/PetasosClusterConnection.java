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
package net.fhirfactory.dricats.petasos.ipc.frameworks.cluster.connections.base;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.jgroups.Address;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;

import net.fhirfactory.dricats.configuration.api.interfaces.PetasosConfigurationFileService;
import net.fhirfactory.dricats.interfaces.petasos.participant.capabilities.ProcessingPlantRoleSupportInterface;
import net.fhirfactory.dricats.model.component.valuesets.ComponentStatusEnum;
import net.fhirfactory.dricats.model.configuration.filebased.archetypes.valuesets.ClusterFunctionNameEnum;
import net.fhirfactory.dricats.model.petasos.ipc.endpoints.valuesets.ClusterServiceTypeEnum;
import net.fhirfactory.dricats.model.petasos.ipc.endpoints.valuesets.JGroupsChannelStatusEnum;
import net.fhirfactory.dricats.model.petasos.ipc.technologies.jgroups.JGroupsChannelConnectorSummary;
import net.fhirfactory.dricats.model.petasos.ipc.technologies.jgroups.JGroupsChannelProbeQuery;
import net.fhirfactory.dricats.model.petasos.ipc.technologies.jgroups.JGroupsChannelProbeReport;
import net.fhirfactory.dricats.model.petasos.participant.datatypes.PetasosParticipantId;
import net.fhirfactory.dricats.model.petasos.participant.valuesets.NetworkSecurityZoneEnum;
import net.fhirfactory.dricats.petasos.ipc.frameworks.cluster.management.interfaces.PetasosClusterConnectionRegistrationService;
import net.fhirfactory.dricats.petasos.ipc.frameworks.cluster.management.map.PetasosClusterMembershipCheckScheduleMap;
import net.fhirfactory.dricats.petasos.ipc.frameworks.cluster.management.map.PetasosClusterMembershipSharedMap;
import net.fhirfactory.dricats.petasos.ipc.frameworks.cluster.management.map.datatypes.PetasosClusterMembershipCheckScheduleElement;
import net.fhirfactory.dricats.petasos.ipc.technologies.datatypes.PetasosAdapterAddress;
import net.fhirfactory.dricats.petasos.ipc.technologies.jgroups.JGroupsClusterConnection;
import net.fhirfactory.dricats.petasos.ipc.technologies.jgroups.JGroupsClusterConnectionWatchdog;
import net.fhirfactory.dricats.petasos.observations.metrics.PetasosMetricAgentFactory;
import net.fhirfactory.dricats.petasos.observations.metrics.agents.EndpointMetricsAgent;
import net.fhirfactory.dricats.petasos.observations.metrics.agents.ProcessingPlantMetricsAgent;
import net.fhirfactory.dricats.petasos.observations.metrics.agents.ProcessingPlantMetricsAgentAccessor;
import net.fhirfactory.dricats.petasos.participant.processingplant.PetasosEnabledProcessingPlantInformationAccessor;

public abstract class PetasosClusterConnection extends JGroupsClusterConnection {

    private boolean endpointCheckScheduled;
    private PetasosParticipantId participantId;
    private PetasosClusterMembershipCheckScheduleMap integrationPointCheckScheduleMap;
    private EndpointMetricsAgent metricsAgent;

    @Inject
    private PetasosClusterMembershipSharedMap endpointMap;

    @Inject
    private JGroupsClusterConnectionWatchdog coreSubsystemPetasosEndpointsWatchdog;

    @Inject
    private PetasosClusterConnectionRegistrationService endpointRegistrationService;

    @Inject
    private PetasosMetricAgentFactory metricsFactory;

    @Inject
    private ProcessingPlantMetricsAgentAccessor processingPlantMetricsAgent;

    @Inject
    private ProcessingPlantRoleSupportInterface processingPlantCapabilityStatement;

    @Inject
    private PetasosConfigurationFileService petasosConfigurationFileService;
    
    @Inject
    private PetasosEnabledProcessingPlantInformationAccessor processingPlantInformationAccessor;

    //
    // Constructor
    //

    public PetasosClusterConnection(){
        super();
        endpointCheckScheduled = false;
        integrationPointCheckScheduleMap = new PetasosClusterMembershipCheckScheduleMap();
    }

    //
    // Abstract Methods
    //

    abstract protected void executePostConstructActivities();
    abstract protected void doIntegrationPointBusinessFunctionCheck(JGroupsChannelConnectorSummary integrationPointSummary, boolean isRemoved, boolean isAdded);

    //
    // PostConstruct Initialisation
    //

    @PostConstruct
    public void initialise() {
        getLogger().debug(".initialise(): Entry");
        if (isInitialised()) {
            getLogger().debug(".initialise(): Exit, already initialised!");
            return;
        }
        // Derive my Endpoint (Topology)
        getLogger().info(".initialise(): Step 1: [Resolve ChannelConnector] Start");
        populateMyParameters();
        getLogger().info(".initialise(): Step 1: [Resolve ChannelConnector] Finished, componentId ->{}", getComponentId());

        // Initialise Operational Status
        getLogger().info(".initialise(): Step 2: [Update Channel Status] Start");
        getLocalConnectionSetStatus().updateStatus(getChannelFunction(), getChannelStatus());
        getLogger().info(".initialise(): Step 2: [Update Channel Status] Finished");
        
        // Derive PetasosParticipantId
        getLogger().info(".initialise(): Step 2: [Update Channel Status] Start");
        buildPetasosParticipantId();
        getLogger().info(".initialise(): Step 2: [Update Channel Status] Finished");

        // Initialise my JChannel
        getLogger().info(".initialise(): Step 3: [Initialise my JChannel Connection & Join Cluster/Group] Start");
        establishJChannel();
        getLogger().info(".initialise(): Step 3: [Initialise my JChannel Connection & Join Cluster/Group] Finished, channel ->{}", getChannel());

        // Update Operational Status
        getLogger().info(".initialise(): Step 4: [Update my JGroupsIntegrationPoint status to OPERATIONAL] Start");
        setChannelStatus(JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_OPERATIONAL);
        getLocalConnectionSetStatus().updateStatus(getChannelFunction(), getChannelStatus());
        getLogger().info(".initialise(): Step 4: [Update my JGroupsIntegrationPoint status to OPERATIONAL] Finished");

        // Schedule an initial endpoint scan
        getLogger().info(".initialise(): Step 5: [Schedule a general IntegrationPoint scan] Start");
        scheduleEndpointScan();
        getLogger().info(".initialise(): Step 5: [Schedule a general IntegrationPoint scan] Finished");

        // Now kickstart the ongoing Endpoint Validation Process
        getLogger().info(".initialise(): Step 6: [Schedule general IntegrationPoint validation watchdog] Start");
        scheduleEndpointValidation();
        getLogger().info(".initialise(): Step 6: [Schedule general IntegrationPoint validation watchdog] Finished");

        // Call any subclass PostConstruct methods.
        getLogger().info(".initialise(): Step 7: [Executing subclass PostConstruct activities] Start");
        executePostConstructActivities();
        getLogger().info(".initialise(): Step 7: [Executing subclass PostConstruct activities] Finished");

        // Register myself with a WUP for Metrics Reporting
        getLogger().info(".initialise(): Step 9: [Registering with WUP for Metrics] Start");
        endpointRegistrationService.registerConnector(getChannelFunction(),this);
        getLogger().info(".initialise(): Step 9: [Registering with WUP for Metrics] Finished");

        // Create my Metrics Agent
        getLogger().info(".initialise(): Step 10: [Registering with WUP for Metrics] Start");
        this.metricsAgent = metricsFactory.newEndpointMetricsAgent(processingPlantCapabilityStatement,getComponentId(),getParticipantId().getParticipantName(), "Internal", getChannelName());
        getLogger().info(".initialise(): Step 10: [Registering with WUP for Metrics] Finished");
        // We're done!
        setInitialised(true);
    }

    //
    // Getters (and Setters)
    //

    protected EndpointMetricsAgent getMetricsAgent(){
        return(this.metricsAgent);
    }

    protected PetasosClusterMembershipSharedMap getIntegrationPointMap(){
        return(endpointMap);
    }

    protected JGroupsClusterConnectionWatchdog getCoreSubsystemPetasosEndpointsWatchdog(){
        return(coreSubsystemPetasosEndpointsWatchdog);
    }

    protected ProcessingPlantMetricsAgent getProcessingPlantMetricsAgent(){
        return(processingPlantMetricsAgent.getMetricsAgent());
    }

    protected PetasosConfigurationFileService getPetasosConfigurationFileService(){
        return(petasosConfigurationFileService);
    }
    
    protected PetasosParticipantId getParticipantId(){
        return(participantId);
    }

    protected PetasosEnabledProcessingPlantInformationAccessor getProcessingPlantInformationAccessor(){
        return(this.processingPlantInformationAccessor);
    }
    
    //
    // Build PetasosParticipantId
    //
    
    public void buildPetasosParticipantId() {
    	PetasosParticipantId participantId = new PetasosParticipantId();
    	participantId.setParticipantName(getChannelName());
    	participantId.setParticipantDisplayName(getClusterFunction().getParticipantName());
    	participantId.setParticipantVersion(getComponentId().getVersion());
    	participantId.setSubsystemName(resolveSubsystemName());
    	this.participantId = participantId;
    }

    //
    // JGroups Integration Point Probe (rpc and handler)
    //

    /**
     *
     * @param targetIntegrationPoint
     * @return
     */
    public JGroupsChannelProbeReport probeJGroupsIntegrationPoint(JGroupsChannelConnectorSummary targetIntegrationPoint){
        getLogger().debug(".probeJGroupsIntegrationPoint(): Entry, targetIntegrationPoint->{}", targetIntegrationPoint);
        JGroupsChannelProbeQuery query = createJGroupsIPQuery(toJGroupsChannelConnectorSummary());
        try {
            Object objectSet[] = new Object[1];
            Class classSet[] = new Class[1];
            objectSet[0] = query;
            classSet[0] = JGroupsChannelProbeQuery.class;
            RequestOptions requestOptions = new RequestOptions( ResponseMode.GET_FIRST, getRPCUnicastTimeout());
            Address endpointAddress = getTargetMemberAddress(targetIntegrationPoint.getChannelName());
            JGroupsChannelProbeReport report = null;
            synchronized(getChannelLock()) {
                report = getRPCDispatcher().callRemoteMethod(endpointAddress, "probeJGroupsIntegrationPointHandler", objectSet, classSet, requestOptions);
            }
            getLogger().debug(".probeJGroupsIntegrationPoint(): Exit, report->{}", report);
            return(report);
        } catch (NoSuchMethodException e) {
            getLogger().error(".probeJGroupsIntegrationPoint(): Error (NoSuchMethodException)->", e);
            return(null);
        } catch (Exception e) {
            getLogger().error(".probeJGroupsIntegrationPoint: Error (GeneralException) ->",e);
            return(null);
        }
    }

    /**
     *
     * @param sourceIntegrationPoint
     * @return
     */
    public JGroupsChannelProbeReport probeJGroupsIntegrationPointHandler(JGroupsChannelProbeQuery sourceIntegrationPoint){
        getLogger().debug(".probeJGroupsIntegrationPointHandler(): Entry, sourceIntegrationPoint->{}", sourceIntegrationPoint);
        getIntegrationPointMap().addJGroupsIntegrationPoint(sourceIntegrationPoint);
        JGroupsChannelProbeReport report = createJGroupsIPReport(this);
        getLogger().debug(".probeJGroupsIntegrationPointHandler(): Exit, report->{}", report);
        return(report);
    }

    //
    // Subsystem Name Derivation
    //

    protected String deriveIntegrationPointSubsystemName(String endpointName) {
        getLogger().debug(".deriveIntegrationPointSubsystemName(): Entry, endpointName->{}", endpointName);
        if(StringUtils.isEmpty(endpointName)){
            return(null);
        }
        String serviceName = getComponentNameUtilities().getProcessingPlantParticipantNameFromChannelName(endpointName);
        getLogger().debug(".deriveIntegrationPointSubsystemName(): Exit, serviceName->{}", serviceName);
        return(serviceName);
    }

    //
    // JGroups Integration Point Status Check
    //

    public ComponentStatusEnum checkProcessingPlantConnection(JGroupsChannelConnectorSummary targetProcessingPlant) {
        getLogger().debug(".checkProcessingPlantConnection(): Entry, targetJGroupsIP->{}", targetProcessingPlant);
        if(targetProcessingPlant == null){
            getLogger().debug(".checkProcessingPlantConnection(): Exit, targetJGroupsIP is null");
            return(ComponentStatusEnum.SOFTWARE_COMPONENT_FAILED);
        }
        if(StringUtils.isEmpty(targetProcessingPlant.getSubsystemParticipantName())){
            getLogger().debug(".checkProcessingPlantConnection(): Exit, targetJGroupsIP.getEndpointName() is empty");
            return(ComponentStatusEnum.SOFTWARE_COMPONENT_FAILED);
        }
        String targetSubsystemName = getComponentNameUtilities().getSubsystemNameFromEndpointName(targetProcessingPlant.getSubsystemParticipantName());
        String mySubsystemName = resolveSubsystemName();
        if(targetSubsystemName.contentEquals(mySubsystemName)){
            getLogger().debug(".checkProcessingPlantConnection(): Exit, Endpoint is one of mine!");
            return(processingPlantInformationAccessor.getProcessingPlant().getLivelinessStatus());
        }
        JGroupsChannelProbeReport report = probeJGroupsIntegrationPoint(targetProcessingPlant);
        ComponentStatusEnum endpointStatus = null;
        if(report != null){
            endpointStatus = report.getParticipantStatus();
        } else {
            endpointStatus = ComponentStatusEnum.SOFTWARE_COMPONENT_FAILED;
        }
        getLogger().debug(".checkProcessingPlantConnection(): Exit, Endpoint endpointStatus->{}", endpointStatus);
        return(endpointStatus);
    }



    //
    // JGroupsIntegrationPointSummary Helpers
    //

    public JGroupsChannelConnectorSummary buildFromChannelName(String channelName){
        getLogger().debug(".buildFromChannelName(): Entry, channelName->{}", channelName);
        JGroupsChannelConnectorSummary jgroupsIPSummary = new JGroupsChannelConnectorSummary();
        jgroupsIPSummary.setSubsystemParticipantName(getComponentNameUtilities().getProcessingPlantParticipantNameFromChannelName(channelName));
        jgroupsIPSummary.setChannelName(channelName);
        String functionName = getComponentNameUtilities().getEndpointFunctionFromChannelName(channelName);
        ClusterFunctionNameEnum functionEnum = ClusterFunctionNameEnum.fromGroupName(functionName);
        jgroupsIPSummary.setFunction(functionEnum);
        String site = getComponentNameUtilities().getEndpointSiteFromChannelName(channelName);
        jgroupsIPSummary.setSite(site);
        String endpointZoneName = getComponentNameUtilities().getEndpointZoneFromChannelName(channelName);
        NetworkSecurityZoneEnum networkSecurityZoneEnum = NetworkSecurityZoneEnum.fromSecurityZoneCamelCaseString(endpointZoneName);
        jgroupsIPSummary.setZone(networkSecurityZoneEnum);
        getLogger().debug(".buildFromChannelName(): Exit, jgroupsIPSummary->{}", jgroupsIPSummary);
        return(jgroupsIPSummary);
    }

    //
    // Query / Report Entity Creation Methods
    //

    protected JGroupsChannelConnectorSummary createSummary(){
        JGroupsChannelConnectorSummary summary = toJGroupsChannelConnectorSummary();
        if(getProcessingPlantInformationAccessor().getProcessingPlant() != null) {
            summary.setParticipantStatus(getProcessingPlantInformationAccessor().getProcessingPlant().getLivelinessStatus());
        }
        return(summary);
    }

    protected JGroupsChannelConnectorSummary toSummary(){
        return(createSummary());
    }

    protected JGroupsChannelProbeQuery createJGroupsIPQuery(JGroupsChannelConnectorSummary integrationPointSummary){
        JGroupsChannelProbeQuery query = new JGroupsChannelProbeQuery(integrationPointSummary);
        query.setParticipantStatus(getProcessingPlantInformationAccessor().getProcessingPlant().getLivelinessStatus());
        query.setUniqueIdQualifier(getComponentNameUtilities().getCurrentUUID());
        return(query);
    }

    protected JGroupsChannelProbeReport createJGroupsIPReport(JGroupsClusterConnection integrationPoint){
        JGroupsChannelConnectorSummary summary = integrationPoint.toJGroupsChannelConnectorSummary();
        summary.setParticipantStatus(getProcessingPlantInformationAccessor().getProcessingPlant().getLivelinessStatus());
        JGroupsChannelProbeReport report = new JGroupsChannelProbeReport(summary);
        return(report);
    }

    //
    // Endpoint (JGroupsIntegrationPoint) Lifecycle Watchdog Services
    //

    @Override
    public void processInterfaceSuspect(PetasosAdapterAddress suspectInterface) {

    }

    /**
     * This method parses the list of "interfaces" ADDED (exposed/visible) to a JChannel instance (i.e. visible within the
     * same JGroups cluster) and works out if a scan of the enpoint is (a) not another instance (different POD) of
     * this service and is implementing the same "function".
     *
     * Note, it has to check the "name" quality/validity/structure - as sometimes JGroups can pass some wacky values
     * to us...
     *
     * @param addedInterface
     */
    @Override
    public void processInterfaceAddition(PetasosAdapterAddress addedInterface){
        getLogger().info(".interfaceAdded(): Entry, addedInterface->{}", addedInterface);
        String endpointSubsystemName = getComponentNameUtilities().getSubsystemNameFromEndpointName(addedInterface.getAddressName());
        String endpointFunctionName = getComponentNameUtilities().getEndpointFunctionFromChannelName(addedInterface.getAddressName());
        if(StringUtils.isNotEmpty(endpointSubsystemName) && StringUtils.isNotEmpty(endpointFunctionName)) {
            boolean itIsAnotherInstanceOfMe = endpointSubsystemName.contentEquals(resolveSubsystemName());
            boolean itIsSameType = endpointFunctionName.contentEquals(ClusterServiceTypeEnum.PETASOS_TOPOLOGY_ENDPOINT.getDisplayName());
            if (!itIsAnotherInstanceOfMe && itIsSameType) {
                getLogger().debug(".interfaceAdded(): itIsAnotherInstanceOfMe && !itIsSameType");
                String endpointChannelName = addedInterface.getAddressName();
                JGroupsChannelConnectorSummary jgroupsIP = buildFromChannelName(endpointChannelName);
                integrationPointCheckScheduleMap.scheduleJGroupsIntegrationPointCheck(jgroupsIP, false, true);
                scheduleEndpointValidation();
            }
        }
        getLogger().debug(".interfaceAdded(): Exit");
    }

    /**
     * This method parses the list of "interfaces" REMOVED (exposed/visible) to a JChannel instance (i.e. visible within the
     * same JGroups cluster) and works out if a scan of the enpoint is (a) not another instance (different POD) of
     * this service and is implementing the same "function".
     *
     * Note, it has to check the "name" quality/validity/structure - as sometimes JGroups can pass some wacky values
     * to us...
     *
     * @param removedInterface
     */
    @Override
    public void processInterfaceRemoval(PetasosAdapterAddress removedInterface){
        getLogger().debug(".interfaceRemoved(): Entry, removedInterface->{}", removedInterface);
        String endpointSubsystemName = getComponentNameUtilities().getSubsystemNameFromEndpointName(removedInterface.getAddressName());
        String endpointFunctionName = getComponentNameUtilities().getEndpointFunctionFromChannelName(removedInterface.getAddressName());
        if(StringUtils.isNotEmpty(endpointSubsystemName) && StringUtils.isNotEmpty(endpointFunctionName)) {
            boolean itIsAnotherInstanceOfMe = endpointSubsystemName.contentEquals(resolveSubsystemName());
            boolean itIsSameType = endpointFunctionName.contentEquals(ClusterServiceTypeEnum.PETASOS_TOPOLOGY_ENDPOINT.getDisplayName());
            if (!itIsAnotherInstanceOfMe && itIsSameType) {
                getLogger().trace(".interfaceRemoved(): !itIsAnotherInstanceOfMe && itIsSameType");
                String endpointChannelName = removedInterface.getAddressName();
                JGroupsChannelConnectorSummary jgroupsIP = buildFromChannelName(endpointChannelName);
                integrationPointCheckScheduleMap.scheduleJGroupsIntegrationPointCheck(jgroupsIP, true, false);
                scheduleEndpointValidation();
            }
        }
        getLogger().debug(".interfaceRemoved(): Exit");
    }

    public void scheduleEndpointScan(){
        getLogger().debug(".scheduleEndpointScan(): Entry");
        List<Address> groupMembers = getAllGroupMembers();
        for(Address currentGroupMember: groupMembers){
            if(currentGroupMember.toString().contains(ClusterFunctionNameEnum.PETASOS_TOPOLOGY_SERVICES.getGroupName())) {
                String channelName = currentGroupMember.toString();
                JGroupsChannelConnectorSummary jgroupsIP = buildFromChannelName(channelName);
                integrationPointCheckScheduleMap.scheduleJGroupsIntegrationPointCheck(jgroupsIP, false, true);
                getLogger().trace(".scheduleEndpointScan(): Added ->{} to scan", jgroupsIP);
            }
        }
        getLogger().debug(".scheduleEndpointScan(): Exit");
    }

    public void scheduleEndpointValidation() {
        getLogger().debug(".scheduleEndpointValidation(): Entry (isEndpointCheckScheduled->{})", endpointCheckScheduled);
        if (endpointCheckScheduled) {
            // do nothing, it is already scheduled
        } else {
            TimerTask endpointValidationTask = new TimerTask() {
                public void run() {
                    getLogger().debug(".endpointValidationTask(): Entry");
                    boolean doAgain = performEndpointValidationCheck();
                    getLogger().debug(".endpointValidationTask(): doAgain ->{}", doAgain);
                    if (!doAgain) {
                        cancel();
                        endpointCheckScheduled = false;
                    }
                    getLogger().debug(".endpointValidationTask(): Exit");
                }
            };
            String timerName = "EndpointValidationWatchdogTask";
            Timer timer = new Timer(timerName);
            long startupDelay = getPetasosConfigurationFileService().getPropertyFile().getDefaultPetasosIPCWatchdogStartupDelay() * 1000L;
            long checkPeriod = getPetasosConfigurationFileService().getPropertyFile().getDefaultPetasosIPCWatchdogCheckPeriod() * 1000L;
            timer.schedule(endpointValidationTask,startupDelay, checkPeriod);
            endpointCheckScheduled = true;
        }
        getLogger().debug(".scheduleEndpointValidation(): Exit");
    }

    /**
     * This method retrieves the list of "JGroupsIntegrationPoints" to be "Probed" from the IntegrationPointMap.EndpointsToCheck
     * (ConcurrentHashMap) and attempts to retrieve their JGroupsIntegrationPointSummary instance.
     *
     * It then uses this JGroupsIntegrationPointSummary instance (returnedEndpointFromTarget) to update the IntegrationPointMap with
     * the current details (from the source, so to speak).
     *
     * It keeps a list of endpoints that it couldn't check and re-schedules their validation check.
     *
     * It also checks the Subsystem-to-IntegrationPoint map and ensures this aligns with the information provided.
     *
     * It then checks to see if there is a need to do another check/validation iteration and returns the result.
     *
     * @return True if another validation is required, false otherwise.
     */
    public boolean performEndpointValidationCheck(){
        getLogger().debug(".performEndpointValidationCheck(): Entry");
        List<PetasosClusterMembershipCheckScheduleElement> endpointsToCheck = integrationPointCheckScheduleMap.getEndpointsToCheck();
        List<PetasosClusterMembershipCheckScheduleElement> redoList = new ArrayList<>();
        getLogger().trace(".performEndpointValidationCheck(): Iterate through...");
        for(PetasosClusterMembershipCheckScheduleElement currentScheduleElement: endpointsToCheck) {
            getLogger().trace(".performEndpointValidationCheck(): currentScheduleElement->{}", currentScheduleElement);
            if(currentScheduleElement.isEndpointAdded()) {
                boolean wasProcessed = checkEndpointAddition(currentScheduleElement);
                if(wasProcessed) {
                    getLogger().trace(".performEndpointValidationCheck(): item was processed!");
                } else {
                    getLogger().trace(".performEndpointValidationCheck(): item was NOT processed, adding to redo list");
                    redoList.add(currentScheduleElement);
                }
            }
            if(currentScheduleElement.isEndpointRemoved()){
                checkEndpointRemoval(currentScheduleElement);
            }
        }
        for(PetasosClusterMembershipCheckScheduleElement redoItem: redoList){
            getLogger().trace(".performEndpointValidationCheck(): Re-Adding to schedule the redoItem->{}", redoItem);
            integrationPointCheckScheduleMap.scheduleJGroupsIntegrationPointCheck(redoItem.getJgroupsIPSummary(), false, true);
        }
        if(integrationPointCheckScheduleMap.isCheckScheduleIsEmpty()){
            getLogger().debug(".performEndpointValidationCheck(): Exit, perform again->false");
            return(false);
        } else {
            if(getLogger().isTraceEnabled()){
                for(PetasosClusterMembershipCheckScheduleElement currentScheduledElement: integrationPointCheckScheduleMap.getEndpointsToCheck()){
                    getLogger().trace(".performEndpointValidationCheck(): Will Check Endpoint->{}", currentScheduledElement.getJgroupsIPSummary().getChannelName());
                }
            }
            getLogger().debug(".performEndpointValidationCheck(): Exit, perform again->true");
            return(true);
        }
    }

    /**
     * This method checks (using the supplied the provided PetasosEndpointCheckScheduleElement) the JGroupsIntegrationPoint
     * and ascertains its operational state. Depending on the status, it either schedules a follow-up check or marks
     * the JGroupsIntegrationPoint as Operational or Failed...
     *
     * @param currentScheduleElement
     * @return
     */
    protected boolean checkEndpointAddition(PetasosClusterMembershipCheckScheduleElement currentScheduleElement){
        getLogger().debug(".checkEndpointAddition(): Entry, currentScheduleElement->{}", currentScheduleElement);
        String subsystemParticipantName = currentScheduleElement.getJgroupsIPSummary().getSubsystemParticipantName();
        getLogger().trace(".checkEndpointAddition(): check to see if scheduled element is another instance of me! my subsystemParticipantName->{},", resolveSubsystemName());
        if(subsystemParticipantName.equalsIgnoreCase(resolveSubsystemName())){
            getLogger().debug(".checkEndpointAddition(): Endpoint is for same subsystem as me, do nothing! ");
            return(true);
        }
        String endpointChannelName = currentScheduleElement.getJgroupsIPSummary().getChannelName();
        JGroupsChannelConnectorSummary synchronisedJGroupsIP = synchroniseEndpointCache(currentScheduleElement);
        if(synchronisedJGroupsIP != null){
            switch(synchronisedJGroupsIP.getIntegrationPointStatus()){
                case JGROUPS_CHANNEL_LOCAL_STATUS_OPERATIONAL:{
                    getIntegrationPointMap().updateParticipantIntegrationPointMembership(synchronisedJGroupsIP.getSubsystemParticipantName(), currentScheduleElement.getJgroupsIPSummary().getChannelName());
                    doIntegrationPointBusinessFunctionCheck(synchronisedJGroupsIP, false, true);
                    getLogger().debug(".checkEndpointAddition(): Does not need re-checking, returning -true- (was processed)");
                    return(true);
                }
                case JGROUPS_CHANNEL_REMOTE_STATUS_SUSPECT:
                case JGROUPS_CHANNEL_REMOTE_STATUS_REACHABLE:
                case JGROUPS_CHANNEL_REMOTE_STATUS_DETECTED:
                case JGROUPS_CHANNEL_LOCAL_STATUS_STARTED:{
                    getLogger().debug(".checkEndpointAddition(): Needs re-checking, returning -false- (wasn't completely processed)");
                    return (false);
                }
                case JGROUPS_CHANNEL_REMOTE_STATUS_SAME:
                case JGROUPS_CHANNEL_REMOTE_STATUS_UNREACHABLE:
                case JGROUPS_CHANNEL_LOCAL_STATUS_FAILED:
                default:{
                    doIntegrationPointBusinessFunctionCheck(synchronisedJGroupsIP, true, false);
                    getLogger().debug(".checkEndpointAddition(): We've rescheduled the removal of this endpoint returning -true- (was processed)");
                    return (true);
                }
            }
        }
        getLogger().debug(".checkEndpointAddition(): there is nothing to check, so returning->true (was processed)");
        return (true);
    }

    protected void checkEndpointRemoval(PetasosClusterMembershipCheckScheduleElement currentScheduleElement){
        getLogger().debug(".checkEndpointRemoval(): Entry, currentScheduleElement->{}", currentScheduleElement);
        String subsystemParticipantName = currentScheduleElement.getJgroupsIPSummary().getSubsystemParticipantName();
        if(subsystemParticipantName.equalsIgnoreCase(resolveSubsystemName())){
            getLogger().debug(".checkEndpointRemoval(): Endpoint is for same subsystem as me, do nothing! ");
            return;
        }
        getIntegrationPointMap().deleteSubsystemIntegrationPoint(currentScheduleElement.getJgroupsIPSummary().getChannelName());
        getLogger().debug(".checkEndpointRemoval(): Endpoint removed");
    }

    private JGroupsChannelConnectorSummary synchroniseEndpointCache(PetasosClusterMembershipCheckScheduleElement currentScheduleElement){
        getLogger().debug(".synchroniseEndpointCache: Entry, currentScheduleElement->{}", currentScheduleElement);
        if(currentScheduleElement == null){
            getLogger().debug(".synchroniseEndpointCache: Exit, currentScheduleElement is null");
            return(null);
        }
        String subsystemParticipantName = currentScheduleElement.getJgroupsIPSummary().getSubsystemParticipantName();
        if(subsystemParticipantName.equalsIgnoreCase(resolveSubsystemName())){
            getLogger().debug(".synchroniseEndpointCache(): Endpoint is for same subsystem as me, do nothing! ");
            return(null);
        }
        String channelName = currentScheduleElement.getJgroupsIPSummary().getChannelName();
        getLogger().trace(".synchroniseEndpointCache: Checking to see if endpoint is already in EndpointMap");
        JGroupsChannelConnectorSummary cachedEndpoint = getIntegrationPointMap().getJGroupsIntegrationPointSummary(channelName);
        getLogger().trace(".synchroniseEndpointCache: Retrieved PetasosEndpoint->{}", cachedEndpoint);
        boolean doProbe = true;
        boolean isToBeRemoved = false;
        if(cachedEndpoint != null){
            switch(cachedEndpoint.getIntegrationPointStatus()) {
                case JGROUPS_CHANNEL_REMOTE_STATUS_SUSPECT:
                case JGROUPS_CHANNEL_REMOTE_STATUS_REACHABLE:
                case JGROUPS_CHANNEL_LOCAL_STATUS_STARTED:
                case JGROUPS_CHANNEL_REMOTE_STATUS_SAME:
                case JGROUPS_CHANNEL_REMOTE_STATUS_DETECTED:{
                    getLogger().trace(".synchroniseEndpointCache: Endpoint is ok, but not operational, going to have to Probe it!!");
                    doProbe = true;
                    break;
                }
                case JGROUPS_CHANNEL_LOCAL_STATUS_OPERATIONAL:
                {
                    getLogger().debug(".synchroniseEndpointCache(): Endpoint is operational, do nothing! ");
                    return(cachedEndpoint);
                }
                case JGROUPS_CHANNEL_REMOTE_STATUS_UNREACHABLE:
                case JGROUPS_CHANNEL_LOCAL_STATUS_FAILED:
                case JGROUPS_CHANNEL_LOCAL_STATUS_UNKNOWN:
                default:{
                    getLogger().trace(".synchroniseEndpointCache(): Endpoint is in a poor state, remove it from our cache! ");
                    doProbe = false;
                    isToBeRemoved = true;
                }
            }
        }
        if(doProbe) {
            getLogger().debug(".synchroniseEndpointCache(): [Performing Endpoint Probe] Start");
            if (isTargetAddressActive(currentScheduleElement.getJgroupsIPSummary().getChannelName())) {
                getLogger().trace(".synchroniseEndpointCache(): Probing (or attempting to Probe) the Endpoint");
                JGroupsChannelProbeReport report = probeJGroupsIntegrationPoint(currentScheduleElement.getJgroupsIPSummary());
                getLogger().trace(".synchroniseEndpointCache(): report->{}", report);
                if (report != null) {
                    getLogger().trace(".synchroniseEndpointCache(): Probe succeded, so let's synchronise/update local cache");
                    if (cachedEndpoint == null) {
                        cachedEndpoint = report;
                        getLogger().trace(".synchroniseEndpointCache(): addedPetasosEndpoint->{}", cachedEndpoint);
                        if (!StringUtils.isEmpty(report.getSubsystemParticipantName())) {
                            getIntegrationPointMap().updateParticipantIntegrationPointMembership(report.getSubsystemParticipantName(), currentScheduleElement.getJgroupsIPSummary().getSubsystemParticipantName());
                        }
                    } else {
                        cachedEndpoint.setParticipantStatus(report.getParticipantStatus());
                        cachedEndpoint.setLastRefreshInstant(report.getReportInstant());
                    }
                    getLogger().debug(".synchroniseEndpointCache(): [Performing Endpoint Probe] Finish");
                    getLogger().debug(".synchroniseEndpointCache(): Exit, Endpoint Probed and local Endpoint Registry updated");
                    return(cachedEndpoint);
                } else {
                    getLogger().trace(".synchroniseEndpointCache(): Probe failed, we should consider removing it!");
                    isToBeRemoved = true;
                }
            } else {
                getLogger().trace(".synchroniseEndpointCache(): Couldn't even find the endpoint, we should consider removing it!");
                isToBeRemoved = true;
            }
            getLogger().debug(".synchroniseEndpointCache(): [Performing Endpoint Probe] Finish");
        }
        if(isToBeRemoved){
            getLogger().trace(".synchroniseEndpointCache(): We should remove the Endpoint from our Cache and ToDo schedule!");
            int maxRetryCount = getPetasosConfigurationFileService().getPropertyFile().getDefaultPeerFailureRetries();
            int retryCountSoFar = currentScheduleElement.getRetryCount();
            if(retryCountSoFar > maxRetryCount){
                getLogger().trace(".synchroniseEndpointCache(): we've tried to probe endpoint MAX_PROBE_RETRIES ({}) times and failed, so delete it", maxRetryCount);
                integrationPointCheckScheduleMap.scheduleJGroupsIntegrationPointCheck(currentScheduleElement.getJgroupsIPSummary(), true, false);
            } else {
                getLogger().trace(".synchroniseEndpointCache(): probe has failed ({}) times, but we will try again", retryCountSoFar);
                retryCountSoFar += 1;
                integrationPointCheckScheduleMap.scheduleJGroupsIntegrationPointCheck(currentScheduleElement.getJgroupsIPSummary(), false, true, retryCountSoFar);
            }
            getLogger().debug(".synchroniseEndpointCache(): Could not find Endpoint, have rescheduled endpoint check");
            return(null);
        }
        getLogger().debug(".synchroniseEndpointCache(): Exit");
        return(cachedEndpoint);
    }
    


}
