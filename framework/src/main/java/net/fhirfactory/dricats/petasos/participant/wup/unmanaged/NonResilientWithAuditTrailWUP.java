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
package net.fhirfactory.dricats.petasos.participant.wup.unmanaged;

import ca.uhn.fhir.parser.IParser;
import net.fhirfactory.dricats.interfaces.topology.PegacornTopologyFactoryInterface;
import net.fhirfactory.dricats.interfaces.topology.ProcessingPlantInterface;
import net.fhirfactory.pegacorn.core.model.componentid.PegacornSystemComponentTypeTypeEnum;
import net.fhirfactory.pegacorn.core.model.componentid.TopologyNodeFunctionFDN;
import net.fhirfactory.pegacorn.core.model.componentid.TopologyNodeFunctionFDNToken;
import net.fhirfactory.dricats.model.petasos.dataparcel.DataParcelManifest;
import net.fhirfactory.dricats.model.petasos.dataparcel.DataParcelTypeDescriptor;
import net.fhirfactory.dricats.model.petasos.dataparcel.valuesets.DataParcelDirectionEnum;
import net.fhirfactory.dricats.model.petasos.dataparcel.valuesets.DataParcelTypeEnum;
import net.fhirfactory.dricats.model.petasos.resilience.activitymatrix.sta.TransactionStatusElement;
import net.fhirfactory.dricats.model.petasos.task.PetasosActionableTask;
import net.fhirfactory.dricats.model.petasos.task.PetasosFulfillmentTask;
import net.fhirfactory.dricats.model.petasos.task.datatypes.work.datatypes.ParcelOfWorkType;
import net.fhirfactory.dricats.model.petasos.uow.UoW;
import net.fhirfactory.dricats.model.petasos.uow.UoWPayload;
import net.fhirfactory.pegacorn.core.model.topology.nodes.WorkUnitProcessorSoftwareComponent;
import net.fhirfactory.dricats.model.transaction.valuesets.PegacornTransactionTypeEnum;
import net.fhirfactory.pegacorn.deployment.topology.manager.TopologyIM;
import net.fhirfactory.dricats.internals.fhir.r4.internal.topics.FHIRElementTopicFactory;
import net.fhirfactory.dricats.internals.fhir.r4.resources.identifier.PegacornIdentifierDataTypeHelpers;
import net.fhirfactory.pegacorn.petasos.tasking.factories.PetasosActionableTaskFactory;
import net.fhirfactory.pegacorn.petasos.tasking.factories.PetasosFulfillmentTaskFactory;
import net.fhirfactory.pegacorn.petasos.tasking.factories.PetasosTaskJobCardFactory;
import net.fhirfactory.pegacorn.petasos.tasking.oversight.LocalPetasosActionableTaskActivityController;
import net.fhirfactory.pegacorn.petasos.tasking.fulfilment.LocalPetasosFulfilmentTaskActivityController;
import net.fhirfactory.dricats.petasos.participant.workshops.base.UnmonitoredWorkshop;
import net.fhirfactory.dricats.petasos.participant.wup.unmanaged.audit.TransactionalWUPAuditEntryManager;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public abstract class NonResilientWithAuditTrailWUP extends RouteBuilder {

    private WorkUnitProcessorSoftwareComponent topologyNode;
    private PetasosFulfillmentTask currentFulfillmentTask;
    private boolean isInitialised;
    private IParser parserR4;

    @Inject
    private TopologyIM topologyIM;

    @Inject
    private FHIRElementTopicFactory topicIDBuilder;

    @Inject
    private TransactionalWUPAuditEntryManager auditEntryManager;

    @Inject
    private ProcessingPlantInterface processingPlant;

    @Inject
    private net.fhirfactory.pegacorn.util.FHIRContextUtility FHIRContextUtility;

    @Inject
    private PetasosTaskJobCardFactory jobCardFactory;

    @Inject
    private PegacornIdentifierDataTypeHelpers identifierHelpers;

    @Inject
    private PetasosActionableTaskFactory actionableTaskFactory;

    @Inject
    private PetasosFulfillmentTaskFactory fulfillmentTaskFactory;

    @Inject
    private LocalPetasosActionableTaskActivityController actionableTaskActivityController;

    @Inject
    private LocalPetasosFulfilmentTaskActivityController fulfilmentTaskActivityController;

    //
    // Constructor(s)
    //

    public NonResilientWithAuditTrailWUP(){
        super();
        this.isInitialised = false;
    }

    //
    // Abstract Methods
    //

    abstract protected Logger getLogger();
    abstract protected String specifyWUPInstanceName();
    abstract protected String specifyWUPInstanceVersion();
    abstract protected UnmonitoredWorkshop specifyWorkshop();
    abstract protected void executePostConstructActivities();

    //
    // Post Construct
    //

    @PostConstruct
    protected void initialise() {
        getLogger().debug(".initialise(): Entry");
        if (!isInitialised) {
            getLogger().trace(".initialise(): AccessBase is NOT initialised");
            this.parserR4 = FHIRContextUtility.getJsonParser();
            this.isInitialised = true;
            processingPlant.initialisePlant();
            buildWUPNodeElement();
            executePostConstructActivities();
        }
    }

    private void buildWUPNodeElement(){
        getLogger().debug(".buildWUPNodeElement(): Entry");
        String participantName = getWorkshop().getWorkshopPetasosParticipant().getParticipantName() + "." + specifyWUPInstanceName();
        WorkUnitProcessorSoftwareComponent wupNode = getTopologyFactory()
                .createWorkUnitProcessor(specifyWUPInstanceName(),specifyWUPInstanceVersion(), participantName, getWorkshop().getWorkshopPetasosParticipant(), DricatsSoftwareComponentTypeEnum.WUP);
        getTopologyIM().addTopologyNode(specifyWorkshop().getWorkshopPetasosParticipant().getComponentFDN(), wupNode);
        wupNode.setResilienceMode(specifyWorkshop().getWorkshopPetasosParticipant().getResilienceMode());
        wupNode.setConcurrencyMode(specifyWorkshop().getWorkshopPetasosParticipant().getConcurrencyMode());
        this.topologyNode = wupNode;
    }

    //
    // Getters and Setters
    //


    public PetasosFulfillmentTask getCurrentFulfillmentTask() {
        return currentFulfillmentTask;
    }

    public void setCurrentFulfillmentTask(PetasosFulfillmentTask currentFulfillmentTask) {
        this.currentFulfillmentTask = currentFulfillmentTask;
    }

    public TopologyNodeFunctionFDN getNodeFunctionFDN(){
        return(topologyNode.getNodeFunctionFDN());
    }

    public TopologyNodeFunctionFDNToken getNodeFunctionFDNToken(){
        return(topologyNode.getNodeFunctionFDN().getFunctionToken());
    }

    public WorkUnitProcessorSoftwareComponent getTopologyNode() {
        return topologyNode;
    }

    public boolean isInitialised() {
        return isInitialised;
    }

    public IParser getParserR4() {
        return parserR4;
    }

    public FHIRElementTopicFactory getTopicIDBuilder() {
        return topicIDBuilder;
    }

    public net.fhirfactory.pegacorn.util.FHIRContextUtility getFHIRContextUtility() {
        return FHIRContextUtility;
    }

    public ProcessingPlantInterface getProcessingPlant() {
        return processingPlant;
    }

    protected TransactionalWUPAuditEntryManager getAuditEntryManager(){
        return(auditEntryManager);
    }

    protected TopologyIM getTopologyIM(){
        return(topologyIM);
    }

    protected PegacornTopologyFactoryInterface getTopologyFactory(){
        return(processingPlant.getTopologyFactory());
    }

    protected UnmonitoredWorkshop getWorkshop(){
        return(specifyWorkshop());
    }

    protected String getWUPInstanceVersion(){
        return(specifyWUPInstanceVersion());
    }

    protected PetasosTaskJobCardFactory getJobCardFactory() {
        return jobCardFactory;
    }

    protected PegacornIdentifierDataTypeHelpers getIdentifierHelpers() {
        return identifierHelpers;
    }

    protected PetasosActionableTaskFactory getActionableTaskFactory() {
        return actionableTaskFactory;
    }

    protected PetasosFulfillmentTaskFactory getFulfillmentTaskFactory() {
        return fulfillmentTaskFactory;
    }

    protected LocalPetasosActionableTaskActivityController getActionableTaskActivityController() {
        return actionableTaskActivityController;
    }

    protected LocalPetasosFulfilmentTaskActivityController getFulfilmentTaskActivityController() {
        return fulfilmentTaskActivityController;
    }

    //
    // Business Methods
    //


    protected void initialiseActivity(String resourceType, boolean resilientActivity){
        getLogger().debug(".initialiseActivity(): Entry, resourceType->{}, resilientActivity->{}", resourceType, resilientActivity);
        //
        // Build a UoW for the activity
        DataParcelTypeDescriptor dataParcelTypeDescriptor = topicIDBuilder.newTopicToken(resourceType);
        UoWPayload payload = new UoWPayload();
        DataParcelManifest payloadManifest = new DataParcelManifest();
        payloadManifest.setContentDescriptor(dataParcelTypeDescriptor);
        payloadManifest.setDataParcelFlowDirection(DataParcelDirectionEnum.INFORMATION_FLOW_API_ACTIVITY_REQUEST);
        payload.setPayload("Query");
        payload.setPayloadManifest(payloadManifest);
        TaskWorkItem workItem = new TaskWorkItem(payload);
        //
        // Build and register a Petasos Actionable Task
        PetasosActionableTask petasosActionableTask = getActionableTaskFactory().newMessageBasedActionableTask(workItem);
        getActionableTaskActivityController().registerActionableTask(petasosActionableTask);
        //
        // Build and register a Petasos Fulfillment Task
        PetasosFulfillmentTask fulfillmentTask = getFulfillmentTaskFactory().newFulfillmentTask(petasosActionableTask, getTopologyNode());
        getFulfilmentTaskActivityController().registerFulfillmentTask(fulfillmentTask, false);
        //
        // Now assign our "current fulfillment task"
        setCurrentFulfillmentTask(fulfillmentTask);
    }

    protected void initialiseActivity(UoW uow, boolean resilientActivity){
        getLogger().debug(".initialiseActivity(): Entry, uow->{}, resilientActivity->{}", uow, resilientActivity);
        //
        // Build a UoW for the activity
        TaskWorkItem workItem = new TaskWorkItem(uow.getIngresContent());
        //
        // Build and register a Petasos Actionable Task
        PetasosActionableTask petasosActionableTask = getActionableTaskFactory().newMessageBasedActionableTask(workItem);
        getActionableTaskActivityController().registerActionableTask(petasosActionableTask);
        //
        // Build and register a Petasos Fulfillment Task
        PetasosFulfillmentTask fulfillmentTask = getFulfillmentTaskFactory().newFulfillmentTask(petasosActionableTask, getTopologyNode());
        getFulfilmentTaskActivityController().registerFulfillmentTask(fulfillmentTask, false);
        //
        // Now assign our "current fulfillment task"
        setCurrentFulfillmentTask(fulfillmentTask);
    }

    protected void finaliseWUPActivity(){
        setCurrentFulfillmentTask(null);
    }

    /**
     *
     * @param resourceID
     * @param resourceType
     * @param resourceIdentifier
     * @param fhirResource
     * @param action
     * @return
     */
    protected TransactionStatusElement beginRESTfulTransaction(IdType resourceID, String resourceType, Identifier resourceIdentifier, Resource fhirResource, PegacornTransactionTypeEnum action){
        getLogger().debug(".beginRESTfulTransaction(): Entry, resourceID->{}, resourceType->{}, resourceIdentifier->{}, fhirResource->{}, actopm->{}", resourceID, resourceType, resourceIdentifier, fhirResource, action);
        String resourceKey = identifierHelpers.generatePrintableInformationFromIdentifier(resourceIdentifier);
        initialiseActivity(resourceType, false);
        TransactionStatusElement transactionStatus = auditEntryManager.beginTransaction(getCurrentFulfillmentTask().getTaskJobCard(), resourceKey, fhirResource.fhirType(), fhirResource, action );
        return(transactionStatus);
    }

    /**
     *
     * @param uow
     * @param action
     * @return
     */
    protected TransactionStatusElement beginAPITransaction(UoW uow, PegacornTransactionTypeEnum action){
        if(uow != null) {
            if(StringUtils.isNotEmpty(uow.getIngresContent().getPayload())) {
                initialiseActivity(uow, false);
                TransactionStatusElement transactionStatus = auditEntryManager.beginTransaction(getCurrentFulfillmentTask().getTaskJobCard(), uow, action);
                return (transactionStatus);
            }
        }
        return(null);
    }

    /**
     *
     * @param transaction
     */
    protected void endRESTfulTransaction(TransactionStatusElement transaction){
        auditEntryManager.endTransaction(transaction);
        finaliseWUPActivity();
    }

    /**
     *
     * @param transaction
     */
    protected void endAPITransaction(TransactionStatusElement transaction){
        auditEntryManager.endTransaction(transaction);
        finaliseWUPActivity();
    }


    protected TransactionStatusElement beginSearchTransaction(String resourceType, String resourceVersion, Map<Property, Serializable> parameterSet){
        initialiseActivity(resourceType, false);
        Set<Property> parameterKeys = parameterSet.keySet();
        String searchParameters = new String();
        int totalCount = parameterKeys.size();
        int counter = 0;
        for(Property currentKey: parameterKeys){
            String parameterValue = parameterSet.get(currentKey).toString();
            searchParameters = searchParameters + currentKey+"="+parameterValue;
            if(counter < (totalCount -1)){
                searchParameters = searchParameters+"&";
            }
        }
        UoWPayload newPayload = new UoWPayload();
        newPayload.setPayload("Resource="+resourceType+"?search=");
        DataParcelTypeDescriptor typeDescriptor = topicIDBuilder.newTopicToken(resourceType, resourceVersion);
        DataParcelManifest parcelManifest = new DataParcelManifest();
        parcelManifest.setContentDescriptor(typeDescriptor);
        parcelManifest.setDataParcelType(DataParcelTypeEnum.SEARCH_QUERY_DATA_PARCEL_TYPE);
        newPayload.setPayloadManifest(parcelManifest);
        UoW uow = new UoW(newPayload);
        TransactionStatusElement transactionStatus = auditEntryManager.beginTransaction(getCurrentFulfillmentTask().getTaskJobCard(),uow, PegacornTransactionTypeEnum.SEARCH);
        return(transactionStatus);
    }

    protected void endSearchTransaction(Bundle resultSet, TransactionStatusElement startingTransaction){
        String searchAnswerSummary = buildSearchResultString(resultSet);
        UoWPayload payload = new UoWPayload();
        payload.setPayload(searchAnswerSummary);
        DataParcelManifest parcelManifest = startingTransaction.getUnitOfWork().getPayloadTopicID();
        parcelManifest.setDataParcelType(DataParcelTypeEnum.SEARCH_RESULT_DATA_PARCEL_TYPE);
        payload.setPayloadManifest(parcelManifest);
        startingTransaction.getUnitOfWork().getEgressContent().addPayloadElement(payload);
//        auditEntryManager.endTransaction(searchAnswerCount, resourceType , null,action,success,startingTransaction,getNodeInstanceID(),getWUPInstanceVersion());
    }

    //
    // Helpers
    //

    private String buildSearchResultString(Bundle searchResult){
        if(searchResult == null) {
            return("Search Failed");
        }
        int resultCount = searchResult.getTotal();
        if(resultCount == 0){
            return("Search Succeeded: Result Count = 0");
        }
        String resultString = "Search Succeeded: Result Count = " + resultCount + ": Entries --> ";
        for(Bundle.BundleEntryComponent currentBundleEntry: searchResult.getEntry()){
            Resource currentResource = currentBundleEntry.getResource();
            if(currentResource.hasId()){
                resultString = resultString + currentResource.getId();
            } else {
                resultString = resultString + "[Resource Has No Id]";
            }
            if(resultCount > 1) {
                resultString = resultString + ", ";
            }
            resultCount -= 1;
        }
        return(resultString);
    }

    public void setTopologyNode(WorkUnitProcessorSoftwareComponent topologyNode) {
        this.topologyNode = topologyNode;
    }
}
