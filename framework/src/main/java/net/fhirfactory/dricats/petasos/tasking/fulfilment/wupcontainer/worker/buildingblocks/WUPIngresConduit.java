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
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoW;
import net.fhirfactory.dricats.petasos.tasking.fulfilment.naming.PetasosPathwayExchangePropertyNames;
import net.fhirfactory.dricats.petasos.tasking.caches.local.PetasosFulfillmentTaskSharedInstance;
import org.apache.camel.Exchange;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * @author Mark A. Hunter
 * @since 2020-07-05
 */
@ApplicationScoped
public class WUPIngresConduit {

    @Inject
    PetasosPathwayExchangePropertyNames exchangePropertyNames;

    private static final Logger LOG = LoggerFactory.getLogger(WUPIngresConduit.class);
    protected Logger getLogger(){
        return(LOG);
    }

    /**
     * This function strips the WUPJobCard and ParcelStatusElement from the ingresParcel, and injects them into the
     * Camel Exchange element for extraction by the WUPEgressConduit module. It then extracts the actual UoW and
     * returns this for forwarding into the WUP itself. This way, the only thing the Business Logic developer need
     * worry about is the UoW on which they are acting.
     *
     * @param fulfillmentTask The WorkUnitTransportPacket for the associated UoW - containing the WUPJobCard & ParcelStatusElement for the activity
     * @param camelExchange The Apache Camel Exchange object, for injecting the WUPJobCard & ParcelStatusElement into
     * @return A UoW (Unit of Work) object for injection into the WUP for processing by the Business Logic
     */
    public UoW forwardIntoWUP(PetasosFulfillmentTaskSharedInstance fulfillmentTask, Exchange camelExchange){
        if(getLogger().isInfoEnabled()){
            getLogger().info(".forwardIntoWUP(): Entry, fulfillmentTaskId/ActionableTaskId->{}/{}", fulfillmentTask.getTaskId(), fulfillmentTask.getActionableTaskId());
        }
        getLogger().debug(".forwardIntoWUP(): Entry, fulfillmentTask->{}", fulfillmentTask);
        //
        // Extract and Clone the UoW
        UoW theUoW = SerializationUtils.clone(fulfillmentTask.getTaskWorkItem());
        //
        // Inject the fulfillment task into the Camel Exchange for extraction on the other side of the WUP
        camelExchange.setProperty(PetasosPropertyConstants.WUP_PETASOS_FULFILLMENT_TASK_EXCHANGE_PROPERTY, fulfillmentTask);

        //
        // Brute force logging, using the Tasks or AuditTrail repositories for this information normally.
        getLogger().trace("ProcessingMessage->{}", theUoW.getIngresContent().getPayload());

        //
        // All done, return the UoW which will be passed to the WUP
        getLogger().debug(".forwardIntoWUP(): Exit, returning the UoW --> {}", theUoW);
        return(theUoW);
    }
}
