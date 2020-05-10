package com.krishagni.catissueplus.core.de.ui;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import edu.common.dynamicextensions.domain.nui.Container;
import edu.common.dynamicextensions.domain.nui.Control;
import edu.wustl.dynamicextensions.formdesigner.mapper.DefaultControlMapper;
import edu.wustl.dynamicextensions.formdesigner.mapper.Properties;

public class PvFieldMapper extends DefaultControlMapper {
	@Override
	public Control propertiesToControl(Properties properties, Container container) {
		PvControl control = new PvControl();
		setCommonProperties(properties, control);

		Object settings = properties.get("settings");
		if (settings instanceof Map) {
			Map<String, Object> settingsMap = (Map<String, Object>) settings;
			if (settingsMap.get("attribute") != null) {
				control.setAttribute(settingsMap.get("attribute").toString());
			}

			if (settingsMap.get("leafValue") != null) {
				control.setLeafNode(Boolean.parseBoolean(settingsMap.get("leafValue").toString()));
			}

			if (settingsMap.get("defaultValue") != null) {
				control.setDefaultValue(settingsMap.get("defaultValue").toString());
			}
		}

		return control;
	}

	@Override
	public Properties controlToProperties(Control control, Container container) {
		PvControl pvCtrl = (PvControl) control;

		Properties controlProps = new Properties();
		controlProps.setProperty("type", "fancyControl");
		controlProps.setProperty("fancyControlType", "pvField");
		getCommonProperties(controlProps, control);

		Map<String, Object> settingsMap = new HashMap<>();
		settingsMap.put("attribute", pvCtrl.getAttribute());
		settingsMap.put("leafValue", pvCtrl.isLeafNode());
		if (StringUtils.isNotBlank(pvCtrl.getDefaultValue())) {
			settingsMap.put("defaultValue", pvCtrl.getDefaultValue());
		}

		controlProps.setProperty("settings", settingsMap);
		return controlProps;
	}
}
