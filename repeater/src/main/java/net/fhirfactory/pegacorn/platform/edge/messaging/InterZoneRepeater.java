/*
 * Copyright (c) 2021 Mark A. Hunter (ACT Health)
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
package net.fhirfactory.pegacorn.platform.edge.messaging;

import java.sql.Date;
import java.time.Instant;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jgroups.stack.GossipRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// import net.fhirfactory.pegacorn.platform.edge.itops.PetasosITOpsService;
import net.fhirfactory.pegacorn.platform.edge.itops.configuration.JGroupsGossipRouterNode;

public class InterZoneRepeater{
    private static final Logger LOG = LoggerFactory.getLogger(InterZoneRepeater.class);

    GossipRouter interzoneRepeaterIPC;
    GossipRouter interzoneRepeaterTopology;
    GossipRouter interzoneRepeaterSubscriptions;
    GossipRouter interzoneRepeaterMetrics;
    GossipRouter interzoneRepeaterInterception;
    GossipRouter interzoneRepeaterInfinispan;
    GossipRouter interzoneRepeaterAudit;
    GossipRouter interzoneRepeaterTasking;
    GossipRouter interzoneRepeaterDatagrid;
    // PetasosITOpsService petasosServices;
    JGroupsGossipRouterNode gossipRouterNode;

    //
    // Business Methods
    //

    public void run(){
        getLogger().debug("InterZoneRepeater::run(): Entry");
        gossipRouterNode = new JGroupsGossipRouterNode();
        // petasosServices = new PetasosITOpsService(gossipRouterNode);
        // petasosServices.start();
        initialiseRepeater();
        eventLoop();
    }

    private void initialiseRepeater(){

        //
        // Initialise IPC Repeater
        String interzoneRepeaterIPCIPAddress = gossipRouterNode.getPropertyFile().getMultizoneRepeaterIPC().getServerHostname();
        int interzoneRepeaterIPCPortNumber = gossipRouterNode.getPropertyFile().getMultizoneRepeaterIPC().getServerPort();
        this.interzoneRepeaterIPC = new GossipRouter(interzoneRepeaterIPCIPAddress, interzoneRepeaterIPCPortNumber);
        try {
            getInterzoneRepeaterIPC().start();
        } catch (Exception e) {
            getLogger().error(".initialiseRepeater(): Error, can not initialise IPC Repeater -->{}", ExceptionUtils.getStackTrace(e));
        }
        getLogger().info("initialiseRepeater(): interzoneRepeaterIPC Bound To = Address->{}, Port->{}",getInterzoneRepeaterIPC().bindAddress(), getInterzoneRepeaterIPC().port());

        //
        // Initialise Topology Repeater
        String interzoneRepeaterTopologyIPAddress = gossipRouterNode.getPropertyFile().getMultizoneRepeaterTopology().getServerHostname();
        int interzoneRepeaterTopologyPortNumber = gossipRouterNode.getPropertyFile().getMultizoneRepeaterTopology().getServerPort();
        this.interzoneRepeaterTopology = new GossipRouter(interzoneRepeaterTopologyIPAddress, interzoneRepeaterTopologyPortNumber);
        try {
            getInterzoneRepeaterTopology().start();
        } catch (Exception e) {
            getLogger().error(".initialiseRepeater(): Error, can not initialise Topology Repeater -->{}", ExceptionUtils.getStackTrace(e));
        }
        getLogger().info("initialiseRepeater(): interzoneRepeaterTopology Bound To = Address->{}, Port->{}",getInterzoneRepeaterTopology().bindAddress(), getInterzoneRepeaterTopology().port());

        //
        // Initialise Subscription Repeater
        String interzoneRepeaterSubscriptionIPAddress = gossipRouterNode.getPropertyFile().getMultizoneRepeaterSubscriptions().getServerHostname();
        int interzoneRepeaterSubscriptionPortNumber = gossipRouterNode.getPropertyFile().getMultizoneRepeaterSubscriptions().getServerPort();
        this.interzoneRepeaterSubscriptions = new GossipRouter(interzoneRepeaterSubscriptionIPAddress, interzoneRepeaterSubscriptionPortNumber);
        try {
            getInterzoneRepeaterSubscriptions().start();
        } catch (Exception e) {
            getLogger().error(".initialiseRepeater(): Error, can not initialise Subscriptions Repeater -->{}", ExceptionUtils.getStackTrace(e));
        }
        getLogger().info("initialiseRepeater(): interzoneRepeaterSubscriptions Bound To = Address->{}, Port->{}",getInterzoneRepeaterSubscriptions().bindAddress(), getInterzoneRepeaterSubscriptions().port());

        //
        // Initialise Metrics Repeater
        String interzoneRepeaterMetricsIPAddress = gossipRouterNode.getPropertyFile().getMultizoneRepeaterMetrics().getServerHostname();
        int interzoneRepeaterMetricsPortNumber = gossipRouterNode.getPropertyFile().getMultizoneRepeaterMetrics().getServerPort();
        this.interzoneRepeaterMetrics = new GossipRouter(interzoneRepeaterMetricsIPAddress, interzoneRepeaterMetricsPortNumber);
        try {
            getInterzoneRepeaterMetrics().start();
        } catch (Exception e) {
            getLogger().error(".initialiseRepeater(): Error, can not initialise Metrics Repeater -->{}", ExceptionUtils.getStackTrace(e));
        }
        getLogger().info("initialiseRepeater(): interzoneRepeaterMetrics Bound To = Address->{}, Port->{}",getInterzoneRepeaterMetrics().bindAddress(), getInterzoneRepeaterMetrics().port());

        //
        // Initialise Interception Repeater
        String interzoneRepeaterInterceptionIPAddress = gossipRouterNode.getPropertyFile().getMultizoneRepeaterInterception().getServerHostname();
        int interzoneRepeaterInterceptionPortNumber = gossipRouterNode.getPropertyFile().getMultizoneRepeaterInterception().getServerPort();
        this.interzoneRepeaterInterception = new GossipRouter(interzoneRepeaterInterceptionIPAddress, interzoneRepeaterInterceptionPortNumber);
        try {
            getInterzoneRepeaterInterception().start();
        } catch (Exception e) {
            getLogger().error(".initialiseRepeater(): Error, can not initialise Interception Repeater -->{}", ExceptionUtils.getStackTrace(e));
        }
        getLogger().info("initialiseRepeater(): interzoneRepeaterInterception Bound To = Address->{}, Port->{}",getInterzoneRepeaterInterception().bindAddress(), getInterzoneRepeaterInterception().port());

        //
        // Initialise Infinispan Repeater
        String interzoneRepeaterInfinispanIPAddress = gossipRouterNode.getPropertyFile().getMultizoneRepeaterInfinspan().getServerHostname();
        int interzoneRepeaterInfinispanPortNumber = gossipRouterNode.getPropertyFile().getMultizoneRepeaterInfinspan().getServerPort();
        this.interzoneRepeaterInfinispan = new GossipRouter(interzoneRepeaterInfinispanIPAddress, interzoneRepeaterInfinispanPortNumber);
        try {
            getInterzoneRepeaterInfinispan().start();
        } catch (Exception e) {
            getLogger().error(".initialiseRepeater(): Error, can not initialise Infinispan Repeater -->{}", ExceptionUtils.getStackTrace(e));
        }
        getLogger().info("initialiseRepeater(): interzoneRepeaterInfinispan Bound To = Address->{}, Port->{}",getInterzoneRepeaterInfinispan().bindAddress(), getInterzoneRepeaterInfinispan().port());

        //
        // Initialise Audit Repeater
        String interzoneRepeaterAuditIPAddress = gossipRouterNode.getPropertyFile().getMultizoneRepeaterAudit().getServerHostname();
        int interzoneRepeaterAuditPortNumber = gossipRouterNode.getPropertyFile().getMultizoneRepeaterAudit().getServerPort();
        this.interzoneRepeaterAudit = new GossipRouter(interzoneRepeaterAuditIPAddress, interzoneRepeaterAuditPortNumber);
        try {
            getInterzoneRepeaterAudit().start();
        } catch (Exception e) {
            getLogger().error(".initialiseRepeater(): Error, can not initialise Audit Repeater -->{}", ExceptionUtils.getStackTrace(e));
        }
        getLogger().info("initialiseRepeater(): interzoneRepeaterAudit Bound To = Address->{}, Port->{}",getInterzoneRepeaterAudit().bindAddress(), getInterzoneRepeaterAudit().port());

        //
        // Initialise Tasking Repeater
        String interzoneRepeaterTaskingIPAddress = gossipRouterNode.getPropertyFile().getMultizoneRepeaterTasking().getServerHostname();
        int interzoneRepeaterTaskingPortNumber = gossipRouterNode.getPropertyFile().getMultizoneRepeaterTasking().getServerPort();
        this.interzoneRepeaterTasking = new GossipRouter(interzoneRepeaterTaskingIPAddress, interzoneRepeaterTaskingPortNumber);
        try {
            getInterzoneRepeaterTasking().start();
        } catch (Exception e) {
            getLogger().error(".initialiseRepeater(): Error, can not initialise Tasking Repeater -->{}", ExceptionUtils.getStackTrace(e));
        }
        getLogger().info("initialiseRepeater(): interzoneRepeaterTasking Bound To = Address->{}, Port->{}",getInterzoneRepeaterTasking().bindAddress(), getInterzoneRepeaterTasking().port());

        //
        // Initialise Tasking Repeater
        String interzoneRepeaterDatagridIPAddress = gossipRouterNode.getPropertyFile().getMultizoneRepeaterDatagrid().getServerHostname();
        int interzoneRepeaterDatagridPortNumber = gossipRouterNode.getPropertyFile().getMultizoneRepeaterDatagrid().getServerPort();
        this.interzoneRepeaterDatagrid = new GossipRouter(interzoneRepeaterDatagridIPAddress, interzoneRepeaterDatagridPortNumber);
        try {
            getInterzoneRepeaterDatagrid().start();
        } catch (Exception e) {
            getLogger().error(".initialiseRepeater(): Error, can not initialise Datagrid Repeater -->{}", ExceptionUtils.getStackTrace(e));
        }
        getLogger().info("initialiseRepeater(): interzoneRepeaterDatagrid Bound To = Address->{}, Port->{}",getInterzoneRepeaterDatagrid().bindAddress(), getInterzoneRepeaterDatagrid().port());

    }

    private void eventLoop(){
        while(true) {
            try {
                Thread.sleep(300000);
                // petasosServices.updateDate();
                printSomeStatistics();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void printSomeStatistics(){
        getLogger().info(".printSomeStatistics(): Print Details(" + Date.from(Instant.now()).toString() +")");
        getLogger().info(".printSomeStatistics(): ---Interzone Repeater IPC---");
        getLogger().info(".printSomeStatistics(): Addressing Mappings ->{}", getInterzoneRepeaterIPC().dumpAddresssMappings());
        getLogger().info(".printSomeStatistics(): Routing Table ->{}", getInterzoneRepeaterIPC().dumpRoutingTable());
        getLogger().info(".printSomeStatistics(): ---Interzone Repeater Topology---");
        getLogger().info(".printSomeStatistics(): Addressing Mappings ->{}", getInterzoneRepeaterTopology().dumpAddresssMappings());
        getLogger().info(".printSomeStatistics(): Routing Table ->{}", getInterzoneRepeaterTopology().dumpRoutingTable());
        getLogger().info(".printSomeStatistics(): ---Interzone Repeater Subscriptions---");
        getLogger().info(".printSomeStatistics(): Addressing Mappings ->{}", getInterzoneRepeaterSubscriptions().dumpAddresssMappings());
        getLogger().info(".printSomeStatistics(): Routing Table ->{}", getInterzoneRepeaterSubscriptions().dumpRoutingTable());
        getLogger().info(".printSomeStatistics(): ---Interzone Repeater Metrics---");
        getLogger().info(".printSomeStatistics(): Addressing Mappings ->{}", getInterzoneRepeaterMetrics().dumpAddresssMappings());
        getLogger().info(".printSomeStatistics(): Routing Table ->{}", getInterzoneRepeaterMetrics().dumpRoutingTable());
        getLogger().info(".printSomeStatistics(): ---Interzone Repeater Interception---");
        getLogger().info(".printSomeStatistics(): Addressing Mappings ->{}", getInterzoneRepeaterInterception().dumpAddresssMappings());
        getLogger().info(".printSomeStatistics(): Routing Table ->{}", getInterzoneRepeaterInterception().dumpRoutingTable());
        getLogger().info(".printSomeStatistics(): ---Interzone Repeater Infinispan---");
        getLogger().info(".printSomeStatistics(): Addressing Mappings ->{}", getInterzoneRepeaterInfinispan().dumpAddresssMappings());
        getLogger().info(".printSomeStatistics(): Routing Table ->{}", getInterzoneRepeaterInfinispan().dumpRoutingTable());
        getLogger().info(".printSomeStatistics(): ---Interzone Repeater Audit---");
        getLogger().info(".printSomeStatistics(): Addressing Mappings ->{}", getInterzoneRepeaterAudit().dumpAddresssMappings());
        getLogger().info(".printSomeStatistics(): Routing Table ->{}", getInterzoneRepeaterAudit().dumpRoutingTable());
        getLogger().info(".printSomeStatistics(): ---Interzone Repeater Tasking---");
        getLogger().info(".printSomeStatistics(): Addressing Mappings ->{}", getInterzoneRepeaterTasking().dumpAddresssMappings());
        getLogger().info(".printSomeStatistics(): Routing Table ->{}", getInterzoneRepeaterTasking().dumpRoutingTable());
        getLogger().info(".printSomeStatistics(): ---Interzone Repeater Datagrid---");
        getLogger().info(".printSomeStatistics(): Addressing Mappings ->{}", getInterzoneRepeaterDatagrid().dumpAddresssMappings());
        getLogger().info(".printSomeStatistics(): Routing Table ->{}", getInterzoneRepeaterDatagrid().dumpRoutingTable());
    }

    //
    // Getters (and Setters)
    //

    protected GossipRouter getInterzoneRepeaterIPC() {
        return interzoneRepeaterIPC;
    }

    protected GossipRouter getInterzoneRepeaterTopology() {
        return interzoneRepeaterTopology;
    }

    protected GossipRouter getInterzoneRepeaterSubscriptions() {
        return interzoneRepeaterSubscriptions;
    }

    protected GossipRouter getInterzoneRepeaterMetrics() {
        return interzoneRepeaterMetrics;
    }

    protected GossipRouter getInterzoneRepeaterInterception() {
        return interzoneRepeaterInterception;
    }

    protected GossipRouter getInterzoneRepeaterInfinispan() {
        return interzoneRepeaterInfinispan;
    }

    protected GossipRouter getInterzoneRepeaterAudit() {
        return interzoneRepeaterAudit;
    }

    protected GossipRouter getInterzoneRepeaterTasking() {
        return interzoneRepeaterTasking;
    }

    protected GossipRouter getInterzoneRepeaterDatagrid() {
        return interzoneRepeaterDatagrid;
    }

    protected Logger getLogger(){
        return(LOG);
    }
}
