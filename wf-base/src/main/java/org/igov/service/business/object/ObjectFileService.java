package org.igov.service.business.object;

import org.activiti.engine.HistoryService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.task.Attachment;
import org.igov.service.conf.MongoCreateAttachmentCmd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Oleksii Khalikov
 * Created on 30.01.2016.
 */
@Service
public class ObjectFileService {
    private static final Logger LOG = LoggerFactory.getLogger(ObjectFileService.class);

    @Autowired
    private HistoryService oHistoryService;

    @Autowired
    private TaskService oTaskService;

    /**
     * Перенос атачментов задач активити в mongo DB
     * @param nStartFrom Порядковый номер процесса с которого начинать обработку аттачментов
     * @param nChunkSize Размер блока для выборки процесса на обработку
     * @param nProcessId Айдишник конкретного процесса
     * @return
     */
    public String moveAttachsToMongo(String nStartFrom, String nChunkSize, String nProcessId){
        long totalMaxProcesses = oHistoryService.createHistoricProcessInstanceQuery().count();
        long maxProcesses = totalMaxProcesses;

        long nStartFromProcess = 0;
        if (nStartFrom != null){
            nStartFromProcess = Long.valueOf(nStartFrom);
        }

        int nStep = 100;
        if (nChunkSize != null){
            nStep = Integer.valueOf(nChunkSize);
            maxProcesses = nStartFromProcess + nStep;
        }

        LOG.info("Total number of processes: {}. Processing instances from {} to {}", totalMaxProcesses, nStartFromProcess, maxProcesses);

        for (long i = nStartFromProcess; i < maxProcesses; i = i + 10){

            LOG.info("Processing processes from {} to {}", i, (i + 10));
            List<HistoricProcessInstance> processInstances = new LinkedList<HistoricProcessInstance>();
            if (nProcessId != null){
                HistoricProcessInstance task = oHistoryService.createHistoricProcessInstanceQuery().processInstanceId(nProcessId).singleResult();
                LOG.info("Found process by ID:{}", nProcessId);
                processInstances.add(task);
            } else {
                processInstances = oHistoryService.createHistoricProcessInstanceQuery().listPage((int)i, (int)(i + 10));
            }
            LOG.info("Number of process:{}", processInstances.size());
            for (HistoricProcessInstance procesInstance : processInstances){
                List<Attachment> attachments = oTaskService.getProcessInstanceAttachments(procesInstance.getId());
                if (attachments != null && !attachments.isEmpty()){
                    LOG.info("Found {} attachments for the process instance:{}", attachments.size(), procesInstance.getId());

                    for (Attachment attachment : attachments){
                        if (!((org.activiti.engine.impl.persistence.entity.AttachmentEntity)attachment).getContentId().startsWith(
                                MongoCreateAttachmentCmd.MONGO_KEY_PREFIX)){
                            try {
                                LOG.info("Found process with attachment not in mongo. Attachment ID:{}", attachment.getId());
                                InputStream is = oTaskService.getAttachmentContent(attachment.getId());
                                LOG.info("Got content for attachment. Attachment ID: {}", attachment.getId());
                                Attachment newAttachment = oTaskService.createAttachment(attachment.getType(), attachment.getTaskId(),
                                        attachment.getProcessInstanceId(), attachment.getName(), attachment.getDescription(), is);
                                LOG.info("Created new attachment with ID:{} new attachment:{} old attachment:{} ", newAttachment.getId(), newAttachment, attachment);
                                oTaskService.deleteAttachment(attachment.getId());
                                LOG.info("Removed old attachment with ID: {}", attachment.getId());
                            } catch (Exception e){
                                LOG.error("Exception occured while moving attachment: {}", e.getMessage());
                                LOG.trace("FAIL:", e);
                            }
                        } else {
                            LOG.info("Attachment {} is already in Mongo with ID:{}",
                                    attachment.getId(), ((org.activiti.engine.impl.persistence.entity.AttachmentEntity)attachment).getContentId());
                        }
                    }
                } else {
                    LOG.info("No attachments found for the process with ID:{}", procesInstance.getId());
                }
            }
            if (nProcessId != null){
                break;
            }
        }

        return "OK";
    }
}
