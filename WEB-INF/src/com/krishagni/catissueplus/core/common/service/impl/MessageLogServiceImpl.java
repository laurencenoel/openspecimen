package com.krishagni.catissueplus.core.common.service.impl;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.domain.MessageLog;
import com.krishagni.catissueplus.core.common.events.MessageLogCriteria;
import com.krishagni.catissueplus.core.common.service.MessageHandler;
import com.krishagni.catissueplus.core.common.service.MessageLogService;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.Utility;

public class MessageLogServiceImpl implements MessageLogService {
	private static final Log logger = LogFactory.getLog(MessageLogServiceImpl.class);

	private Map<String, MessageHandler> handlers = new HashMap<>();

	private DaoFactory daoFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public void registerHandler(String extAppName, MessageHandler handler) {
		handlers.put(extAppName, handler);
	}

	@Override
	public void retryPendingMessages() {
		try {
			started();

			MessageLogCriteria crit = new MessageLogCriteria()
				.processStatus(MessageLog.ProcessStatus.PENDING)
				.toDate(Calendar.getInstance().getTime())
				.maxRetries(getMaxRetries());

			int startAt = 0, maxLogs = 50;
			boolean endOfMsgLogs = false;
			while (!endOfMsgLogs) {
				List<MessageLog> msgLogs = getLogs(crit.startAt(startAt).maxResults(maxLogs));
				endOfMsgLogs = (msgLogs.size() < maxLogs);

				for (MessageLog msgLog : msgLogs) {
					String error = null;
					String recordIds = null;
					try {
						MessageHandler handler = handlers.get(msgLog.getExternalApp());
						if (handler == null) {
							logger.error("No handler registered to process messages of external app: " + msgLog.getExternalApp());
							continue;
						}

						recordIds = handler.process(msgLog);
					} catch (Exception e) {
						logger.error("Error while processing message with log id = " + msgLog.getId(), e);
						error = Utility.getErrorMessage(e);
					} finally {
						updateStatus(msgLog, recordIds, error);
					}
				}
			}
		} finally {
			finished();
		}
	}

	private int getMaxRetries() {
		return ConfigUtil.getInstance().getIntSetting("common", "max_eapp_msg_retries", 5);
	}

	private void started() {
		handlers.values().forEach(MessageHandler::onStart);
	}

	private void finished() {
		handlers.values().forEach(MessageHandler::onComplete);
	}

	@PlusTransactional
	private List<MessageLog> getLogs(MessageLogCriteria crit) {
		return daoFactory.getMessageLogDao().getMessages(crit);
	}

	@PlusTransactional
	private void updateStatus(MessageLog msgLog, String recordIds, String error) {
		if (StringUtils.isNotBlank(recordIds) && StringUtils.isBlank(error)) {
			msgLog.setRecordId(recordIds);
			msgLog.setProcessStatus(MessageLog.ProcessStatus.PROCESSED);
		} else if (StringUtils.isNotBlank(error)) {
			msgLog.setError(error);
		}

		msgLog.incrNoOfRetries();
		msgLog.setProcessTime(Calendar.getInstance().getTime());
		daoFactory.getMessageLogDao().saveOrUpdate(msgLog);
	}
}
