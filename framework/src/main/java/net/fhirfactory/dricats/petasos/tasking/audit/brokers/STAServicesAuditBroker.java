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
package net.fhirfactory.dricats.petasos.tasking.audit.brokers;

import net.fhirfactory.dricats.petasos.tasking.audit.transformers.PetasosFulfillmentTask2FHIRAuditEvent;
import net.fhirfactory.pegacorn.core.interfaces.auditing.PetasosAuditEventServiceAgentInterface;
import net.fhirfactory.pegacorn.core.model.petasos.resilience.activitymatrix.sta.TransactionStatusElement;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoW;
import net.fhirfactory.pegacorn.core.model.petasos.wup.PetasosTaskJobCard;
import net.fhirfactory.pegacorn.core.model.transaction.valuesets.PegacornTransactionTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class STAServicesAuditBroker {

    private static final Logger LOG = LoggerFactory.getLogger(STAServicesAuditBroker.class);

    @Inject
    PetasosAuditEventServiceAgentInterface auditWriter;

    @Inject
    PetasosFulfillmentTask2FHIRAuditEvent parcel2auditevent;

    public void logAPIActivity(TransactionStatusElement transactionStatus) {
        LOG.debug(".logAPIActivity(): Entry, transactionStatus->{}", transactionStatus);
        if (transactionStatus == null) {
            throw (new IllegalArgumentException(".logAPIActivity(): wup, action or theUoW are null"));
        }
    }

    public TransactionStatusElement logAPIActivity(PetasosTaskJobCard jobCard, UoW uow, PegacornTransactionTypeEnum transactionType){
        LOG.debug(".logAPIActivity(): Entry, jobCard->{}, uow->{}, transactionType->{}", jobCard, uow, transactionType);
        TransactionStatusElement statusElement = new TransactionStatusElement();
        return(statusElement);
    }
}
