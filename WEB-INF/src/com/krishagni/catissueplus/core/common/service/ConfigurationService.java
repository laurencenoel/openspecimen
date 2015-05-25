package com.krishagni.catissueplus.core.common.service;

import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.common.events.ConfigSettingDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

public interface ConfigurationService {
	
	public ResponseEvent<List<ConfigSettingDetail>> getSettings(RequestEvent<String> req);
	
	public ResponseEvent<ConfigSettingDetail> saveSetting(RequestEvent<ConfigSettingDetail> req);
	
	//
	// Internal to app APIs
	//
	public Integer getIntSetting(String module, String name, Integer ... defValue);
	
	public Double getFloatSetting(String module, String name, Double ... defValue);

	public String getStrSetting(String module, String name, String ... defValue);
	
	public void reload();
	
	public void registerChangeListener(String module, ConfigChangeListener callback);
	
	public Map<String, Object> getLocaleSettings();
	
	public String getDeDateFormat();
	
	public String getTimeFormat();
	
	public String getDeDateTimeFormat();
}
