package net.fhirfactory.dricats.petasos.tasking.fulfilment.wupcontainer.worker.archetypes;

import net.fhirfactory.dricats.petasos.tasking.fulfilment.wupcontainer.worker.archetypes.common.GenericSTAWUPTemplate;
import net.fhirfactory.pegacorn.core.model.petasos.wup.valuesets.WUPArchetypeEnum;

public abstract class GenericSTAServerWUPTemplate extends GenericSTAWUPTemplate {

    public GenericSTAServerWUPTemplate() {
        super();
    }

    @Override
    protected WUPArchetypeEnum specifyWUPArchetype(){
        return(WUPArchetypeEnum.WUP_NATURE_API_ANSWER);
    }
}
