package com.krishagni.catissueplus.core.administrative.events;

import java.util.Date;

import org.hibernate.envers.Audited;

import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;

@Audited
public class ContainerMaintenance extends BaseEntity {
	private Date lastMaintained;
	
	private StorageContainer storageContainer;
	
	public Date getLastMaintained() {
		return lastMaintained;
	}

	public void setLastMaintained(Date lastMaintained) {
		this.lastMaintained = lastMaintained;
	}

	public StorageContainer getStorageContainer() {
		return storageContainer;
	}

	public void setStorageContainer(StorageContainer storageContainer) {
		this.storageContainer = storageContainer;
	}
}
