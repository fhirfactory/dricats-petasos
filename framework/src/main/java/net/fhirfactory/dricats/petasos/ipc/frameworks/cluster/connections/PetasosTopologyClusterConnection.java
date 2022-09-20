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

import net.fhirfactory.dricats.interfaces.petasos.participant.topology.ProcessingPlantConfigurationServiceInterface;
import net.fhirfactory.dricats.model.configuration.filebased.archetypes.valuesets.ClusterFunctionNameEnum;
import net.fhirfactory.dricats.model.petasos.ipc.technologies.jgroups.JGroupsChannelConnectorSummary;
import net.fhirfactory.dricats.model.simplified.resources.summaries.isc.ISCProcessingPlantSummary;
import net.fhirfactory.dricats.petasos.ipc.frameworks.cluster.connections.base.PetasosClusterConnection;
import net.fhirfactory.dricats.petasos.observations.configuration.topology.cache.PetasosDistributedTopologyNodeDM;
import net.fhirfactory.dricats.petasos.observations.configuration.topology.cache.PetasosLocalTopologySnapshot;
import net.fhirfactory.dricats.petasos.observations.configuration.topology.factories.PetasosProcessingPlantSummaryFactory;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jgroups.Address;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;

import javax.inject.Inject;


public abstract class PetasosTopologyClusterConnection extends PetasosClusterConnection {

    private boolean topologySynchronisationDaemonInitialised;

    @Inject
    private PetasosLocalTopologySnapshot topologyReportingDM;

    @Inject
    private ProcessingPlantConfigurationServiceInterface processingPlant;

    @Inject
    private PetasosDistributedTopologyNodeDM topologyNodeDM;

    @Inject
    private PetasosProcessingPlantSummaryFactory processingPlantSummaryFactory;

    //
    // Constructor
    //

    public PetasosTopologyClusterConnection(){
        super();
        topologySynchronisationDaemonInitialised = false;
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

    protected PetasosLocalTopologySnapshot getTopologyReportingDM(){
        return(topologyReportingDM);
    }


    //
    // Endpoint Definition
    //

    @Override
    protected ClusterFunctionNameEnum getClusterFunction() {
        return (ClusterFunctionNameEnum.PETASOS_TOPOLOGY_SERVICES);
    }

    //
    // Asynchronous Update of Topology triggered by changes in the JGroups Cluster Membership
    //

    @Override
    protected void doIntegrationPointBusinessFunctionCheck(JGroupsChannelConnectorSummary integrationPoint, boolean isRemoved, boolean isAdded) {
        getLogger().debug(".doIntegrationPointBusinessFunctionCheck(): Entry, integrationPoint->{}, isRemoved->{}, isAdded->{}", integrationPoint, isRemoved, isAdded);
        if(integrationPoint == null){
            getLogger().debug(".doIntegrationPointBusinessFunctionCheck(): integrationPoint is null, exiting!");
            return;
        }
        if(isRemoved){
            getLogger().trace(".doIntegrationPointBusinessFunctionCheck(): Is a -remove- activity");
            topologyNodeDM.removeDiscoveredProcessingPlant(integrationPoint);
            getLogger().debug(".doIntegrationPointBusinessFunctionCheck(): Exit, finished removing associated software components from cache!");
            return;
        }
        if(isAdded){
            getLogger().trace(".doIntegrationPointBusinessFunctionCheck(): Is an -add- activity");
            ISCProcessingPlantSummary processingPlant = probeProcessingPlantTopologyDetail(integrationPoint);
            if(processingPlant != null){
                 topologyNodeDM.addProcessingPlant(integrationPoint, processingPlant);
            }
            getLogger().debug(".doIntegrationPointBusinessFunctionCheck(): Exit, finished adding associated software components into cache!");
            return;
        }
        getLogger().debug(".doIntegrationPointBusinessFunctionCheck(): Exit, nothing to do!");
    }

    //
    // Topology (Detailed) Information Collection
    //

    protected ISCProcessingPlantSummary probeProcessingPlantTopologyDetail(JGroupsChannelConnectorSummary targetChannelConnector){
        getLogger().debug(".probeEndpointTopologyDetail(): Entry, targetChannelConnector->{}", targetChannelConnector);
        try {
            Object objectSet[] = new Object[1];
            Class classSet[] = new Class[1];
            objectSet[0] = targetChannelConnector;
            classSet[0] = JGroupsChannelConnectorSummary.class;
            RequestOptions requestOptions = new RequestOptions( ResponseMode.GET_FIRST, getRPCUnicastTimeout());
            Address endpointAddress = getTargetMemberAddress(targetChannelConnector.getChannelName());
            ISCProcessingPlantSummary node = null;
            synchronized (getChannelLock()) {
                node = getRPCDispatcher().callRemoteMethod(endpointAddress, "probeProcessingPlantTopologyDetailHandler", objectSet, classSet, requestOptions);
            }
            getMetricsAgent().incrementRemoteProcedureCallCount();
            getLogger().debug(".probeEndpointTopologyDetail(): Exit, response->{}", node);
            return(node);
        } catch (NoSuchMethodException e) {
            getMetricsAgent().incrementRemoteProcedureCallFailureCount();
            getLogger().error(".probeEndpointTopologyDetail(): Error (NoSuchMethodException) message->{}, stacktrace->{}", ExceptionUtils.getMessage(e), ExceptionUtils.getStackTrace(e));
            return(null);
        } catch (Exception e) {
            getMetricsAgent().incrementRemoteProcedureCallFailureCount();
            getLogger().error(".probeEndpointTopologyDetail: Error (GeneralException) message->{}, stacktrace->{}", ExceptionUtils.getMessage(e), ExceptionUtils.getStackTrace(e));
            return(null);
        }
    }

    public ISCProcessingPlantSummary probeProcessingPlantTopologyDetailHandler(JGroupsChannelConnectorSummary sourceChannelConnector) {
        getLogger().debug(".probeEndpointTopologyDetailHandler(): Entry, sourceChannelConnector->{}", sourceChannelConnector);
        ISCProcessingPlantSummary myProcessingPlantSummary = getTopologyReportingDM().getCurrentState();
        getMetricsAgent().incrementRemoteProcedureCallHandledCount();
        return(myProcessingPlantSummary);
    }

}
