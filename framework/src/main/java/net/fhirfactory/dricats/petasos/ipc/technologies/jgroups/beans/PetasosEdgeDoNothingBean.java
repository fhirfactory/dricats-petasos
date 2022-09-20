/*
 * Copyright (c) 2021 Mark A. Hunter
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
package net.fhirfactory.dricats.petasos.ipc.technologies.jgroups.beans;

import javax.enterprise.context.ApplicationScoped;

import org.apache.camel.Exchange;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.work.UoW;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.work.UoWPayload;
import net.fhirfactory.dricats.model.petasos.tasking.definition.datatypes.work.UoWProcessingOutcomeEnum;

@ApplicationScoped
public class PetasosEdgeDoNothingBean {
    private static final Logger LOG = LoggerFactory.getLogger(PetasosEdgeDoNothingBean.class);

    protected Logger getLogger(){
        return(LOG);
    }

    ObjectMapper jsonMapper;

    public PetasosEdgeDoNothingBean(){
        jsonMapper = new ObjectMapper();
    }

    public UoW doNothing(UoW uow, Exchange camelExchange){
        getLogger().debug(".enforceInboundPolicy(): Entry, uow->{}", uow);
        if(uow == null){
            return(null);
        }
        if(!uow.hasIngresContent()){
            return(uow);
        }
        UoWPayload clonedIngresPayload = SerializationUtils.clone(uow.getIngresContent());
        clonedIngresPayload.getPayloadManifest().getContentDescriptor().setDataParcelDiscriminatorType("DoNothingPassThroughFlag");
        clonedIngresPayload.getPayloadManifest().getContentDescriptor().setDataParcelDiscriminatorValue("Done");
        uow.getEgressContent().addPayloadElement(clonedIngresPayload);
        uow.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_SUCCESS);
        return(uow);
    }
}
