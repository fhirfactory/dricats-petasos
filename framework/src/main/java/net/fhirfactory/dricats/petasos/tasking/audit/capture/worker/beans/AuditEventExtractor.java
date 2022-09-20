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
package net.fhirfactory.dricats.petasos.tasking.audit.capture.worker.beans;

import ca.uhn.fhir.parser.IParser;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoW;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoWPayload;
import net.fhirfactory.pegacorn.util.FHIRContextUtility;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.AuditEvent;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class AuditEventExtractor {

    private IParser jsonFHIRParser;

    @Inject
    private FHIRContextUtility fhirContextUtility;

    @PostConstruct
    public void initialise(){
        jsonFHIRParser = fhirContextUtility.getJsonParser();
    }

    public AuditEvent extractAuditEvent(UoW uow){
        AuditEvent auditEvent = new AuditEvent();
        if(uow == null){
            return(auditEvent);
        }
        UoWPayload ingresContent = uow.getIngresContent();
        if (ingresContent == null) {
            return(auditEvent);
        }
        String payload = ingresContent.getPayload();
        if(StringUtils.isEmpty(payload)){
            return(auditEvent);
        }
        auditEvent = jsonFHIRParser.parseResource(AuditEvent.class, payload);
        return(auditEvent);
    }
}
