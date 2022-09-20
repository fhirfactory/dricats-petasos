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
package net.fhirfactory.dricats.petasos.ipc.frameworks.cluster.connections;

import net.fhirfactory.dricats.interfaces.interception.PetasosInterceptionAgentServicesInterface;
import net.fhirfactory.dricats.interfaces.interception.PetasosInterceptionServerInterface;
import net.fhirfactory.dricats.model.configuration.filebased.archetypes.valuesets.ClusterFunctionNameEnum;
import net.fhirfactory.dricats.model.petasos.ipc.technologies.jgroups.JGroupsChannelConnectorSummary;
import net.fhirfactory.dricats.model.petasos.tasking.definition.PetasosFulfillmentTask;
import net.fhirfactory.dricats.petasos.ipc.frameworks.cluster.connections.base.PetasosClusterConnection;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.lang3.StringUtils;
import org.jgroups.Address;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

public abstract class PetasosInterceptionClusterConnection extends PetasosClusterConnection implements PetasosInterceptionAgentServicesInterface {
    private static final Logger LOG = LoggerFactory.getLogger(PetasosInterceptionClusterConnection.class);

    @Produce
    private ProducerTemplate camelProducer;

    @Inject
    PetasosInterceptionServerInterface interceptionHandler;

    //
    // Constructor(s)
    //

    public PetasosInterceptionClusterConnection(){
        super();
    }

    //
    // PostConstruct Activities
    //

    @Override
    protected void executePostConstructActivities() {

    }

    //
    // Getters (and Setters)
    //

    @Override
    protected Logger getLogger() {
        return (LOG);
    }

    protected ProducerTemplate getCamelProducer(){
        return(camelProducer);
    }

    @Override
    protected ClusterFunctionNameEnum getClusterFunction() {
        return (ClusterFunctionNameEnum.PETASOS_INTERCEPTION_SERVICES);
    }

    //
    // Processing Plant check triggered by JGroups Cluster membership change
    //

    @Override
    protected void doIntegrationPointBusinessFunctionCheck(JGroupsChannelConnectorSummary integrationPointSummary, boolean isRemoved, boolean isAdded) {

    }

    //
    // Business Methods
    //

    public Address getInterceptionCollectorTargetAddress(String endpointServiceName){
        getLogger().debug(".getInterceptionCollectorTargetAddress(): Entry, endpointServiceName->{}", endpointServiceName);
        if(StringUtils.isEmpty(endpointServiceName)){
            getLogger().debug(".getInterceptionCollectorTargetAddress(): Exit, endpointServiceName is empty");
            return(null);
        }
        List<Address> endpointAddressSet = getGroupMembersForProcessingPlantParticipantName(endpointServiceName);
        if(endpointAddressSet.isEmpty()){
            getLogger().debug(".getInterceptionCollectorTargetAddress(): Exit, endpointAddressSet is empty");
            return(null);
        }
        Address endpointJGroupsAddress = endpointAddressSet.get(0);
        getLogger().debug(".getInterceptionCollectorTargetAddress(): Exit, selected address->{}", endpointJGroupsAddress);
        return(endpointJGroupsAddress);
    }


    //
    // Interception Methods
    //

    @Override
    public PetasosFulfillmentTask redirectFulfillmentTask(String collectorServiceName, PetasosFulfillmentTask task){
        getLogger().trace(".redirectFulfillmentTask(): Entry, collectorServiceName->{}, event->{}", collectorServiceName, task);
        JGroupsChannelConnectorSummary myIP = createSummary();
        Address targetAddress = getInterceptionCollectorTargetAddress(collectorServiceName);
        if(targetAddress == null){
            getLogger().error(".redirectFulfillmentTask(): Cannot find collectServiceName candidate->{}", collectorServiceName);
            return(null);
        }
        try {
            Object objectSet[] = new Object[2];
            Class classSet[] = new Class[2];
            objectSet[0] = task;
            classSet[0] = PetasosFulfillmentTask.class;
            objectSet[1] = myIP;
            classSet[1] = JGroupsChannelConnectorSummary.class;
            RequestOptions requestOptions = new RequestOptions( ResponseMode.GET_FIRST, getRPCUnicastTimeout());
            PetasosFulfillmentTask redirectedTaskOutcome = null;
            synchronized (getChannelLock()) {
                redirectedTaskOutcome = getRPCDispatcher().callRemoteMethod(targetAddress, "redirectFulfillmentTaskHandler", objectSet, classSet, requestOptions);
            }
            getMetricsAgent().incrementRemoteProcedureCallCount();
            getLogger().debug(".redirectFulfillmentTask(): Exit, redirectedTask->{}", redirectedTaskOutcome);
            return(redirectedTaskOutcome);
        } catch (NoSuchMethodException e) {
            getMetricsAgent().incrementRemoteProcedureCallFailureCount();
            getLogger().error(".redirectFulfillmentTask(): Error (NoSuchMethodException) ->{}", e.getMessage());
            return(null);
        } catch (Exception e) {
            getMetricsAgent().incrementRemoteProcedureCallFailureCount();
            e.printStackTrace();
            getLogger().error(".redirectFulfillmentTask: Error (GeneralException) ->{}", e.getMessage());
            return(null);
        }
    }

    public PetasosFulfillmentTask redirectFulfillmentTaskHandler(PetasosFulfillmentTask task, JGroupsChannelConnectorSummary endpointIdentifier){
        getLogger().trace(".redirectFulfillmentTaskHandler(): Entry, task->{}, endpointIdentifier->{}", task, endpointIdentifier);
        PetasosFulfillmentTask redirectedTaskOutcome = null;
        if((task != null) && (endpointIdentifier != null)) {
            redirectedTaskOutcome = interceptionHandler.redirectFulfillmentTask(task, endpointIdentifier);
        }
        getMetricsAgent().incrementRemoteProcedureCallHandledCount();
        getLogger().debug(".redirectFulfillmentTaskHandler(): Exit, redirectedTaskOutcome->{}", redirectedTaskOutcome);
        return(redirectedTaskOutcome);
    }
}
