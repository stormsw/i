/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.igov.service.business.action.task.core;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.UserTask;
import org.activiti.engine.*;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.form.FormData;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.history.HistoricDetail;
import org.activiti.engine.history.HistoricFormProperty;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.identity.Group;
import org.activiti.engine.impl.util.json.JSONArray;
import org.activiti.engine.impl.util.json.JSONObject;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.task.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.igov.io.GeneralConfig;
import org.igov.io.db.kv.temp.IBytesDataInmemoryStorage;
import org.igov.io.mail.Mail;
import org.igov.model.action.event.HistoryEvent_Service_StatusType;
import org.igov.model.action.task.core.ProcessDTOCover;
import org.igov.model.action.task.core.TaskAssigneeCover;
import org.igov.model.action.task.core.entity.TaskAssigneeI;
import org.igov.model.flow.FlowSlotTicketDao;
import org.igov.service.business.access.BankIDConfig;
import org.igov.service.business.action.event.HistoryEventService;
import org.igov.service.business.action.task.form.QueueDataFormType;
import org.igov.service.exception.CRCInvalidException;
import org.igov.service.exception.CommonServiceException;
import org.igov.service.exception.RecordNotFoundException;
import org.igov.service.exception.TaskAlreadyUnboundException;
import org.igov.util.ToolLuna;
import org.igov.util.ToolJS;
import org.igov.util.JSON.JsonDateTimeSerializer;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.script.ScriptException;
import java.io.File;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.igov.io.fs.FileSystemData.getFiles_PatternPrint;
import org.igov.util.ToolFS;
import static org.igov.util.Tool.sO;

/**
 *
 * @author bw
 */
//@Component
@Service
public class ActionTaskService {
    public static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd:HH-mm-ss", Locale.ENGLISH);
    public static final String CANCEL_INFO_FIELD = "sCancelInfo";
    private static final int DEFAULT_REPORT_FIELD_SPLITTER = 59;
    private static final int MILLIS_IN_HOUR = 1000 * 60 * 60;

    private static final Logger LOG = LoggerFactory.getLogger(ActionTaskService.class);
    @Autowired
    private BankIDConfig oBankIDConfig;
    //@Autowired
    //private ExceptionCommonController exceptionController;
    //@Autowired
    //private ExceptionCommonController exceptionController;
    @Autowired
    private RuntimeService oRuntimeService;
    /////////////////////////////////////////////////////////////////////////////////////////////////////
    @Autowired
    private TaskService oTaskService;
    //private HistoryService historyService;
    @Autowired
    private HistoryEventService oHistoryEventService;
    //private FormService formService;
    @Autowired
    private Mail oMail;
    //@Autowired
    //private RuntimeService oRuntimeService;
    //@Autowired
    //private TaskService oTaskService;
    @Autowired
    private RepositoryService oRepositoryService;
    @Autowired
    private FormService oFormService;
    @Autowired
    private IBytesDataInmemoryStorage oBytesDataInmemoryStorage;
    @Autowired
    private IdentityService oIdentityService;
    @Autowired
    private HistoryService oHistoryService;
    @Autowired
    private GeneralConfig oGeneralConfig;
    @Autowired
    private FlowSlotTicketDao flowSlotTicketDao;

    
    public static String parseEnumValue(String sEnumName) {
        LOG.info("(sEnumName={})", sEnumName);
        String res = StringUtils.defaultString(sEnumName);
        LOG.info("(sEnumName(2)={})", sEnumName);
        if (res.contains("|")) {
            String[] as = sEnumName.split("\\|");
            LOG.info("(as.length - 1={})", (as.length - 1));
            LOG.info("(as={})", as);
            res = as[as.length - 1];
        }
        return res;
    }

    /*@ExceptionHandler({CRCInvalidException.class, EntityNotFoundException.class, RecordNotFoundException.class, TaskAlreadyUnboundException.class})
    @ResponseBody
    public ResponseEntity<String> handleAccessException(Exception e) throws CommonServiceException {
    return exceptionController.catchActivitiRestException(new CommonServiceException(
    ExceptionCommonController.BUSINESS_ERROR_CODE,
    e.getMessage(), e,
    HttpStatus.FORBIDDEN));
    }*/
    public static String parseEnumProperty(FormProperty property) {
        Object oValues = property.getType().getInformation("values");
        if (oValues instanceof Map) {
            Map<String, String> mValue = (Map) oValues;
            LOG.info("(m={})", mValue);
            String sName = property.getValue();
            LOG.info("(sName={})", sName);
            String sValue = mValue.get(sName);
            LOG.info("(sValue={})", sValue);
            return parseEnumValue(sValue);
        } else {
            LOG.error("Cannot parse values for property - {}", property);
            return "";
        }
    }

    public static String parseEnumProperty(FormProperty property, String sName) {
        Object oValues = property.getType().getInformation("values");
        if (oValues instanceof Map) {
            Map<String, String> mValue = (Map) oValues;
            LOG.info("(m={})", mValue);
            LOG.info("(sName={})", sName);
            String sValue = mValue.get(sName);
            LOG.info("(sValue={})", sValue);
            return parseEnumValue(sValue);
        } else {
            LOG.error("Cannot parse values for property - {}", property);
            return "";
        }
    }

    /*public static String createTable_TaskPropertiesBefore(String soData) {
        return createTable_TaskProperties(soData, false);
    }*/
    public static String createTable_TaskProperties(String saField, Boolean bNew) {
        if (saField == null || "[]".equals(saField) || "".equals(saField)) {
            return "";
        }
        //StringBuilder tableStr = new StringBuilder("Поле \t/ Тип \t/ Поточне значення\n");
        
        /*osTable.append("<td>").append("Поле").append("</td>");
        osTable.append("<td>").append("Тип").append("</td>");
        osTable.append("<td>").append("Поточне значення").append("</td>");*/
        JSONObject oFields = new JSONObject("{ \"soData\":" + saField + "}");
        JSONArray aField = oFields.getJSONArray("soData");
        StringBuilder osTable = new StringBuilder();
        
        osTable.append("<style>table.QuestionFields td { border-style: solid;}</style>");
        osTable.append("<table class=\"QuestionFields\">");
        osTable.append("<tr>");
        osTable.append("<td>").append("Поле").append("</td>");
        if(bNew){
            osTable.append("<td>").append("Старе значення").append("</td>");
            osTable.append("<td>").append("Нове значення").append("</td>");
        }else{
            osTable.append("<td>").append("Значення").append("</td>");
        }
        osTable.append("<td>").append("Коментар").append("</td>");
        osTable.append("</tr>");
        for (int i = 0; i < aField.length(); i++) {
            JSONObject oField = aField.getJSONObject(i);
            /*Object oID=oField.opt("id");
            Object oType=oField.opt("type");
            Object oValue=oField.opt("value");
            osTable.append("<tr>");
            osTable.append("<td>").append(oID!=null?oID:"").append("</td>");
            osTable.append("<td>").append(oType!=null?oType:"").append("</td>");
            osTable.append("<td>").append(oValue!=null?oValue:"").append("</td>");*/
        /*
        sID: item.id,
        sName: item.name,
        sType: item.type,
        sValue: item.value,
        sValueNew: "",
        sNotify: $scope.clarifyFields[item.id].text
        */
            Object sName=oField.opt("sName");
            if(sName==null){
                sName = oField.opt("sID");
            }
            if(sName==null){
                sName = oField.opt("id");
            }
            Object oValue=oField.opt("sValue");
            if(oValue==null){
                oValue = oField.opt("value");
            }
            osTable.append("<tr>");
            osTable.append("<td>").append(sName!=null?sName:"").append("</td>");
            if(bNew){
                Object oValueNew=oField.opt("sValueNew");
                osTable.append("<td>").append(oValue!=null?oValue:"").append("</td>");
                osTable.append("<td>").append(oValueNew!=null?oValueNew:"").append("</td>");
                osTable.append("<td>").append((oValueNew+"").equals(oValue+"")?"(Не змінилось)":"(Змінилось)").append("</td>");
            }else{
                Object oNotify=oField.opt("sNotify");
                osTable.append("<td>").append(oValue!=null?oValue:"").append("</td>");
                osTable.append("<td>").append(oNotify!=null?oNotify:"").append("</td>");
            }
            osTable.append("</tr>");
            /*osTable.append(record.opt("id") != null ? record.get("id") : "?")
                    .append(" \t ")
                    .append(record.opt("type") != null ? record.get("type").toString() : "??")
                    .append(" \t ")
                    .append(record.opt("value") != null ? record.get("value").toString() : "")
                    .append(" \n");*/
        }
        osTable.append("</table>");
        return osTable.toString();
    }

    public TaskQuery buildTaskQuery(String sLogin, String bAssigned) {
        TaskQuery taskQuery = oTaskService.createTaskQuery();
        if (bAssigned != null) {
            if (!Boolean.valueOf(bAssigned)) {
                taskQuery.taskUnassigned();
                if (sLogin != null && !sLogin.isEmpty()) {
                    taskQuery.taskCandidateUser(sLogin);
                }
            } else if (sLogin != null && !sLogin.isEmpty()) {
                taskQuery.taskAssignee(sLogin);
            }
        } else {
            if (sLogin != null && !sLogin.isEmpty()) {
                taskQuery.taskCandidateOrAssigned(sLogin);
            }
        }
        return taskQuery;
    }

    public void cancelTasksInternal(Long nID_Order, String sInfo) throws CommonServiceException, CRCInvalidException, RecordNotFoundException, TaskAlreadyUnboundException {
        String nID_Process = getOriginalProcessInstanceId(nID_Order);
        getTasksByProcessInstanceId(nID_Process);
        LOG.info("(nID_Order={},nID_Process={},sInfo={})", nID_Order, nID_Process, sInfo);
        HistoricProcessInstance processInstance = oHistoryService.createHistoricProcessInstanceQuery().processInstanceId(nID_Process).singleResult();
        FormData formData = oFormService.getStartFormData(processInstance.getProcessDefinitionId());
        List<String> asID_Field = AbstractModelTask.getListField_QueueDataFormType(formData);
        List<String> queueDataList = AbstractModelTask.getVariableValues(oRuntimeService, nID_Process, asID_Field);
        if (queueDataList.isEmpty()) {
            LOG.error(String.format("Queue data list for Process Instance [id = '%s'] not found", nID_Process));
            throw new RecordNotFoundException("\u041c\u0435\u0442\u0430\u0434\u0430\u043d\u043d\u044b\u0435 \u044d\u043b\u0435\u043a\u0442\u0440\u043e\u043d\u043d\u043e\u0439 \u043e\u0447\u0435\u0440\u0435\u0434\u0438 \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u044b");
        }
        for (String queueData : queueDataList) {
            Map<String, Object> m = QueueDataFormType.parseQueueData(queueData);
            long nID_FlowSlotTicket = QueueDataFormType.get_nID_FlowSlotTicket(m);
            LOG.info("(nID_Order={},nID_FlowSlotTicket={})", nID_Order, nID_FlowSlotTicket);
            if (!flowSlotTicketDao.unbindFromTask(nID_FlowSlotTicket)) {
                throw new TaskAlreadyUnboundException("\u0417\u0430\u044f\u0432\u043a\u0430 \u0443\u0436\u0435 \u043e\u0442\u043c\u0435\u043d\u0435\u043d\u0430");
            }
        }
        oRuntimeService.setVariable(nID_Process, CANCEL_INFO_FIELD, String.format(
                "[%s] \u0417\u0430\u044f\u0432\u043a\u0430 \u0441\u043a\u0430\u0441\u043e\u0432\u0430\u043d\u0430: %s",
                DateTime.now(), sInfo == null ? "" : sInfo));
    }


    private String addCalculatedFields(String saFieldsCalc, TaskInfo curTask, String currentRow) {
        HistoricTaskInstance details = oHistoryService.createHistoricTaskInstanceQuery().includeProcessVariables().taskId(curTask.getId()).singleResult();
        LOG.info("Process variables of the task {}:{}", curTask.getId(), details.getProcessVariables());
        if (details != null && details.getProcessVariables() != null) {
            Set<String> headersExtra = new HashSet<>();
            for (String key : details.getProcessVariables().keySet()) {
                if (!key.startsWith("sBody")) {
                    headersExtra.add(key);
                }
            }
            saFieldsCalc = StringUtils.substringAfter(saFieldsCalc, "\"");
            saFieldsCalc = StringUtils.substringBeforeLast(saFieldsCalc, "\"");
            for (String expression : saFieldsCalc.split(";")) {
                String variableName = StringUtils.substringBefore(expression, "=");
                String condition = StringUtils.substringAfter(expression, "=");
                LOG.info("Checking variable with (name={}, condition={}, expression={}) ", variableName, condition, expression);
                try {
                    Object conditionResult = getObjectResultofCondition(headersExtra, details, details, condition);
                    currentRow = currentRow + ";" + conditionResult;
                    LOG.info("Adding calculated field {} with the value {}", variableName, conditionResult);
                } catch (Exception oException) {
                    LOG.error("Error: {}, occured while processing (variable={}) ",oException.getMessage(), variableName);
                    LOG.debug("FAIL:", oException);
                }
            }
        }
        return currentRow;
    }

    public ResponseEntity<String> unclaimUserTask(String nID_UserTask) throws CommonServiceException, RecordNotFoundException {
        Task task = oTaskService.createTaskQuery().taskId(nID_UserTask).singleResult();
        if (task == null) {
            throw new RecordNotFoundException();
        }
        if (task.getAssignee() == null || task.getAssignee().isEmpty()) {
            return new ResponseEntity<>("Not assigned UserTask", HttpStatus.OK);
        }
        oTaskService.unclaim(task.getId());
        return new ResponseEntity<>("", HttpStatus.OK);
    }

    /*public void setInfo_ToActiviti(String snID_Process, String saField, String sBody) {
        try {
            LOG.info(String.format("try to set saField=%s and sBody=%s to snID_Process=%s", saField, sBody, snID_Process));
            oRuntimeService.setVariable(snID_Process, "saFieldQuestion", saField);
            oRuntimeService.setVariable(snID_Process, "sQuestion", sBody);
            LOG.info(String.format("completed set saField=%s and sBody=%s to snID_Process=%s", saField, sBody, snID_Process));
        } catch (Exception oException) {
            LOG.error("error: {}, during set variables to Activiti!", oException.getMessage());
        }
    }*/

    public void loadFormPropertiesToMap(FormData formData, Map<String, Object> variables, Map<String, String> formValues) {
        List<FormProperty> aFormProperty = formData.getFormProperties();
        if (!aFormProperty.isEmpty()) {
            for (FormProperty oFormProperty : aFormProperty) {
                String sType = oFormProperty.getType().getName();
                if (variables.containsKey(oFormProperty.getId())) {
                    if ("enum".equals(sType)) {
                        Object variable = variables.get(oFormProperty.getId());
                        if (variable != null) {
                            String sID_Enum = variable.toString();
                            LOG.info("execution.getVariable()(sID_Enum={})", sID_Enum);
                            String sValue = parseEnumProperty(oFormProperty, sID_Enum);
                            formValues.put(oFormProperty.getId(), sValue);
                        }
                    } else {
                        formValues.put(oFormProperty.getId(), variables.get(oFormProperty.getId()) != null ? String.valueOf(variables.get(oFormProperty.getId())) : null);
                    }
                }
            }
        }
    }

    public Date getBeginDate(Date date) {
        if (date == null) {
            return DateTime.now().minusDays(1).toDate();
        }
        return date;
    }

    private Object getObjectResultofCondition(Set<String> headersExtra, HistoricTaskInstance currTask, HistoricTaskInstance details, String condition) throws ScriptException, NoSuchMethodException {
        Map<String, Object> params = new HashMap<>();
        for (String headerExtra : headersExtra) {
            Object variableValue = details.getProcessVariables().get(headerExtra);
            String propertyValue = sO(variableValue);
            params.put(headerExtra, propertyValue);
        }
        params.put("sAssignedLogin", currTask.getAssignee());
        params.put("sID_UserTask", currTask.getTaskDefinitionKey());
        LOG.info("Calculating expression with (params={})", params);
        Object conditionResult = new ToolJS().getObjectResultOfCondition(new HashMap<String, Object>(),
                params, condition);
        LOG.info("Condition of the expression is {}", conditionResult.toString());
        return conditionResult;
    }

    public ProcessDefinition getProcessDefinitionByTaskID(String nID_Task){
        HistoricTaskInstance historicTaskInstance = oHistoryService.createHistoricTaskInstanceQuery()
                .taskId(nID_Task).singleResult();
        String sBP = historicTaskInstance.getProcessDefinitionId();
        ProcessDefinition processDefinition = oRepositoryService.createProcessDefinitionQuery()
                .processDefinitionId(sBP).singleResult();
        return processDefinition;
    }

    protected void processExtractFieldsParameter(Set<String> headersExtra, HistoricTaskInstance currTask, String saFields, Map<String, Object> line) {
        HistoricTaskInstance details = oHistoryService.createHistoricTaskInstanceQuery().includeProcessVariables().taskId(currTask.getId()).singleResult();
        LOG.info("Process variables of the task {}:{}", currTask.getId(), details.getProcessVariables());
        if (details != null && details.getProcessVariables() != null) {
            LOG.info("(Cleaned saFields={})", saFields);
            String[] expressions = saFields.split(";");
            if (expressions != null) {
                for (String expression : expressions) {
                    String variableName = StringUtils.substringBefore(expression, "=");
                    String condition = StringUtils.substringAfter(expression, "=");
                    LOG.info("Checking variable with (name={}, condition={}, expression={})", variableName, condition, expression);
                    try {
                        Object conditionResult = getObjectResultofCondition(headersExtra, currTask, details, condition);
                        line.put(variableName, conditionResult);
                    } catch (Exception oException) {
                        LOG.error("Error: {}, occured while processing variable {}", oException.getMessage(), variableName);
                        LOG.debug("FAIL:", oException);
                    }
                }
            }
        }
    }

    private void loadCandidateStarterGroup(ProcessDefinition processDef, Set<String> candidateCroupsToCheck) {
        List<IdentityLink> identityLinks = oRepositoryService.getIdentityLinksForProcessDefinition(processDef.getId());
        LOG.info(String.format("Found %d identity links for the process %s", identityLinks.size(), processDef.getKey()));
        for (IdentityLink identity : identityLinks) {
            if (IdentityLinkType.CANDIDATE.equals(identity.getType())) {
                String groupId = identity.getGroupId();
                candidateCroupsToCheck.add(groupId);
                LOG.info("Added candidate starter (group={})", groupId);
            }
        }
    }

    //@RequestMapping("/web")
    //public class StartWebController {
    /*private final Logger LOG = LoggerFactory
    .getLogger(StartWebController.class);
    @Autowired
    private RuntimeService oRuntimeService;
    @Autowired
    private RepositoryService oRepositoryService;
    @Autowired
    private FormService formService;
    @RequestMapping(value = "/activiti/index", method = RequestMethod.GET)
    public ModelAndView index() {
    ModelAndView modelAndView = new ModelAndView("index");
    List<ProcessDefinition> processDefinitions = oRepositoryService.createProcessDefinitionQuery().latestVersion()
    .list();
    modelAndView.addObject("processList", processDefinitions);
    return modelAndView;
    }
    @RequestMapping(value = "/activiti/startForm/{id}", method = RequestMethod.GET)
    public ModelAndView startForm(@PathVariable("id") String id) {
    StartFormData sfd = oFormService.getStartFormData(id);
    List<FormProperty> fpList = sfd.getFormProperties();
    ModelAndView modelAndView = new ModelAndView("startForm");
    modelAndView.addObject("fpList", fpList);
    modelAndView.addObject("id", id);
    return modelAndView;
    }
    @RequestMapping(value = "/activiti/startProcess/{id}", method = RequestMethod.POST)
    public ModelAndView startProcess(@PathVariable("id") String id, @RequestParam Map<String, String> params) {
    ProcessInstance pi = oFormService.submitStartFormData(id, params);
    ModelAndView modelAndView = new ModelAndView("startedProcess");
    modelAndView.addObject("pi", pi.getProcessInstanceId());
    modelAndView.addObject("bk", pi.getBusinessKey());
    return modelAndView;
    }*/
    public String getOriginalProcessInstanceId(Long nID_Protected) throws CRCInvalidException {
        return Long.toString(ToolLuna.getValidatedOriginalNumber(nID_Protected));
    }

    public Attachment getAttachment(String attachmentId, String nID_Task, Integer nFile, String processInstanceId) {
        List<Attachment> attachments = oTaskService.getProcessInstanceAttachments(processInstanceId);
        Attachment attachmentRequested = null;
        for (int i = 0; i < attachments.size(); i++) {
            if (attachments.get(i).getId().equalsIgnoreCase(attachmentId) || (null != nFile && nFile.equals(i + 1))) {
                attachmentRequested = attachments.get(i);
                break;
            }
        }
        if (attachmentRequested == null && !attachments.isEmpty()) {
            attachmentRequested = attachments.get(0);
        }
        if (attachmentRequested == null) {
            throw new ActivitiObjectNotFoundException("Attachment for nID_Task '" + nID_Task + "' not found.");
        }
        return attachmentRequested;
    }

    public Attachment getAttachment(String attachmentId, String nID_Task, String processInstanceId) {
        List<Attachment> attachments = oTaskService.getProcessInstanceAttachments(processInstanceId);
        Attachment attachmentRequested = null;
        for (int i = 0; i < attachments.size(); i++) {
            if (attachments.get(i).getId().equalsIgnoreCase(attachmentId)) {
                attachmentRequested = attachments.get(i);
                break;
            }
        }
        if (attachmentRequested == null) {
            throw new ActivitiObjectNotFoundException("Attachment for nID_Task '" + nID_Task + "' not found.");
        }
        return attachmentRequested;
    }

    public void fillTheCSVMapHistoricTasks(String sID_BP, Date dateAt, Date dateTo, List<HistoricTaskInstance> foundResults, SimpleDateFormat sDateCreateDF, List<Map<String, Object>> csvLines, String pattern, Set<String> tasksIdToExclude, String saFieldsCalc, String[] headers) {
        if (CollectionUtils.isEmpty(foundResults)) {
            LOG.info(String.format("No historic tasks found for business process %s for date period %s - %s", sID_BP, DATE_TIME_FORMAT.format(dateAt), DATE_TIME_FORMAT.format(dateTo)));
            return;
        }
        LOG.info(String.format("Found %s historic tasks for business process %s for date period %s - %s", foundResults.size(), sID_BP, DATE_TIME_FORMAT.format(dateAt), DATE_TIME_FORMAT.format(dateTo)));
        if (pattern != null) {
            LOG.info("List of fields to retrieve: {}", pattern);
        } else {
            LOG.info("Will retreive all fields from tasks");
        }
        LOG.info("Tasks to skip {}", tasksIdToExclude);
        for (HistoricTaskInstance curTask : foundResults) {
            if (tasksIdToExclude.contains(curTask.getId())) {
                LOG.info("Skipping historic task {} from processing as it is already in the response", curTask.getId());
                continue;
            }
            String currentRow = pattern;
            Map<String, Object> variables = curTask.getProcessVariables();
            LOG.info("Loaded historic variables for the task {}|{}", curTask.getId(), variables);
            currentRow = replaceFormProperties(currentRow, variables);
            if (saFieldsCalc != null) {
                currentRow = addCalculatedFields(saFieldsCalc, curTask, currentRow);
            }
            if (pattern != null) {
                currentRow = replaceReportFields(sDateCreateDF, curTask, currentRow);
                currentRow = currentRow.replaceAll("\\$\\{.*?\\}", "");
            }
            String[] values = currentRow.split(";");
            if (headers.length != values.length) {
                LOG.info("Size of header :{} Size of values array:{}", headers.length, values.length);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < headers.length; i++) {
                    sb.append(headers[i]);
                    sb.append(";");
                }
                LOG.info("(headers={})", sb.toString());
                sb = new StringBuilder();
                for (int i = 0; i < values.length; i++) {
                    sb.append(values[i]);
                    sb.append(";");
                }
                LOG.info("(values={})", sb.toString());
            }
            Map<String, Object> currRow = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                currRow.put(headers[i], values[i]);
            }
            csvLines.add(currRow);
        }
    }

    /*
     * private void clearEmptyValues(Map<String, Object> params) {
     * Iterator<String> iterator = params.keySet().iterator(); while
     * (iterator.hasNext()){ String key = iterator.next(); if (params.get(key)
     * == null){ iterator.remove(); } } }
     */
    private void addTasksDetailsToLine(Set<String> headersExtra, HistoricTaskInstance currTask, Map<String, Object> resultLine) {
        LOG.debug("(currTask={})", currTask.getId());
        HistoricTaskInstance details = oHistoryService.createHistoricTaskInstanceQuery().includeProcessVariables().taskId(currTask.getId()).singleResult();
        if (details != null && details.getProcessVariables() != null) {
            for (String headerExtra : headersExtra) {
                Object variableValue = details.getProcessVariables().get(headerExtra);
                resultLine.put(headerExtra, variableValue);
            }
        }
    }

    private Set<String> findExtraHeadersForDetail(List<HistoricTaskInstance> foundResults, List<String> headers) {
        Set<String> headersExtra = new TreeSet<>();
        for (HistoricTaskInstance currTask : foundResults) {
            HistoricTaskInstance details = oHistoryService.createHistoricTaskInstanceQuery().includeProcessVariables().taskId(currTask.getId()).singleResult();
            if (details != null && details.getProcessVariables() != null) {
                LOG.info("(proccessVariavles={})", details.getProcessVariables());
                for (String key : details.getProcessVariables().keySet()) {
                    if (!key.startsWith("sBody")) {
                        headersExtra.add(key);
                    }
                }
            }
        }
        headers.addAll(headersExtra);
        return headersExtra;
    }

    private String replaceFormProperties(String currentRow, Map<String, Object> data) {
        String res = currentRow;
        for (Map.Entry<String, Object> property : data.entrySet()) {
            LOG.info(String.format("Matching property %s:%s with fieldNames", property.getKey(), property.getValue()));
            if (currentRow != null && res.contains("${" + property.getKey() + "}")) {
                LOG.info(String.format("Found field with id %s in the pattern. Adding value to the result", "${" + property.getKey() + "}"));
                if (property.getValue() != null) {
                    String sValue = property.getValue().toString();
                    LOG.info("(sValue={})", sValue);
                    if (sValue != null) {
                        LOG.info(String.format("Replacing field with the value %s", sValue));
                        res = res.replace("${" + property.getKey() + "}", sValue);
                    }
                }
            }
        }
        return res;
    }

    private String replaceFormProperties(String currentRow, TaskFormData data) {
        String res = currentRow;
        for (FormProperty property : data.getFormProperties()) {
            LOG.info(String.format("Matching property %s:%s:%s with fieldNames", property.getId(), property.getName(), property.getType().getName()));
            if (currentRow != null && res.contains("${" + property.getId() + "}")) {
                LOG.info(String.format("Found field with id %s in the pattern. Adding value to the result", "${" + property.getId() + "}"));
                String sValue = getPropertyValue(property);
                if (sValue != null) {
                    LOG.info(String.format("Replacing field with the value %s", sValue));
                    res = res.replace("${" + property.getId() + "}", sValue);
                }
            }
        }
        return res;
    }

    public Charset getCharset(String sID_Codepage) {
        Charset charset;
        String codePage = sID_Codepage.replaceAll("-", "");
        try {
            if ("win1251".equalsIgnoreCase(codePage) || "CL8MSWIN1251".equalsIgnoreCase(codePage)) {
                codePage = "CP1251";
            }
            charset = Charset.forName(codePage);
            LOG.debug("use charset - {}", charset);
        } catch (IllegalArgumentException e) {
            LOG.error("Error: {}. Do not support charset - {}", e.getMessage(), codePage);
            throw new ActivitiObjectNotFoundException("Statistics for the business task for charset '" + codePage + "' cannot be construct.", Task.class, e);
        }
        return charset;
    }

    public String getFileExtention(MultipartFile file) {
        String[] parts = file.getOriginalFilename().split("\\.");
        if (parts.length != 0) {
            return parts[parts.length - 1];
        }
        return "";
    }

    public String getFileExtention(String fileName) {
        String[] parts = fileName.split("\\.");
        if (parts.length != 0) {
            return parts[parts.length - 1];
        }
        return "";
    }
    /*private static class TaskAlreadyUnboundException extends Exception {
    private TaskAlreadyUnboundException(String message) {
    super(message);
    }
    }*/

    public Map<String, Object> createCsvLine(boolean bDetail, Set<String> headersExtra, HistoricTaskInstance currTask, String saFields) {
        Map<String, Object> line = new HashMap<>();
        line.put("nID_Process", currTask.getProcessInstanceId());
        line.put("sLoginAssignee", currTask.getAssignee());
        Date startDate = currTask.getStartTime();
        line.put("sDateTimeStart", DATE_TIME_FORMAT.format(startDate));
        line.put("nDurationMS", String.valueOf(currTask.getDurationInMillis()));
        long durationInHours = currTask.getDurationInMillis() / MILLIS_IN_HOUR;
        line.put("nDurationHour", String.valueOf(durationInHours));
        line.put("sName", currTask.getName());
        if (bDetail) {
            addTasksDetailsToLine(headersExtra, currTask, line);
        }
        if (saFields != null) {
            processExtractFieldsParameter(headersExtra, currTask, saFields, line);
        }
        return line;
    }

    public String getSeparator(String sID_BP, String nASCI_Spliter) {
        if (nASCI_Spliter == null) {
            return String.valueOf(Character.toChars(DEFAULT_REPORT_FIELD_SPLITTER));
        }
        if (!StringUtils.isNumeric(nASCI_Spliter)) {
            LOG.error("ASCI code is not a number {}", nASCI_Spliter);
            throw new ActivitiObjectNotFoundException("Statistics for the business task with name '" + sID_BP + "' not found. Wrong splitter.", Task.class);
        }
        return String.valueOf(Character.toChars(Integer.valueOf(nASCI_Spliter)));
    }

    public String formHeader(String saFields, List<HistoricTaskInstance> foundHistoricResults, String saFieldsCalc) {
        String res = null;
        if (saFields != null && !"".equals(saFields.trim())) {
            LOG.info("Fields have custom header names");
            StringBuilder sb = new StringBuilder();
            String[] fields = saFields.split(";");
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].contains("\\=")) {
                    sb.append(StringUtils.substringBefore(fields[i], "\\="));
                } else {
                    sb.append(fields[i]);
                }
                if (i < fields.length - 1) {
                    sb.append(";");
                }
            }
            res = sb.toString();
            res = res.replaceAll("\\$\\{", "");
            res = res.replaceAll("\\}", "");
            LOG.info("Formed header from list of fields: {}", res);
        } else {
            if (foundHistoricResults != null && !foundHistoricResults.isEmpty()) {
                HistoricTaskInstance historicTask = foundHistoricResults.get(0);
                Set<String> keys = historicTask.getProcessVariables().keySet();
                StringBuilder sb = new StringBuilder();
                Iterator<String> iter = keys.iterator();
                while (iter.hasNext()) {
                    sb.append(iter.next());
                    if (iter.hasNext()) {
                        sb.append(";");
                    }
                }
                res = sb.toString();
            }
            LOG.info("Formed header from all the fields of a task: {}", res);
        }
        if (saFieldsCalc != null) {
            saFieldsCalc = StringUtils.substringAfter(saFieldsCalc, "\"");
            saFieldsCalc = StringUtils.substringBeforeLast(saFieldsCalc, "\"");
            String[] params = saFieldsCalc.split(";");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < params.length; i++) {
                String currParam = params[i];
                String cutHeader = StringUtils.substringBefore(currParam, "=");
                LOG.info("Adding header to the csv file from saFieldsCalc: {}", cutHeader);
                sb.append(cutHeader);
                if (i < params.length - 1) {
                    sb.append(";");
                }
            }
            res = res + ";" + sb.toString();
            LOG.info("Header with calculated fields: {}", res);
        }
        return res;
    }

    public Task getTaskByID(String nID_Task) {
        return oTaskService.createTaskQuery().taskId(nID_Task).singleResult();
    }

    private List<Task> getTasksByProcessInstanceId(String processInstanceID) throws RecordNotFoundException {
        List<Task> tasks = oTaskService.createTaskQuery().processInstanceId(processInstanceID).list();
        if (tasks == null || tasks.isEmpty()) {
            LOG.error(String.format("Tasks for Process Instance [id = '%s'] not found", processInstanceID));
            throw new RecordNotFoundException();
        }
        return tasks;
    }

    private String getPropertyValue(FormProperty property) {
        String sValue;
        String sType = property.getType().getName();
        LOG.info("(sType={})", sType);
        if ("enum".equalsIgnoreCase(sType)) {
            sValue = parseEnumProperty(property);
        } else {
            sValue = property.getValue();
        }
        LOG.info("(sValue={})", sValue);
        return sValue;
    }

    public Set<String> findExtraHeaders(Boolean bDetail, List<HistoricTaskInstance> foundResults, List<String> headers) {
        if (bDetail) {
            Set<String> headersExtra = findExtraHeadersForDetail(foundResults, headers);
            return headersExtra;
        } else {
            return new TreeSet<>();
        }
    }

    private String replaceReportFields(SimpleDateFormat sDateCreateDF, Task curTask, String currentRow) {
        String res = currentRow;
        for (TaskReportField field : TaskReportField.values()) {
            if (res.contains(field.getPattern())) {
                res = field.replaceValue(res, curTask, sDateCreateDF);
            }
        }
        return res;
    }

    private String replaceReportFields(SimpleDateFormat sDateCreateDF, HistoricTaskInstance curTask, String currentRow) {
        String res = currentRow;
        for (TaskReportField field : TaskReportField.values()) {
            if (res.contains(field.getPattern())) {
                res = field.replaceValue(res, curTask, sDateCreateDF);
            }
        }
        return res;
    }

    /*private String createTable(String soData) throws UnsupportedEncodingException {
        if (soData == null || "[]".equals(soData) || "".equals(soData)) {
            return "";
        }
        StringBuilder tableStr = new StringBuilder("<table><tr><th>Поле</th><th>Тип </th><th> Поточне значення</th></tr>");
        JSONObject jsnobject = new JSONObject("{ soData:" + soData + "}");
        JSONArray jsonArray = jsnobject.getJSONArray("soData");
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject record = jsonArray.getJSONObject(i);
            tableStr.append("<tr><td>")
                    .append(record.opt("id") != null ? record.get("id") : "?")
                    .append("</td><td>")
                    .append(record.opt("type") != null ? record.get("type").toString() : "??")
                    .append("</td><td>")
                    .append(record.opt("value") != null ? record.get("value")
                            .toString() : "").append("</td></tr>");
        }
        tableStr.append("</table>");
        return tableStr.toString();
    }*/

    private void loadCandidateGroupsFromTasks(ProcessDefinition processDef, Set<String> candidateCroupsToCheck) {
        BpmnModel bpmnModel = oRepositoryService.getBpmnModel(processDef.getId());
        for (FlowElement flowElement : bpmnModel.getMainProcess().getFlowElements()) {
            if (flowElement instanceof UserTask) {
                UserTask userTask = (UserTask) flowElement;
                List<String> candidateGroups = userTask.getCandidateGroups();
                if (candidateGroups != null && !candidateGroups.isEmpty()) {
                    candidateCroupsToCheck.addAll(candidateGroups);
                    LOG.info(String.format("Added candidate groups %s from user task %s", candidateGroups,
                            userTask.getId()));
                }
            }
        }
    }
    
    /**
     * saFeilds paramter may contain name of headers or can be empty. Before
     * forming the result - we need to cut header names
     *
     //     * @param saFields
     //     * @param foundHistoricResults
     //     * @return
     */
    public String processSaFields(String saFields, List<HistoricTaskInstance> foundHistoricResults) {
        String res = null;
        if (saFields != null) {
            LOG.info("saFields has custom header names");
            StringBuilder sb = new StringBuilder();
            String[] fields = saFields.split(";");
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].contains("=")) {
                    sb.append(StringUtils.substringAfter(fields[i], "="));
                } else {
                    sb.append(fields[i]);
                }
                if (i < fields.length - 1) {
                    sb.append(";");
                }
            }
            res = sb.toString();
        } else {
            if (foundHistoricResults != null && !foundHistoricResults.isEmpty()) {
                HistoricTaskInstance historicTask = foundHistoricResults.get(0);
                Set<String> keys = historicTask.getProcessVariables().keySet();
                StringBuilder sb = new StringBuilder();
                Iterator<String> iter = keys.iterator();
                while (iter.hasNext()) {
                    sb.append("${").append(iter.next()).append("}");
                    if (iter.hasNext()) {
                        sb.append(";");
                    }
                }
                res = sb.toString();
            }
            LOG.info("Formed header from all the fields of a task: {}", res);
        }
        return res;
    }

    // private Long getProcessId(String sID_Order, Long nID_Protected, Long
    // nID_Process) {
    // Long result = null;
    // if (nID_Process != null) {
    // result = nID_Process;
    // } else if (nID_Protected != null) {
    // result = ToolLuna.getOriginalNumber(nID_Protected);
    // } else if (sID_Order != null && !sID_Order.isEmpty()) {
    // Long protectedId;
    // if (sID_Order.contains("-")) {
    // int dash_position = sID_Order.indexOf("-");
    // protectedId = Long.valueOf(sID_Order.substring(dash_position + 1));
    // } else {
    // protectedId = Long.valueOf(sID_Order);
    // }
    // result = ToolLuna.getOriginalNumber(protectedId);
    // }
    // return result;
    // }
    

    public Long getIDProtectedFromIDOrder(String sID_order) {
        StringBuilder ID_Protected = new StringBuilder();
        int hyphenPosition = sID_order.lastIndexOf("-");
        if (hyphenPosition < 0) {
            for (int i = 0; i < sID_order.length(); i++){
                buildID_Protected(sID_order, ID_Protected, i);
            }
        } else {
            for (int i = hyphenPosition + 1; i < sID_order.length(); i++) {
                buildID_Protected(sID_order, ID_Protected, i);
            }
        }
        return Long.parseLong(ID_Protected.toString());
    }

    private void buildID_Protected(String sID_order, StringBuilder ID_Protected, int i) {
        String ch = "" + sID_order.charAt(i);
        Scanner scanner = new Scanner(ch);
        if(scanner.hasNextInt()){
            ID_Protected.append(ch);
        }
    }

    public Date getEndDate(Date date) {
        if (date == null) {
            return DateTime.now().toDate();
        }
        return date;
    }

    public List<String> getTaskIdsByProcessInstanceId(String processInstanceID) throws RecordNotFoundException {
        List<Task> aTask = getTasksByProcessInstanceId(processInstanceID);
        List<String> res = new ArrayList<>();
        for (Task task : aTask) {
            res.add(task.getId());
        }
        return res;
    }

    public void fillTheCSVMap(String sID_BP, Date dateAt, Date dateTo, List<Task> foundResults, SimpleDateFormat sDateCreateDF, List<Map<String, Object>> csvLines, String pattern, String saFieldsCalc, String[] headers) {
        if (CollectionUtils.isEmpty(foundResults)) {
            LOG.info(String.format("No tasks found for business process %s for date period %s - %s", sID_BP, DATE_TIME_FORMAT.format(dateAt), DATE_TIME_FORMAT.format(dateTo)));
            return;
        }
        LOG.info(String.format("Found %s tasks for business process %s for date period %s - %s", foundResults.size(), sID_BP, DATE_TIME_FORMAT.format(dateAt), DATE_TIME_FORMAT.format(dateTo)));
        if (pattern != null) {
            LOG.info("List of fields to retrieve: }{", pattern);
        } else {
            LOG.info("Will retreive all fields from tasks");
        }
        for (Task curTask : foundResults) {
            String currentRow = pattern;
            LOG.trace("Process task - {}", curTask);
            TaskFormData data = oFormService.getTaskFormData(curTask.getId());
            currentRow = replaceFormProperties(currentRow, data);
            if (saFieldsCalc != null) {
                currentRow = addCalculatedFields(saFieldsCalc, curTask, currentRow);
            }
            if (pattern != null) {
                currentRow = replaceReportFields(sDateCreateDF, curTask, currentRow);
                currentRow = currentRow.replaceAll("\\$\\{.*?\\}", "");
            }
            String[] values = currentRow.split(";");
            Map<String, Object> currRow = new HashMap<>();
            for (int i = 0; i < values.length; i++) {
                currRow.put(headers[i], values[i]);
            }
            csvLines.add(currRow);
        }
    }

    public String[] createStringArray(Map<String, Object> csvLine, List<String> headers) {
        List<String> result = new LinkedList<>();
        for (String header : headers) {
            Object value = csvLine.get(header);
            result.add(value == null ? "" : value.toString());
        }
        return result.toArray(new String[result.size()]);
    }

    /**
     * Получение списка бизнес процессов к которым у пользователя есть доступ
     * @param sLogin - Логин пользователя
     * @return
     */
    public List<Map<String, String>> getBusinessProcessesForUser(String sLogin){

        if (sLogin.isEmpty()) {
            LOG.error("Unable to found business processes for user with empty login");
            throw new ActivitiObjectNotFoundException(
                    "Unable to found business processes for user with empty login",
                    ProcessDefinition.class);
        }

        List<Map<String, String>> result = new LinkedList<>();

        LOG.info(String.format(
                "Selecting business processes for the user with login: %s",
                sLogin));

        List<ProcessDefinition> processDefinitionsList = oRepositoryService
                .createProcessDefinitionQuery().active().latestVersion().list();
        if (CollectionUtils.isNotEmpty(processDefinitionsList)) {
            LOG.info(String.format("Found %d active process definitions",
                    processDefinitionsList.size()));

            List<Group> groups = oIdentityService.createGroupQuery().groupMember(sLogin).list();
            if (groups != null && !groups.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (Group group : groups) {
                    sb.append(group.getId());
                    sb.append(",");
                }
                LOG.info("Found {}  groups for the user {}:{}", groups.size(), sLogin, sb.toString());
            }

            for (ProcessDefinition processDef : processDefinitionsList) {
                LOG.info("process definition id: {}", processDef.getId());

                Set<String> candidateCroupsToCheck = new HashSet<>();
                loadCandidateGroupsFromTasks(processDef, candidateCroupsToCheck);

                loadCandidateStarterGroup(processDef, candidateCroupsToCheck);

                findUsersGroups(groups, result, processDef, candidateCroupsToCheck);
            }
        } else {
            LOG.info("Have not found active process definitions.");
        }

        return result;
    }

    private void findUsersGroups(List<Group> groups, List<Map<String, String>> res, ProcessDefinition processDef, Set<String> candidateCroupsToCheck) {
        for (Group group : groups) {
            for (String groupFromProcess : candidateCroupsToCheck) {
                if (groupFromProcess.contains("${")) {
                    groupFromProcess = groupFromProcess.replaceAll("\\$\\{?.*}", "(.*)");
                }
                if (group.getId().matches(groupFromProcess)) {
                    Map<String, String> process = new HashMap<>();
                    process.put("sID", processDef.getKey());
                    process.put("sName", processDef.getName());
                    LOG.info(String.format("Added record to response %s", process.toString()));
                    res.add(process);
                    return;
                }
            }
        }
    }

    
    public Map<String, String> getTaskFormDataInternal(Long nID_Task) throws CommonServiceException {
        Map<String, String> result = new HashMap<>();
        Task task = oTaskService.createTaskQuery().taskId(nID_Task.toString()).singleResult();
        LOG.info("Found task with (ID={}, process inctanse ID={})", nID_Task, task.getProcessInstanceId());
        FormData taskFormData = oFormService.getTaskFormData(task.getId());
        Map<String, Object> variables = oRuntimeService.getVariables(task.getProcessInstanceId());
        if (taskFormData != null) {
            loadFormPropertiesToMap(taskFormData, variables, result);
        }
        return result;
    }
    
    
    public Map<String, Object> sendProccessToGRESInternal(Long nID_Task) throws CommonServiceException {
        Map<String, Object> res = new HashMap<>();

        Task task = oTaskService.createTaskQuery().taskId(nID_Task.toString()).singleResult();

        LOG.info("Found task with (ID={}, process inctanse ID={})", nID_Task, task.getProcessInstanceId());

        HistoricProcessInstance processInstance = oHistoryService.createHistoricProcessInstanceQuery().processInstanceId(
                task.getProcessInstanceId()).singleResult();

        ProcessDefinition processDefinition = oRepositoryService.createProcessDefinitionQuery()
                .processDefinitionId(task.getProcessDefinitionId()).singleResult();

        FormData startFormData = oFormService.getStartFormData(processInstance.getProcessDefinitionId());
        FormData taskFormData = oFormService.getTaskFormData(task.getId());

        res.put("nID_Task", nID_Task.toString());
        res.put("nID_Proccess", task.getProcessInstanceId());
        res.put("sProcessName", processDefinition.getName());
        res.put("sProcessDefinitionKey", processDefinition.getKey());

        Map<String, Object> variables = oRuntimeService.getVariables(task.getProcessInstanceId());

        Map<String, String> startFormValues = new HashMap<>();
        Map<String, String> taskFormValues = new HashMap<>();
        if (startFormData != null) {
            loadFormPropertiesToMap(startFormData, variables, startFormValues);
        }
        if (taskFormData != null) {
            loadFormPropertiesToMap(taskFormData, variables, taskFormValues);
        }

        res.put("startFormData", startFormValues);
        res.put("taskFormData", taskFormValues);

        return res;
    }    
 
    public String updateHistoryEvent_Service(HistoryEvent_Service_StatusType oHistoryEvent_Service_StatusType, String sID_Order,
            String saField, String sBody, String sToken, String sUserTaskName
        ) throws Exception {

        Map<String, String> mParam = new HashMap<>();
        //params.put("sID_Order", sID_Order);
        //Long nID_StatusType
        mParam.put("nID_StatusType", oHistoryEvent_Service_StatusType.getnID() + "");
        mParam.put("soData", saField);
        //params.put("sHead", sHead);
        mParam.put("sBody", sBody);
        mParam.put("sToken", sToken);
        //params.put("sUserTaskName", sUserTaskName);
        return oHistoryEventService.updateHistoryEvent(sID_Order, sUserTaskName, true, mParam);
    }
    
    public List<Task> getTasksForChecking(String sLogin,
            Boolean bEmployeeUnassigned) {
        List<Task> tasks;
        if (bEmployeeUnassigned) {
            //tasks = oTaskService.createTaskQuery().taskUnassigned().active().list();
            tasks = oTaskService.createTaskQuery().taskCandidateUser(sLogin).taskUnassigned().active().list();
            LOG.info("Looking for unassigned tasks. Found {} tasks", (tasks != null ? tasks.size() : 0));
        } else {
            tasks = oTaskService.createTaskQuery().taskAssignee(sLogin).active().list();
            LOG.info("Looking for tasks assigned to user:{}. Found {} tasks", sLogin, (tasks != null ? tasks.size() : 0));
        }
        return tasks;
    }

    /*public static void main(String[] args) {
        System.out.println(createTable_TaskProperties("[{'id':'bankIdfirstName','type':'string','value':'3119325858'}]"));
    }*/

    public static void replacePatterns(DelegateExecution execution, DelegateTask task, Logger LOG) {
        try {
            LOG.info("(task.getId()={})", task.getId());
            //LOG.info("execution.getId()=" + execution.getId());
            //LOG.info("task.getVariable(\"sBody\")=" + task.getVariable("sBody"));
            //LOG.info("execution.getVariable(\"sBody\")=" + execution.getVariable("sBody"));

            EngineServices oEngineServices = execution.getEngineServices();
            RuntimeService oRuntimeService = oEngineServices.getRuntimeService();
            TaskFormData oTaskFormData = oEngineServices
                    .getFormService()
                    .getTaskFormData(task.getId());

            LOG.info("Found taskformData={}", oTaskFormData);
            if (oTaskFormData == null) {
                return;
            }

            Collection<File> asPatterns = getFiles_PatternPrint();
            for (FormProperty oFormProperty : oTaskFormData.getFormProperties()) {
                String sFieldID = oFormProperty.getId();
                String sExpression = oFormProperty.getName();

                LOG.info("(sFieldID={})", sFieldID);
                //LOG.info("sExpression=" + sExpression);
                LOG.info("(sExpression.length()={})", sExpression != null ? sExpression.length() + "" : "");

                if (sExpression == null || sFieldID == null || !sFieldID.startsWith("sBody")) {
                    continue;
                }

                for (File oFile : asPatterns) {
                    String sName = "pattern/print/" + oFile.getName();
                    //LOG.info("sName=" + sName);

                    if (sExpression.contains("[" + sName + "]")) {
                        LOG.info("sExpression.contains! (sName={})", sName);

                        String sData = ToolFS.getFileString(oFile, null);
                        //LOG.info("sData=" + sData);
                        LOG.info("(sData.length()={})", sData != null ? sData.length() + "" : "null");
                        if (sData == null) {
                            continue;
                        }

                        sExpression = sExpression.replaceAll("\\Q[" + sName + "]\\E", sData);
                        //                        LOG.info("sExpression=" + sExpression);

                        //LOG.info("[replacePatterns](sFieldID=" + sFieldID + "):1-Ok!");
                        oRuntimeService.setVariable(task.getProcessInstanceId(), sFieldID, sExpression);
/*                        LOG.info("[replacePatterns](sFieldID=" + sFieldID + "):2-Ok:" + oRuntimeService
                                .getVariable(task.getProcessInstanceId(), sFieldID));*/
                        LOG.info("setVariable Ok! (sFieldID={})", sFieldID);
                    }
                    LOG.info("Ok! (sName={})",sName);
                }
                LOG.info("Ok! (sFieldID={})", sFieldID);
            }
        } catch (Exception oException) {
            LOG.error("FAIL:", oException);
        }
    }    
    
    
    public static String setStringFromFieldExpression(Expression expression,
            DelegateExecution execution, Object value) {
        if (expression != null && value != null) {
            expression.setValue(value, execution);
        }
        return null;
    }

    /**
     * получаем по задаче ид процесса
     *
     * @param nID_Task ИД-номер таски
     * @return processInstanceId
     */
    public String getProcessInstanceIDByTaskID(String nID_Task) {

        HistoricTaskInstance historicTaskInstanceQuery = oHistoryService
                .createHistoricTaskInstanceQuery().taskId(nID_Task)
                .singleResult();
        String processInstanceId = historicTaskInstanceQuery
                .getProcessInstanceId();
        if (processInstanceId == null) {
            throw new ActivitiObjectNotFoundException(String.format(
                    "ProcessInstanceId for taskId '{%s}' not found.", nID_Task),
                    Attachment.class);
        }
        return processInstanceId;
    }

    /**
     * Получение процесса по его ИД
     *
     * @param sProcessInstanceID
     * @return ProcessInstance
     */
    public HistoricProcessInstance getProcessInstancyByID(String sProcessInstanceID) {
        HistoricProcessInstance processInstance = oHistoryService
                .createHistoricProcessInstanceQuery()
                .processInstanceId(sProcessInstanceID).includeProcessVariables()
                .singleResult();
        if (processInstance == null) {
            throw new ActivitiObjectNotFoundException(String.format(
                    "ProcessInstance for processInstanceId '{%s}' not found.",
                    sProcessInstanceID), Attachment.class);
        }
        return processInstance;
    }

    /**
     * Получение данных о процессе по Таске
     * @param nID_Task - номер-ИД таски
     * @return DTO-объект ProcessDTOCover
     */
    public ProcessDTOCover getProcessInfoByTaskID(String nID_Task){
        LOG.info("start process getting Task Data by nID_Task = {}",  nID_Task);

        HistoricTaskInstance historicTaskInstance = oHistoryService.createHistoricTaskInstanceQuery()
                .taskId(nID_Task).singleResult();

        String sBP = historicTaskInstance.getProcessDefinitionId();
        LOG.info("id-бизнес-процесса (БП) sBP={}", sBP);

        ProcessDefinition processDefinition = oRepositoryService.createProcessDefinitionQuery()
                .processDefinitionId(sBP).singleResult();

        String sName = processDefinition.getName();
        LOG.info("название услуги (БП) sName={}", sName);

        Date oProcessInstanceStartDate = oHistoryService.createProcessInstanceHistoryLogQuery(getProcessInstanceIDByTaskID(
                nID_Task)).singleResult().getStartTime();
        DateTimeFormatter formatter = JsonDateTimeSerializer.DATETIME_FORMATTER;
        String sDateCreate = formatter.print(oProcessInstanceStartDate.getTime());
        LOG.info("дата создания таски sDateCreate={}", sDateCreate);

        Long nID = Long.valueOf(historicTaskInstance.getProcessInstanceId());
        LOG.info("id процесса (nID={})", nID.toString());

        ProcessDTOCover oProcess = new ProcessDTOCover(sName, sBP, nID, sDateCreate);
        LOG.info("Created ProcessDTOCover={}", oProcess.toString());

        return oProcess;
    }

    /**
     * Получение полей стартовой формы по ID таски
     * @param nID_Task номер-ИД таски, для которой нужно найти процесс и вернуть поля его стартовой формы.
     * @return
     * @throws RecordNotFoundException
     */
    public Map<String, Object> getStartFormData(Long nID_Task) throws RecordNotFoundException {
        Map<String, Object> mReturn = new HashMap();
        HistoricTaskInstance oHistoricTaskInstance = oHistoryService.createHistoricTaskInstanceQuery()
                .taskId(nID_Task.toString()).singleResult();
        LOG.info("(oHistoricTaskInstance={})", oHistoricTaskInstance);
        if (oHistoricTaskInstance != null) {
            String snID_Process = oHistoricTaskInstance.getProcessInstanceId();
            LOG.info("(snID_Process={})", snID_Process);
            List<HistoricDetail> aHistoricDetail = null;
            if(snID_Process != null){
                aHistoricDetail = oHistoryService.createHistoricDetailQuery().formProperties()
                        .executionId(snID_Process).list();
            }
            LOG.info("(aHistoricDetail={})", aHistoricDetail);
            if(aHistoricDetail == null){
                throw new RecordNotFoundException("aHistoricDetail");
            }
            for (HistoricDetail oHistoricDetail : aHistoricDetail) {
                HistoricFormProperty oHistoricFormProperty = (HistoricFormProperty) oHistoricDetail;
                mReturn.put(oHistoricFormProperty.getPropertyId(), oHistoricFormProperty.getPropertyValue());
            }
        }else{
            HistoricProcessInstance oHistoricProcessInstance = oHistoryService.createHistoricProcessInstanceQuery().processInstanceId(nID_Task.toString()).singleResult();
            LOG.info("(oHistoricProcessInstance={})", oHistoricProcessInstance);
            //if(oHistoricProcessInstance==null){
            //    throw new RecordNotFoundException("oHistoricProcessInstance");
            //}

            //oHistoricProcessInstance.getId()
            /*
            for(Map.Entry<String,Object> oHistoricProcess : oHistoricProcessInstance.getProcessVariables().entrySet()){
                mReturn.put(oHistoricProcess.getKey(), oHistoricProcess.getValue());
            }
            */

            /*FormData oFormData = formService.getStartFormData(oHistoricProcessInstance.getProcessDefinitionId());
            if(oFormData==null){
                throw new RecordNotFoundException("oFormData");
            }
            List<FormProperty> aFormProperty = oFormData.getFormProperties();
            for (FormProperty oFormProperty : aFormProperty) {
                mReturn.put(oFormProperty.getId(), oFormProperty.getValue());
            }*/
            //Task oTask = oActionTaskService.findBasicTask(nID_Task.toString());


            /*TaskFormData oTaskFormData = formService.getTaskFormData(nID_Task);
            if(oTaskFormData==null){
                throw new RecordNotFoundException("oTaskFormData");
            }
            List<FormProperty> aFormProperty = oTaskFormData.getFormProperties();
            for (FormProperty oFormProperty : aFormProperty) {
                mReturn.put(oFormProperty.getId(), oFormProperty.getValue());
            }*/

            List<Task> activeTasks = null;
            TaskQuery taskQuery = oTaskService.createTaskQuery();
            taskQuery.taskId(nID_Task.toString());
            activeTasks = taskQuery.list();//.active()
            LOG.info("(nID_Task={})",nID_Task);
            if(activeTasks.isEmpty()){
                taskQuery = oTaskService.createTaskQuery();
                LOG.info("1)activeTasks.isEmpty()");
                taskQuery.processInstanceId(nID_Task.toString());
                activeTasks = taskQuery.list();//.active()
                if(activeTasks.isEmpty() && oHistoricProcessInstance!=null){
                    taskQuery = oTaskService.createTaskQuery();
                    LOG.info("2)activeTasks.isEmpty()(oHistoricProcessInstance.getId()={})",oHistoricProcessInstance.getId());
                    taskQuery.processInstanceId(oHistoricProcessInstance.getId());
                    activeTasks = taskQuery.list();//.active()
                    /*if(activeTasks.isEmpty()){
                        taskQuery = oTaskService.createTaskQuery();
                        LOG.info("3)activeTasks.isEmpty()(oHistoricProcessInstance.getSuperProcessInstanceId()={})",oHistoricProcessInstance.getSuperProcessInstanceId());
                        taskQuery.processInstanceId(oHistoricProcessInstance.getSuperProcessInstanceId());
                        activeTasks = taskQuery.list();//.active()
                        if(activeTasks.isEmpty()){
                            if(oHistoricProcessInstance.getSuperProcessInstanceId()!= null){
                                taskQuery = oTaskService.createTaskQuery();
                                LOG.info("4)activeTasks.isEmpty()(oHistoricProcessInstance.getSuperProcessInstanceId()={})",oHistoricProcessInstance.getSuperProcessInstanceId());
                                taskQuery.taskId(oHistoricProcessInstance.getSuperProcessInstanceId());
                                activeTasks = taskQuery.list();//.active()

                            }
                            if(activeTasks.isEmpty() && oHistoricProcessInstance.getId()!=null){
                                taskQuery = oTaskService.createTaskQuery();
                                LOG.info("5)activeTasks.isEmpty()(oHistoricProcessInstance.getId(){})",oHistoricProcessInstance.getId());
                                taskQuery.taskId(oHistoricProcessInstance.getId());
                                activeTasks = taskQuery.list();//.active()
                            }
                        }
                    }*/
                }
            }
            for (Task currTask : activeTasks) {
                TaskFormData data = oFormService.getTaskFormData(currTask.getId());
                if (data != null) {
                    LOG.info("Found TaskFormData for task {}.", currTask.getId());
                    for (FormProperty property : data.getFormProperties()) {
                        mReturn.put(property.getId(), property.getValue());

                        /*String sValue = "";
                        String sType = property.getType().getName();
                        if ("enum".equalsIgnoreCase(sType)) {
                            sValue = oActionTaskService.parseEnumProperty(property);
                        } else {
                            sValue = property.getValue();
                        }
                        LOG.info("taskId=" + currTask.getId() + "propertyName=" + property.getName() + "sValue=" + sValue);
                        if (sValue != null) {
                            if (sValue.toLowerCase().contains(searchTeam)) {
                                res.add(currTask.getId());
                            }
                        }*/
                    }
                } else {
                    LOG.info("Not found TaskFormData for task {}. Skipping from processing.", currTask.getId());
                }
            }

            /*TaskFormData data = formService.getTaskFormData(nID_Task);
            Map<String, String> newProperties = new HashMap<>();
            for (FormProperty oFormProperty : data.getFormProperties()) {
                if (oFormProperty.isWritable()) {
                    newProperties.put(oFormProperty.getId(), oFormProperty.getValue());
                }
            }*/


            //EngineServices oEngineServices = execution.getEngineServices();
            //engineServices = execution.getEngineServices();
            //RuntimeService oRuntimeService = engineServices.getRuntimeService();
            /*TaskFormData oTaskFormData = oEngineServices
                    .getFormService()
                    .getTaskFormData(nID_Task);

            LOG.info("Found taskformData={}", oTaskFormData);
            if (oTaskFormData == null) {
                return;
            }*/
/*
            Collection<File> asPatterns = getFiles_PatternPrint();
            for (FormProperty oFormProperty : oTaskFormData.getFormProperties()) {
                String sFieldID = oFormProperty.getId();
                String sExpression = oFormProperty.getName();

            }
*/


        }
        return mReturn;
    }

    public boolean deleteProcess(Long nID_Order, String sLogin, String sReason) throws Exception{
        boolean success = false;
        String nID_Process = null;

        nID_Process = String.valueOf(ToolLuna.getValidatedOriginalNumber(nID_Order));

        //String sID_Order,
        String sID_Order = oGeneralConfig.sID_Order_ByOrder(nID_Order);

        HistoryEvent_Service_StatusType oHistoryEvent_Service_StatusType = HistoryEvent_Service_StatusType.REMOVED;
        String sUserTaskName = oHistoryEvent_Service_StatusType.getsName_UA();
        String sBody = sUserTaskName;
        //        String sID_status = "Заявка была удалена";
        if (sLogin != null) {
            sBody += " (" + sLogin + ")";
        }
        if (sReason != null) {
            sBody += ": " + sReason;
        }
        Map<String, String> mParam = new HashMap<>();
        mParam.put("nID_StatusType", oHistoryEvent_Service_StatusType.getnID() + "");
        mParam.put("sBody", sBody);
        LOG.info("Deleting process {}: {}", nID_Process, sUserTaskName);
        try {
            oRuntimeService.deleteProcessInstance(nID_Process, sReason);
        } catch (ActivitiObjectNotFoundException e) {
            LOG.info("Could not find process {} to delete: {}", nID_Process, e);
            throw new RecordNotFoundException();
        }

        oHistoryEventService.updateHistoryEvent(
                //processInstanceID,
                sID_Order,
                sUserTaskName, false, mParam);

        success = true;
        return success;
    }

    /**
     * Загрузка задач из Activiti
     * @param sAssignee - ID авторизированого субъекта
     * @return
     */
    public List<TaskAssigneeI> getTasksByAssignee(String sAssignee){
        List<Task> tasks = oTaskService.createTaskQuery().taskAssignee(sAssignee).list();
        List<TaskAssigneeI> facadeTasks = new ArrayList<>();
        TaskAssigneeCover adapter = new TaskAssigneeCover();
        for (Task task : tasks) {
            facadeTasks.add(adapter.apply(task));
        }
        return facadeTasks;
    }

    public List<TaskAssigneeI> getTasksByAssigneeGroup(String sGroup){
        List<Task> tasks = oTaskService.createTaskQuery().taskCandidateGroup(sGroup).list();
        List<TaskAssigneeI> facadeTasks = new ArrayList<>();
        TaskAssigneeCover adapter = new TaskAssigneeCover();
        for (Task task : tasks) {
            facadeTasks.add(adapter.apply(task));
        }
        return facadeTasks;
    }
}
