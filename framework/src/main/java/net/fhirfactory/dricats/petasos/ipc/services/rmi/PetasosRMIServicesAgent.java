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
package net.fhirfactory.dricats.petasos.ipc.services.rmi;

import net.fhirfactory.dricats.interfaces.capabilities.CapabilityUtilisationBrokerInterface;
import net.fhirfactory.dricats.interfaces.observations.topology.PetasosTopologyReportingServiceProviderNameInterface;
import net.fhirfactory.dricats.model.capabilities.base.CapabilityUtilisationRequest;
import net.fhirfactory.dricats.model.capabilities.base.CapabilityUtilisationResponse;
import net.fhirfactory.dricats.model.petasos.ipc.technologies.jgroups.JGroupsChannelConnectorSummary;
import net.fhirfactory.dricats.model.transaction.model.PegacornTransactionMethodOutcome;
import net.fhirfactory.dricats.petasos.ipc.frameworks.cluster.connections.PetasosRMIClusterConnection;
import org.jgroups.Address;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class PetasosRMIServicesAgent extends PetasosRMIClusterConnection implements CapabilityUtilisationBrokerInterface {
    private static final Logger LOG = LoggerFactory.getLogger(PetasosRMIServicesAgent.class);



    @Inject
    private PetasosTopologyReportingServiceProviderNameInterface topologyReportingProvider;

    //
    // Constructor(s)
    //

    public PetasosRMIServicesAgent(){
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

    //
    // Post Construct (invoked from Superclass)
    //

    //
    // RMI Services Method Support
    //

    public Boolean executeRMI(String serviceProviderName, String methodName, Object[] objectSet, Class[] classSet){
        getLogger().info(".executeRMI(): Entry, serviceProviderName->{}, methodName->{}", serviceProviderName, methodName);
        JGroupsChannelConnectorSummary myJGroupsIP = createSummary();
        Address targetAddress = getTargetMemberAddress(serviceProviderName);
        try {
            int objectSetLength = objectSet.length;
            int classSetLength = classSet.length;
            Object rmiObjectSet[] = new Object[objectSetLength+1];
            Class rmiClassSet[] = new Class[classSetLength+1];
            for(int counter = 0; counter < objectSetLength; counter += 1){
                rmiObjectSet[counter] = objectSet[counter];
            }
            objectSet[objectSetLength] = myJGroupsIP;
            for(int counter = 0; counter < classSetLength; counter += 1){
                rmiClassSet[counter] = classSet[counter];
            }
            classSet[objectSetLength] = JGroupsChannelConnectorSummary.class;
            RequestOptions requestOptions = new RequestOptions( ResponseMode.GET_FIRST, getRPCUnicastTimeout());
            PegacornTransactionMethodOutcome response = null;
            synchronized (getChannelLock()){
                response = getRPCDispatcher().callRemoteMethod(targetAddress, methodName, objectSet, classSet, requestOptions);
            }
            getMetricsAgent().incrementRemoteProcedureCallCount();
            Boolean created = response.getCreated();
            getLogger().info(".executeRMI(): Exit, response->{}", response);
            return(created);
        } catch (NoSuchMethodException e) {
            getMetricsAgent().incrementRemoteProcedureCallFailureCount();
            getLogger().error(".executeRMI(): Error (NoSuchMethodException) ->{}", e.getMessage());
            return(null);
        } catch (Exception e) {
            getMetricsAgent().incrementRemoteProcedureCallFailureCount();
            e.printStackTrace();
            getLogger().error(".executeRMI: Error (GeneralException) ->{}", e.getMessage());
            return(null);
        }
    }

    @Override
    public CapabilityUtilisationResponse executeTask(String preferredCapabilityProvider, CapabilityUtilisationRequest taskRequest) {
        return null;
    }
}
