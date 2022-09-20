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

package net.fhirfactory.dricats.petasos.participant.wup.messagebased;

import net.fhirfactory.dricats.constants.petasos.PetasosPropertyConstants;
import net.fhirfactory.dricats.interfaces.topology.ProcessingPlantRoleSupportInterface;
import net.fhirfactory.dricats.model.petasos.wup.valuesets.WUPArchetypeEnum;
import net.fhirfactory.pegacorn.core.model.topology.endpoints.base.IPCTopologyEndpoint;
import net.fhirfactory.pegacorn.core.model.topology.endpoints.interact.StandardInteractClientTopologyEndpointPort;
import net.fhirfactory.pegacorn.petasos.participant.wup.GenericMessageBasedWUPTemplate;
import net.fhirfactory.pegacorn.petasos.participant.wup.MessageBasedWUPEndpointContainer;
import net.fhirfactory.pegacorn.petasos.tasking.observations.metrics.agents.EndpointMetricsAgent;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.model.RouteDefinition;

public abstract class InteractEgressMessagingGatewayWUP extends GenericMessageBasedWUPTemplate {

    private EndpointMetricsAgent endpointMetricsAgent;

    private ProcessingPlantRoleSupportInterface processingPlantCapabilityStatement;

    //
    // Constructor(s)
    //

    public InteractEgressMessagingGatewayWUP() {
        super();
        this.endpointMetricsAgent = null;
    }

    //
    // Abstract Methods
    //

    protected abstract String specifyEgressTopologyEndpointName();
    protected abstract String specifyEndpointParticipantName();

    //
    // Getters and Setters
    //

    protected EndpointMetricsAgent getEndpointMetricsAgent(){
        return(endpointMetricsAgent);
    }

    protected void setEndpointMetricsAgent(EndpointMetricsAgent agent){
        this.endpointMetricsAgent = agent;
    }

    protected RouteDefinition fromInteractEgressService(String uri) {
        SourceSystemDetailInjector sourceSystemDetailInjector = new SourceSystemDetailInjector();
        PortDetailInjector portDetailInjector = new PortDetailInjector();
        RouteDefinition route = fromIncludingPetasosServices(uri);
        route
                .process(sourceSystemDetailInjector)
                .process(portDetailInjector);
        return route;
    }

    protected String getSourceSystemName() {
        getLogger().debug(".getSourceSystemName(): Entry, ingresEndpoint->{}", getEgressEndpoint());
        IPCTopologyEndpoint endpointTopologyNode = getEgressEndpoint().getEndpointTopologyNode();
        getLogger().trace(".getSourceSystemName(): endpointTopologyNode->{}", endpointTopologyNode);
        String sourceSystemName = endpointTopologyNode.getConnectedSystemName();
        getLogger().debug(".getSourceSystemName(): Exit, sourceSystemName->{}", sourceSystemName);
        return (sourceSystemName);
    }

    protected class SourceSystemDetailInjector implements Processor {

        @Override
        public void process(Exchange exchange) throws Exception {
            getLogger().debug("SourceSystemDetailInjector.process(): Entry");
            boolean alreadyInPlace = false;
            if (exchange.hasProperties()) {
                String sourceSystem = exchange.getProperty(PetasosPropertyConstants.WUP_INTERACT_EGRESS_SOURCE_SYSTEM_NAME, String.class);
                if (sourceSystem != null) {
                    alreadyInPlace = true;
                }
            }
            if (!alreadyInPlace) {
                exchange.setProperty(PetasosPropertyConstants.WUP_INTERACT_EGRESS_SOURCE_SYSTEM_NAME, getSourceSystemName());
            }
        }
    }

    //
    // Route Helper Functions
    //

    protected RouteDefinition fromIncludingPetasosServicesForEndpointsWithNoExceptionHandling(String uri) {
        NodeDetailInjector nodeDetailInjector = new NodeDetailInjector();
        AuditAgentInjector auditAgentInjector = new AuditAgentInjector();
        TaskReportAgentInjector taskReportAgentInjector = new TaskReportAgentInjector();
        MetricsAgentInjector metricsAgentInjector = new MetricsAgentInjector();
        RouteDefinition route = from(uri);
        route
                .process(nodeDetailInjector)
                .process(auditAgentInjector)
                .process(taskReportAgentInjector)
                .process(metricsAgentInjector)
        ;
        return route;
    }

    public class MetricsAgentInjector implements Processor{
        @Override
        public void process(Exchange camelExchange) throws Exception{
            getLogger().debug("MetricsAgentInjector.process(): Entry");
            camelExchange.setProperty(PetasosPropertyConstants.ENDPOINT_METRICS_AGENT_EXCHANGE_PROPERTY, getEndpointMetricsAgent());
        }
    }

    //
    // Overridden Superclass Methods
    //

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

    //
    // Subclass Route Helper
    //

    public class PortDetailInjector implements Processor {
        @Override
        public void process(Exchange exchange) throws Exception {
            getLogger().debug("PortDetailInjector.process(): Entry");
            boolean alreadyInPlace = false;
            if(exchange.hasProperties()) {
                String egressPortType = exchange.getProperty(PetasosPropertyConstants.WUP_INTERACT_PORT_TYPE, String.class);
                if (egressPortType != null) {
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
                        exchange.setProperty(PetasosPropertyConstants.WUP_METRICS_AGENT_EXCHANGE_PROPERTY, getMetricsAgent());
                        exchange.setProperty(PetasosPropertyConstants.ENDPOINT_METRICS_AGENT_EXCHANGE_PROPERTY, getEndpointMetricsAgent());
                        break;
                    }
                    case FILE_SHARE_SINK: {
                        StandardInteractClientTopologyEndpointPort clientTopologyEndpoint = (StandardInteractClientTopologyEndpointPort) getEgressEndpoint().getEndpointTopologyNode();
                        exchange.setProperty(PetasosPropertyConstants.WUP_INTERACT_PORT_TYPE, clientTopologyEndpoint.getEndpointType().getToken());
                        exchange.setProperty(PetasosPropertyConstants.ENDPOINT_PORT_VALUE, getEgressEndpoint().getEndpointSpecification());
                        exchange.setProperty(PetasosPropertyConstants.ENDPOINT_TOPOLOGY_NODE_EXCHANGE_PROPERTY, clientTopologyEndpoint);
                        exchange.setProperty(PetasosPropertyConstants.WUP_METRICS_AGENT_EXCHANGE_PROPERTY, getMetricsAgent());
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
