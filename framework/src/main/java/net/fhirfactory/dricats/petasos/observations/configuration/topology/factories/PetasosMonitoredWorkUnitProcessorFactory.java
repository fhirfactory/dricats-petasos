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
package net.fhirfactory.dricats.petasos.observations.configuration.topology.factories;

import net.fhirfactory.dricats.model.simplified.resources.summaries.isc.ISCEndpointSummary;
import net.fhirfactory.dricats.model.simplified.resources.summaries.isc.ISCWorkUnitProcessorSummary;
import net.fhirfactory.dricats.model.topology.endpoints.base.EndpointTopologyNode;
import net.fhirfactory.dricats.model.topology.endpoints.jgroups.JGroupsClusterConnectorETN;
import net.fhirfactory.dricats.model.topology.nodes.softwarecomponents.workunitprocessors.base.WorkUnitProcessorTopologyNode;
import net.fhirfactory.dricats.model.topology.nodes.softwarecomponents.workunitprocessors.petasos.base.PetasosEnabledAPIClientTN;
import net.fhirfactory.dricats.model.topology.nodes.softwarecomponents.workunitprocessors.petasos.base.PetasosEnabledExternalIPCEgressWUPTopologyNode;
import net.fhirfactory.dricats.model.topology.nodes.softwarecomponents.workunitprocessors.petasos.base.PetasosEnabledExternalIPCIngresWUPTopologyNode;
import net.fhirfactory.dricats.model.topology.nodes.softwarecomponents.workunitprocessors.petasos.ipc.base.PetasosIPCServiceAgentTopologyNode;
import net.fhirfactory.dricats.petasos.observations.configuration.topology.factories.common.PetasosMonitoredComponentFactory;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class PetasosMonitoredWorkUnitProcessorFactory extends PetasosMonitoredComponentFactory {
    private static final Logger LOG = LoggerFactory.getLogger(PetasosMonitoredWorkUnitProcessorFactory.class);

    @Inject
    private PetasosEndpointSummaryFactory endpointFactory;

    @Override
    protected Logger getLogger() {
        return (LOG);
    }

    public ISCWorkUnitProcessorSummary newWorkUnitProcessor(String workshopParticipantName, WorkUnitProcessorTopologyNode wupTopologyNode){
        getLogger().debug(".newWorkUnitProcessor(): wupTopologyNode->{}", wupTopologyNode);
        ISCWorkUnitProcessorSummary wup = new ISCWorkUnitProcessorSummary();
        wup = (ISCWorkUnitProcessorSummary) newPetasosMonitoredComponent(wup, wupTopologyNode);
        wup.setWorkshopParticipantName(workshopParticipantName);

        if(wupTopologyNode instanceof PetasosEnabledAPIClientTN){
            PetasosEnabledAPIClientTN apiClientTN = (PetasosEnabledAPIClientTN) wupTopologyNode;
            EndpointTopologyNode apiEndpoint = apiClientTN.getApiEndpoint();
            try{
                ISCEndpointSummary apiEndpointSummary = (ISCEndpointSummary)endpointFactory.newEndpoint(wup.getName(), apiEndpoint);
                wup.addEndpoint(apiEndpointSummary);
            } catch (Exception ex){
                getLogger().warn(".newWorkUnitProcessor(): Unable to create EndpointSummary for ->{}, error->{} ", wupTopologyNode, ExceptionUtils.getMessage(ex));
            }
        }

        if(wupTopologyNode instanceof PetasosEnabledExternalIPCEgressWUPTopologyNode) {
            PetasosEnabledExternalIPCEgressWUPTopologyNode egressWUPTopologyNode = (PetasosEnabledExternalIPCEgressWUPTopologyNode)wupTopologyNode;
            if (egressWUPTopologyNode.getEgressEndpoint() != null) {
                EndpointTopologyNode egressEndpoint = egressWUPTopologyNode.getEgressEndpoint();
                try {
                    ISCEndpointSummary egressMonitoredEndpoint = (ISCEndpointSummary) endpointFactory.newEndpoint(wup.getName(), egressEndpoint);
                    wup.addEndpoint(egressMonitoredEndpoint);
                } catch (Exception ex) {
                    getLogger().warn(".newWorkUnitProcessor(): Unable to create EndpointSummary for ->{}, error->{} ", egressEndpoint, ExceptionUtils.getMessage(ex));
                }

            }
        }

        if(wupTopologyNode instanceof PetasosEnabledExternalIPCIngresWUPTopologyNode) {
            PetasosEnabledExternalIPCIngresWUPTopologyNode ingresWUPTopologyNode = (PetasosEnabledExternalIPCIngresWUPTopologyNode)wupTopologyNode;
            if (ingresWUPTopologyNode.getIngresEndpoint() != null) {
                EndpointTopologyNode ingresEndpoint = ingresWUPTopologyNode.getIngresEndpoint();
                try {
                    ISCEndpointSummary ingresMonitoredEndpoint = (ISCEndpointSummary) endpointFactory.newEndpoint(wup.getName(), ingresEndpoint);
                    wup.addEndpoint(ingresMonitoredEndpoint);
                } catch (Exception ex) {
                    getLogger().warn(".newWorkUnitProcessor(): Unable to create EndpointSummary for ->{}, error->{} ", ingresEndpoint, ExceptionUtils.getMessage(ex));
                }
            }
        }

        if(wupTopologyNode instanceof PetasosIPCServiceAgentTopologyNode){
            PetasosIPCServiceAgentTopologyNode serviceAgentTopologyNode = (PetasosIPCServiceAgentTopologyNode) wupTopologyNode;
            JGroupsClusterConnectorETN clusterConnector = serviceAgentTopologyNode.getClusterConnector();
            try {
                ISCEndpointSummary jgroupsEndpoint = (ISCEndpointSummary) endpointFactory.newEndpoint(wup.getName(), clusterConnector);
                wup.addEndpoint(jgroupsEndpoint);
            } catch (Exception ex){
                getLogger().warn(".newWorkUnitProcessor(): Unable to create EndpointSummary for ->{}, error->{} ", clusterConnector, ExceptionUtils.getMessage(ex));
            }
        }

        getLogger().debug(".newWorkUnitProcessor(): wup->{}", wup);
        return(wup);
    }
}
