/*
 * Copyright (c) 2020 MAHun
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
package net.fhirfactory.dricats.petasos.tasking.caches.shared;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.fhirfactory.pegacorn.core.model.componentid.ComponentIdType;
import net.fhirfactory.pegacorn.core.model.petasos.task.datatypes.identity.datatypes.TaskIdType;
import net.fhirfactory.pegacorn.core.model.petasos.wup.PetasosTaskJobCard;
import net.fhirfactory.pegacorn.core.model.petasos.wup.valuesets.PetasosTaskExecutionStatusEnum;
import net.fhirfactory.pegacorn.core.model.topology.mode.ConcurrencyModeEnum;
import net.fhirfactory.pegacorn.core.model.topology.mode.ResilienceModeEnum;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class PetasosTaskJobCardSharedInstance {
    private static final Logger LOG = LoggerFactory.getLogger(PetasosTaskJobCardSharedInstance.class);

    PetasosTaskJobCard localJobCard;

    ParticipantSharedTaskJobCardCache jobCardCache;

    //
    // Constructor(s)
    //

    public PetasosTaskJobCardSharedInstance(TaskIdType actionableTaskId, ParticipantSharedTaskJobCardCache cache){
        this.jobCardCache = cache;
        PetasosTaskJobCard jobCard = getJobCardCache().getJobCard(actionableTaskId);
        if(jobCard == null){
            getLogger().warn(".PetasosTaskJobCardSharedInstance(): No Jobcard for provided actionableTaskId->{}", actionableTaskId);
            PetasosTaskJobCard newJobCard = new PetasosTaskJobCard();
            newJobCard.setActionableTaskId(actionableTaskId);
            newJobCard.setLastActivityCheckInstant(Instant.now());
            getJobCardCache().registerJobCard(newJobCard);
            this.localJobCard = newJobCard;
        } else {
            this.localJobCard = jobCard;
        }
    }

    public PetasosTaskJobCardSharedInstance(PetasosTaskJobCard jobCard, ParticipantSharedTaskJobCardCache cache){
        this.jobCardCache = cache;
        if(jobCard != null) {
            PetasosTaskJobCard cachedJobCard = null;
            synchronized (getJobCardCache().getCacheLock()){
                cachedJobCard = getJobCardCache().getJobCard(jobCard.getActionableTaskId());
            }
            if(cachedJobCard != null){
                this.localJobCard = SerializationUtils.clone(cachedJobCard);
            } else {
                getJobCardCache().registerJobCard(jobCard);
                this.localJobCard = SerializationUtils.clone(jobCard);
            }
        }
    }


    //
    // Business Methods
    //

    @JsonIgnore
    public boolean hasExecutingFulfillmentTaskId(){
        boolean hasValue = getLocalJobCard().hasExecutingFulfillmentTaskId();
        return(hasValue);
    }

    public TaskIdType getExecutingFulfillmentTaskId() {
        return getLocalJobCard().getExecutingFulfillmentTaskId();
    }

    public void setExecutingFulfillmentTaskId(TaskIdType fulfillmentTaskIdentifier) {
        getLocalJobCard().setExecutingFulfillmentTaskId(fulfillmentTaskIdentifier);
    }

    @JsonIgnore
    public boolean hasActionableTaskId(){
        boolean hasValue = getLocalJobCard().hasActionableTaskId();
        return(hasValue);
    }

    public TaskIdType getActionableTaskId() {
        return (getLocalJobCard().getActionableTaskId());
    }

    public void setActionableTaskId(TaskIdType actionableTaskIdentifier) {
        getLocalJobCard().setActionableTaskId(actionableTaskIdentifier);
    }

    @JsonIgnore
    public boolean hasExecutingFulfillmentTaskIdAssignmentInstant(){
        boolean hasValue = getLocalJobCard().hasExecutingFulfillmentTaskIdAssignmentInstant();
        return(hasValue);
    }

    public Instant getExecutingFulfillmentTaskIdAssignmentInstant() {
        return (getLocalJobCard().getExecutingFulfillmentTaskIdAssignmentInstant());
    }

    public void setExecutingFulfillmentTaskIdAssignmentInstant(Instant localUpdateInstant) {
        getLocalJobCard().setExecutingFulfillmentTaskIdAssignmentInstant(localUpdateInstant);
    }

    @JsonIgnore
    public boolean hasLastActivityCheckInstant(){
        boolean hasValue = getLocalJobCard().hasLastActivityCheckInstant();
        return(hasValue);
    }

    public Instant getLastActivityCheckInstant() {
        return(getLocalJobCard().getLastActivityCheckInstant());
    }

    public void setLastActivityCheckInstant(Instant coordinatorUpdateInstant) {
        getLocalJobCard().setLastActivityCheckInstant(coordinatorUpdateInstant);
    }

    @JsonIgnore
    public boolean hasCurrentStatus(){
        boolean hasValue = getLocalJobCard().hasCurrentStatus();
        return(hasValue);
    }

    public PetasosTaskExecutionStatusEnum getCurrentStatus() {
        return(getLocalJobCard().getCurrentStatus());
    }

    public void setCurrentStatus(PetasosTaskExecutionStatusEnum currentStatus) {
        getLocalJobCard().setCurrentStatus(currentStatus);
    }

    @JsonIgnore
    public boolean hasLastRequestedStatus(){
        boolean hasValue = getLocalJobCard().hasLastRequestedStatus();
        return(hasValue);
    }

    public PetasosTaskExecutionStatusEnum getLastRequestedStatus() {
        return(getLocalJobCard().getLastRequestedStatus());
    }

    public void setLastRequestedStatus(PetasosTaskExecutionStatusEnum requestedStatus) {
        getLocalJobCard().setLastRequestedStatus(requestedStatus);
    }

    @JsonIgnore
    public boolean hasGrantedStatus(){
        boolean hasValue = getLocalJobCard().hasGrantedStatus();
        return(hasValue);
    }

    public PetasosTaskExecutionStatusEnum getGrantedStatus() {
        return(getLocalJobCard().getGrantedStatus());
    }

    public void setGrantedStatus(PetasosTaskExecutionStatusEnum grantedStatus) {
        getLocalJobCard().setGrantedStatus(grantedStatus);
    }

    @JsonIgnore
    public boolean hasClusterMode(){
        boolean hasValue = getLocalJobCard().hasClusterMode();
        return(hasValue);
    }

    public ConcurrencyModeEnum getClusterMode() {
        return (getLocalJobCard().getClusterMode());
    }

    public void setClusterMode(ConcurrencyModeEnum clusterMode) {
        getLocalJobCard().setClusterMode(clusterMode);
    }

    @JsonIgnore
    public boolean hasSystemMode(){
        boolean hasValue = getLocalJobCard().hasSystemMode();
        return(hasValue);
    }

    public ResilienceModeEnum getSystemMode() {
        return (getLocalJobCard().getSystemMode());
    }

    public void setSystemMode(ResilienceModeEnum systemMode) {
        getLocalJobCard().setSystemMode(systemMode);
    }

    @JsonIgnore
    public boolean hasExecutingProcessingPlant(){
        boolean hasValue = getLocalJobCard().hasExecutingProcessingPlant();
        return(hasValue);
    }

    public ComponentIdType getExecutingProcessingPlant() {
        return (getLocalJobCard().getExecutingProcessingPlant());
    }

    public void setExecutingProcessingPlant(ComponentIdType processingPlant) {
        getLocalJobCard().setExecutingProcessingPlant(processingPlant);
    }

    @JsonIgnore
    public boolean hasExecutingWorkUnitProcessor(){
        boolean hasValue = getLocalJobCard().hasExecutingWorkUnitProcessor();
        return(hasValue);
    }

    public ComponentIdType getExecutingWorkUnitProcessor() {
        return (getLocalJobCard().getExecutingWorkUnitProcessor());
    }

    public void setExecutingWorkUnitProcessor(ComponentIdType workUnitProcessor) {
        getLocalJobCard().setExecutingWorkUnitProcessor(workUnitProcessor);
    }

    @JsonIgnore
    boolean hasActionableTaskAffinityNode(){
        boolean hasValue = getLocalJobCard().hasActionableTaskAffinityNode();
        return(hasValue);
    }

    public ComponentIdType getActionableTaskAffinityNode() {
        return (getLocalJobCard().getActionableTaskAffinityNode());
    }

    public void setActionableTaskAffinityNode(ComponentIdType actionableTaskAffinityNode) {
        getLocalJobCard().setActionableTaskAffinityNode(actionableTaskAffinityNode);
    }

    @JsonIgnore
    boolean hasProcessingPlantParticipantName(){
        boolean hasValue = getLocalJobCard().hasProcessingPlantParticipantName();
        return(hasValue);
    }

    public String getProcessingPlantParticipantName() {
        return (getLocalJobCard().getProcessingPlantParticipantName());
    }

    public void setProcessingPlantParticipantName(String processingPlantParticipantName) {
        getLocalJobCard().setProcessingPlantParticipantName(processingPlantParticipantName);
    }

    //
    // getLock()
    //

    public Object getLock(){
        TaskIdType actionableTaskId = getActionableTaskId();
        Object jobCackLockObject = getJobCardCache().getJobCardLock(actionableTaskId);
        return(jobCackLockObject);
    }

    //
    // The "Synchronisation Methods"
    //

    public void refresh(){
        TaskIdType actionableTaskId = getActionableTaskId();
        synchronized (getJobCardCache().getJobCardLock(actionableTaskId)){
            this.localJobCard = SerializationUtils.clone(getJobCardCache().getJobCard(actionableTaskId));
        }
    }

    public void update(){
        TaskIdType taskId = getLocalJobCard().getActionableTaskId();
        PetasosTaskJobCard cachedJobCard = getJobCardCache().getJobCard(taskId);
        cachedJobCard.update(getLocalJobCard());
        this.localJobCard = SerializationUtils.clone(cachedJobCard);
    }

    public void lockAndUpdate(){
        TaskIdType actionableTaskId = getActionableTaskId();
        synchronized (getJobCardCache().getJobCardLock(actionableTaskId)){
            update();
        }
    }

    //
    // Getters and Setters
    //

    protected ParticipantSharedTaskJobCardCache getJobCardCache(){
        return(this.jobCardCache);
    }

    protected Logger getLogger(){
        return(this.LOG);
    }

    protected PetasosTaskJobCard getLocalJobCard(){
        return(this.localJobCard);
    }

    //
    // To String
    //

    @Override
    public String toString() {
        return "PetasosTaskJobCardSharedInstance{" +
                "executingFulfillmentTaskId=" + getExecutingFulfillmentTaskId() +
                ", actionableTaskId=" + getActionableTaskId() +
                ", executingFulfillmentTaskIdAssignmentInstant=" + getExecutingFulfillmentTaskIdAssignmentInstant() +
                ", lastActivityCheckInstant=" + getLastActivityCheckInstant() +
                ", currentStatus=" + getCurrentStatus() +
                ", lastRequestedStatus=" + getLastRequestedStatus() +
                ", grantedStatus=" + getGrantedStatus() +
                ", clusterMode=" + getClusterMode() +
                ", systemMode=" + getSystemMode() +
                ", executingProcessingPlant=" + getExecutingProcessingPlant() +
                ", executingWorkUnitProcessor=" + getExecutingWorkUnitProcessor() +
                ", actionableTaskAffinityNode=" + getActionableTaskAffinityNode() +
                ", processingPlantParticipantName='" + getProcessingPlantParticipantName() + '\'' +
                '}';
    }
}
