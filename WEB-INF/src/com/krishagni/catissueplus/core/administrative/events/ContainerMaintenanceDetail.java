package com.krishagni.catissueplus.core.administrative.events;

import java.util.Date;

import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.krishagni.catissueplus.core.common.AttributeModifiedSupport;
import com.krishagni.catissueplus.core.common.ListenAttributeChanges;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@ListenAttributeChanges
public class ContainerMaintenanceDetail extends AttributeModifiedSupport {
	private Long id;
	
	private Date lastMaintained;
	
	private Long storageContainerId;
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getLastMaintained() {
		return lastMaintained;
	}

	public void setLastMaintained(Date lastMaintained) {
		this.lastMaintained = lastMaintained;
	}

	public Long getStorageContainerId() {
		return storageContainerId;
	}

	public void setStorageContainerId(Long storageContainerId) {
		this.storageContainerId = storageContainerId;
	}

	public static ContainerMaintenanceDetail from(ContainerMaintenance containerMaintenance) {
		ContainerMaintenanceDetail result = new ContainerMaintenanceDetail();
		
		result.setLastMaintained(containerMaintenance.getLastMaintained());
		result.setStorageContainerId(containerMaintenance.getStorageContainer().getId());
		result.setOpComments(containerMaintenance.getOpComments());
		
		return result;
	}
}
 