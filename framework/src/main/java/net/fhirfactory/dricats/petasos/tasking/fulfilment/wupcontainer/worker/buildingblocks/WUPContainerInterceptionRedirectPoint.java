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

package net.fhirfactory.dricats.petasos.tasking.fulfilment.wupcontainer.worker.buildingblocks;

import net.fhirfactory.pegacorn.core.constants.petasos.PetasosPropertyConstants;
import net.fhirfactory.pegacorn.core.model.componentid.TopologyNodeFunctionFDNToken;
import net.fhirfactory.pegacorn.core.model.petasos.task.datatypes.interception.TaskInterceptionType;
import net.fhirfactory.pegacorn.core.model.topology.nodes.WorkUnitProcessorSoftwareComponent;
import net.fhirfactory.dricats.model.petasos.tasking.routing.identification.WUPComponentNames;
import net.fhirfactory.dricats.petasos.tasking.caches.local.PetasosFulfillmentTaskSharedInstance;
import net.fhirfactory.dricats.petasos.tasking.observations.metrics.agents.WorkUnitProcessorMetricsAgent;
import org.apache.camel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.time.Instant;


@ApplicationScoped
public class WUPContainerInterceptionRedirectPoint {
    private static final Logger LOG = LoggerFactory.getLogger(WUPContainerInterceptionRedirectPoint.class);

    protected Logger getLogger(){
        return(LOG);
    }

    @Produce
    private ProducerTemplate routeInjector;


    /**
     * The WUP-Interception-Redirect-Point is the point at which traffic may be re-directed to another Participant
     * (either within the same ProcessingPlant or another one). The corresponding WUP-Interception-Return-Point
     * is the point at which traffic is re-injected into the MOA-WUP route.
     *
     * When interception is active, it is important to note that the MOA-WUP instance is still subject to the
     * standard processing timeouts, retries, etc. processes that are defined/configured for the MOA-WUP type.
     * The implementation of the interception function merely re-routes the content from the standard WUP-User-Space
     * processing activity to an alternative.
     *
     * @param fulfillmentTask     The PetasosFulfillmentTask that "may or may not" be intercepted
     * @param camelExchange    The Apache Camel Exchange object
     * @return Should either return the ingres point into the associated WUP Ingres Conduit or null (if the packet is to be discarded)
     */

    public void interceptionRedirectDecisionPoint(PetasosFulfillmentTaskSharedInstance fulfillmentTask, Exchange camelExchange) {
        if(getLogger().isInfoEnabled()){
            getLogger().info(".interceptionRedirectDecisionPoint(): Entry, fulfillmentTaskId/ActionableTaskId->{}/{}", fulfillmentTask.getTaskId(), fulfillmentTask.getActionableTaskId());
        }
        getLogger().debug(".interceptionRedirectDecisionPoint(): Enter, fulfillmentTask->{}", fulfillmentTask);
        WorkUnitProcessorSoftwareComponent wupSoftwareComponent = camelExchange.getProperty(PetasosPropertyConstants.WUP_TOPOLOGY_NODE_EXCHANGE_PROPERTY_NAME, WorkUnitProcessorSoftwareComponent.class);

        //
        // Should we redirect the Task?
        boolean isBeingRedirected = false;
        String redirectionTarget = null;
        if(wupSoftwareComponent.hasRedirectionControl()){
            boolean redirectionFlagSet = wupSoftwareComponent.getRedirectionControl().getRedirectionActive();
            boolean isInRedirectionWindow = false;
            if(wupSoftwareComponent.getRedirectionControl().hasRedirectionStart()){
                if(Instant.now().isAfter(wupSoftwareComponent.getRedirectionControl().getRedirectionStart())){
                    if(wupSoftwareComponent.getRedirectionControl().hasRedirectionFinish()){
                        if(Instant.now().isBefore(wupSoftwareComponent.getRedirectionControl().getRedirectionFinish())){
                            isInRedirectionWindow = true;
                        } else {
                            isInRedirectionWindow = false;
                        }
                    } else {
                        isInRedirectionWindow = true;
                    }
                }
            }
            if(redirectionFlagSet || isInRedirectionWindow){
                if(wupSoftwareComponent.getRedirectionControl().hasRedirectionTargetParticipantName()){
                    isBeingRedirected = true;
                    redirectionTarget = wupSoftwareComponent.getRedirectionControl().getRedirectionTargetParticipantName();
                }
            }
        }

        //
        // Get out metricsAgent & do add some metrics
        WorkUnitProcessorMetricsAgent metricsAgent = camelExchange.getProperty(PetasosPropertyConstants.WUP_METRICS_AGENT_EXCHANGE_PROPERTY, WorkUnitProcessorMetricsAgent.class);
        // Now, continue with business logic
        // Get Route Names
        TopologyNodeFunctionFDNToken wupToken = fulfillmentTask.getTaskFulfillment().getFulfillerWorkUnitProcessor().getNodeFunctionFDN().getFunctionToken();
        getLogger().trace(".interceptionRedirectDecisionPoint(): wupFunctionToken (NodeElementFunctionToken) for this activity --> {}", wupToken);
        WUPComponentNames nameSet = new WUPComponentNames(wupToken);
        getLogger().trace(".interceptionRedirectDecisionPoint(): So, we will now determine if the Packet should be forwarded or discarded");
        if (isBeingRedirected) {
            getLogger().debug(".interceptionRedirectDecisionPoint(): Returning null, as message is to be discarded (isToBeDiscarded == true)");
            metricsAgent.touchLastActivityFinishInstant();
            //
            // 1st Redirect and get Result
            PetasosFulfillmentTaskSharedInstance petasosFulfillmentTaskSharedInstance = redirectTask(redirectionTarget, fulfillmentTask);
            TaskInterceptionType interception = new TaskInterceptionType();
            interception.setTargetParticipant(redirectionTarget);
            interception.setInterceptionRedirectInstant(Instant.now());
            fulfillmentTask.setTaskInterception(interception);
            PetasosFulfillmentTaskSharedInstance postRedirectionFulfillmentTask = redirectTask(redirectionTarget, fulfillmentTask);
            postRedirectionFulfillmentTask.getTaskInterception().setInterceptionReturnInstant(Instant.now());
            //
            // Then forward to return-point
            String targetEndpoint = nameSet.getEndPointWUPContainerInterceptionReturnPointIngres();
            routeInjector.sendBody(targetEndpoint, ExchangePattern.InOnly, postRedirectionFulfillmentTask);
        } else {
            getLogger().trace(".interceptionRedirectDecisionPoint(): We return the channel name of the associated WUP Ingres Conduit");
            String targetEndpoint = nameSet.getEndPointWUPIngresConduitIngres();
            getLogger().debug(".interceptionRedirectDecisionPoint(): Returning route to the WUP Ingres Conduit instance --> {}", targetEndpoint);
            routeInjector.sendBody(targetEndpoint, ExchangePattern.InOnly, fulfillmentTask);
        }
    }

    public PetasosFulfillmentTaskSharedInstance redirectTask(String targetParticipantName, PetasosFulfillmentTaskSharedInstance fulfillmentTask){
        return(fulfillmentTask);
    }
}
