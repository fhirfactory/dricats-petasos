package net.fhirfactory.dricats.petasos.tasking.observations.synchronisation.worker.common;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;

public abstract class AgentWorkerBase extends RouteBuilder {

    //
    // Constructor(s)
    //

    public AgentWorkerBase(){
        super();
    }

    //
    // Abstract Methods
    //

    abstract protected String getFriendlyName();

    //
    // Class Kickstarter
    //

    @Override
    public void configure() throws Exception {
        String name = getFriendlyName();

        from("timer://"+name+"?delay=1000&repeatCount=1")
                .routeId(getClass().getName())
                .log(LoggingLevel.DEBUG, "Starting....");
    }
}
