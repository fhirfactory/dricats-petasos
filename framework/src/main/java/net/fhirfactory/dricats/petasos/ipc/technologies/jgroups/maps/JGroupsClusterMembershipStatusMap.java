/*
 * The MIT License
 *
 * Copyright 2022 Mark A. Hunter.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.fhirfactory.dricats.petasos.ipc.technologies.jgroups.maps;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.context.ApplicationScoped;
import net.fhirfactory.dricats.model.configuration.filebased.archetypes.valuesets.ClusterFunctionNameEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author MAHun
 */
@ApplicationScoped
public class JGroupsClusterMembershipStatusMap {
    private static final Logger LOG = LoggerFactory.getLogger(JGroupsClusterMembershipStatusMap.class);
    
    private ConcurrentHashMap<ClusterFunctionNameEnum, Set<String>> currentMembershipMap;
    private ConcurrentHashMap<ClusterFunctionNameEnum, Set<String>> removedMembersMap;
    private ConcurrentHashMap<ClusterFunctionNameEnum, Set<String>> addedMembersMap;
    private Object currentMembershipMapLock;
    private Object removedMembersMapLock;
    private Object addedMembersMapLock;
    private Instant lastRemovedVerifiedInstant;
    private Instant lastAddedVerifiedInstant;
    
    //
    // Constructor(s)
    //
    
    public JGroupsClusterMembershipStatusMap(){
        this.currentMembershipMap = new ConcurrentHashMap<>();
        this.removedMembersMap = new ConcurrentHashMap<>();
        this.addedMembersMap = new ConcurrentHashMap<>();
        this.currentMembershipMapLock = new Object();
        this.removedMembersMapLock = new Object();
        this.addedMembersMapLock = new Object();
        this.lastAddedVerifiedInstant = Instant.now();
        this.lastRemovedVerifiedInstant = Instant.now();
    }
    
    //
    // Getters (and Setters)
    //

    protected static Logger getLogger() {
        return LOG;
    }

    protected ConcurrentHashMap<ClusterFunctionNameEnum, Set<String>> getCurrentMembershipMap() {
        return currentMembershipMap;
    }

    protected ConcurrentHashMap<ClusterFunctionNameEnum, Set<String>> getRemovedMembersMap() {
        return removedMembersMap;
    }

    protected ConcurrentHashMap<ClusterFunctionNameEnum, Set<String>> getAddedMembersMap() {
        return addedMembersMap;
    }

    protected Object getCurrentMembershipMapLock() {
        return currentMembershipMapLock;
    }

    protected Object getRemovedMembersMapLock() {
        return removedMembersMapLock;
    }

    protected Object getAddedMembersMapLock() {
        return addedMembersMapLock;
    }

    public Instant getLastRemovedVerifiedInstant() {
        return lastRemovedVerifiedInstant;
    }

    public Instant getLastAddedVerifiedInstant() {
        return lastAddedVerifiedInstant;
    }
    
    public void updateLastRemovedVerifiedInstant(){
        this.lastAddedVerifiedInstant = Instant.now();
    }
    
    public void updateLastAddedVerifiedInstant(){
        this.lastRemovedVerifiedInstant = Instant.now();
    }
    
    //
    // Business Methods
    //
}
