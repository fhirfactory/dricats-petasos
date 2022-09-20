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

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.MembershipListener;
import org.jgroups.View;
import org.jgroups.blocks.RpcDispatcher;
import org.slf4j.Logger;

import net.fhirfactory.dricats.model.component.datatypes.ComponentId;
import net.fhirfactory.dricats.model.configuration.filebased.archetypes.valuesets.ClusterFunctionNameEnum;
import net.fhirfactory.dricats.model.petasos.ipc.endpoints.valuesets.JGroupsChannelStatusEnum;
import net.fhirfactory.dricats.model.petasos.ipc.technologies.jgroups.datatypes.JGroupsClientAdapter;
import net.fhirfactory.dricats.petasos.ipc.technologies.datatypes.PetasosAdapterAddress;
import net.fhirfactory.dricats.petasos.ipc.technologies.datatypes.PetasosAdapterAddressTypeEnum;

public abstract class JGroupsChannel extends RouteBuilder implements MembershipListener {

	private ComponentId componentId;
	private String groupName;
	private String channelName;
	private String configurationFileName;
	private String configurationName;
	List<JGroupsClientAdapter> adapterList;
	private JGroupsChannelStatusEnum channelStatus;	

	private boolean initialised;
	private JChannel channel;
	private RpcDispatcher rpcDispatcher;

	private Object channelLock;

	private ArrayList<Address> previousScannedMembership;
	private ArrayList<Address> currentScannedMembership;
	private Object currentScannedMembershipLock;

	private static Long RPC_UNICAST_TIMEOUT = 5000L;

	private static int INITIALISATION_RETRY_COUNT = 5;
	private static Long INITIALISATION_RETRY_WAIT = 500L;

	//
	// Constructor(s)
	//

	public JGroupsChannel() {
		this.channel = null;
		this.previousScannedMembership = new ArrayList<>();
		this.currentScannedMembership = new ArrayList<>();
		this.rpcDispatcher = null;
		this.currentScannedMembershipLock = new Object();
		this.channelLock = new Object();
	}

	//
	// Abstract Methods
	//

	abstract protected Logger getLogger();

	abstract public void processInterfaceAddition(PetasosAdapterAddress addedInterface);

	abstract public void processInterfaceRemoval(PetasosAdapterAddress removedInterface);

	abstract public void processInterfaceSuspect(PetasosAdapterAddress suspectInterface);
	
	abstract public ClusterFunctionNameEnum getChannelFunction();


	//
	// JGroups Group/Cluster Membership Event Listener
	//

	@Override
	public void viewAccepted(View newView) {
		getLogger().debug(".viewAccepted(): Entry, JGroups View Changed!");
		List<Address> addressList = newView.getMembers();
		getLogger().trace(".viewAccepted(): Got the Address set via view, now iterate through and see if one is suitable");
		if (getChannel() != null) {
			getLogger().debug("JGroupsCluster->{}", getChannel().getClusterName());
		} else {
			getLogger().debug("JGroupsCluster still Forming");
		}
		synchronized (this.currentScannedMembershipLock) {
			this.previousScannedMembership.clear();
			this.previousScannedMembership.addAll(this.currentScannedMembership);
			this.currentScannedMembership.clear();
			this.currentScannedMembership.addAll(addressList);
		}

		if (getLogger().isInfoEnabled()) {
			for (Address currentAddress : addressList) {
				getLogger().debug("Visible Member->{}", currentAddress);
			}
		}

		//
		// A Report
		//
		if ((getChannel() != null) && getLogger().isDebugEnabled()) {
			getLogger().debug(".viewAccepted(): -------- Starting Channel Report -------");
			String channelProperties = getChannel().getProperties();
			getLogger().debug(".viewAccepted(): Properties->{}", channelProperties);
			String jchannelState = getChannel().getState();
			getLogger().debug(".viewAccepted(): State->{}", jchannelState);
			getLogger().debug(".viewAccepted(): -------- End Channel Report -------");
		}
		//
		// Handle View Change
		getLogger().debug(".viewAccepted(): Checking PubSub Participants");
		List<PetasosAdapterAddress> removals = getMembershipRemovals(previousScannedMembership, currentScannedMembership);
		List<PetasosAdapterAddress> additions = getMembershipAdditions(previousScannedMembership, currentScannedMembership);
		getLogger().debug(".viewAccepted(): Changes(MembersAdded->{}, MembersRemoved->{}", additions.size(), removals.size());
		getLogger().debug(".viewAccepted(): Iterating through ActionInterfaces");
		for (PetasosAdapterAddress currentAddedElement : additions) {
			processInterfaceAddition(currentAddedElement);
		}
		for (PetasosAdapterAddress currentRemovedElement : removals) {
			processInterfaceRemoval(currentRemovedElement);
		}
		getLogger().debug(".viewAccepted(): PubSub Participants check completed");
		getLogger().debug(".viewAccepted(): Exit");
	}

	@Override
	public void suspect(Address suspected_mbr) {
		MembershipListener.super.suspect(suspected_mbr);
	}

	@Override
	public void block() {
		MembershipListener.super.block();
	}

	@Override
	public void unblock() {
		MembershipListener.super.unblock();
	}

	private List<PetasosAdapterAddress> getMembershipAdditions(List<Address> oldList, List<Address> newList) {
		List<PetasosAdapterAddress> additions = new ArrayList<>();
		for (Address newListElement : newList) {
			if (oldList.contains(newListElement)) {
				// do nothing
			} else {
				PetasosAdapterAddress currentPetasosAdapterAddress = new PetasosAdapterAddress();
				currentPetasosAdapterAddress.setAddressName(newListElement.toString());
				currentPetasosAdapterAddress.setJGroupsAddress(newListElement);
				currentPetasosAdapterAddress.setAddressType(PetasosAdapterAddressTypeEnum.ADDRESS_TYPE_JGROUPS);
				additions.add(currentPetasosAdapterAddress);
			}
		}
		return (additions);
	}

	private List<PetasosAdapterAddress> getMembershipRemovals(List<Address> oldList, List<Address> newList) {
		List<PetasosAdapterAddress> removals = new ArrayList<>();
		for (Address oldListElement : oldList) {
			if (newList.contains(oldListElement)) {
				// no nothing
			} else {
				PetasosAdapterAddress currentPetasosAdapterAddress = new PetasosAdapterAddress();
				currentPetasosAdapterAddress.setAddressName(oldListElement.toString());
				currentPetasosAdapterAddress.setJGroupsAddress(oldListElement);
				currentPetasosAdapterAddress.setAddressType(PetasosAdapterAddressTypeEnum.ADDRESS_TYPE_JGROUPS);
				removals.add(currentPetasosAdapterAddress);
			}
		}
		return (removals);
	}

	//
	// JChannel Initialisation
	//

	protected void establishJChannel() {
		getLogger().info(".establishJChannel(): Entry, fileName->{}, groupName->{}, channelName->{}",getConfigurationFileName(), getChannelName(), getGroupName());
		int retryCount = 0;
		synchronized (getChannelLock()) {
			while (retryCount < 5) {
				try {
					getLogger().trace(".establishJChannel(): Creating JChannel");
					getLogger().trace(".establishJChannel(): Getting the required ProtocolStack");
					JChannel newChannel = new JChannel(getConfigurationFileName());
					getLogger().trace(".establishJChannel(): JChannel initialised, now setting JChannel name");
					newChannel.name(getChannelName());
					getLogger().trace(".establishJChannel(): JChannel Name set, now set ensure we don't get our own messages");
					newChannel.setDiscardOwnMessages(true);
					getLogger().trace(".establishJChannel(): Now setting RPCDispatcher");
					RpcDispatcher newRPCDispatcher = new RpcDispatcher(newChannel, this);
					newRPCDispatcher.setMembershipListener(this);
					getLogger().trace(".establishJChannel(): RPCDispatcher assigned, now connect to JGroup");
					newChannel.connect(getGroupName());
					getLogger().trace(".establishJChannel(): Connected to JGroup complete, now assigning class attributes");
					this.setChannel(newChannel);
					this.setRPCDispatcher(newRPCDispatcher);
					getLogger().trace(".establishJChannel(): Exit, JChannel & RPCDispatcher created");
					break;
				} catch (Exception e) {
					getLogger().error(".establishJChannel(): Cannot establish JGroups Channel, error->{}", ExceptionUtils.getMessage(e));
					if (retryCount < INITIALISATION_RETRY_COUNT) {
						getLogger().error(".establishJChannel(): Cannot establish JGroups Channel, retrying");
					}
				}
				retryCount += 1;
				if (retryCount >= INITIALISATION_RETRY_COUNT) {
					break;
				} else {
					try {
						Thread.sleep(INITIALISATION_RETRY_WAIT);
					} catch (Exception e) {
						getLogger().warn(".establishJChannel():Sleep period interrupted, warn->{}", ExceptionUtils.getMessage(e));
					}
				}
			}
		}
	}

	//
	// Getters and Setters
	//

	public JChannel getChannel() {
		return channel;
	}

	public void setChannel(JChannel ipcChannel) {
		this.channel = ipcChannel;
	}

	public RpcDispatcher getRPCDispatcher() {
		return rpcDispatcher;
	}

	protected void setRPCDispatcher(RpcDispatcher rpcDispatcher) {
		this.rpcDispatcher = rpcDispatcher;
	}

	public boolean isInitialised() {
		return initialised;
	}

	public void setInitialised(boolean initialised) {
		this.initialised = initialised;
	}

	public Long getRPCUnicastTimeout() {
		return (RPC_UNICAST_TIMEOUT);
	}

	public ArrayList<Address> getCurrentScannedMembership() {
		ArrayList<Address> clonedList = new ArrayList<>();
		return currentScannedMembership;
	}

	public void setCurrentScannedMembership(ArrayList<Address> currentScannedMembership) {
		this.currentScannedMembership = currentScannedMembership;
	}

	public Object getCurrentScannedMembershipLock() {
		return currentScannedMembershipLock;
	}

	protected Object getChannelLock() {
		return (this.channelLock);
	}

	/**
	 * @return the componentId
	 */
	public ComponentId getComponentId() {
		return componentId;
	}

	/**
	 * @param componentId the componentId to set
	 */
	public void setComponentId(ComponentId componentId) {
		this.componentId = componentId;
	}

	/**
	 * @return the groupName
	 */
	public String getGroupName() {
		return groupName;
	}

	/**
	 * @param groupName the groupName to set
	 */
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	/**
	 * @return the channelName
	 */
	public String getChannelName() {
		return channelName;
	}

	/**
	 * @param channelName the channelName to set
	 */
	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}

	/**
	 * @return the configurationFileName
	 */
	public String getConfigurationFileName() {
		return configurationFileName;
	}

	/**
	 * @param configurationFileName the configurationFileName to set
	 */
	public void setConfigurationFileName(String configurationFileName) {
		this.configurationFileName = configurationFileName;
	}

	/**
	 * @return the configurationName
	 */
	public String getConfigurationName() {
		return configurationName;
	}

	/**
	 * @param configurationName the configurationName to set
	 */
	public void setConfigurationName(String configurationName) {
		this.configurationName = configurationName;
	}

	/**
	 * @return the adapterList
	 */
	public List<JGroupsClientAdapter> getAdapterList() {
		return adapterList;
	}

	/**
	 * @param adapterList the adapterList to set
	 */
	public void setAdapterList(List<JGroupsClientAdapter> adapterList) {
		this.adapterList = adapterList;
	}

	/**
	 * @return the channelStatus
	 */
	public JGroupsChannelStatusEnum getChannelStatus() {
		return channelStatus;
	}

	/**
	 * @param channelStatus the channelStatus to set
	 */
	public void setChannelStatus(JGroupsChannelStatusEnum channelStatus) {
		this.channelStatus = channelStatus;
	}

	/**
	 * @param channelLock the channelLock to set
	 */
	public void setChannelLock(Object channelLock) {
		this.channelLock = channelLock;
	}

	//
	// JGroups Membership Methods
	//

	public List<Address> getAllViewMembers() {
		if (getChannel() == null) {
			return (new ArrayList<>());
		}
		if (getChannel().getView() == null) {
			return (new ArrayList<>());
		}
		try {
			List<Address> members = null;
			synchronized (getChannelLock()) {
				members = getChannel().getView().getMembers();
			}
			return (members);
		} catch (Exception ex) {
			getLogger().warn(".getAllMembers(): Failed to get View Members, Error: Message->{}, StackTrace->{}", ExceptionUtils.getMessage(ex), ExceptionUtils.getStackTrace(ex));
		}
		return (new ArrayList<>());
	}

	public Address getTargetMemberAddress(String name) {
		getLogger().debug(".getTargetMemberAddress(): Entry, name->{}", name);
		if (getChannel() == null) {
			getLogger().debug(".getTargetMemberAddress(): Channel is null, exit returning (null)");
			return (null);
		}
		getLogger().trace(".getTargetMemberAddress(): Channel is NOT null, get updated Address set via view");
		List<Address> addressList = getAllViewMembers();
		Address foundAddress = null;
		synchronized (this.currentScannedMembershipLock) {
			getLogger().trace(".getTargetMemberAddress(): Got the Address set via view, now iterate through and see if one is suitable");
			for (Address currentAddress : addressList) {
				getLogger().trace(".getTargetMemberAddress(): Iterating through Address list, current element->{}", currentAddress);
				if (currentAddress.toString().contentEquals(name)) {
					getLogger().trace(".getTargetMemberAddress(): Exit, A match!");
					foundAddress = currentAddress;
					break;
				}
			}
		}
		getLogger().debug(".getTargetMemberAddress(): Exit, address->{}", foundAddress);
		return (foundAddress);
	}

	protected boolean isTargetAddressActive(String addressName) {
		getLogger().debug(".isTargetAddressActive(): Entry, addressName->{}", addressName);
		if (getChannel() == null) {
			getLogger().debug(".isTargetAddressActive(): Channel is null, exit returning -false-");
			return (false);
		}
		if (StringUtils.isEmpty(addressName)) {
			getLogger().debug(".isTargetAddressActive(): addressName is empty, exit returning -false-");
			return (false);
		}
		getLogger().trace(".isTargetAddressActive(): Channel is NOT null, get updated Address set via view");
		List<Address> addressList = getAllViewMembers();
		boolean addressIsActive = false;
		synchronized (this.currentScannedMembershipLock) {
			getLogger().trace(".isTargetAddressActive(): Got the Address set via view, now iterate through and see our address is there");
			for (Address currentAddress : addressList) {
				getLogger().trace(".isTargetAddressActive(): Iterating through Address list, current element->{}", currentAddress);
				if (currentAddress.toString().contentEquals(addressName)) {
					getLogger().trace(".isTargetAddressActive(): Exit, A match");
					addressIsActive = true;
					break;
				}
			}
		}
		getLogger().debug(".isTargetAddressActive(): Exit, addressIsActive->{}", addressIsActive);
		return (addressIsActive);
	}

	public List<Address> getAllGroupMembers() {
		getLogger().debug(".getAllClusterTargets(): Entry");
		List<Address> addressList = getAllViewMembers();
		getLogger().debug(".getAllClusterTargets(): Exit, petasosAdapterAddresses->{}", addressList);
		return (addressList);
	}

	public List<String> getAllGroupMemberChannelNames() {
		getLogger().debug(".getAllGroupMemberChannelNames(): Entry");
		List<Address> addressList = getAllViewMembers();
		List<String> channelNames = new ArrayList<>();
		for (Address currentAddress : addressList) {
			String currentName = currentAddress.toString();
			channelNames.add(currentName);
		}
		getLogger().debug(".getAllGroupMemberChannelNames(): Exit, channelNames->{}", channelNames);
		return (channelNames);
	}

	protected Address getMyAddress() {
		if (getChannel() != null) {
			Address myAddress = getChannel().getAddress();
			return (myAddress);
		}
		return (null);
	}

	//
	// Route
	//

	@Override
	public void configure() throws Exception {
		String endpointName = getChannelName();

		from("timer://" + endpointName + "?delay=1000&repeatCount=1").routeId("ProcessingPlant::" + endpointName).log(LoggingLevel.DEBUG, "Starting....");
	}

}
