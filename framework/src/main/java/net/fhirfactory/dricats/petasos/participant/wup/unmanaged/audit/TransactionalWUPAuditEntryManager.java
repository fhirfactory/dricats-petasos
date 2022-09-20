/*
 * Copyright (c) 2020 Mark A. Hunter
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
package net.fhirfactory.dricats.petasos.participant.wup.unmanaged.audit;

import ca.uhn.fhir.parser.IParser;
import net.fhirfactory.dricats.constants.systemwide.DeploymentSystemIdentificationInterface;
import net.fhirfactory.dricats.model.petasos.dataparcel.DataParcelManifest;
import net.fhirfactory.dricats.model.petasos.dataparcel.DataParcelTypeDescriptor;
import net.fhirfactory.dricats.model.petasos.dataparcel.valuesets.DataParcelTypeEnum;
import net.fhirfactory.dricats.model.petasos.resilience.activitymatrix.sta.TransactionStatusElement;
import net.fhirfactory.dricats.model.petasos.uow.UoW;
import net.fhirfactory.dricats.model.petasos.uow.UoWPayload;
import net.fhirfactory.dricats.model.petasos.uow.UoWProcessingOutcomeEnum;
import net.fhirfactory.dricats.model.petasos.wup.PetasosTaskJobCard;
import net.fhirfactory.dricats.model.transaction.valuesets.PegacornTransactionTypeEnum;
import net.fhirfactory.dricats.internals.fhir.r4.internal.topics.FHIRElementTopicFactory;
import net.fhirfactory.pegacorn.petasos.tasking.audit.brokers.STAServicesAuditBroker;
import net.fhirfactory.pegacorn.util.FHIRContextUtility;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class TransactionalWUPAuditEntryManager {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionalWUPAuditEntryManager.class);

    private static final String FHIR_VERSION = "4.0.1";
    
    @Inject 
    private FHIRContextUtility FHIRContextUtility;
    
    private IParser parserR4;

    @Inject
    private FHIRElementTopicFactory topicIDBuilder;

    @Inject
    private STAServicesAuditBroker servicesBroker;

    @Inject
    private DeploymentSystemIdentificationInterface systemIdentificationInterface;

    @PostConstruct
    protected void initialise() {
        LOG.debug(".initialise(): Entry");
        this.parserR4 = FHIRContextUtility.getJsonParser();
        LOG.debug(".initialise(): Exit");
    }


    public TransactionStatusElement beginTransaction(PetasosTaskJobCard jobCard, String auditCommentary, String resourceType, Resource fhirResource, PegacornTransactionTypeEnum action) {
        LOG.debug(".beginTransaction(): Entry, jobCard->{}, auditCommentary->{}, fhriResource->{}, action->{}", jobCard, auditCommentary, fhirResource, action);
        LOG.trace(".beginTransaction(): Create the UoW for accessor utilisation");
        UoWPayload payload = new UoWPayload();
        boolean encodingFailure = false;
        String errorString = "";
        String auditTrailPayload = "";
        switch (action) {
            case REVIEW:
                LOG.trace(".endTransaction(): Review/Get --> Logging the request");
                auditTrailPayload = "Action: Get --> ";
                break;
            case CREATE:
                LOG.trace(".endTransaction(): Create --> Logging the request");
                auditTrailPayload = "Action: Create --> ";
                break;
            case DELETE:
                LOG.trace(".endTransaction(): Delete --> Logging the request");
                auditTrailPayload = "Action: Delete --> ";
                break;
            case UPDATE:
                LOG.trace(".endTransaction(): Update --> Logging the request");
                auditTrailPayload = "Action: Update --> ";
                break;
        }
        if(fhirResource != null) {
            LOG.trace(".beginTransaction(): Converting FHIR element into a (JSON) String");
            String resourceAsString = null;
            try {
                LOG.trace(".beginTransaction(): Using IParser --> {}", parserR4);
                resourceAsString = parserR4.encodeResourceToString(fhirResource);
                LOG.trace(".beginTransaction(): Add JSON String (encoded FHIR element) to the UoWPayload");
                String fullPayloadString = auditTrailPayload + resourceAsString;
                payload.setPayload(fullPayloadString);
                LOG.trace(".beginTransaction(): Construct a TopicToken to describe the payload & add it to the Payload");
                DataParcelTypeDescriptor payloadToken = topicIDBuilder.newTopicToken(resourceType, FHIR_VERSION);
                DataParcelManifest payloadManifest = new DataParcelManifest();
                payloadManifest.setContentDescriptor(payloadToken);
                payloadManifest.setDataParcelType(DataParcelTypeEnum.GENERAL_DATA_PARCEL_TYPE);
                payload.setPayloadManifest(payloadManifest);
            } catch (Exception Ex) {
                LOG.info(".beginTransaction(): Failed to Encode --> {}", Ex.toString());
                errorString = Ex.toString();
                encodingFailure = true;
            }
        } else {
            String fullPayloadString = auditTrailPayload + auditCommentary;
            payload.setPayload(fullPayloadString);
            LOG.trace(".beginTransaction(): Construct a TopicToken to describe the payload & add it to the Payload");
            DataParcelTypeDescriptor payloadToken = topicIDBuilder.newTopicToken(resourceType, FHIR_VERSION);
            DataParcelManifest payloadManifest = new DataParcelManifest();
            payloadManifest.setContentDescriptor(payloadToken);
            payloadManifest.setDataParcelType(DataParcelTypeEnum.GENERAL_DATA_PARCEL_TYPE);
        }
        UoW theUoW;
        if (encodingFailure) {
            LOG.trace(".beginTransaction(): Failed to encode incoming content....");
            payload.setPayload("Error encoding content --> " + errorString);
            DataParcelTypeDescriptor typeDescriptor = new DataParcelTypeDescriptor();
            typeDescriptor.setDataParcelDefiner(systemIdentificationInterface.getSystemName());
            typeDescriptor.setDataParcelCategory("DataTypes");
            typeDescriptor.setDataParcelSubCategory("Error");
            typeDescriptor.setDataParcelResource("JSONConversionErrorMessage");
            typeDescriptor.setVersion("1.0.0");
            DataParcelManifest payloadManifest = new DataParcelManifest();
            payloadManifest.setContentDescriptor(typeDescriptor);
            payloadManifest.setDataParcelType(DataParcelTypeEnum.GENERAL_DATA_PARCEL_TYPE);
            LOG.trace(".beginTransaction(): Create the UoW with the fhirResource/TopicToken as the Ingres Payload");
            theUoW = new UoW(payload);
            theUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
        } else {
            LOG.trace(".beginTransaction(): Instantiate the UoW with the fhirResource/TopicToken as the Ingres Payload");
            theUoW = new UoW(payload);
            theUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_NOTSTARTED);
        }
        TransactionStatusElement currentTransaction = servicesBroker.logAPIActivity(jobCard, theUoW, action);
        LOG.debug(".beginTransaction(): Exit --> Registration aftermath: currentTransaction (PetasosParcelAuditTrailEntry) --> {}", currentTransaction);
        return (currentTransaction);
    }

    public TransactionStatusElement beginTransaction(PetasosTaskJobCard jobCard, UoW uow, PegacornTransactionTypeEnum action) {
        TransactionStatusElement currentTransaction = servicesBroker.logAPIActivity(jobCard, uow, action);
        return(currentTransaction);
    }

    public void updateTransaction(TransactionStatusElement transactionStatusElement){

    }

    public void endTransaction(TransactionStatusElement transactionStatus) {
        LOG.debug(".endTransaction(): Entry");
/*        UoW updatedUoW = startingTransaction.getActualUoW();
        String auditTrailPayload = null;
        if (success) {
            switch (action) {
                case REVIEW:
                    LOG.trace(".endTransaction(): Successful Review/Get --> Logging the outcome");
                    auditTrailPayload = "Action: Get, Result --> ";
                    break;
                case  UPDATE:
                    LOG.trace(".endTransaction(): Successful Update --> Logging the outcome");
                    auditTrailPayload = "Action: Update, Result --> ";
                    break;
                case  CREATE:
                    LOG.trace(".endTransaction(): Successful Create --> Logging the outcome");
                    auditTrailPayload = "Action: Create, Result --> ";
                    break;
                case  DELETE:
                    LOG.trace(".endTransaction(): Successful Delete --> Logging the outcome");
                    auditTrailPayload = "Action: Delete, Result --> ";
                    break;
                case  SEARCH:
                    LOG.trace(".endTransaction(): Successful Delete --> Logging the outcome");
                    auditTrailPayload = "Action: Search, Result --> ";
                    break;
            }
            if(fhirResource != null) {
                LOG.trace(".endTransaction(): fhirResource.type --> {}", fhirResource.getResourceType());
                if(parserR4 == null) {LOG.error("Warning Will Robinson!!!!");}
                auditTrailPayload = auditTrailPayload  + parserR4.encodeResourceToString(fhirResource);
            } else {
                auditTrailPayload = auditTrailPayload + auditEntryString;
            }
            UoWPayload newPayload = new UoWPayload();
            newPayload.setPayload(auditTrailPayload);
            TopicToken payloadToken = topicIDBuilder.createTopicToken(resourceType, version);
            newPayload.setPayloadTopicID(payloadToken);
            updatedUoW.getEgressContent().addPayloadElement(newPayload);
            updatedUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_SUCCESS);
            LOG.trace(".endTransaction(): Calling the Audit Trail Generator ");
            PetasosParcelAuditTrailEntry currentTransaction = servicesBroker.transactionAuditEntry(wupInstance, action.toString(), updatedUoW, startingTransaction);
        } else {
            updatedUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
            LOG.trace(".endTransaction(): Calling the Audit Trail Generator ");
            PetasosParcelAuditTrailEntry currentTransaction = servicesBroker.transactionAuditEntry(wupInstance, action.toString(), updatedUoW, startingTransaction);
        }
*/        LOG.debug(".endTransaction(): exit, my work is done!");
    }
}
