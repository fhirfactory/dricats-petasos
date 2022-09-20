package net.fhirfactory.dricats.petasos.tasking.fulfilment.wupcontainer.worker.archetypes;

import net.fhirfactory.dricats.petasos.tasking.fulfilment.wupcontainer.worker.archetypes.common.GenericSTAWUPTemplate;
import net.fhirfactory.pegacorn.core.model.petasos.wup.valuesets.WUPArchetypeEnum;

public abstract class GenericSTAClientWUPTemplate extends GenericSTAWUPTemplate {

    public GenericSTAClientWUPTemplate() {
        super();
    }

    @Override
    protected WUPArchetypeEnum specifyWUPArchetype(){
        return(WUPArchetypeEnum.WUP_NATURE_API_CLIENT);
    }
}
