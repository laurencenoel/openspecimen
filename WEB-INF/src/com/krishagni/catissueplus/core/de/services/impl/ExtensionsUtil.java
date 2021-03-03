package com.krishagni.catissueplus.core.de.services.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.FormErrorCode;
import com.krishagni.catissueplus.core.de.events.ExtensionDetail;

import edu.common.dynamicextensions.nutility.FileUploadMgr;

public class ExtensionsUtil {

	public static void initFileFields(String filesDir, ExtensionDetail extension) {
		if (extension == null || extension.getAttrs() == null || extension.getAttrs().isEmpty()) {
			return;
		}

		for (ExtensionDetail.AttrDetail attr : extension.getAttrs()) {
			Object attrValue = attr.getValue();
			if (attrValue instanceof List) {
				List sfRecs = (List) attrValue;
				if (sfRecs.isEmpty() || !(sfRecs.get(0) instanceof Map)) {
					continue;
				}

				for (Map<String, Object> sfRec : (List<Map<String, Object>>) sfRecs) {
					for (Map.Entry<String, Object> sfAttr : sfRec.entrySet()) {
						if (!(sfAttr.getValue() instanceof Map)) {
							continue;
						}

						Map<String, String> fileDetail = getFileDetail(filesDir, (Map<String, Object>) sfAttr.getValue());
						if (fileDetail != null) {
							sfAttr.setValue(fileDetail);
						}
					}
				}
			} else if (attrValue instanceof Map) {
				Map<String, String> fileDetail = getFileDetail(filesDir, (Map<String, Object>) attrValue);
				if (fileDetail != null) {
					attr.setValue(fileDetail);
				}
			}
		}
	}

	private static Map<String, String> getFileDetail(String filesDir, Map<String, Object> fcv) {
		if (!Objects.equals(fcv.get("defile"), Boolean.TRUE)) {
			return null;
		}

		String filename = (String) fcv.get("filename");
		if (StringUtils.isBlank(filename)) {
			return null;
		}

		return uploadFile(filesDir, filename);
	}

	private static Map<String, String> uploadFile(String filesDir, String filename) {
		FileInputStream fin = null;
		
		try {
			File fileToUpload = new File(filesDir + File.separator + filename);
			if (!fileToUpload.exists()) {
				filesDir = filesDir.replaceFirst("file2","file");
				fileToUpload = new File(filesDir + File.separator + filename);
			}
			fin = new FileInputStream(fileToUpload);
			String fileId = FileUploadMgr.getInstance().saveFile(fin);

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
}
