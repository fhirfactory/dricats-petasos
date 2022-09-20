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

import net.fhirfactory.dricats.petasos.tasking.caches.local.PetasosFulfillmentTaskSharedInstance;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;


@ApplicationScoped
public class WUPContainerInterceptionReturnPoint {
    private static final Logger LOG = LoggerFactory.getLogger(WUPContainerInterceptionReturnPoint.class);

    protected Logger getLogger(){
        return(LOG);
    }


    /**
     * The return point for redirected tasks (if redirection is active) - otherwise just a passthrough!
     *
     * @param fulfillmentTask     The PetasosFulfillmentTaskSharedInstance that is relevant for this thread
     * @param camelExchange    The Apache Camel Exchange object, used to store a Semaphore as we iterate through Dynamic Route options
     * @return A PetasosFulfillmentTaskSharedInstance to be passed on
     */
    public PetasosFulfillmentTaskSharedInstance returnRedirectedTask(PetasosFulfillmentTaskSharedInstance fulfillmentTask, Exchange camelExchange) {
        if(getLogger().isInfoEnabled()){
            getLogger().info(".returnRedirectedTask(): Entry, fulfillmentTaskId/ActionableTaskId->{}/{}", fulfillmentTask.getTaskId(), fulfillmentTask.getActionableTaskId());
        }
        getLogger().debug(".returnRedirectedTask(): Entry, fulfillmentTask->{}", fulfillmentTask);
        //
        // I am a nothing bean - merely a "point" to "point to". I may, at a later date, have some logic and
        // reporting in me --> but for now --> nothing :( I feel like such a disappointment.
        //
        getLogger().debug(".returnRedirectedTask(): Exit, fulfillmentTask->{}", fulfillmentTask);
        return(fulfillmentTask);
    }
}
