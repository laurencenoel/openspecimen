package com.krishagni.catissueplus.core.de.services.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CprErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.VisitErrorCode;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenResolver;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.FormErrorCode;
import com.krishagni.catissueplus.core.de.events.FormDataDetail;
import com.krishagni.catissueplus.core.de.events.FormRecordCriteria;
import com.krishagni.catissueplus.core.de.repository.FormDao;
import com.krishagni.catissueplus.core.de.services.FormService;
import com.krishagni.catissueplus.core.importer.events.ImportObjectDetail;
import com.krishagni.catissueplus.core.importer.services.ObjectImporter;

import edu.common.dynamicextensions.domain.nui.Container;
import edu.common.dynamicextensions.domain.nui.Control;
import edu.common.dynamicextensions.domain.nui.FileUploadControl;
import edu.common.dynamicextensions.domain.nui.SignatureControl;
import edu.common.dynamicextensions.domain.nui.SubFormControl;
import edu.common.dynamicextensions.napi.FormData;
import edu.common.dynamicextensions.nutility.FileUploadMgr;

public class ExtensionsImporter implements ObjectImporter<Map<String, Object>, Map<String, Object>> {
	
	private FormService formSvc;
	
	private FormDao formDao;
	
	private DaoFactory daoFactory;

	private SpecimenResolver specimenResolver;
	
	public void setFormSvc(FormService formSvc) {
		this.formSvc = formSvc;
	}
	
	public void setFormDao(FormDao formDao) {
		this.formDao = formDao;
	}
	
	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setSpecimenResolver(SpecimenResolver specimenResolver) {
		this.specimenResolver = specimenResolver;
	}

	@SuppressWarnings("unchecked")
	@Override
	public ResponseEvent<Map<String, Object>> importObject(RequestEvent<ImportObjectDetail<Map<String, Object>>> req) {
		try {
			ImportObjectDetail<Map<String, Object>> importDetail = req.getPayload();
			
			Map<String, Object> extnObj = importDetail.getObject();
			String recordId = (String)extnObj.get("recordId");
			if (importDetail.isCreate() && StringUtils.isNotBlank(recordId)) {
				return ResponseEvent.userError(FormErrorCode.REC_ID_SPECIFIED_FOR_CREATE);
			} else if (!importDetail.isCreate() && StringUtils.isBlank(recordId)) {
				return ResponseEvent.userError(FormErrorCode.REC_ID_REQUIRED);
			}

			if (importDetail.isCreate() || !isDelete(extnObj)) {
				return createOrUpdateRecord(importDetail);
			} else {
				return deleteRecord(importDetail);
			}
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	private ResponseEvent<Map<String, Object>> createOrUpdateRecord(ImportObjectDetail<Map<String, Object>> importDetail) {
		Map<String, Object> extnObj = importDetail.getObject();
		Map<String, Object> formValueMap = (Map<String, Object>)extnObj.get("formValueMap");
		if (formValueMap == null) {
			return ResponseEvent.response(Collections.emptyMap());
		}

		Map<String, String> params  = importDetail.getParams();
		String entityType = params.get("entityType");
		Long objectId = null;
		CollectionProtocol cp = null;

		if (entityType.equals("Participant") || entityType.equals("CommonParticipant")) {
			String ppid = (String)extnObj.get("ppid");
			String cpShortTitle = (String)extnObj.get("cpShortTitle");
			CollectionProtocolRegistration cpr = daoFactory.getCprDao().getCprByCpShortTitleAndPpid(cpShortTitle, ppid);
			if (cpr == null) {
				return ResponseEvent.userError(CprErrorCode.NOT_FOUND);
			}
			
			objectId = cpr.getId();
			cp = cpr.getCollectionProtocol();
		} else if (entityType.equals("SpecimenCollectionGroup")) {
			String visitName = (String)extnObj.get("visitName");
			Visit visit = daoFactory.getVisitsDao().getByName(visitName);
			if (visit == null) {
				return ResponseEvent.userError(VisitErrorCode.NOT_FOUND, visitName);
			}
			
			objectId = visit.getId();
			cp = visit.getCollectionProtocol();
		} else if (entityType.equals("Specimen") || entityType.equals("SpecimenEvent")) {
			String label = (String)extnObj.get("specimenLabel");
			String cpShortTitle = (String)extnObj.get("cpShortTitle");
			String barcode = (String)extnObj.get("barcode");

			Specimen specimen = specimenResolver.getSpecimen(null, cpShortTitle, label, barcode);
			objectId = specimen.getId();
			cp = specimen.getCollectionProtocol();
		}

		String formName = params.get("formName");
		Container form = Container.getContainer(formName);
		if (form == null) {
			return ResponseEvent.userError(FormErrorCode.NOT_FOUND, formName, 1);
		}
		
		Long formCtxId = formDao.getFormCtxtId(form.getId(), entityType, cp.getId());
		if (formCtxId == null) {
			return ResponseEvent.userError(FormErrorCode.NO_ASSOCIATION, cp.getShortTitle(), form.getCaption());
		}
		
		Map<String, Object> appData = new HashMap<>();
		appData.put("formCtxtId", formCtxId);
		appData.put("objectId", objectId);
		formValueMap.put("appData", appData);

		initFileFields(importDetail.getUploadedFilesDir(), form, formValueMap);
		
		String recordId = (String)extnObj.get("recordId");
		if (StringUtils.isNotBlank(recordId)) {
			formValueMap.put("id", Long.parseLong(recordId));
		}
				
		FormData formData = FormData.getFormData(form, formValueMap, true, null);
		
		FormDataDetail formDataDetail = new FormDataDetail();
		formDataDetail.setFormId(form.getId());
		formDataDetail.setRecordId(formData.getRecordId());
		formDataDetail.setFormData(formData);
		formDataDetail.setPartial(true);
		ResponseEvent<FormDataDetail> resp = formSvc.saveFormData(new RequestEvent<>(formDataDetail));
		resp.throwErrorIfUnsuccessful();
		
		return ResponseEvent.response(resp.getPayload().getFormData().getFieldNameValueMap(true));
	}

	private void initFileFields(String filesDir, Container form, Map<String, Object> formValueMap) {
		for (Control ctrl : form.getControls()) {
			String fieldName = ctrl.getUserDefinedName();

			if (ctrl instanceof SubFormControl) {
				SubFormControl sfCtrl = (SubFormControl)ctrl;
				if (sfCtrl.isOneToOne()) {
					Map<String, Object> sfValueMap = (HashMap<String, Object>)formValueMap.get(fieldName);
					initFileFields(filesDir, sfCtrl.getSubContainer(), sfValueMap);
				} else {
					List<Map<String, Object>> sfValueMapList = (List<Map<String, Object>>)formValueMap.get(fieldName);
					if (sfValueMapList != null) {
						for (Map<String, Object> sfValueMap : sfValueMapList) {
							initFileFields(filesDir, sfCtrl.getSubContainer(), sfValueMap);
						}
					}
				}

			} else if (ctrl instanceof FileUploadControl || ctrl instanceof SignatureControl) {
				String filename = null;
				Object value = formValueMap.get(fieldName);
				if (value instanceof String) {
					filename = (String) value;
				} else if (value instanceof Map) {
					Map<String, Object> fcv = (Map<String, Object>) value;
					filename = (String) fcv.get("filename");
				}

				if (StringUtils.isNotBlank(filename)) {
					if (ctrl instanceof SignatureControl) {
						String extn = null;
						int lastDotIdx = filename.lastIndexOf(".");
						if (lastDotIdx != -1) {
							extn = filename.substring(lastDotIdx + 1);
						}

						Map<String, String> fileDetail = uploadFile(filesDir, filename, extn);
						formValueMap.put(fieldName, fileDetail.get("fileId"));
					} else {
						Map<String, String> fileDetail = uploadFile(filesDir, filename);
						formValueMap.put(fieldName, fileDetail);
					}
				}
			}
		}
	}

	private Map<String, String> uploadFile(String filesDir, String filename) {
		return uploadFile(filesDir, filename, null);
	}

	private Map<String, String> uploadFile(String filesDir, String filename, String extn) {
		FileInputStream fin = null;
		try {
			File fileToUpload = new File(filesDir + File.separator + filename);
			fin = new FileInputStream(fileToUpload);
			String fileId = FileUploadMgr.getInstance().saveFile(fin, extn);

			Map<String, String> fileDetail = new HashMap<>();
			fileDetail.put("filename", filename);
			fileDetail.put("fileId", fileId);
			fileDetail.put("contentType", Utility.getContentType(fileToUpload));

			return fileDetail;
		} catch (FileNotFoundException fnfe) {
			throw OpenSpecimenException.userError(FormErrorCode.UPLOADED_FILE_NOT_FOUND, filename);
		} finally {
			IOUtils.closeQuietly(fin);
		}
	}
	
	private ResponseEvent<Map<String, Object>> deleteRecord(ImportObjectDetail<Map<String, Object>> importDetail) {
		String recordId = (String)importDetail.getObject().get("recordId");
		String formName = importDetail.getParams().get("formName");
		
		Container form = Container.getContainer(formName);
		if (form == null) {
			return ResponseEvent.userError(FormErrorCode.NOT_FOUND, formName, 1);
		}
		
		FormRecordCriteria crit = new FormRecordCriteria();
		crit.setFormId(form.getId());
		crit.setRecordId(Long.parseLong(recordId));
		ResponseEvent<Long> resp = formSvc.deleteRecord(new RequestEvent<>(crit));
		resp.throwErrorIfUnsuccessful();
		
		return ResponseEvent.response(importDetail.getObject());
	}
	
	private boolean isDelete(Map<String, Object> extnObj) {
		String activityStatus = (String)extnObj.get("activityStatus");
		if (StringUtils.isBlank(activityStatus)) {
			return false;
		}
		
		if (!Status.isValidActivityStatus(activityStatus)) {
			throw OpenSpecimenException.userError(ActivityStatusErrorCode.INVALID);
		}
		
		return Status.ACTIVITY_STATUS_DISABLED.getStatus().equals(activityStatus);
	}
}
