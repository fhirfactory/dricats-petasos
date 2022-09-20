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

package net.fhirfactory.dricats.petasos.participant.wup.messagebased.mllp;

import net.fhirfactory.dricats.configuration.defaults.petasos.PetasosPropertyConstants;
import net.fhirfactory.dricats.interfaces.petasos.participant.capabilities.ProcessingPlantRoleSupportInterface;
import net.fhirfactory.dricats.interfaces.petasos.participant.endpoint.WorkUnitProcessorEndpointInterface;
import net.fhirfactory.dricats.model.component.datatypes.ComponentId;
import net.fhirfactory.dricats.model.configuration.filebased.segments.connectedsystems.ConnectedSystemPort;
import net.fhirfactory.dricats.model.configuration.filebased.segments.endpoints.mllp.MLLPSenderEndpointSegment;
import net.fhirfactory.dricats.model.petasos.participant.archetypes.wup.valuesets.WUPArchetypeEnum;
import net.fhirfactory.dricats.model.petasos.participant.components.wup.valuesets.WUPArchetypeEnum;
import net.fhirfactory.dricats.model.petasos.participant.datatypes.PetasosParticipantId;
import net.fhirfactory.dricats.petasos.observations.metrics.agents.EndpointMetricsAgent;
import net.fhirfactory.dricats.model.petasos.participant.components.endpoint.InternalCamelRoute;
import net.fhirfactory.dricats.petasos.participant.endpoint.base.valuesets.EndpointStatusEnum;
import net.fhirfactory.dricats.petasos.participant.endpoint.mllp.MLLPSenderEndpoint;
import net.fhirfactory.dricats.petasos.participant.endpoint.mllp.adapters.MLLPSenderAdapter;
import net.fhirfactory.dricats.petasos.participant.wup.base.GenericMessageBasedWUPTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.model.RouteDefinition;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.time.Instant;
import java.util.UUID;

import static net.fhirfactory.dricats.model.petasos.participant.valuesets.EndpointTypeEnum.MLLP_CLIENT;

public abstract class MLLPSenderWUP extends GenericMessageBasedWUPTemplate {

    private MLLPSenderEndpoint egressEndpoint;
    private InternalCamelRoute ingresEndpoint;

    private EndpointMetricsAgent endpointMetricsAgent;
    @Inject
    private ProcessingPlantRoleSupportInterface processingPlantCapabilityStatement;

    //
    // Constructor(s)
    //

    public MLLPSenderWUP() {
        super();
        this.endpointMetricsAgent = null;
        getLogger().debug(".MLLPSenderWUP(): Entry, Default constructor");
    }

    //
    // Abstract Methods
    //

    protected abstract String specifyEgressEndpointConfigName();
    protected abstract String specifyEgressEndpointParticipantName();
    protected abstract String specifyEgressEndpointParticipantVersion();

    //
    // Getters and Setters
    //

    protected EndpointMetricsAgent getEndpointMetricsAgent(){
        return(endpointMetricsAgent);
    }

    protected void setEndpointMetricsAgent(EndpointMetricsAgent agent){
        this.endpointMetricsAgent = agent;
    }



    //
    // Endpoint Builder
    //

    @Override
    protected WorkUnitProcessorEndpointInterface specifyEgressEndpoint(){
        getLogger().debug(".buildEgressEndpoint(): Entry");
        MLLPSenderEndpointSegment mllpSenderEndpointConfig = getProcessingPlantConfigurationService().getMLLPSenderEndpointConfig(specifyEgressEndpointConfigName());
        if(mllpSenderEndpointConfig == null){
            throw new RuntimeException("Cannot Resolve MLLP Sender Configuration for (" + specifyEgressEndpointConfigName() + ")");
        }
        //
        // Base Information
        MLLPSenderEndpoint mllpSenderEndpoint = new MLLPSenderEndpoint();
        mllpSenderEndpoint.setEndpointType(MLLP_CLIENT);
        mllpSenderEndpoint.setEndpointStatus(EndpointStatusEnum.DRICATS_ENDPOINT_STATUS_DETECTED);
        mllpSenderEndpoint.setEndpointConfigurationName(specifyEgressEndpointConfigName());
        mllpSenderEndpoint.setServer(false);
        //
        // ComponentId
        ComponentId mllpSenderEndpointComponentId = new ComponentId(specifyEgressEndpointConfigName(), getComponentVersion());
        mllpSenderEndpoint.setEndpointComponentId(mllpSenderEndpointComponentId);
        //
        // ParticipantId
        PetasosParticipantId participantId = new PetasosParticipantId(getPetasosParticipant().getParticipantId().getSubsystemName(), getPetasosParticipant().getParticipantId().getParticipantName(), specifyEgressEndpointParticipantName(), specifyEgressEndpointParticipantVersion());
        mllpSenderEndpoint.setEndpointParticipantId(participantId);
        //
        // endpoint adapters
        Boolean keepAlive = mllpSenderEndpointConfig.getKeepAlive();
        Integer connectionTimeout = mllpSenderEndpointConfig.getConnectionTimeout();
        ConnectedSystemPort targetPort1 = mllpSenderEndpointConfig.getConnectedSystem().getTargetPort1();
        MLLPSenderAdapter mllpSenderAdapter1 = buildMLLPSenderAdapter(targetPort1, keepAlive, connectionTimeout, 0, true);
        mllpSenderEndpoint.getMLLPClientAdapters().add(mllpSenderAdapter1);
        ConnectedSystemPort targetPort2 = mllpSenderEndpointConfig.getConnectedSystem().getTargetPort2();
        if(targetPort2 != null){
            MLLPSenderAdapter mllpSenderAdapter2 = buildMLLPSenderAdapter(targetPort2, keepAlive, connectionTimeout, 1, false);
            mllpSenderEndpoint.getMLLPClientAdapters().add(mllpSenderAdapter2);
        }
        ConnectedSystemPort targetPort3 = mllpSenderEndpointConfig.getConnectedSystem().getTargetPort3();
        if(targetPort3 != null){
            MLLPSenderAdapter mllpSenderAdapter3 = buildMLLPSenderAdapter(targetPort3, keepAlive, connectionTimeout, 2, false);
            mllpSenderEndpoint.getMLLPClientAdapters().add(mllpSenderAdapter3);
        }
        //
        //
        getLogger().debug(".buildEgressEndpoint(): Exit, mllpSenderEndpoint->{}", mllpSenderEndpoint);
        return(mllpSenderEndpoint);
    }

    protected MLLPSenderAdapter buildMLLPSenderAdapter(ConnectedSystemPort targetPort, boolean keepAlive, Integer connectionTimeout, Integer priority, boolean active){
        MLLPSenderAdapter mllpSenderAdapter = new MLLPSenderAdapter();
        mllpSenderAdapter.setKeepAlive(keepAlive);
        mllpSenderAdapter.setConnectTimeout(connectionTimeout);
        mllpSenderAdapter.setEncrypted(targetPort.getEncryptionRequired());
        mllpSenderAdapter.setHostName(targetPort.getTargetPortDNSName());
        mllpSenderAdapter.setPortNumber(targetPort.getTargetPortValue());
        mllpSenderAdapter.setPriority(priority);
        mllpSenderAdapter.setActive(active);
        ComponentId adapterId = new ComponentId();
        adapterId.setId(UUID.randomUUID().toString());
        adapterId.setDisplayName("MLLPSender("+targetPort.getTargetPortDNSName()+":"+targetPort.getTargetPortValue()+")");
        adapterId.setIdValidityEndInstant(Instant.EPOCH);
        adapterId.setIdValidityStartInstant(Instant.now());
        mllpSenderAdapter.setComponentId(adapterId);
        return(mllpSenderAdapter);
    }

    @Override
    protected WorkUnitProcessorEndpointInterface specifyIngresEndpoint(){
        getLogger().debug(".buildIngresEndpoint(): Entry");
        InternalCamelRoute internalIngresEndpoint = new InternalCamelRoute(getPetasosParticipant().getParticipantId(), null);
        getLogger().debug(".buildIngresEndpoint(): Exit, internalIngresEndpoint->{}", internalIngresEndpoint);
        return(internalIngresEndpoint);
    }

    //
    //
    //

    protected class DestinationDetailInjector implements Processor {
        @Override
        public void process(Exchange exchange) throws Exception {
            getLogger().debug("SourceSystemDetailInjector.process(): Entry");
            boolean alreadyInPlace = false;
            if (exchange.hasProperties()) {
                String targetSystem = exchange.getProperty(PetasosPropertyConstants.WUP_INTERACT_EGRESS_TARGET_SYSTEM_NAME, String.class);
                if (targetSystem != null) {
                    alreadyInPlace = true;
                }
            }
            if (!alreadyInPlace) {
                MLLPSenderEndpoint senderEP = (MLLPSenderEndpoint)getEgressEndpoint();
                MLLPSenderAdapter activeAdapter = null;
                for(MLLPSenderAdapter currentAdapter: senderEP.getMLLPClientAdapters()){
                    if(currentAdapter.isActive()){
                        activeAdapter = currentAdapter;
                        break;
                    }
                }
                if(activeAdapter != null) {
                    if(StringUtils.isNotEmpty(activeAdapter.getTargetSystemName())) {
                        String targetSystem = activeAdapter.getTargetSystemName();
                        exchange.setProperty(PetasosPropertyConstants.WUP_INTERACT_EGRESS_TARGET_SYSTEM_NAME, targetSystem);
                    }
                }
            }
            getLogger().debug("SourceSystemDetailInjector.process(): Exit");
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
            camelExchange.setProperty(PetasosPropertyConstants.WUP_METRICS_AGENT_EXCHANGE_PROPERTY, getMetricsAgent());
        }
    }

    //
    // Overridden Superclass Methods
    //

    @Override
    protected WUPArchetypeEnum specifyWUPArchetype(){
        return(WUPArchetypeEnum.WUP_NATURE_MESSAGE_EXTERNAL_EGRESS_POINT);
    }


    @Override
    protected void establishEndpointMetricAgents(){
        getLogger().debug(".establishEndpointMetricAgents(): Entry");
        MLLPSenderEndpointSegment mllpSenderEndpointConfig = getProcessingPlantConfigurationService().getMLLPSenderEndpointConfig(specifyEgressEndpointConfigName());
        String connectedSystem = mllpSenderEndpointConfig.getConnectedSystem().getSubsystemName();
        String endpointDescription = getEgressEndpoint().getEndpointDescription();
        this.endpointMetricsAgent = getMetricAgentFactory().newEndpointMetricsAgent(
                processingPlantCapabilityStatement,
                getEgressEndpoint().getEndpointComponentId(),
                getEgressEndpoint().getEndpointParticipantId().getParticipantName(),
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
                MLLPSenderEndpoint senderEP = (MLLPSenderEndpoint)getEgressEndpoint();
                exchange.setProperty(PetasosPropertyConstants.WUP_INTERACT_PORT_TYPE, senderEP.getEndpointType().getToken());
                exchange.setProperty(PetasosPropertyConstants.ENDPOINT_DESCRIPTION, senderEP.getEndpointDescription());
            }
        }
    }

    //
    // Route Helpers
    //

    protected RouteDefinition fromInteractEgressService(String uri) {
        TaskReportAgentInjector targetSystemDetailInjector = new TaskReportAgentInjector();
        PortDetailInjector portDetailInjector = new PortDetailInjector();
        RouteDefinition route = fromIncludingPetasosServices(uri);
        route
                .process(targetSystemDetailInjector)
                .process(portDetailInjector);
        return route;
    }
}
