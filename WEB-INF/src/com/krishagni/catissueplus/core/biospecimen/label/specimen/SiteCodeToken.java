package com.krishagni.catissueplus.core.biospecimen.label.specimen;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;

public class SiteCodeToken extends AbstractSpecimenLabelToken {

	public SiteCodeToken() {
		this.name = "SITE_CODE";
	}
	
	@Override
	public String getLabel(Specimen specimen) {
		return specimen.getVisit().getSite().getCode();
	}
}
