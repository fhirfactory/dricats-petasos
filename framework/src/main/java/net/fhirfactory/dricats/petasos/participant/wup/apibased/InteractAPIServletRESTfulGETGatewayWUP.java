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

package net.fhirfactory.dricats.petasos.participant.wup.apibased;

import net.fhirfactory.pegacorn.petasos.tasking.fulfilment.wupcontainer.worker.archetypes.GenericSTAServerWUPTemplate;
import net.fhirfactory.dricats.model.petasos.uow.UoW;
import net.fhirfactory.dricats.model.petasos.wup.valuesets.WUPClusterModeEnum;
import net.fhirfactory.dricats.model.petasos.wup.valuesets.WUPSystemModeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class InteractAPIServletRESTfulGETGatewayWUP extends GenericSTAServerWUPTemplate {
    private static final Logger LOG = LoggerFactory.getLogger(InteractAPIServletRESTfulGETGatewayWUP.class);

    protected Logger getLogger(){
        return(LOG);
    }
   
    public InteractAPIServletRESTfulGETGatewayWUP(){
        super();
    }

    public void registerActivityStart(UoW unitOfWork, WUPClusterModeEnum clusterMode, WUPSystemModeEnum systemMode){
        getLogger().debug(".registerActivityStart(): Entry, unitOfWork --> {}", unitOfWork);
//        ActivityID newActivityID = new ActivityID();
//        newActivityID.setPresentWUPFunctionToken(this.getWUP().getNodeFunctionFDN().getFunctionToken());
//        newActivityID.setPresentWUPIdentifier(this.getWUPIdentifier());
    }

    public void registerActivityFinish(UoW unitOfWork){
        getLogger().debug(".registerActivityFinish(): Entry, unitOfWork --> {}", unitOfWork);
    }
}
