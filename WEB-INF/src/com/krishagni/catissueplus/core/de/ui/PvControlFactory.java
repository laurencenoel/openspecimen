package com.krishagni.catissueplus.core.de.ui;

import java.util.Properties;

import org.w3c.dom.Element;

import edu.common.dynamicextensions.domain.nui.Control;
import edu.common.dynamicextensions.domain.nui.factory.AbstractLookupControlFactory;
import edu.common.dynamicextensions.nutility.ParserUtil;

public class PvControlFactory extends AbstractLookupControlFactory {
	public static PvControlFactory getInstance() {
		return new PvControlFactory();
	}

	@Override
	public String getType() {
		return "pvField";
	}

	@Override
	public Control parseControl(Element ele, int row, int xPos, Properties props) {
		PvControl ctrl = (PvControl) super.parseControl(ele, row, xPos, props);
		ctrl.setAttribute(ParserUtil.getTextValue(ele, "attribute"));
		ctrl.setLeafNode(ParserUtil.getBooleanValue(ele, "leafValue"));
		ctrl.setRootNode(ParserUtil.getBooleanValue(ele, "rootValue"));
		ctrl.setDefaultValue(ParserUtil.getTextValue(ele, "defaultValue"));
		return ctrl;
	}

	@Override
	protected Control createControl() {
		return new PvControl();
	}
}