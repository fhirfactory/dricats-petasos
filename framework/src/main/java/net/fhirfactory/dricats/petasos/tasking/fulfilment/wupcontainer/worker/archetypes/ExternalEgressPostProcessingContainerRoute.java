/*
 * Copyright (c) 2020 Mark A. Hunter (ACT Health)
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

package net.fhirfactory.dricats.petasos.tasking.fulfilment.wupcontainer.worker.archetypes;

import net.fhirfactory.dricats.petasos.tasking.fulfilment.wupcontainer.worker.buildingblocks.WUPContainerEgressGatekeeper;
import net.fhirfactory.pegacorn.core.constants.petasos.PetasosPropertyConstants;
import net.fhirfactory.pegacorn.core.model.topology.nodes.WorkUnitProcessorSoftwareComponent;
import net.fhirfactory.dricats.petasos.tasking.audit.brokers.PetasosFulfillmentTaskAuditServicesBroker;
import net.fhirfactory.dricats.petasos.tasking.fulfilment.wupcontainer.worker.buildingblocks.WUPContainerEgressProcessor;
import net.fhirfactory.dricats.petasos.tasking.fulfilment.wupcontainer.worker.buildingblocks.WUPEgressConduit;
import net.fhirfactory.dricats.petasos.tasking.fulfilment.common.BasePetasosContainerRoute;
import net.fhirfactory.dricats.model.petasos.tasking.routing.identification.WUPComponentNames;
import net.fhirfactory.dricats.petasos.tasking.observations.metrics.agents.WorkUnitProcessorMetricsAgent;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalEgressPostProcessingContainerRoute extends BasePetasosContainerRoute {
    private static final Logger LOG = LoggerFactory.getLogger(ExternalEgressPostProcessingContainerRoute.class);
    protected Logger getLogger(){
        return(LOG);
    }

    private WorkUnitProcessorSoftwareComponent wupTopologyNode;
    private WUPComponentNames nameSet;
    private WorkUnitProcessorMetricsAgent metricsAgent;

    public ExternalEgressPostProcessingContainerRoute(CamelContext camelCTX, WorkUnitProcessorSoftwareComponent wupNode, PetasosFulfillmentTaskAuditServicesBroker auditTrailBroker, WorkUnitProcessorMetricsAgent metricsAgent) {
        super(camelCTX, auditTrailBroker);
        getLogger().debug(".ExternalIngresWUPContainerRoute(): Entry, context --> ###, wupNode --> {}", wupNode );
        this.wupTopologyNode = wupNode;
        nameSet = new WUPComponentNames(wupTopologyNode.getNodeFunctionFDN().getFunctionToken());
        this.metricsAgent = metricsAgent;
    }

    @Override
    public void configure() {
        getLogger().debug(".configure(): Entry!, for wupNodeElement --> {}", this.wupTopologyNode);
        getLogger().debug("ExternalIngresWUPContainerRoute :: EndPointWUPIngres --> Per Implementation Specified");
        getLogger().debug("ExternalIngresWUPContainerRoute :: EndPointWUPEgress --> {}", nameSet.getEndPointWUPEgress() );
        getLogger().debug("ExternalIngresWUPContainerRoute :: EndPointWUPEgressConduitEgress --> {}", nameSet.getEndPointWUPEgressConduitEgress());
        getLogger().debug("ExternalIngresWUPContainerRoute :: EndPointWUPContainerEgressProcessorEgress --> {}", nameSet.getEndPointWUPContainerEgressProcessorEgress());

        specifyCamelExecutionExceptionHandler();

        ExternalEgressPostProcessingContainerRoute.NodeDetailInjector nodeDetailInjector = new ExternalEgressPostProcessingContainerRoute.NodeDetailInjector();
      
        fromWithStandardExceptionHandling(nameSet.getEndPointWUPEgress())
        		.log(LoggingLevel.DEBUG, "from(nameSet.getEndPointWUPEgress()) --> ${body}")
                .process(nodeDetailInjector)
                .routeId(nameSet.getRouteWUPEgress2WUPEgressConduitEgress())
                .bean(WUPEgressConduit.class, "receiveFromWUP(*, Exchange)")
                .to(nameSet.getEndPointWUPEgressConduitEgress());

        fromWithStandardExceptionHandling(nameSet.getEndPointWUPEgressConduitEgress())
                .routeId(nameSet.getRouteWUPEgressConduitEgress2WUPEgressProcessorIngres())
                .to(nameSet.getEndPointWUPContainerEgressProcessorIngres());

        fromWithStandardExceptionHandling(nameSet.getEndPointWUPContainerEgressProcessorIngres())
                .routeId(nameSet.getRouteWUPContainerEgressProcessor())
                .process(nodeDetailInjector)
                .bean(WUPContainerEgressProcessor.class, "egressContentProcessor(*, Exchange)")
                .to(nameSet.getEndPointWUPContainerEgressProcessorEgress());

        fromWithStandardExceptionHandling(nameSet.getEndPointWUPContainerEgressProcessorEgress())
                .routeId(nameSet.getRouteWUPEgressProcessorEgress2WUPEgressGatekeeperIngres())
                .to(nameSet.getEndPointWUPContainerEgressGatekeeperIngres());

        fromWithStandardExceptionHandling(nameSet.getEndPointWUPContainerEgressGatekeeperIngres())
                .routeId(nameSet.getRouteWUPContainerEgressGateway())
                .process(nodeDetailInjector)
                .bean(WUPContainerEgressGatekeeper.class, "egressGatekeeper(*, Exchange)")
                .to(PetasosPropertyConstants.TASK_OUTCOME_COLLECTION_QUEUE);

    }

    protected class NodeDetailInjector implements Processor {
        @Override
        public void process(Exchange exchange) throws Exception {
            getLogger().debug("NodeDetailInjector.process(): Entry");
            boolean alreadyInPlace = false;
            if(exchange.hasProperties()) {
                WorkUnitProcessorSoftwareComponent wupTN = exchange.getProperty(PetasosPropertyConstants.WUP_TOPOLOGY_NODE_EXCHANGE_PROPERTY_NAME, WorkUnitProcessorSoftwareComponent.class);
                if (wupTN != null) {
                    alreadyInPlace = true;
                }
            }
            if(!alreadyInPlace) {
                exchange.setProperty(PetasosPropertyConstants.WUP_TOPOLOGY_NODE_EXCHANGE_PROPERTY_NAME, getWupTopologyNode());
            }
            exchange.setProperty(PetasosPropertyConstants.WUP_METRICS_AGENT_EXCHANGE_PROPERTY, metricsAgent);
        }
    }

    public WorkUnitProcessorSoftwareComponent getWupTopologyNode() {
        return wupTopologyNode;
    }
}
