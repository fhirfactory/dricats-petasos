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
import net.fhirfactory.dricats.model.petasos.tasking.routing.identification.WUPComponentNames;
import net.fhirfactory.dricats.petasos.tasking.caches.local.PetasosFulfillmentTaskSharedInstance;
import net.fhirfactory.dricats.petasos.tasking.observations.metrics.agents.WorkUnitProcessorMetricsAgent;
import org.apache.camel.Exchange;
import org.apache.camel.RecipientList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;


@ApplicationScoped
public class WUPContainerIngresGatekeeper {
    private static final Logger LOG = LoggerFactory.getLogger(WUPContainerIngresGatekeeper.class);

    protected Logger getLogger(){
        return(LOG);
    }


    /**
     * The WUP-Ingres-Gatekeeper  is the decision point about whether to actually process the PetasosFulfillmentTask
     * or directly forward it to the TASK_OUTCOME_COLLECTION_QUEUE. The later scenario typically signifies that another
     * MOA-WUP has successfully (or otherwise) completed processing the PetasosFulfillmentTask and, as such, this
     * instance should be discarded.
     *
     * @param fulfillmentTask     The PetasosFulfillmentTask that is to be forwarded to the Intersection (if all is OK)
     * @param camelExchange    The Apache Camel Exchange object, used to store a Semaphore as we iterate through Dynamic Route options
     * @return Should either return the ingres point into the associated WUP Ingres Conduit or PetasosPropertyConstants.TASK_OUTCOME_COLLECTION_QUEUE
     */
    @RecipientList
    public List<String> ingresGatekeeper(PetasosFulfillmentTaskSharedInstance fulfillmentTask, Exchange camelExchange) {
        if(getLogger().isInfoEnabled()){
            getLogger().info(".ingresGatekeeper(): Entry, fulfillmentTaskId/ActionableTaskId->{}/{}", fulfillmentTask.getTaskId(), fulfillmentTask.getActionableTaskId());
        }
        getLogger().debug(".ingresGatekeeper(): Enter, fulfillmentTask->{}", fulfillmentTask);
        // Get Route Names
        TopologyNodeFunctionFDNToken wupToken = fulfillmentTask.getTaskFulfillment().getFulfillerWorkUnitProcessor().getNodeFunctionFDN().getFunctionToken();
        getLogger().trace(".ingresGatekeeper(): wupFunctionToken (NodeElementFunctionToken) for this activity --> {}", wupToken);
        //
        // Get out metricsAgent & do add some metrics
        WorkUnitProcessorMetricsAgent metricsAgent = camelExchange.getProperty(PetasosPropertyConstants.WUP_METRICS_AGENT_EXCHANGE_PROPERTY, WorkUnitProcessorMetricsAgent.class);
        // Now, continue with business logic
        WUPComponentNames nameSet = new WUPComponentNames(wupToken);
        ArrayList<String> targetList = new ArrayList<String>();
        getLogger().trace(".ingresGatekeeper(): So, we will now determine if the Packet should be forwarded or discarded");
        if (fulfillmentTask.getTaskFulfillment().isToBeDiscarded()) {
            getLogger().debug(".ingresGatekeeper(): Returning null, as message is to be discarded (isToBeDiscarded == true)");
            metricsAgent.touchLastActivityFinishInstant();
            targetList.add(PetasosPropertyConstants.TASK_OUTCOME_COLLECTION_QUEUE);
            return (targetList);
        } else {
            getLogger().trace(".ingresGatekeeper(): We return the channel name of the associated WUP Interception Redirect Point");
            String targetEndpoint = nameSet.getEndPointWUPContainerInterceptionRedirectPointIngres();
            targetList.add(targetEndpoint);
            getLogger().debug(".ingresGatekeeper(): Returning route to the WUP Ingres Conduit instance --> {}", targetEndpoint);
            return (targetList);
        }
    }
}
