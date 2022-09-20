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
package net.fhirfactory.dricats.petasos.ipc.services.audit;

import net.fhirfactory.dricats.model.configuration.filebased.archetypes.valuesets.ClusterFunctionNameEnum;
import net.fhirfactory.dricats.petasos.ipc.frameworks.cluster.connections.PetasosAuditClusterConnection;

import org.hl7.fhir.r4.model.AuditEvent;
import org.jgroups.Address;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import net.fhirfactory.dricats.interfaces.observations.topology.PetasosTopologyReportingServiceProviderNameInterface;
import net.fhirfactory.dricats.interfaces.petasos.ipc.services.agents.audit.PetasosAuditEventAgentProxyInterface;
import net.fhirfactory.dricats.model.petasos.ipc.technologies.jgroups.JGroupsChannelConnectorSummary;
import net.fhirfactory.dricats.model.transaction.model.PegacornTransactionMethodOutcome;

@ApplicationScoped
public class PetasosAuditServicesAgent extends PetasosAuditClusterConnection implements PetasosAuditEventAgentProxyInterface {
    private static final Logger LOG = LoggerFactory.getLogger(PetasosAuditServicesAgent.class);
    
    

    @Inject
    private PetasosTopologyReportingServiceProviderNameInterface topologyReportingProvider;

    //
    // Constructor(s)
    //

    public PetasosAuditServicesAgent(){
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

    @Override
    public ClusterFunctionNameEnum getChannelFunction() {
        return (ClusterFunctionNameEnum.PETASOS_AUDIT_SERVICES);
    }

    //
    // Post Construct (invoked from Superclass)
    //

    @Override
    protected void executePostConstructInstanceActivities() {

    }

    //
    // Metrics (Client) RPC Method Support
    //

    @Override
    public Boolean logAuditEvent(String serviceProviderName, AuditEvent event){
        getLogger().info(".logAuditEvent(): Entry, serviceProviderName->{}, event->{}", serviceProviderName, event);
        JGroupsChannelConnectorSummary myJGroupsIP = createSummary();
        Address targetAddress = getTargetMemberAddress(serviceProviderName);
        try {
            Object objectSet[] = new Object[2];
            Class classSet[] = new Class[2];
            objectSet[0] = event;
            classSet[0] = AuditEvent.class;
            objectSet[1] = myJGroupsIP;
            classSet[1] = JGroupsChannelConnectorSummary.class;
            RequestOptions requestOptions = new RequestOptions( ResponseMode.GET_FIRST, getRPCUnicastTimeout());
            PegacornTransactionMethodOutcome response = null;
            synchronized (getChannelLock()){
                response = getRPCDispatcher().callRemoteMethod(targetAddress, "logAuditEventHandler", objectSet, classSet, requestOptions);
            }
            getMetricsAgent().incrementRemoteProcedureCallCount();
            Boolean created = response.getCreated();
            getLogger().info(".logAuditEvent(): Exit, response->{}", response);
            return(created);
        } catch (NoSuchMethodException e) {
            getMetricsAgent().incrementRemoteProcedureCallFailureCount();
            getLogger().error(".logAuditEvent(): Error (NoSuchMethodException) ->{}", e.getMessage());
            return(null);
        } catch (Exception e) {
            getMetricsAgent().incrementRemoteProcedureCallFailureCount();
            e.printStackTrace();
            getLogger().error(".logAuditEvent: Error (GeneralException) ->{}", e.getMessage());
            return(null);
        }
    }

    @Override
    public Boolean logAuditEvent(String serviceProviderName, List<AuditEvent> eventList){
        getLogger().trace(".logAuditEvent(): Entry, serviceProviderName->{}, eventList->{}", serviceProviderName, eventList);
        JGroupsChannelConnectorSummary jgroupsIP = createSummary();
        Address targetAddress = getTargetMemberAddress(serviceProviderName);
        try {
            Object objectSet[] = new Object[2];
            Class classSet[] = new Class[2];
            objectSet[0] = eventList;
            classSet[0] = List.class;
            objectSet[1] = jgroupsIP;
            classSet[1] = JGroupsChannelConnectorSummary.class;
            RequestOptions requestOptions = new RequestOptions( ResponseMode.GET_FIRST, getRPCUnicastTimeout());
            PegacornTransactionMethodOutcome response = null;
            synchronized (getChannelLock()){
                response = getRPCDispatcher().callRemoteMethod(targetAddress, "logMultipleAuditEventHandler", objectSet, classSet, requestOptions);
            }
            getMetricsAgent().incrementRemoteProcedureCallCount();
            Boolean created = response.getCreated();
            getLogger().debug(".logAuditEvent(): Exit, response->{}", response);
            return(created);
        } catch (NoSuchMethodException e) {
            getMetricsAgent().incrementRemoteProcedureCallFailureCount();
            getLogger().error(".logAuditEvent(): Error (NoSuchMethodException) ->{}", e.getMessage());
            return(null);
        } catch (Exception e) {
            getMetricsAgent().incrementRemoteProcedureCallFailureCount();
            e.printStackTrace();
            getLogger().error(".logAuditEvent: Error (GeneralException) ->{}", e.getMessage());
            return(null);
        }
    }
}
