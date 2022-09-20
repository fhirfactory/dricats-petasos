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
package net.fhirfactory.dricats.petasos.ipc.technologies.jgroups;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.jgroups.Address;
import org.jgroups.util.UUID;

import net.fhirfactory.dricats.interfaces.petasos.participant.topology.ProcessingPlantConfigurationServiceInterface;
import net.fhirfactory.dricats.model.component.datatypes.ComponentId;
import net.fhirfactory.dricats.model.component.valuesets.ComponentStatusEnum;
import net.fhirfactory.dricats.model.configuration.filebased.archetypes.PetasosClusterPropertyFile;
import net.fhirfactory.dricats.model.configuration.filebased.archetypes.common.WildflyBasedServerPropertyFile;
import net.fhirfactory.dricats.model.configuration.filebased.archetypes.valuesets.ClusterFunctionNameEnum;
import net.fhirfactory.dricats.model.petasos.ipc.endpoints.valuesets.JGroupsChannelStatusEnum;
import net.fhirfactory.dricats.model.petasos.ipc.technologies.jgroups.JGroupsChannelConnectorSummary;
import net.fhirfactory.dricats.model.petasos.ipc.technologies.jgroups.JGroupsChannelNamingUtilities;
import net.fhirfactory.dricats.model.petasos.participant.valuesets.NetworkSecurityZoneEnum;
import net.fhirfactory.dricats.petasos.ipc.technologies.jgroups.maps.JGroupsClusterConnectionStatusMap;

public abstract class JGroupsClusterConnection extends JGroupsChannel {

	private static final String VERSION = "1.0.0";
	private String uniqueQualifier;

	@Inject
	private ProcessingPlantConfigurationServiceInterface processingPlantConfigurationService;

	@Inject
	private JGroupsChannelNamingUtilities componentNameUtilities;
	
	@Inject
	private JGroupsClusterConnectionStatusMap localConnectionSetStatus;

	//
	// Constructor
	//

	public JGroupsClusterConnection() {
		super();
		this.uniqueQualifier = UUID.randomUUID().toString();
	}

	//
	// Abstract Methods
	//

	protected abstract ClusterFunctionNameEnum getClusterFunction();

	//
	// Getters and Setters
	//

	public ProcessingPlantConfigurationServiceInterface getProcessingPlant() {
		return processingPlantConfigurationService;
	}

	protected JGroupsChannelNamingUtilities getComponentNameUtilities() {
		return (componentNameUtilities);
	}
	
	protected String getUniqueQualifier() {
		return(this.uniqueQualifier);
	}
	
	protected JGroupsClusterConnectionStatusMap getLocalConnectionSetStatus() {
		return(localConnectionSetStatus);
	}

	//
	// Business Methods
	//

	/**
	 * This method gets all the members of a JGroups Cluster whose name CONTAINS the
	 * given service name parameter. A simple "contains()" string method is applied
	 * to each member retrieved from the JGroups View (where their address is
	 * converted to a String - via .toString()).
	 *
	 * @param processingPlantParticipantName
	 * @return a list of PetasosAdapterAddress elements for all the "members" of the
	 *         cluster with the same subsystem name.
	 */
	public List<Address> getGroupMembersForProcessingPlantParticipantName(String processingPlantParticipantName) {
		getLogger().debug(".getGroupMembersForProcessingPlantParticipantName(): Entry, subsystemName->{}", processingPlantParticipantName);
		List<Address> addressSet = new ArrayList<>();
		if (getChannel() == null) {
			getLogger().debug(".getGroupMembersForProcessingPlantParticipantName(): Exit, Channel is null, exit returning (null)");
			return (addressSet);
		}
		getLogger().debug(".getGroupMembersForProcessingPlantParticipantName(): Channel is NOT null, get updated Address set via view");
		List<Address> addressList = getAllViewMembers();
		synchronized (getCurrentScannedMembershipLock()) {
			for (Address currentAddress : addressList) {
				getLogger().debug(".getGroupMembersForProcessingPlantParticipantName(): Checking->{}", currentAddress);
				String currentSubsystemName = getComponentNameUtilities().getProcessingPlantParticipantNameFromChannelName(currentAddress.toString());
				if (currentSubsystemName.contains(processingPlantParticipantName)) {
					getLogger().debug(".getGroupMembersForProcessingPlantParticipantName(): contains subsystem name, adding to list");
					addressSet.add(currentAddress);
				}
			}
		}
		getLogger().debug(".getGroupMembersForProcessingPlantParticipantName(): Exit, addressSet->{}", addressSet);
		return (addressSet);
	}

	/**
	 * This method returns "the first" entry of the list of members (endpoints,
	 * channels) that belong to a particular Subsystem.
	 *
	 * @param processingPlantParticipantName
	 * @return
	 */
	public Address getTargetMemberAdapterInstanceForProcessingPlantParticipantName(String processingPlantParticipantName) {
		getLogger().debug(".getTargetMemberAdapterInstanceForProcessingPlantParticipantName(): Entry, processingPlantParticipantName->{}", processingPlantParticipantName);
		if (getChannel() == null) {
			getLogger().debug(".getTargetMemberAdapterInstanceForProcessingPlantParticipantName(): Exit, Channel is null, exit returning (null)");
			return (null);
		}
		if (StringUtils.isEmpty(processingPlantParticipantName)) {
			getLogger().debug(".getTargetMemberAdapterInstanceForProcessingPlantParticipantName(): Exit, subsystemName is null, exit returning (null)");
			return (null);
		}

		List<Address> potentialInterfaces = getGroupMembersForProcessingPlantParticipantName(processingPlantParticipantName);
		if (potentialInterfaces.isEmpty()) {
			getLogger().debug(".getTargetMemberAdapterInstanceForProcessingPlantParticipantName(): Exit, no available interfaces supporting function");
			return (null);
		} else {
			Address selectedInterface = potentialInterfaces.get(0);
			getLogger().debug(".getTargetMemberAdapterInstanceForProcessingPlantParticipantName(): Exit, selectedInterface->{}", selectedInterface);
			return (selectedInterface);
		}
	}

	/**
	 * This method gets all the members of a JGroups Cluster whose name begins with
	 * the given namePrefix parameter. A simple "startsWith()" string method is
	 * applied to each member retrieved from the JGroups View (where their address
	 * is converted to a String - via .toString()).
	 *
	 * @param namePrefix A string to be checked against using the String function
	 *                   "startsWith()"
	 * @return a list of String's representing all members whose name begins with
	 *         the given prefix
	 */
	public List<String> getClusterMemberSetBasedOnPrefix(String namePrefix) {
		getLogger().debug(".getClusterMemberSetBasedOnPrefix(): Entry, namePrefix->{}", namePrefix);
		List<String> memberListBasedOnPrefix = new ArrayList<>();
		if (getChannel() == null) {
			getLogger().debug(".getClusterMemberSetBasedOnPrefix(): Exit, Channel is null, returning empty set");
			return (memberListBasedOnPrefix);
		}
		if (StringUtils.isEmpty(namePrefix)) {
			getLogger().debug(".getClusterMemberSetBasedOnPrefix(): Exit, namePrefix is null, returning empty set");
			return (memberListBasedOnPrefix);
		}
		getLogger().trace(".getClusterMemberSetBasedOnPrefix(): Channel is NOT null & namePrefix is not empty, let's get updated Address set via view");
		List<String> memberList = this.getAllGroupMemberChannelNames();
		getLogger().trace(".getClusterMemberSetBasedOnPrefix(): Got the Address set via view, now iterate through and see if one is suitable");
		for (String currentMemberName : memberList) {
			getLogger().trace(".getClusterMemberSetBasedOnPrefix(): Iterating through Address list, current element->{}", currentMemberName);
			if (currentMemberName.startsWith(namePrefix)) {
				getLogger().debug(".getClusterMemberSetBasedOnPrefix(): currentMemberName is a match for given prefix, so adding it to list");
				memberListBasedOnPrefix.add(currentMemberName);
			}
		}
		getLogger().debug(".getClusterMemberSetBasedOnPrefix(): Exit, memberListBasedOnPrefix->{}", memberListBasedOnPrefix);
		return (memberListBasedOnPrefix);
	}

	/**
	 * This method returns the JGroups Address (interface) for the given cluster
	 * member name (memberName).
	 *
	 * @param memberName The Cluster Member name for which we would like the JGroups
	 *                   Address of
	 * @return The JGroups Address matching the given memberName or -null- if not
	 *         found within the JGroups Cluster
	 */
	public Address getTargetAddressForClusterMember(String memberName) {
		getLogger().debug(".getTargetAddressForClusterMember(): Entry, memberName->{}", memberName);
		Address targetAddress = getTargetMemberAddress(memberName);
		getLogger().debug(".getTargetAddressForClusterMember(): Exit, targetAddress->{}", targetAddress);
		return (targetAddress);
	}

	/**
	 * This function retrieves the "first" JGroups Cluster Member whose name begins
	 * with the supplied namePrefix.
	 *
	 * @param namePrefix The namePrefix used to find the "first" JGroups Cluster
	 *                   Member whose name starts with it
	 * @return The "first" entry in the list of possible JGroups Cluster Members
	 *         whose name begins with the supplied namePrefix
	 */
	public String getFirstClusterMemberBasedOnPrefix(String namePrefix) {
		getLogger().debug(".getFirstClusterMemberBasedOnPrefix(): Entry, namePrefix->{}", namePrefix);
		if (getChannel() == null) {
			getLogger().debug(".getFirstClusterMemberBasedOnPrefix(): Exit, Channel is null, exit returning (null)");
			return (null);
		}
		if (StringUtils.isEmpty(namePrefix)) {
			getLogger().debug(".getFirstClusterMemberBasedOnPrefix(): Exit, namePrefix is null, exit returning (null)");
			return (null);
		}
		List<String> potentialInterfaces = getClusterMemberSetBasedOnPrefix(namePrefix);
		if (potentialInterfaces.isEmpty()) {
			getLogger().debug(".getFirstClusterMemberBasedOnPrefix(): Exit, no available interfaces supporting function");
			return (null);
		} else {
			String selectedMember = potentialInterfaces.get(0);
			getLogger().debug(".getFirstClusterMemberBasedOnPrefix(): Exit, selectedInterface->{}", selectedMember);
			return (selectedMember);
		}
	}

	/**
	 * This method checks whether, for the given Cluster Member (name), there is an
	 * associated "active" JGroups Cluster Address.
	 * 
	 * @param memberName The member name of the JGroups Cluster we are checking to
	 *                   see is still active
	 * @return TRUE if an "active" Address can be found, FALSE otherwises
	 */
	protected boolean isTargetClusterAddressActive(String memberName) {
		getLogger().debug(".isTargetClusterAddressActive(): Entry, memberName->{}", memberName);
		boolean isActive = isTargetAddressActive(memberName);
		getLogger().debug(".isTargetClusterAddressActive(): Exit, isActive->{}", isActive);
		return (false);
	}

	/**
	 * This method returns the JGroups Address of our JGroups Channel instance into
	 * the Cluster
	 * 
	 * @return a JGroups Address representing our connection into the JGroups
	 *         Cluster
	 */
	protected Address getMyClusterAddress() {
		if (getChannel() != null) {
			Address myAddress = getChannel().getAddress();
			return (myAddress);
		}
		return (null);
	}

	//
	//  buildJGroupsClusterConnector
	//
	protected void populateMyParameters() {
		getLogger().debug(".buildJGroupsClusterConnector(): Entry, clusterFunction->{}", getClusterFunction());

		PetasosClusterPropertyFile propertyFile = processingPlantConfigurationService.getPetasosClusterConfigurationFile();
		
		String configurationFileName = null;
		
		switch (getClusterFunction()) {
			case PETASOS_AUDIT_SERVICES: {
				configurationFileName = propertyFile.getPetasosAuditServicesEndpoint().getConfigurationFilename();
				break;
			}
			case PETASOS_MEDIA_SERVICES: {
				configurationFileName = propertyFile.getPetasosMediaServicesEndpoint().getConfigurationFilename();
				break;
			}
			case PETASOS_INTERCEPTION_SERVICES: {
				configurationFileName = propertyFile.getPetasosInterceptionEndpoint().getConfigurationFilename();
				break;
			}
			case PETASOS_RMI_SERVICES: {
				configurationFileName = propertyFile.getPetasosMessagingEndpoint().getConfigurationFilename();
				break;
			}
			case PETASOS_OBSERVATION_SERVICES: {
				configurationFileName = propertyFile.getPetasosObservationsEndpoint().getConfigurationFilename();
				break;
			}
			case PETASOS_ROUTING_SERVICES: {
				configurationFileName = propertyFile.getPetasosRoutingServicesEndpoint().getConfigurationFilename();
				break;
			}
			case PETASOS_TASKING_SERVICES: {
				configurationFileName = propertyFile.getPetasosTaskingServicesEndpoint().getConfigurationFilename();
				break;
			}
			case PETASOS_TOPOLOGY_SERVICES: {
				configurationFileName = propertyFile.getPetasosTopologyServicesEndpoint().getConfigurationFilename();
				break;
			}
		}
		setConfigurationFileName(configurationFileName);

		// ComponentId
		ComponentId connectorId = new ComponentId();
		connectorId.setId(getUniqueQualifier());
		connectorId.setIdValidityStartInstant(Instant.now());
		connectorId.setIdValidityEndInstant(Instant.MAX);
		connectorId.setName(getClusterFunction().getConfigName());
		connectorId.setVersion(VERSION);
		connectorId.setDisplayName(getClusterFunction().getConfigName()+"("+getUniqueQualifier()+")");
		setComponentId(connectorId);
		
		// Channel Name
		String channelName = componentNameUtilities.buildChannelName(resolveSiteName(), resolveNetworkZoneName(), resolveSubsystemName(), getClusterFunction().getParticipantName(), getUniqueQualifier());
		setChannelName(channelName);
		
		// Group Name
		this.setGroupName(getClusterFunction().getGroupName());
	
		// Channel Status
		this.setChannelStatus(JGroupsChannelStatusEnum.JGROUPS_CHANNEL_LOCAL_STATUS_STARTED);
		
		getLogger().error(".buildJGroupsClusterConnector(): Exit, done....");
	}
		
	//
	// Other Helpers
	// 
	
	protected String resolveSiteName() {
		getLogger().debug(".resolveSiteName(): Entry");
		WildflyBasedServerPropertyFile coreConfigurationFile = processingPlantConfigurationService.getCoreConfigurationFile();
		String siteName = coreConfigurationFile.getDeploymentMode().getSiteName();
		getLogger().debug(".resolveSiteName(): Exit, siteName->{}", siteName);
		return(siteName);
	}
	
	protected String resolveNetworkZoneName() {
		getLogger().debug(".resolveNetworkZoneName(): Entry");
		WildflyBasedServerPropertyFile coreConfigurationFile = processingPlantConfigurationService.getCoreConfigurationFile();
		String zoneName = coreConfigurationFile.getDeploymentZone().getSecurityZoneName();
		getLogger().debug(".resolveNetworkZoneName(): Exit, zoneName->{}", zoneName);
		return(zoneName);
	}
	
	protected NetworkSecurityZoneEnum resolveNetworkZone() {
		getLogger().debug(".resolveNetworkZone(): Entry");
		String zoneName = resolveNetworkZoneName();
		NetworkSecurityZoneEnum zone = NetworkSecurityZoneEnum.fromDisplayName(zoneName);
		getLogger().debug(".resolveNetworkZone(): Exit, zone->{}", zone);
		return(zone);
	}
	
	protected String resolveSubsystemName() {
		getLogger().debug(".resolveSubsystemName(): Entry");
		WildflyBasedServerPropertyFile coreConfigurationFile = processingPlantConfigurationService.getCoreConfigurationFile();
		String subsystemName = coreConfigurationFile.getSubsystemInstant().getSubsystemParticipantName();
		getLogger().debug(".resolveSubsystemName(): Exit, subsystemName->{}", subsystemName);
		return(subsystemName);
	}
    
    //
    // toJGroupsChannelConnectorSummary()
    //
    
    public JGroupsChannelConnectorSummary toJGroupsChannelConnectorSummary() {
    	JGroupsChannelConnectorSummary summary = new JGroupsChannelConnectorSummary();
    	summary.setComponentId(getComponentId());
    	summary.setChannelName(getChannelName());
    	summary.setFunction(getChannelFunction());
    	summary.setIntegrationPointStatus(getChannelStatus());
    	summary.setLastRefreshInstant(Instant.now());
    	summary.setSite(resolveSiteName());
    	summary.setZone(resolveNetworkZone());
    	summary.setSubsystemParticipantName(resolveSubsystemName());
    	summary.setUniqueIdQualifier(getUniqueQualifier());
    	summary.setProcessingPlantInstanceId(null);
    	switch(getChannelStatus()) {
	    	case JGROUPS_CHANNEL_LOCAL_STATUS_FAILED:
	    		summary.setParticipantStatus(ComponentStatusEnum.SOFTWARE_COMPONENT_FAILED);
	    		break;
	    	case JGROUPS_CHANNEL_LOCAL_STATUS_OPERATIONAL:
	    		summary.setParticipantStatus(ComponentStatusEnum.SOFTWARE_COMPONENT_OPERATIONAL);
	    		break;
	    	case JGROUPS_CHANNEL_LOCAL_STATUS_STARTED:
	    		summary.setParticipantStatus(ComponentStatusEnum.SOFTWARE_COMPONENT_STARTING);
	    		break;
	    	case JGROUPS_CHANNEL_LOCAL_STATUS_UNKNOWN:
    		default:
	    		summary.setParticipantStatus(ComponentStatusEnum.SOFTWARE_COMPONENT_STATUS_UNKNOWN);
    	}
    	return(summary);
    }
}
