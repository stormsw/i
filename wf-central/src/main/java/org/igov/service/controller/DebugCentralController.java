package org.igov.service.controller;

import io.swagger.annotations.*;
import java.util.HashMap;
import java.util.LinkedList;
import org.igov.service.business.subject.SubjectMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.RandomStringUtils;
import org.igov.service.business.action.task.bp.BpService;
import org.igov.io.GeneralConfig;
import static org.igov.io.Log.oLog_External;
import org.igov.model.action.event.HistoryEvent_Service;
import org.igov.model.action.event.HistoryEvent_ServiceDao;
import org.igov.model.subject.message.SubjectMessage;
import org.igov.model.subject.message.SubjectMessagesDao;
import org.igov.model.subject.organ.SubjectOrganJoinAttribute;
import static org.igov.service.business.subject.SubjectMessageService.sMessageHead;
import org.igov.service.exception.CommonServiceException;
import org.igov.util.JSON.JsonRestUtils;
import org.springframework.http.HttpStatus;

//import com.google.common.base.Optional;
/**
 * @author BW
 */
@Controller
@Api(tags = {"DebugCentralController"}, description = "Дебаг и тест центральный")
//@RequestMapping(value = "/debug")
public class DebugCentralController {

    private static final Logger LOG = LoggerFactory.getLogger(DebugCentralController.class);
    
    private static final Logger LOG_MIN = LoggerFactory.getLogger("Log_External");
    private static final Logger LOG_MID = LoggerFactory.getLogger("Log_External_Mid");
    private static final Logger LOG_MAX = LoggerFactory.getLogger("Log_External_Big");

    
    
    @Autowired
    private HistoryEvent_ServiceDao historyEventServiceDao;
    @Autowired
    private SubjectMessagesDao subjectMessagesDao;
    @Autowired
    private GeneralConfig generalConfig;
    @Autowired
    private BpService bpService;

    @Autowired
    private SubjectMessageService subjectMessageService;

    @Deprecated //Нужно будет удалить после недели работы продеплоеной в прод версии (для обратной временной совместимости)
    /**
     * Сохранение сообщения оценки
     *
     * @param sID_Order Строка-ИД заявки (временно опциональный)
     * @param sID_Rate Строка-ИД Рнйтинга/оценки (число от 1 до 5)
     * @param nID_Protected Номер-ИД заявки, защищенный по алгоритму Луна,
     * опционально(для обратной совместимости)
     * @throws CommonServiceException
     */
    @ApiOperation(value = "/messages/setMessageRate", notes = "##### DebugCentralController(SubjectMessageController) - Сообщения субьектов. Установка сообщения-оценки #####\n\n")
    @RequestMapping(value = "/messages/setMessageRate", method = RequestMethod.GET)//Rate
    public @ResponseBody
    String setMessageRate(
            @ApiParam(value = "Строка-ИД рейтинга/оценки (число от 1 до 5)", required = true) @RequestParam(value = "sID_Rate", required = true) String sID_Rate,
            @ApiParam(value = "Номер-ИД заявки, защищенный по алгоритму Луна, опционально(для обратной совместимости)", required = true) @RequestParam(value = "nID_Protected", required = true) Long nID_Order,
            HttpServletResponse oResponse) throws CommonServiceException {

        String sID_Order = "0-" + nID_Order;
        if ("".equals(sID_Rate.trim())) {
            LOG.warn("Parameter sID_Rate) is absent! (sID_Order={}, sID_Rate{})", sID_Order, sID_Rate);
            throw new CommonServiceException(404, "Incorrect value of sID_Rate! It isn't number.");
        }
        Integer nRate;
        try {
            nRate = Integer.valueOf(sID_Rate);
        } catch (NumberFormatException ex) {
            LOG.warn("incorrect param sID_Rate (not a number): {}", sID_Rate);
            throw new CommonServiceException(404, "Incorrect value of sID_Rate! It isn't number.");
        }
        if (nRate < 1 || nRate > 5) {
            LOG.warn("incorrect param sID_Rate (not in range[1..5]): {}", sID_Rate);
            throw new CommonServiceException(404, "Incorrect value of sID_Rate! It is too short or too long number");
        }

        String sReturn = "Ok!";

        Long nID_HistoryEvent_Service;
        Long nID_Subject;
        HistoryEvent_Service oHistoryEvent_Service;

        try {
            //LOG.info("sID_Order: " + sID_Order + ", nRate: " + nRate);
            oHistoryEvent_Service = historyEventServiceDao.getOrgerByID(sID_Order);
            if (oHistoryEvent_Service == null) {
                throw new CommonServiceException(404, "(sID_Order: " + sID_Order + ", nRate: " + nRate + "): Record of HistoryEvent_Service, with sID_Order=" + sID_Order + " - not found!");
            }
            nID_HistoryEvent_Service = oHistoryEvent_Service.getId();
            nID_Subject = oHistoryEvent_Service.getnID_Subject();

            String sToken = null;
            Integer nRateWas = oHistoryEvent_Service.getnRate();
            if (nRateWas != null && nRateWas > 0) {
                //throw new CommonServiceException(404, "(sID_Order: " + sID_Order + "): Record of HistoryEvent_Service, with sID_Order="+sID_Order+" - alredy has nRateWas="+nRateWas);
                sReturn = "Record of HistoryEvent_Service, with sID_Order=" + sID_Order + " - already has nRateWas=" + nRateWas;
                LOG.warn("{} (nID_HistoryEvent_Service={}, nID_Subject={})", sReturn, nID_HistoryEvent_Service, nID_Subject);
            } else {

                oHistoryEvent_Service.setnRate(nRate);
                //LOG.info(String.format("set nRate=%s in sID_Order=%s", nRate, sID_Order));
                sToken = RandomStringUtils.randomAlphanumeric(15);
                //HistoryEvent_Service oHistoryEvent_Service = historyEventServiceDao.getOrgerByID(sID_Order);
                oHistoryEvent_Service.setsToken(sToken);

                LOG.info("save HistoryEvent_Service... (nID_HistoryEvent_Service={}, nID_Subject={}, sID_Order={}, nRate={})", nID_HistoryEvent_Service, nID_Subject, sID_Order, nRate);
                historyEventServiceDao.saveOrUpdate(oHistoryEvent_Service);

                Long nID_SubjectMessageType = 0l;
                SubjectMessage oSubjectMessage_Rate = subjectMessageService.createSubjectMessage(
                                sMessageHead(nID_SubjectMessageType, sID_Order),
                                "Оцінка " + sID_Rate + " (по шкалі від 2 до 5)", nID_Subject, "", "", "sID_Rate=" + sID_Rate, nID_SubjectMessageType);
                if (nID_HistoryEvent_Service != null) {
                    oSubjectMessage_Rate.setnID_HistoryEvent_Service(nID_HistoryEvent_Service);
                }
                subjectMessagesDao.setMessage(oSubjectMessage_Rate);
            }

            //сохранения сообщения с рейтингом, а на региональном сервере, т.к. именно там хранится экземпляр БП.
            if (oHistoryEvent_Service.getnID_Proccess_Feedback() != null) {//issue 1006
                String snID_Process = "" + oHistoryEvent_Service.getnID_Proccess_Feedback();
                Integer nID_Server = oHistoryEvent_Service.getnID_Server();
                LOG.info("set rate={} to the nID_Proccess_Feedback={}", nRate, snID_Process);
                List<String> aTaskIds = bpService.getProcessTasks(nID_Server, snID_Process);
                LOG.info("Found '{}' tasks by nID_Proccess_Feedback...", aTaskIds.size());
                if (!aTaskIds.isEmpty()) {//when process is not complete
                    bpService.setVariableToProcessInstance(nID_Server, snID_Process, "nID_Rate", nRate);
                    LOG.info("process is not complete -- change rate in it!");
                    for (String sTaskId : aTaskIds) {
                        bpService.setVariableToActivitiTask(nID_Server, sTaskId, "nID_Rate", nRate);
                    }
                }
            }
            String sURL_Redirect = generalConfig.sHostCentral() + "/feedback?sID_Order=" + sID_Order + "&sSecret=" + sToken;
            LOG.info("Redirecting to URL:{}", sURL_Redirect);
            oResponse.sendRedirect(sURL_Redirect);

        } catch (CommonServiceException oActivitiRestException) {
            LOG.error("FAIL: {}", oActivitiRestException.getMessage());
            throw oActivitiRestException;
        } catch (Exception e) {
            LOG.error("FAIL:", e);
            throw new CommonServiceException(404, "[setMessageRate](sID_Order: " + sID_Order + ", nRate: " + nRate + "): Unknown exception: " + e.getMessage());
        }
        return sReturn;//"Ok!";
    }
    
    

    @ApiOperation(value = "Перенос данных из поля sMail в поле nID_SubjectContact_Mail таблицы SubjectMessage", notes = "##### SubjectMessageController - Сообщения субьектов. Переносит данные с поля sMail в nID_SubjectContact_Mail таблицы SubjectMessage. #####\n\n"
            + "Возвращает список из 100 первых измененных записей таблицы.  Метод также подчищает данные из sMail, устанавливая занчение null\n\n"
            + "HTTP Context: https://test.igov.org.ua/wf/service/subject/message/transferDataFromMail\n\n\n"
            + "Пример ответа:\n"
            + "\n```json\n"
            + "[\n"
            + " {\n"
            + "  \"mail\":\"test@igov.org.ua\",\n"
            + "  \"oMail\":{\"subjectContactType\":{\"sName_EN\":\"Email\",\"sName_UA\":\"Електрона адреса\",\"sName_RU\":\"Электнонный адрес\",\"nID\":1},\"sValue\":\"test@igov.org.ua\",\"sDate\":null,\"nID\":1},\n"
            + "  \"sBody_Indirectly\":\"Body Inderectly\",\n"
            + "  \"nID_HistoryEvent_Service\":3,\n"
            + "  \"nID\":1,\n"
            + "  \"sHead\":\"head\",\n"
            + "  \"sBody\":\"body of subject message\",\n"
            + "  \"sDate\":\"2015-12-21 14:09:56.235\",\n"
            + "  \"nID_Subject\":1,\n"
            + "  \"sContacts\":\"contact\",\n"
            + "  \"sData\":\"data\",\n"
            + "  \"oSubjectMessageType\":{\"sDescription\":\"Оценка услуги\",\"nID\":1,\"sName\":\"ServiceRate\"}\n"
            + " },\n"
            + " {\n"
            + "  \"mail\":\"test@igov.org.ua\",\n"
            + "  \"oMail\":{\"subjectContactType\":{\"sName_EN\":\"Email\",\"sName_UA\":\"Електрона адреса\",\"sName_RU\":\"Электнонный адрес\",\"nID\":1},\"sValue\":\"test@igov.org.ua\",\"sDate\":null,\"nID\":1},\n"
            + "  \"sBody_Indirectly\":\"Body Inderectly\",\n"
            + "  \"nID_HistoryEvent_Service\":4,\n"
            + "  \"nID\":2,\n"
            + "  \"sHead\":\"head2\",\n"
            + "  \"sBody\":\"\",\n"
            + "  \"sDate\":\"2015-12-21 14:09:56.235\",\n"
            + "  \"nID_Subject\":1,\n"
            + "  \"sContacts\":\"contact\",\n"
            + "  \"sData\":\"data\",\n"
            + "  \"oSubjectMessageType\":{\"sDescription\":\"Оценка услуги\",\"nID\":1,\"sName\":\"ServiceRate\"}\n"
            + " }\n"
            + "]\n"
            + "\n```\n"
            + "\n\nДанные из поля sMail таблицы SubjectMessage переносятся в поле nID_SubjectMessage_Mail (объект oMail).\n"
            + "Значения в поле sMail устанавливаются в null\n"
            + "Если происходит исключение во время переноса данных, возвращается 403.\n")
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "В случае появления исключения обработки sql-запросов"),
            @ApiResponse(code = 200, message = "В случае успеха возвращает список первых 100 записей измененных с сущностями SubjectMessage")})
    @RequestMapping(value = "/subject/message/transferDataFromMail", method = RequestMethod.GET)
    public
    @ResponseBody
    List transferDataFromMail() throws CommonServiceException {
        List subjectMessages;
        try {
            subjectMessages = subjectMessagesDao.tranferDataFromMailToSubjectMail();
        } catch (Exception e) {
        	LOG.trace("FAIL:", e);
            throw new CommonServiceException(
                    ExceptionCommonController.BUSINESS_ERROR_CODE,
                    e.getMessage(),
                    HttpStatus.FORBIDDEN
            );
        }
        return subjectMessages;
    }    
    
    
    
    @ApiOperation(value = "Сохранить системное событие", notes = "Необходим для сбора логов из разных источников, например с криентского приложения")
    //, headers = "content-type=text/*"
    //headers = {"Accept=application/json"}, 
    //, produces = "application/json;charset=UTF-8"    
    @RequestMapping(value = "/action/event/setEventSystem", method = {RequestMethod.GET, RequestMethod.POST})
    public
    @ResponseBody
    Long setEventSystem(
            @ApiParam(value = "", required = false) @RequestParam(value = "sType", required = false) String sType,
            @ApiParam(value = "Номер-ИД субьекта", required = false) @RequestParam(value = "nID_Subject", required = false) Long nID_Subject,
            @ApiParam(value = "Номер-ИД сервера", required = false) @RequestParam(value = "nID_Server", required = false) Long nID_Server,
            @ApiParam(value = "", required = false) @RequestParam(value = "sFunction", required = false) String sFunction,
            @ApiParam(value = "", required = false) @RequestParam(value = "sHead", required = false) String sHead,
            @ApiParam(value = "", required = false) @RequestParam(value = "sBody", required = false) String sBody,
            @ApiParam(value = "", required = false) @RequestParam(value = "sError", required = false) String sError,
            @ApiParam(value = "Карта других параметров", required = false) @RequestBody String smData
        ) throws CommonServiceException {
        //List subjectMessages;
        try {
            
            //oLog_External.info("sType={},nID_Subject={},nID_Server={},sFunction={},sHead={},sBody={},sError={},smData={}",sType,nID_Subject,nID_Server,sFunction,sHead,sBody,sError,smData);
            //LOG_MIN.info("sType={},nID_Subject={},sFunction={},sHead={},sBody={},sError={}",sType,nID_Subject,sFunction,sHead,sBody,sError);
            List<String> asParam = new LinkedList();
            Map<String,Object> moResponse = new HashMap();
            String sResponseMessage=null;
            String sResponseCode=null;
            String soResponseData=null;
            String sDate=null;
            List<String> mParamResponse = new LinkedList();
            if(smData!=null){
                Map<String, Object> moData = JsonRestUtils.readObject(smData, Map.class);
                //Map<String, Object> mAttributeReturn = new HashMap();
                //List<SubjectOrganJoinAttribute> aSubjectOrganJoinAttribute = subjectOrganJoinAttributeDao.getSubjectOrganJoinAttributesByParent(nID);
                //var oBody={oResponse:oMessage.oData.oResponse,asParam:oMessage.oData.asParam,sDate:oMessage.sDate};
                    //this.add({oResponse:{sMessage: oResponse.message}});
                    //this.add({oResponse:{sCode: oResponse.code}});
                    //this.add({oResponse:{soData: JSON.stringify(oResponse)}});
                if (moData != null) {
                    //aSubjectOrganJoinAttribute = new LinkedList();
                    if(moData.containsKey("asParam")){
                        asParam = (List<String>) moData.get("asParam");
                    }
                    if(moData.containsKey("oResponse")){
                        moResponse = (Map<String,Object>) moData.get("oResponse");
                        if (moResponse != null) {
                            if(moResponse.containsKey("sMessage")){
                                sResponseMessage=(String) moResponse.get("sMessage");
                            }
                            if(moResponse.containsKey("sCode")){
                                sResponseCode=(String) moResponse.get("sCode");
                            }
                            if(moResponse.containsKey("soData")){
                                soResponseData=(String) moResponse.get("soData");
                            }
                        }
                    }
                    if(moData.containsKey("sDate")){
                        sDate = (String) moData.get("sDate");
                    }
                }
            }
            LOG_MIN.info("{}|{}|{}|{}|{}|{}|{}|{}|{}|",sType,nID_Subject,sFunction,sHead,sBody,sError,sResponseMessage,sResponseCode,soResponseData);
            LOG_MID.info("{}|{}|{}|{}|{}|{}|{}|{}|{}|{}|",sType,nID_Subject,sFunction,sHead,sBody,sError,sResponseMessage,sResponseCode,soResponseData,asParam);
            //LOG_BIG.debug("sType={},nID_Subject={},nID_Server={},sFunction={},sHead={},sBody={},sError={},smData={}",sType,nID_Subject,nID_Server,sFunction,sHead,sBody,sError,smData);
            LOG_MAX.debug("sType={}|nID_Subject={}|nID_Server={}|sFunction={}|sHead={}|sBody={}|sError={}|smData={}|sResponseMessage={}|sResponseCode={}|soResponseData={}|asParam={}|sDate={}",sType,nID_Subject,nID_Server,sFunction,sHead,sBody,sError,sResponseMessage,sResponseCode,soResponseData,asParam,sDate);
            //subjectMessages = subjectMessagesDao.tranferDataFromMailToSubjectMail();
        } catch (Exception e) {
        	LOG.trace("FAIL:", e);
            throw new CommonServiceException(
                    ExceptionCommonController.BUSINESS_ERROR_CODE,
                    e.getMessage(),
                    HttpStatus.FORBIDDEN
            );
        }
        return Long.valueOf(0+"");//subjectMessages
    }        
    

}
