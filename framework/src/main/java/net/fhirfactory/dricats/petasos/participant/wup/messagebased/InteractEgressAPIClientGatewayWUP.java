/*
 * Copyright (c) 2022 ACT Health
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

package net.fhirfactory.dricats.petasos.participant.wup.messagebased;

import net.fhirfactory.dricats.constants.petasos.PetasosPropertyConstants;
import net.fhirfactory.dricats.interfaces.topology.ProcessingPlantRoleSupportInterface;
import net.fhirfactory.pegacorn.core.model.topology.endpoints.file.FileShareSourceTopologyEndpoint;
import net.fhirfactory.pegacorn.core.model.topology.endpoints.http.HTTPServerTopologyEndpoint;
import net.fhirfactory.pegacorn.core.model.topology.endpoints.interact.StandardInteractClientTopologyEndpointPort;
import net.fhirfactory.pegacorn.core.model.topology.endpoints.mllp.MLLPServerEndpoint;
import net.fhirfactory.pegacorn.petasos.participant.wup.MessageBasedWUPEndpointContainer;
import net.fhirfactory.pegacorn.petasos.participant.wup.GenericMessageBasedWUPTemplate;
import net.fhirfactory.dricats.model.petasos.wup.valuesets.WUPArchetypeEnum;
import net.fhirfactory.pegacorn.petasos.tasking.observations.metrics.agents.EndpointMetricsAgent;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.model.RouteDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public abstract class InteractEgressAPIClientGatewayWUP extends GenericMessageBasedWUPTemplate {
    private static final Logger LOG = LoggerFactory.getLogger(InteractEgressAPIClientGatewayWUP.class);

    private EndpointMetricsAgent endpointMetricsAgent;

    @Inject
    private ProcessingPlantRoleSupportInterface processingPlantCapabilityStatement;

    //
    // Constructor(s)
    //

    public InteractEgressAPIClientGatewayWUP() {
        super();
        this.endpointMetricsAgent = null;
//        getLogger().debug(".MessagingIngresGatewayWUP(): Entry, Default constructor");
    }

    //
    // Getters and Setters
    //

    protected EndpointMetricsAgent getEndpointMetricsAgent(){
        return(endpointMetricsAgent);
    }

    protected void setEndpointMetricsAgent(EndpointMetricsAgent agent){
        this.endpointMetricsAgent = agent;
    }

    protected Logger getLogger(){
        return(LOG);
    }

    protected abstract String specifyEgressTopologyEndpointName();

    @Override
    protected WUPArchetypeEnum specifyWUPArchetype(){
        return(WUPArchetypeEnum.WUP_NATURE_MESSAGE_EXTERNAL_EGRESS_POINT);
    }

    protected MessageBasedWUPEndpointContainer establishIngresEndpoint(){
        getLogger().debug(".specifyIngresTopologyEndpoint(): Entry");
        MessageBasedWUPEndpointContainer ingressEndpoint = new MessageBasedWUPEndpointContainer();
        ingressEndpoint.setFrameworkEnabled(true);
        ingressEndpoint.setEndpointSpecification(this.getNameSet().getEndPointWUPIngres());
        getLogger().debug(".specifyIngresTopologyEndpoint(): Exit");
        return(ingressEndpoint);
    }


    @Override
    protected void establishEndpointMetricAgents(){
        getLogger().debug(".establishEndpointMetricAgents(): Entry");
        String connectedSystem = getEgressEndpoint().getEndpointTopologyNode().getConnectedSystemName();
        String endpointDescription = getEgressEndpoint().getEndpointSpecification();
        this.endpointMetricsAgent = getMetricAgentFactory().newEndpointMetricsAgent(
                processingPlantCapabilityStatement,
                getEgressEndpoint().getEndpointTopologyNode().getComponentID(),
                getEgressEndpoint().getEndpointTopologyNode().getParticipantName(),
                connectedSystem,
                endpointDescription);
        getLogger().debug(".establishEndpointMetricAgents(): Exit");
    }

    @Override
    protected void establishEndpointMetricAgents(){
        getLogger().debug(".establishEndpointMetricAgents(): Entry");
        String connectedSystem = getEgressEndpoint().getEndpointTopologyNode().getConnectedSystemName();
        String endpointDescription = getEgressEndpoint().getEndpointSpecification();
        this.endpointMetricsAgent = getMetricAgentFactory().newEndpointMetricsAgent(
                processingPlantCapabilityStatement,
                getEgressEndpoint().getEndpointTopologyNode().getComponentID(),
                getEgressEndpoint().getEndpointTopologyNode().getParticipantName(),
                connectedSystem,
                endpointDescription);
        getLogger().debug(".establishEndpointMetricAgents(): Exit");
    }

    protected RouteDefinition fromInteractEgressService(String uri) {
        InteractEgressAPIClientGatewayWUP.PortDetailInjector portDetailInjector = new InteractEgressAPIClientGatewayWUP.PortDetailInjector();
        RouteDefinition route = fromIncludingPetasosServices(uri);
        route
                .process(portDetailInjector);
        return route;
    }

    //
    // Detail Injectors for Routes
    //

    protected class PortDetailInjector implements Processor {
        @Override
        public void process(Exchange exchange) throws Exception {
            getLogger().debug("PortDetailInjector.process(): Entry");
            boolean alreadyInPlace = false;
            if(exchange.hasProperties()) {
                String ingresPort = exchange.getProperty(PetasosPropertyConstants.WUP_INTERACT_PORT_TYPE, String.class);
                if (ingresPort != null) {
                    alreadyInPlace = true;
                }
            }
            if (!alreadyInPlace) {
                switch(getEgressEndpoint().getEndpointTopologyNode().getEndpointType()) {
                    case MLLP_CLIENT:
                    case HTTP_API_CLIENT: {
                        StandardInteractClientTopologyEndpointPort clientTopologyEndpoint = (StandardInteractClientTopologyEndpointPort) getEgressEndpoint().getEndpointTopologyNode();
                        exchange.setProperty(PetasosPropertyConstants.WUP_INTERACT_PORT_TYPE, clientTopologyEndpoint.getEndpointType().getToken());
                        exchange.setProperty(PetasosPropertyConstants.ENDPOINT_PORT_VALUE, getEgressEndpoint().getEndpointSpecification());
                        exchange.setProperty(PetasosPropertyConstants.ENDPOINT_TOPOLOGY_NODE_EXCHANGE_PROPERTY, clientTopologyEndpoint);
                        exchange.setProperty(PetasosPropertyConstants.ENDPOINT_METRICS_AGENT_EXCHANGE_PROPERTY, getEndpointMetricsAgent());
                        break;
                    }
                    case FILE_SHARE_SINK: {
                        StandardInteractClientTopologyEndpointPort clientTopologyEndpoint = (StandardInteractClientTopologyEndpointPort) getEgressEndpoint().getEndpointTopologyNode();
                        exchange.setProperty(PetasosPropertyConstants.WUP_INTERACT_PORT_TYPE, clientTopologyEndpoint.getEndpointType().getToken());
                        exchange.setProperty(PetasosPropertyConstants.ENDPOINT_PORT_VALUE, getEgressEndpoint().getEndpointSpecification());
                        exchange.setProperty(PetasosPropertyConstants.ENDPOINT_TOPOLOGY_NODE_EXCHANGE_PROPERTY, clientTopologyEndpoint);
                        exchange.setProperty(PetasosPropertyConstants.ENDPOINT_METRICS_AGENT_EXCHANGE_PROPERTY, getEndpointMetricsAgent());
                        break;
                    }
                    default:{
                        // Do nothing
                    }
                }
            }
        }
    }
}
