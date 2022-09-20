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
import net.fhirfactory.dricats.interfaces.petasos.participant.capabilities.ProcessingPlantRoleSupportInterface;
import net.fhirfactory.dricats.model.petasos.participant.archetypes.wup.valuesets.WUPArchetypeEnum;
import net.fhirfactory.dricats.model.petasos.tasking.routing.subscriptions.datatypes.DataParcelManifestSubscriptionMask;
import net.fhirfactory.dricats.petasos.observations.metrics.agents.EndpointMetricsAgent;
import net.fhirfactory.dricats.petasos.participant.wup.base.GenericMessageBasedWUPTemplate;
import net.fhirfactory.dricats.petasos.participant.wup.base.MessageBasedEndpointContainer;
import net.fhirfactory.pegacorn.core.model.topology.endpoints.file.FileShareSourceTopologyEndpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.model.RouteDefinition;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static net.fhirfactory.dricats.model.topology.endpoints.base.valuesets.EndpointTopologyNodeTypeEnum.MLLP_SERVER;

public abstract class InteractIngresMessagingGatewayWUP extends GenericMessageBasedWUPTemplate {

    private EndpointMetricsAgent endpointMetricsAgent;

    @Inject
    private ProcessingPlantRoleSupportInterface processingPlantCapabilityStatement;

    //
    // Constructor(s)
    //

    public InteractIngresMessagingGatewayWUP() {
        super();
        this.endpointMetricsAgent = null;
        getLogger().debug(".MessagingIngresGatewayWUP(): Entry, Default constructor");
    }

    //
    // Abstract Method Declarations
    //

    protected abstract String specifyIngresTopologyEndpointName();
    protected abstract String specifyIngresEndpointVersion();
    protected abstract String specifyEndpointParticipantName();

    //
    // Getters (and Setters)
    //

    protected EndpointMetricsAgent getEndpointMetricsAgent(){
        return(endpointMetricsAgent);
    }

    protected void setEndpointMetricsAgent(EndpointMetricsAgent agent){
        this.endpointMetricsAgent = agent;
    }

    //
    // Superclass Method Overrides/Implementations
    //

    @Override
    protected WUPArchetypeEnum specifyWUPArchetype(){
        return(WUPArchetypeEnum.WUP_NATURE_MESSAGE_EXTERNAL_INGRES_POINT);
    }

    @Override
    protected MessageBasedEndpointContainer specifyEgressEndpoint(){
        getLogger().debug(".specifyEgressTopologyEndpoint(): Entry");
        MessageBasedEndpointContainer egressEndpoint = new MessageBasedEndpointContainer();
        egressEndpoint.setFrameworkEnabled(true);
        egressEndpoint.setEndpointSpecification(this.getNameSet().getEndPointWUPEgress());
        getLogger().debug(".specifyEgressTopologyEndpoint(): Exit");
        return(egressEndpoint);
    }

    @Override
    protected void establishEndpointMetricAgents(){
        getLogger().debug(".establishEndpointMetricAgents(): Entry");
        String connectedSystem = getSourceSystemName();
        String endpointDescription = getIngresEndpoint().getEndpointTopologyNode().getEndpointDescription();
        this.endpointMetricsAgent = getMetricAgentFactory().newEndpointMetricsAgent(
                processingPlantCapabilityStatement,
                getIngresEndpoint().getEndpointTopologyNode().getComponentID(),
                getIngresEndpoint().getEndpointTopologyNode().getParticipantName(),
                connectedSystem,
                endpointDescription);
        getLogger().debug(".establishEndpointMetricAgents(): Exit");
    }

    /**
     * The Ingres Message Gateway doesn't subscribe to ANY topics as it receives it's 
     * input from an external system.
     * 
     * @return An empty Set<TopicToken>
     */
    @Override
    protected List<DataParcelManifestSubscriptionMask> specifySubscriptionTopics() {
        List<DataParcelManifestSubscriptionMask> subTopics = new ArrayList<>();
        return(subTopics);
    }

    /**
     * @param uri
     * @return the RouteBuilder.from(uri) with all exceptions logged but not handled
     */
    protected RouteDefinition fromInteractIngresService(String uri) {
        SourceSystemDetailInjector sourceSystemDetailInjector = new SourceSystemDetailInjector();
        PortDetailInjector portDetailInjector = new PortDetailInjector();
        RouteDefinition route = fromIncludingPetasosServices(uri);
        route
                .process(sourceSystemDetailInjector)
                .process(portDetailInjector)
        ;
        return route;
    }

    protected String getSourceSystemName(){
        getLogger().debug(".getSourceSystemName(): Entry, ingresEndpoint->{}", getIngresEndpoint());
        IPCTopologyEndpoint endpointTopologyNode = getIngresEndpoint().getEndpointTopologyNode();
        getLogger().trace(".getSourceSystemName(): endpointTopologyNode->{}", endpointTopologyNode);
        String sourceSystemName = endpointTopologyNode.getConnectedSystemName();
        getLogger().debug(".getSourceSystemName(): Exit, sourceSystemName->{}", sourceSystemName);
        return(sourceSystemName);
    }

    protected class SourceSystemDetailInjector implements Processor {
        @Override
        public void process(Exchange exchange) throws Exception {
            getLogger().debug("SourceSystemDetailInjector.process(): Entry");
            boolean alreadyInPlace = false;
            if(exchange.hasProperties()) {
                String sourceSystem = exchange.getProperty(PetasosPropertyConstants.WUP_INTERACT_INGRES_SOURCE_SYSTEM_NAME, String.class);
                if (sourceSystem != null) {
                    alreadyInPlace = true;
                }
            }
            if(!alreadyInPlace) {
                exchange.setProperty(PetasosPropertyConstants.WUP_INTERACT_INGRES_SOURCE_SYSTEM_NAME, getSourceSystemName());
            }
        }
    }

    //
    //
    // Route Helper Functions

    /**
     * @param uri
     * @return the RouteBuilder.from(uri) with all exceptions logged but not handled
     */
    protected RouteDefinition fromIncludingPetasosServices(String uri) {
        NodeDetailInjector nodeDetailInjector = new NodeDetailInjector();
        AuditAgentInjector auditAgentInjector = new AuditAgentInjector();
        TaskReportAgentInjector taskReportAgentInjector = new TaskReportAgentInjector();
        RouteDefinition route = fromWithStandardExceptionHandling(uri);
        route
                .process(nodeDetailInjector)
                .process(auditAgentInjector)
                .process(taskReportAgentInjector)
        ;
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
                switch(getIngresEndpoint().getEndpointTopologyNode().getEndpointType()) {
                    case MLLP_SERVER: {
                        MLLPServerEndpoint serverTopologyEndpoint = (MLLPServerEndpoint) getIngresEndpoint().getEndpointTopologyNode();
                        exchange.setProperty(PetasosPropertyConstants.WUP_INTERACT_PORT_TYPE, serverTopologyEndpoint.getEndpointType().getToken());
                        exchange.setProperty(PetasosPropertyConstants.ENDPOINT_PORT_VALUE, serverTopologyEndpoint.getMLLPServerAdapter().getPortNumber());
                        exchange.setProperty(PetasosPropertyConstants.ENDPOINT_TOPOLOGY_NODE_EXCHANGE_PROPERTY, serverTopologyEndpoint);
                        exchange.setProperty(PetasosPropertyConstants.WUP_METRICS_AGENT_EXCHANGE_PROPERTY, getMetricsAgent());
                        exchange.setProperty(PetasosPropertyConstants.ENDPOINT_METRICS_AGENT_EXCHANGE_PROPERTY, getEndpointMetricsAgent());
                        break;
                    }
                    case HTTP_API_SERVER:{
                        HTTPServerTopologyEndpoint serverTopologyEndpoint = (HTTPServerTopologyEndpoint) getIngresEndpoint().getEndpointTopologyNode();
                        exchange.setProperty(PetasosPropertyConstants.WUP_INTERACT_PORT_TYPE, serverTopologyEndpoint.getEndpointType().getToken());
                        exchange.setProperty(PetasosPropertyConstants.ENDPOINT_PORT_VALUE, serverTopologyEndpoint.getHTTPServerAdapter().getPortNumber());
                        exchange.setProperty(PetasosPropertyConstants.ENDPOINT_TOPOLOGY_NODE_EXCHANGE_PROPERTY, serverTopologyEndpoint);
                        exchange.setProperty(PetasosPropertyConstants.WUP_METRICS_AGENT_EXCHANGE_PROPERTY, getMetricsAgent());
                        exchange.setProperty(PetasosPropertyConstants.ENDPOINT_METRICS_AGENT_EXCHANGE_PROPERTY, getEndpointMetricsAgent());
                        break;
                    }
                    case FILE_SHARE_SOURCE:{
                        FileShareSourceTopologyEndpoint serverTopologyEndpoint = (FileShareSourceTopologyEndpoint) getIngresEndpoint().getEndpointTopologyNode();
                        exchange.setProperty(PetasosPropertyConstants.WUP_INTERACT_PORT_TYPE, serverTopologyEndpoint.getEndpointType().getToken());
                        exchange.setProperty(PetasosPropertyConstants.ENDPOINT_PORT_VALUE, serverTopologyEndpoint.getFileShareSourceAdapter().getFilePathAlias());
                        exchange.setProperty(PetasosPropertyConstants.ENDPOINT_TOPOLOGY_NODE_EXCHANGE_PROPERTY, serverTopologyEndpoint);
                        exchange.setProperty(PetasosPropertyConstants.WUP_METRICS_AGENT_EXCHANGE_PROPERTY, getMetricsAgent());
                        exchange.setProperty(PetasosPropertyConstants.ENDPOINT_METRICS_AGENT_EXCHANGE_PROPERTY, getEndpointMetricsAgent());
                        break;
                    }
                    default:{
                        exchange.setProperty(PetasosPropertyConstants.WUP_INTERACT_PORT_TYPE, "Undisclosed");
                        exchange.setProperty(PetasosPropertyConstants.WUP_INTERACT_PORT_TYPE, "Undisclosed");
                    }
                }
            }
        }
    }

}
