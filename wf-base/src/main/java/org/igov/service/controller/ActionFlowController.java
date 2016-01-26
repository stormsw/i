package org.igov.service.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.igov.model.flow.FlowProperty;
import org.igov.model.flow.FlowSlotTicket;
import org.igov.model.subject.SubjectOrganDepartment;
import org.igov.service.business.flow.FlowService;
import org.igov.service.business.flow.slot.ClearSlotsResult;
import org.igov.service.business.flow.slot.Days;
import org.igov.service.business.flow.slot.FlowSlotVO;
import org.igov.service.business.flow.slot.SaveFlowSlotTicketResponse;
import org.igov.service.exception.RecordNotFoundException;
import org.igov.util.convert.JsonDateSerializer;
import org.igov.util.convert.JsonRestUtils;
import org.igov.util.convert.QuartzUtil;
import org.joda.time.DateTime;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * User: goodg_000
 * Date: 21.06.2015
 * Time: 14:02
 */
@Controller
@Api(tags = { "ActionFlowController" }, description = "Действия очередей (слоты потока, расписания и тикеты)")
@RequestMapping(value = "/action/flow")
public class ActionFlowController {

    private static final Logger LOG = LoggerFactory.getLogger(ActionFlowController.class);

	@Autowired
    private FlowService oFlowService;


    /**
     * Получение слотов по сервису сгруппированных по дням.
     * @param nID_Service номер-ИД услуги  (обязательный если нет sID_BP и nID_ServiceData)
     * @param nID_ServiceData ID сущности ServiceData (обязательный если нет sID_BP и nID_Service)
     * @param sID_BP строка-ИД бизнес-процесса (обязательный если нет nID_ServiceData и nID_Service)
     * @param nID_SubjectOrganDepartment ID департамента субьекта-органа  (опциональный, по умолчанию false)
     * @param bAll если false то из возвращаемого объекта исключаются элементы, содержащие "bHasFree":false "bFree":false (опциональный, по умолчанию false)
     * @param nDays колличество дней от сегодняшего включительно(или sDateStart, если задан), до nDays в будующее за который нужно вернуть слоты (опциональный, по умолчанию 177 - пол года)
     * @param nFreeDays дни со слотами будут включаться в результат пока не наберется указанное кол-во свободных дней (опциональный, по умолчанию 60)
     * @param sDateStart опциональный параметр, определяющие дату начала в формате "yyyy-MM-dd", с которую выбрать слоты. При наличии этого параметра слоты возвращаются только за указанный период(число дней задается nDays).
     */
    @ApiOperation(value = "Получение слотов по сервису сгруппированных по дням", notes = "##### Электронные очереди. Получение слотов по сервису сгруппированных по дням #####\n\n"
	        + "HTTP Context: http://server:port/wf/service/action/flow/getFlowSlots_ServiceData\n\n"
	        + "Пример:\n"
	        + "https://test.igov.org.ua/wf/service/action/flow/getFlowSlots_ServiceData?nID_ServiceData=1\n"
	        + "или\n"
	        + "https://test.region.igov.org.ua/wf/service/action/flow/getSheduleFlowIncludes?sID_BP=kiev_mreo_1\n\n"
	        + "Ответ: HTTP STATUS 200\n\n"
	        + "\n```json\n"
	        + "{\n"
	        + "    \"aDay\": [\n"
	        + "        {\n"
	        + "            \"sDate\": \"2015-07-19\",\n"
	        + "            \"bHasFree\": true,\n"
	        + "            \"aSlot\": [\n"
	        + "                {\n"
	        + "                    \"nID\": 1,\n"
	        + "                    \"sTime\": \"18:00\",\n"
	        + "                    \"nMinutes\": 15,\n"
	        + "                    \"bFree\": true\n"
	        + "                }\n"
	        + "            ]\n"
	        + "        },\n"
	        + "        {\n"
	        + "            \"sDate\": \"2015-07-20\",\n"
	        + "            \"bHasFree\": true,\n"
	        + "            \"aSlot\": [\n"
	        + "                {\n"
	        + "                    \"nID\": 3,\n"
	        + "                    \"sTime\": \"18:15\",\n"
	        + "                    \"nMinutes\": 15,\n"
	        + "                    \"bFree\": true\n"
	        + "                }\n"
	        + "            ]\n"
	        + "        }\n"
	        + "    ]\n"
	        + "}\n\n"
	        + "\n```\n"
	        + "Калькулируемые поля в ответе:\n\n"
	        + "- флаг \"bFree\" - является ли слот свободным? Слот считается свободным если на него нету тикетов у которых nID_Task_Activiti равен null, а у тех у которых nID_Task_Activiti = null - время создания тикета (sDateEdit) не позднее чем текущее время минус 5 минут (предопределенная константа)\n\n"
	        + "- флаг \"bHasFree\" равен true , если данных день содержит хотя бы один свободный слот.\n" )
    @RequestMapping(value = "/getFlowSlots_ServiceData", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity getFlowSlots(
	    @ApiParam(value = "номер-ИД услуги  (обязательный если нет sID_BP и nID_ServiceData)", required = false) @RequestParam(value = "nID_Service", required = false) Long nID_Service,
	    @ApiParam(value = "ID сущности ServiceData (обязательный если нет sID_BP и nID_Service)", required = false) @RequestParam(value = "nID_ServiceData", required = false) Long nID_ServiceData,
	    @ApiParam(value = "строка-ИД бизнес-процесса (обязательный если нет nID_ServiceData и nID_Service)", required = false) @RequestParam(value = "sID_BP", required = false) String sID_BP,
	    @ApiParam(value = "ID департамента субьекта-органа", required = false) @RequestParam(value = "nID_SubjectOrganDepartment", required = false) Long nID_SubjectOrganDepartment,
	    @ApiParam(value = "если false то из возвращаемого объекта исключаются элементы, содержащие \"bHasFree\":false \"bFree\":false (опциональный, по умолчанию false)", required = false) @RequestParam(value = "bAll", required = false, defaultValue = "false") boolean bAll,
	    @ApiParam(value = "дни со слотами будут включаться в результат пока не наберется указанное кол-во свободных дней (опциональный, по умолчанию 60)", required = false) @RequestParam(value = "nFreeDays", required = false, defaultValue = "60") int nFreeDays,
	    @ApiParam(value = "количество дней от сегодняшего включительно(или sDateStart, если задан), до nDays в будующее за который нужно вернуть слоты (опциональный, по умолчанию 177 - пол года)", required = false) @RequestParam(value = "nDays", required = false, defaultValue = "177") int nDays,
	    @ApiParam(value = "параметр, определяющие дату начала в формате \"yyyy-MM-dd\", с которую выбрать слоты. При наличии этого параметра слоты возвращаются только за указанный период(число дней задается nDays)", required = false) @RequestParam(value = "sDateStart", required = false) String sDateStart
    ) throws Exception {

        DateTime oDateStart = DateTime.now().withTimeAtStartOfDay();
        oDateStart = oDateStart.plusDays(2);
        DateTime oDateEnd = oDateStart.plusDays(nDays);

        if (sDateStart != null) {
            oDateStart = JsonDateSerializer.DATE_FORMATTER.parseDateTime(sDateStart);
            oDateEnd = oDateStart.plusDays(nDays);
        }

        Days res = oFlowService.getFlowSlots(nID_Service, nID_ServiceData, sID_BP, nID_SubjectOrganDepartment,
                oDateStart, oDateEnd, bAll, nFreeDays);

        return JsonRestUtils.toJsonResponse(res);
    }

    /**
     * @param sID_BP имя Activiti BP
     */
    @ApiOperation(value = "Получение массива объектов SubjectOrganDepartment по ID бизнес процесса", notes = "##### Электронные очереди. Получение массива объектов SubjectOrganDepartment по ID бизнес процесса #####\n\n"
	        + "HTTP Context: https://test.region.igov.org.ua/wf/service/action/flow/getFlowSlots_Department?sID_BP=sID_BP  возвращает массив объектов SubjectOrganDepartment для указанного Activiti BP.\n\n"
	        + "Примеры:\n"
	        + "https://test.region.igov.org.ua/wf/service/action/flow/getFlowSlots_Department?sID_BP=dnepr_dms-89\n\n"
	        + "Ответ:\n\n"
	        + "\n```json\n"
	        + "[\n"
	        + "  {\n"
	        + "    \"sName\": \"ДМС, Днепр, пр. Ильича, 3 (dnepr_dms-89,dnepr_dms-89s)\",\n"
	        + "    \"nID_SubjectOrgan\": 2,\n"
	        + "    \"sGroup_Activiti\": \"dnepr_dms_89_bab\",\n"
	        + "    \"nID\": 13\n"
	        + "  },\n"
	        + "  {\n"
	        + "    \"sName\": \"ДМС, Днепр, вул. Шевченко, 7 (dnepr_dms-89,dnepr_dms-89s)\",\n"
	        + "    \"nID_SubjectOrgan\": 2,\n"
	        + "    \"sGroup_Activiti\": \"dnepr_dms_89_zhovt\",\n"
	        + "    \"nID\": 14\n"
	        + "  }\n"
	        + "]\n"
	        + "\n```\n" )
    @RequestMapping(value = "/getFlowSlots_Department", method = RequestMethod.GET)
    public
    @ResponseBody
    ResponseEntity getFlowSlotDepartment( @ApiParam(value = "имя Activiti BP", required = true)  @RequestParam(value = "sID_BP") String sID_BP
    ) throws Exception {

		SubjectOrganDepartment[] result = oFlowService.getSubjectOrganDepartments(sID_BP);

        return JsonRestUtils.toJsonResponse(result);
    }

	@ApiOperation(value = "Создание или обновление тикета в указанном слоте.", notes = "##### Электронные очереди. Создание или обновление тикета в указанном слоте #####\n\n"
	        + "HTTP Context: http://server:port/wf/service/action/flow/setFlowSlots_ServiceData\n\n"
	        + "Пример: http://test.igov.org.ua/wf/service/action/flow/setFlowSlot_ServiceData\n\n"
	        + "- nID_FlowSlot=1\n"
	        + "- nID_Subject=2\n\n"
	        + "Ответ: HTTP STATUS 200\n\n"
	        + "\n```json\n"
	        + "{ \"nID_Ticket\": 1000 }\n"
	        + "\n```\n"
	        + "Поля в ответе:\n\n"
	        + "-поле \"nID_Ticket\" - ID созданной/измененной сущности FlowSlotTicket.\n" )
    @RequestMapping(value = "/setFlowSlot_ServiceData", method = RequestMethod.POST)
    public
    @ResponseBody
    ResponseEntity saveFlowSlotTicket(@ApiParam(value = "ID сущности FlowSlot", required = true) @RequestParam(value = "nID_FlowSlot") Long nID_FlowSlot,
	    @ApiParam(value = "ID сущнсоти Subject - субьект пользователь услуги, который подписывается на слот", required = true) @RequestParam(value = "nID_Subject") Long nID_Subject,
	    @ApiParam(value = "ID таски активити процесса предоставления услуги (не обязательный - вначале он null, а потом засчивается после подтверждения тикета, и создания процесса)", required = false) @RequestParam(value = "nID_Task_Activiti", required = false) Long nID_Task_Activiti) throws Exception {

        FlowSlotTicket oFlowSlotTicket = oFlowService.saveFlowSlotTicket(nID_FlowSlot, nID_Subject, nID_Task_Activiti);

        return JsonRestUtils.toJsonResponse(new SaveFlowSlotTicketResponse(oFlowSlotTicket.getId()));
    }

    /**
     * Генерация слотов на заданный интервал для заданного потока.
     * @param nID_Flow_ServiceData номер-ИД потока (обязательный если нет sID_BP)
     * @param sID_BP строка-ИД бизнес-процесса потока (обязательный если нет nID_Flow_ServiceData)
     * @param sDateStart дата "начиная с такого-то момента времени", в формате "2015-06-28 12:12:56.001" (опциональный)
     * @param sDateStop дата "заканчивая к такому-то моменту времени", в формате "2015-07-28 12:12:56.001" (опциональный)
     */
    @ApiOperation(value = "Генерация слотов на заданный интервал для заданного потока", notes = "##### Электронные очереди. Генерация слотов на заданный интервал для заданного потока #####\n\n"
	        + "HTTP Context: http://server:port/wf/service/action/flow/buildFlowSlots\n\n"
	        + "Пример: http://test.igov.org.ua/wf/service/action/flow/buildFlowSlots\n\n"
	        + "- nID_Flow_ServiceData=1\n\n"
	        + "- sDateStart=2015-06-01 00:00:00.000\n\n"
	        + "- sDateStop=2015-06-07 00:00:00.000\n\n"
	        + "Ответ: HTTP STATUS 200 + json перечисление всех сгенерированных слотов.\n\n"
	        + "Ниже приведена часть json ответа:\n\n"
	        + "\n```json\n"
	        + "[\n"
	        + "    {\n"
	        + "        \"nID\": 1000,\n"
	        + "        \"sTime\": \"08:00\",\n"
	        + "        \"nMinutes\": 15,\n"
	        + "        \"bFree\": true\n"
	        + "    },\n"
	        + "    {\n"
	        + "        \"nID\": 1001,\n"
	        + "        \"sTime\": \"08:15\",\n"
	        + "        \"nMinutes\": 15,\n"
	        + "        \"bFree\": true\n"
	        + "    },\n"
	        + "    {\n"
	        + "        \"nID\": 1002,\n"
	        + "        \"sTime\": \"08:30\",\n"
	        + "        \"nMinutes\": 15,\n"
	        + "        \"bFree\": true\n"
	        + "    },\n"
	        + "...\n"
	        + "]\n\n"
	        + "Если на указанные даты слоты уже сгенерены то они не будут генерится повторно, и в ответ включаться не будут.\n"
	        + "\n```\n" )
    @RequestMapping(value = "/buildFlowSlots", method = RequestMethod.POST)
    public
    @ResponseBody
    ResponseEntity buildFlowSlots(
	    @ApiParam(value = "номер-ИД потока (обязательный если нет sID_BP)", required = false) @RequestParam(value = "nID_Flow_ServiceData", required = false) Long nID_Flow_ServiceData,
	    @ApiParam(value = "строка-ИД бизнес-процесса потока (обязательный если нет nID_Flow_ServiceData)", required = false) @RequestParam(value = "sID_BP", required = false) String sID_BP,
	    @ApiParam(value = "номер-ИН департамента", required = false) @RequestParam(value = "nID_SubjectOrganDepartment", required = false) Long nID_SubjectOrganDepartment,
	    @ApiParam(value = "дата, начиная с такого-то момента времени, в формате \"2015-06-28 12:12:56.001\"", required = false) @RequestParam(value = "sDateStart", required = false) String sDateStart,
	    @ApiParam(value = "дата, заканчивая к такому-то моменту времени, в формате \"2015-07-28 12:12:56.001\"", required = false) @RequestParam(value = "sDateStop", required = false) String sDateStop) {

		DateTime startDate = oFlowService.parseJsonDateTimeSerializer(sDateStart);
		DateTime stopDate = oFlowService.parseJsonDateTimeSerializer(sDateStop);

		try {
			nID_Flow_ServiceData = oFlowService.determineFlowServiceDataID(
					nID_Flow_ServiceData, sID_BP, nID_SubjectOrganDepartment);
		} catch (RecordNotFoundException e) {
			LOG.error(e.getMessage());
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

        List<FlowSlotVO> res = oFlowService.buildFlowSlots(nID_Flow_ServiceData, startDate, stopDate);

        return JsonRestUtils.toJsonResponse(res);
    }

    /**
     * Удаление слотов на заданный интервал для заданного потока.
     * @param nID_Flow_ServiceData номер-ИД потока (обязательный если нет sID_BP)
     * @param sID_BP строка-ИД бизнес-процесса потока (обязательный если нет nID_Flow_ServiceData)
     * @param sDateStart дата "начиная с такого-то момента времени", в формате "2015-06-28 12:12:56.001" (обязательный)
     * @param sDateStop дата "заканчивая к такому-то моменту времени", в формате "2015-07-28 12:12:56.001" (обязательный)
     * @param bWithTickets удалять ли слоты с тикетами, отвязывая тикеты от слотов? (опциональный, по умолчанию false)
     * @param bWithTickets слоты с тикетами. Елси bWithTickets=true то эти слоты тоже удаляются и будут перечислены в aDeletedSlot, иначе - не удаляются.
     */
    @ApiOperation(value = "Удаление слотов на заданный интервал для заданного потока", notes = "##### Электронные очереди. Удаление слотов на заданный интервал для заданного потока #####\n\n"
	        + "HTTP Context: http://server:port/wf/service/action/flow/clearFlowSlots\n\n"
	        + "Пример:\n"
	        + "\n```\n"
	        + "http://test.igov.org.ua/wf/service/action/flow/clearFlowSlots?nID_Flow_ServiceData=1&sDateStart=2015-06-01 00:00:00.000&sDateStop=2015-06-07 00:00:00.000\n\n"
	        + "\n```\n"
	        + "Ответ: HTTP STATUS 200 + json Обьект содержащий 2 списка:\n\n"
	        + "- aDeletedSlot - удаленные слоты\n"
	        + "- aSlotWithTickets - слоты с тикетами. Елси bWithTickets=true то эти слоты тоже удаляются и будут перечислены в aDeletedSlot, иначе - не удаляются.\n\n"
	        + "Ниже приведена часть json ответа:\n\n"
	        + "\n```json\n"
	        + "{\n"
	        + "    \"aDeletedSlot\": [\n"
	        + "        {\n"
	        + "            \"nID\": 1000,\n"
	        + "            \"sTime\": \"08:00\",\n"
	        + "            \"nMinutes\": 15,\n"
	        + "            \"bFree\": true\n"
	        + "        },\n"
	        + "        {\n"
	        + "            \"nID\": 1001,\n"
	        + "            \"sTime\": \"08:15\",\n"
	        + "            \"nMinutes\": 15,\n"
	        + "            \"bFree\": true\n"
	        + "        },\n"
	        + "        ...\n"
	        + "     ],\n"
	        + "     \"aSlotWithTickets\": []\n"
	        + "}\n"
	        + "\n```\n" )
    @RequestMapping(value = "/clearFlowSlots", method = RequestMethod.DELETE)
    public
    @ResponseBody
    ResponseEntity clearFlowSlots(
	    @ApiParam(value = "номер-ИД потока (обязательный если нет sID_BP)", required = false) @RequestParam(value = "nID_Flow_ServiceData", required = false) Long nID_Flow_ServiceData,
	    @ApiParam(value = "строка-ИД бизнес-процесса потока (обязательный если нет nID_Flow_ServiceData)", required = false) @RequestParam(value = "sID_BP", required = false) String sID_BP,
	    @ApiParam(value = "номер-ИН департамента", required = false) @RequestParam(value = "nID_SubjectOrganDepartment", required = false) Long nID_SubjectOrganDepartment,
            @ApiParam(value = "дата, начиная с такого-то момента времени, в формате \"2015-06-28 12:12:56.001\"", required = true) @RequestParam(value = "sDateStart") String sDateStart,
            @ApiParam(value = "дата, заканчивая к такому-то моменту времени, в формате \"2015-07-28 12:12:56.001\"", required = true) @RequestParam(value = "sDateStop") String sDateStop,
            @ApiParam(value = "слоты с тикетами. Елси bWithTickets=true то эти слоты тоже удаляются и будут перечислены в aDeletedSlot, иначе - не удаляются.", required = false) @RequestParam(value = "bWithTickets", required = false, defaultValue = "false")
            boolean bWithTickets) {

		DateTime startDate = oFlowService.parseJsonDateTimeSerializer(sDateStart);
		DateTime stopDate = oFlowService.parseJsonDateTimeSerializer(sDateStop);

		try {
			nID_Flow_ServiceData = oFlowService.determineFlowServiceDataID(
					nID_Flow_ServiceData, sID_BP, nID_SubjectOrganDepartment);
		} catch (RecordNotFoundException e) {
			LOG.error("Error: {}", e.getMessage());
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		ClearSlotsResult res = oFlowService.clearFlowSlots(nID_Flow_ServiceData, startDate, stopDate, bWithTickets);
        return JsonRestUtils.toJsonResponse(res);
    }

    /**
     * Returns list of included schedules within flow
     *
     * @param nID_Flow_ServiceData - ID of flow
     * @return List of schedule with bExclude=false
     */
    @ApiOperation(value = "Получение расписаний включений", notes = "##### Электронные очереди. Получение расписаний включений #####\n\n"
	        + "HTTP Context: https://test.region.igov.org.ua/wf/service/action/flow/getSheduleFlowIncludes?nID_Flow_ServiceData=flowId\n\n"
	        + "Пример:\n"
	        + "https://test.region.igov.org.ua/wf/service/action/flow/getSheduleFlowIncludes?nID_Flow_ServiceData=1\n\n"
	        + "Пример результата\n\n"
	        + "\n```json\n"
	        + "[\n"
	        + "  {\n"
	        + "    \"sData\": null,\n"
	        + "    \"bExclude\": false,\n"
	        + "    \"sName\": \"Test\",\n"
	        + "    \"sRegionTime\": \"\"10:30-11:30\"\",\n"
	        + "    \"saRegionWeekDay\": \"\"mo,tu\"\",\n"
	        + "    \"sDateTimeAt\": \"\"2010-08-01 10:10:30\"\",\n"
	        + "    \"sDateTimeTo\": \"\"2010-08-01 18:10:00\"\",\n"
	        + "    \"nID\": 20367,\n"
	        + "    \"nID_FlowPropertyClass\": {\n"
	        + "      \"sPath\": \"org.igov.service.business.flow.handler.DefaultFlowSlotScheduler\",\n"
	        + "      \"sBeanName\": \"defaultFlowSlotScheduler\",\n"
	        + "      \"nID\": 1,\n"
	        + "      \"sName\": \"DefaultFlowSlotScheduler\"\n"
	        + "    }\n"
	        + "  },\n"
	        + "  {\n"
	        + "    \"sData\": null,\n"
	        + "    \"bExclude\": false,\n"
	        + "    \"sName\": \"Test\",\n"
	        + "    \"sRegionTime\": \"10:30-11:30\",\n"
	        + "    \"saRegionWeekDay\": \"mo,tu\",\n"
	        + "    \"sDateTimeAt\": \"10:30\",\n"
	        + "    \"sDateTimeTo\": \"12:30\",\n"
	        + "    \"nID\": 20364,\n"
	        + "    \"nID_FlowPropertyClass\": {\n"
	        + "      \"sPath\": \"org.igov.service.business.flow.handler.DefaultFlowSlotScheduler\",\n"
	        + "      \"sBeanName\": \"defaultFlowSlotScheduler\",\n"
	        + "      \"nID\": 1,\n"
	        + "      \"sName\": \"DefaultFlowSlotScheduler\"\n"
	        + "    }\n"
	        + "  }\n"
	        + "]\n"
	        + "\n```\n" )
    @RequestMapping(value = "/getSheduleFlowIncludes", method = RequestMethod.GET)
    public
    @ResponseBody
    List<FlowProperty> getSheduleFlowIncludes(
	    @ApiParam(value = "ID потока", required = false) @RequestParam(value = "nID_Flow_ServiceData", required = false) Long nID_Flow_ServiceData,
	    @ApiParam(value = "строка-ИД БизнесПроцесса", required = false) @RequestParam(value = "sID_BP", required = false) String sID_BP,
	    @ApiParam(value = "номер-ИН департамента", required = false) @RequestParam(value = "nID_SubjectOrganDepartment", required = false) Long nID_SubjectOrganDepartment
    ) throws Exception {

		return oFlowService.getFilteredFlowPropertiesForFlowServiceData(nID_Flow_ServiceData, sID_BP, nID_SubjectOrganDepartment,
                Boolean.FALSE);
    }

    /**
     * Returns list of excluded schedules within flow
     *
     * @param nID_Flow_ServiceData - ID of flow
     * @return List of schedule with bExclude=false
     */
    @ApiOperation(value = "Получение расписаний исключений", notes = "##### Электронные очереди. Получение расписаний исключений #####\n\n"
	        + "HTTP Context: https://test.region.igov.org.ua/wf/service/action/flow/getSheduleFlowExcludes?nID_Flow_ServiceData=flowId*\n\n"
	        + "Пример:\n"
	        + "https://test.region.igov.org.ua/wf/service/action/flow/getSheduleFlowExcludes?nID_Flow_ServiceData=1\n\n"
	        + "Пример результата\n"
	        + "\n```json\n"
	        + "[\n"
	        + "  {\n"
	        + "    \"sData\": null,\n"
	        + "    \"bExclude\": true,\n"
	        + "    \"sName\": \"Test\",\n"
	        + "    \"sRegionTime\": \"10:30-11:30\",\n"
	        + "    \"saRegionWeekDay\": \"mo,tu\"\n"
	        + "    \"sDateTimeAt\": \"2010-08-01 10:10:30\",\n"
	        + "    \"sDateTimeTo\": \"2010-08-01 18:10:00\",\n"
	        + "    \"nID\": 20367,\n"
	        + "    \"nID_FlowPropertyClass\": {\n"
	        + "      \"sPath\": \"org.igov.service.business.flow.handler.DefaultFlowSlotScheduler\",\n"
	        + "      \"sBeanName\": \"defaultFlowSlotScheduler\",\n"
	        + "      \"nID\": 1,\n"
	        + "      \"sName\": \"DefaultFlowSlotScheduler\"\n"
	        + "    }\n"
	        + "  },\n"
	        + "  {\n"
	        + "    \"sData\": null,\n"
	        + "    \"bExclude\": false,\n"
	        + "    \"sName\": \"Test\",\n"
	        + "    \"sRegionTime\": \"10:30-11:30\",\n"
	        + "    \"saRegionWeekDay\": \"mo,tu\",\n"
	        + "    \"sDateTimeAt\": \"10:30\",\n"
	        + "    \"sDateTimeTo\": \"12:30\",\n"
	        + "    \"nID\": 20364,\n"
	        + "    \"nID_FlowPropertyClass\": {\n"
	        + "      \"sPath\": \"org.igov.service.business.flow.handler.DefaultFlowSlotScheduler\",\n"
	        + "      \"sBeanName\": \"defaultFlowSlotScheduler\",\n"
	        + "      \"nID\": 1,\n"
	        + "      \"sName\": \"DefaultFlowSlotScheduler\"\n"
	        + "    }\n"
	        + "  }\n"
	        + "]\n"
	        + "\n```\n" )
    @RequestMapping(value = "/getSheduleFlowExcludes", method = RequestMethod.GET)
    public
    @ResponseBody
    List<FlowProperty> getSheduleFlowExcludes(
	    @ApiParam(value = "ID потока", required = false) @RequestParam(value = "nID_Flow_ServiceData", required = false) Long nID_Flow_ServiceData,
	    @ApiParam(value = "строка-ИД БизнесПроцесса", required = false) @RequestParam(value = "sID_BP", required = false) String sID_BP,
	    @ApiParam(value = "номер-ИН департамента", required = false) @RequestParam(value = "nID_SubjectOrganDepartment", required = false) Long nID_SubjectOrganDepartment
    ) throws Exception {

		return oFlowService.getFilteredFlowPropertiesForFlowServiceData(nID_Flow_ServiceData, sID_BP, nID_SubjectOrganDepartment,
                Boolean.TRUE);
    }

    /**
     * Adds/removes schedule include slot
     *
     * @param nID                  - ID of flow property
     * @param nID_Flow_ServiceData - ID of flow
     * @param sName                - name of the slot
     * @param sRegionTime          - time period, "14:16-16:30"
     * @param saRegionWeekDay      - array of days in a week ("su,mo,tu")
     * @return ID of new FlowProperty
     */
    @ApiOperation(value = "Добавление/изменение расписания включений", notes = "##### Электронные очереди. Добавление/изменение расписания включений #####\n\n"
	        + "HTTP Context: https://test.region.igov.org.ua/wf/service/action/flow/setSheduleFlowInclude?nID_Flow_ServiceData=nID_Flow_ServiceData&sName=sName&sRegionTime=sRegionTime&sDateTimeAt=sDateTimeAt&sDateTimeTo=sDateTimeTo&saRegionWeekDay=saRegionWeekDay\n\n"
	        + "Пример:\n"
	        + "https://test.region.igov.org.ua/wf/service/action/flow/setSheduleFlowInclude?nID_Flow_ServiceData=1&sName=Test&sRegionTime=%2210:30-11:30%22&sDateTimeAt=%222010-08-01%2010:10:30%22&sDateTimeTo=%222010-08-01%2018:10:00%22&saRegionWeekDay=%22mo,tu%22\n\n"
	        + "Пример результата\n"
	        + "\n```json\n"
	        + "{\n"
	        + "  \"sData\": null,\n"
	        + "  \"bExclude\": false,\n"
	        + "  \"sName\": \"Test\",\n"
	        + "  \"sRegionTime\": \"10:30-11:30\",\n"
	        + "  \"saRegionWeekDay\": \"mo,tu\",\n"
	        + "  \"sDateTimeAt\": \"2010-08-01 10:10:30\",\n"
	        + "  \"sDateTimeTo\": \"2010-08-01 18:10:00\",\n"
	        + "  \"nID\": 20367,\n"
	        + "  \"nID_FlowPropertyClass\": {\n"
	        + "    \"sPath\": \"org.igov.service.business.flow.handler.DefaultFlowSlotScheduler\",\n"
	        + "    \"sBeanName\": \"defaultFlowSlotScheduler\",\n"
	        + "    \"nID\": 1,\n"
	        + "    \"sName\": \"DefaultFlowSlotScheduler\"\n"
	        + "  }\n"
	        + "}\n"
	        + "\n```\n" )
    @RequestMapping(value = "/setSheduleFlowInclude", method = RequestMethod.GET)
    public
    @ResponseBody
    FlowProperty setSheduleFlowInclude(
	    @ApiParam(value = "ИД-номер, если задан - редактирование", required = false) @RequestParam(value = "nID", required = false) Long nID,
	    @ApiParam(value = "номер-ИД потока (обязательный если нет sID_BP)", required = false) @RequestParam(value = "nID_Flow_ServiceData", required = false) Long nID_Flow_ServiceData,
	    @ApiParam(value = "строка-ИД бизнес-процесса потока (обязательный если нет nID_Flow_ServiceData)", required = false) @RequestParam(value = "sID_BP", required = false) String sID_BP,
	    @ApiParam(value = "номер-ИН департамента", required = false) @RequestParam(value = "nID_SubjectOrganDepartment", required = false) Long nID_SubjectOrganDepartment,
	    @ApiParam(value = "Строка-название (\"Вечерний прием\")", required = true) @RequestParam(value = "sName") String sName,
	    @ApiParam(value = "Строка период времени (\"14:16-16-30\")", required = true) @RequestParam(value = "sRegionTime") String sRegionTime,

	    @ApiParam(value = "Число, определяющее длительность слота", required = false) @RequestParam(value = "nLen", required = false) Integer nLen,
	    @ApiParam(value = "Строка определяющее тип длительности слота", required = false) @RequestParam(value = "sLenType", required = false) String sLenType,
	    @ApiParam(value = "Строка с данными(выражением), описывающими формулу расписания (например: {\"0 0/30 9-12 ? * TUE-FRI\":\"PT30M\"})", required = false) @RequestParam(value = "sData", required = false) String sData,

	    @ApiParam(value = "Массив дней недели (\"su,mo,tu\")", required = true) @RequestParam(value = "saRegionWeekDay") String saRegionWeekDay,
	    @ApiParam(value = "Строка-дата начала(на) в формате YYYY-MM-DD hh:mm:ss (\"2015-07-31 19:00:00\")", required = true) @RequestParam(value = "sDateTimeAt") String sDateTimeAt,
	    @ApiParam(value = "Строка-дата конца(к) в формате YYYY-MM-DD hh:mm:ss (\"2015-07-31 23:00:00\")", required = true) @RequestParam(value = "sDateTimeTo") String sDateTimeTo) throws Exception {


		if (sRegionTime != null && saRegionWeekDay != null && nLen != null) {
            sData = QuartzUtil.getQuartzFormulaByParameters(sRegionTime, saRegionWeekDay, nLen);
        }

		return oFlowService.setSheduleFlow(nID, nID_Flow_ServiceData, sID_BP, nID_SubjectOrganDepartment, sName,
				sRegionTime, nLen, sLenType, sData, saRegionWeekDay, sDateTimeAt, sDateTimeTo, Boolean.FALSE);
    }

    /**
     * Adds/removes schedule exclude slot
     *
     * @param nID                  - ID of flow property
     * @param nID_Flow_ServiceData - ID of flow
     * @param sName                - name of the slot
     * @param sRegionTime          - time period, "14:16-16:30"
     * @param saRegionWeekDay      - array of days in a week ("su,mo,tu")
     * @return ID of new FlowProperty
     */
    @ApiOperation(value = "Добавление/изменение расписания исключения", notes = "##### Электронные очереди. Добавление/изменение расписания исключения #####\n\n"
	        + "HTTP Context: https://test.region.igov.org.ua/wf/service/action/flow/setSheduleFlowExclude?nID_Flow_ServiceData=nID_Flow_ServiceData&sName=sName&sRegionTime=sRegionTime&sDateTimeAt=sDateTimeAt&sDateTimeTo=sDateTimeTo&saRegionWeekDay=saRegionWeekDay\n\n"
	        + "Пример:\n"
	        + "https://test.region.igov.org.ua/wf/service/action/flow/setSheduleFlowExclude?nID_Flow_ServiceData=1&sName=Test&sRegionTime=%2210:30-11:30%22&sDateTimeAt=%222010-08-01%2010:10:30%22&sDateTimeTo=%222010-08-01%2018:10:00%22&saRegionWeekDay=%22mo,tu%22\n"
	        + "\n```json\n"
	        + "Пример результата\n"
	        + "{\n"
	        + "  \"sData\": null,\n"
	        + "  \"bExclude\": true,\n"
	        + "  \"sName\": \"Test\",\n"
	        + "  \"sRegionTime\": \"10:30-11:30\",\n"
	        + "  \"saRegionWeekDay\": \"mo,tu\",\n"
	        + "  \"sDateTimeAt\": \"2010-08-01 10:10:30\",\n"
	        + "  \"sDateTimeTo\": \"2010-08-01 18:10:00\",\n"
	        + "  \"nID\": 20367,\n"
	        + "  \"nID_FlowPropertyClass\": {\n"
	        + "    \"sPath\": \"org.igov.service.business.flow.handler.DefaultFlowSlotScheduler\",\n"
	        + "    \"sBeanName\": \"defaultFlowSlotScheduler\",\n"
	        + "    \"nID\": 1,\n"
	        + "    \"sName\": \"DefaultFlowSlotScheduler\"\n"
	        + "  }\n"
	        + "}\n"
	        + "\n```\n" )
    @RequestMapping(value = "/setSheduleFlowExclude", method = RequestMethod.GET)
    public
    @ResponseBody
    FlowProperty setSheduleFlowExclude(
	    @ApiParam(value = "ИД-номер //опциональный ,если задан - редактирование", required = false) @RequestParam(value = "nID", required = false) Long nID,
	    @ApiParam(value = "номер-ИД потока (обязательный если нет sID_BP)", required = false) @RequestParam(value = "nID_Flow_ServiceData", required = false) Long nID_Flow_ServiceData,
	    @ApiParam(value = "строка-ИД бизнес-процесса потока (обязательный если нет nID_Flow_ServiceData)", required = false) @RequestParam(value = "sID_BP", required = false) String sID_BP,
	    @ApiParam(value = "номер-ИН департамента", required = false) @RequestParam(value = "nID_SubjectOrganDepartment", required = false) Long nID_SubjectOrganDepartment,
	    @ApiParam(value = "Строка-название (\"Вечерний прием\")", required = true) @RequestParam(value = "sName") String sName,
	    @ApiParam(value = "Строка период времени (\"14:16-16-30\")", required = true) @RequestParam(value = "sRegionTime") String sRegionTime,

	    @ApiParam(value = "Число, определяющее длительность слота", required = false) @RequestParam(value = "nLen", required = false) Integer nLen,
	    @ApiParam(value = "Строка определяющее тип длительности слота", required = false) @RequestParam(value = "sLenType", required = false) String sLenType,
	    @ApiParam(value = "Строка с данными(выражением), описывающими формулу расписания (например: {\"0 0/30 9-12 ? * TUE-FRI\":\"PT30M\"})", required = false) @RequestParam(value = "sData", required = false) String sData,

	    @ApiParam(value = "Массив дней недели (\"su,mo,tu\")", required = true) @RequestParam(value = "saRegionWeekDay") String saRegionWeekDay,
	    @ApiParam(value = "Строка-дата начала(на) в формате YYYY-MM-DD hh:mm:ss (\"2015-07-31 19:00:00\")", required = true) @RequestParam(value = "sDateTimeAt") String sDateTimeAt,
	    @ApiParam(value = "Строка-дата конца(к) в формате YYYY-MM-DD hh:mm:ss (\"2015-07-31 23:00:00\")", required = true) @RequestParam(value = "sDateTimeTo") String sDateTimeTo) throws Exception {


		return oFlowService.setSheduleFlow(nID, nID_Flow_ServiceData, sID_BP, nID_SubjectOrganDepartment, sName,
				sRegionTime, nLen, sLenType, sData, saRegionWeekDay, sDateTimeAt, sDateTimeTo, Boolean.TRUE);
    }

    /**
     * @param nID_Flow_ServiceData номер-ИД потока (обязательный если нет sID_BP)
     * @param sID_BP строка-ИД бизнес-процесса потока (обязательный если нет nID_Flow_ServiceData)
     * @param nID ИД-номер
     */
    @ApiOperation(value = "Удаление расписания включений", notes = "##### Электронные очереди. Удаление расписания включений #####\n\n"
	        + "HTTP Context: https://test.region.igov.org.ua/wf/service/action/flow/removeSheduleFlowInclude?nID_Flow_ServiceData=nID_Flow_ServiceData&nID=nID\n\n"
	        + "Ответ: Массив объектов сущности расписаний включений\n\n"
	        + "Пример:\n"
	        + "https://test.region.igov.org.ua/wf/service/action/flow/removeSheduleFlowInclude?nID_Flow_ServiceData=1&nID=20367\n\n"
	        + "Пример результата\n"
	        + "\n```json\n"
	        + "{\n"
	        + "  \"sData\": null,\n"
	        + "  \"bExclude\": false,\n"
	        + "  \"sName\": \"Test\",\n"
	        + "  \"sRegionTime\": \"10:30-11:30\",\n"
	        + "  \"saRegionWeekDay\": \"mo,tu\",\n"
	        + "  \"sDateTimeAt\": \"2010-08-01 10:10:30\",\n"
	        + "  \"sDateTimeTo\": \"2010-08-01 18:10:00\",\n"
	        + "  \"nID\": 20367,\n"
	        + "  \"nID_FlowPropertyClass\": {\n"
	        + "    \"sPath\": \"org.igov.service.business.flow.handler.DefaultFlowSlotScheduler\",\n"
	        + "    \"sBeanName\": \"defaultFlowSlotScheduler\",\n"
	        + "    \"nID\": 1,\n"
	        + "    \"sName\": \"DefaultFlowSlotScheduler\"\n"
	        + "  }\n"
	        + "}\n"
	        + "\n```\n" )
    @RequestMapping(value = "/removeSheduleFlowInclude", method = RequestMethod.GET)
    public
    @ResponseBody
    List<FlowProperty> removeSheduleFlowInclude(
	    @ApiParam(value = "номер-ИД записи", required = true) @RequestParam(value = "nID") Long nID,
	    @ApiParam(value = "номер-ИД потока (обязательный если нет sID_BP)", required = false) @RequestParam(value = "nID_Flow_ServiceData", required = false) Long nID_Flow_ServiceData,
	    @ApiParam(value = "строка-ИД бизнес-процесса потока (обязательный если нет nID_Flow_ServiceData)", required = false) @RequestParam(value = "sID_BP", required = false) String sID_BP,
	    @ApiParam(value = "ИД-номер", required = false) @RequestParam(value = "nID_SubjectOrganDepartment", required = false) Long nID_SubjectOrganDepartment
    ) throws Exception {

		try {
			nID_Flow_ServiceData = oFlowService
					.determineFlowServiceDataID(nID_Flow_ServiceData, sID_BP, nID_SubjectOrganDepartment);
		} catch (RecordNotFoundException e) {
			LOG.error(e.getMessage());
			throw new Exception(e.getMessage());
		}

        if (nID_Flow_ServiceData != null && nID != null) {
            LOG.info("nID_Flow_ServiceData is not null. Removing flow property with bExclude=false and (ID={})", nID);

			return oFlowService.removeSheduleFlow(nID, nID_Flow_ServiceData, Boolean.FALSE);
		} else {
            LOG.info("nID or nID_Flow_ServiceData are empty. Skipping logic of the method removeSheduleFlowExclude");
        }
        return new LinkedList<FlowProperty>();
    }

    /**
     * @param nID_Flow_ServiceData номер-ИД потока (обязательный если нет sID_BP)
     * @param sID_BP строка-ИД бизнес-процесса потока (обязательный если нет nID_Flow_ServiceData)
     * @param nID ИД-номер
     */
    @ApiOperation(value = "Удаление расписания исключений", notes = "##### Электронные очереди. Удаление расписания исключений #####\n\n"
	        + "HTTP Context: https://test.region.igov.org.ua/wf/service/action/flow/removeSheduleFlowExclude?nID_Flow_ServiceData=nID_Flow_ServiceData&nID=nID\n\n"
	        + "Ответ: Массив объектов сущности расписаний исключений\n\n"
	        + "Пример:\n"
	        + "https://test.region.igov.org.ua/wf/service/action/flow/removeSheduleFlowExclude?nID_Flow_ServiceData=1&nID=20367\n\n"
	        + "Пример результата\n\n"
	        + "\n```json\n"
	        + "{\n"
	        + "  \"sData\": null,\n"
	        + "  \"bExclude\": true,\n"
	        + "  \"sName\": \"Test\",\n"
	        + "  \"sRegionTime\": \"10:30-11:30\",\n"
	        + "  \"saRegionWeekDay\": \"mo,tu\",\n"
	        + "  \"sDateTimeAt\": \"2010-08-01 10:10:30\",\n"
	        + "  \"sDateTimeTo\": \"2010-08-01 18:10:00\",\n"
	        + "  \"nID\": 20367,\n"
	        + "  \"nID_FlowPropertyClass\": {\n"
	        + "    \"sPath\": \"org.igov.service.business.flow.handler.DefaultFlowSlotScheduler\",\n"
	        + "    \"sBeanName\": \"defaultFlowSlotScheduler\",\n"
	        + "    \"nID\": 1,\n"
	        + "    \"sName\": \"DefaultFlowSlotScheduler\"\n"
	        + "  }\n"
	        + "}\n"
	        + "\n```\n" )
    @RequestMapping(value = "/removeSheduleFlowExclude", method = RequestMethod.GET)
    public
    @ResponseBody
    List<FlowProperty> removeSheduleFlowExclude(
	    @ApiParam(value = "ИД-номер", required = true) @RequestParam(value = "nID") Long nID,
	    @ApiParam(value = "номер-ИД потока (обязательный если нет sID_BP)", required = false) @RequestParam(value = "nID_Flow_ServiceData", required = false) Long nID_Flow_ServiceData,
	    @ApiParam(value = "строка-ИД бизнес-процесса потока (обязательный если нет nID_Flow_ServiceData)", required = false) @RequestParam(value = "sID_BP", required = false) String sID_BP,
	    @ApiParam(value = "номер-ИН департамента", required = false) @RequestParam(value = "nID_SubjectOrganDepartment", required = false) Long nID_SubjectOrganDepartment
    ) throws Exception {

		try {
			nID_Flow_ServiceData = oFlowService
					.determineFlowServiceDataID(nID_Flow_ServiceData, sID_BP, nID_SubjectOrganDepartment);
		} catch (RecordNotFoundException e) {
			LOG.error(e.getMessage());
			throw new Exception(e.getMessage());
		}

        if (nID_Flow_ServiceData != null && nID != null) {
            LOG.info("nID_Flow_ServiceData is not null. Removing flow property with bExclude=true and (ID={})", nID);

			return oFlowService.removeSheduleFlow(nID, nID_Flow_ServiceData, Boolean.TRUE);
        } else {
            LOG.info("nID or nID_Flow_ServiceData are empty. Skipping logic of the method removeSheduleFlowExclude");
        }
        return new LinkedList<FlowProperty>();
    }

    /**
     * @param sLogin имя пользоватеял для которого необходимо вернуть тикеты
     * @param bEmployeeUnassigned опциональный параметр (false по умолчанию). Если true - возвращать тикеты не заассайненые на пользователей
     * @param sDate опциональный параметр в формате yyyy-MM-dd. Дата за которую выбирать тикеты. При выборке проверяется startDate тикета (без учета времени. только дата). Если день такой же как и у указанное даты - такой тикет добавляется в результат.
     */
    @ApiOperation(value = "Получение активных тикетов", notes = "##### Электронные очереди. Получение активных тикетов #####\n\n"
	        + "HTTP Context: https://test.region.igov.org.ua/wf/service/action/flow/getFlowSlotTickets?sLogin=sLogin&bEmployeeUnassigned=true|false&sDate=yyyy-MM-dd\n\n"
	        + "возвращает активные тикеты, отсортированные по startDate\n\n"
	        + "Примеры:\n"
	        + "https://test.region.igov.org.ua/wf/service/action/flow/getFlowSlotTickets?sLogin=kermit\n"
	        + "\n```json\n"
	        + "[\n"
	        + "  {\n"
	        + "    \"sDateStart\": \"2015-07-20T15:15:00\",\n"
	        + "    \"sDateEdit\": \"2015-07-06T11:03:52\",\n"
	        + "    \"sTaskDate\": \"2015-07-30T10:03:43\",\n"
	        + "    \"sDateFinish\": \"2015-07-20T15:30:00\",\n"
	        + "    \"nID_FlowSlot\": \"6\",\n"
	        + "    \"sNameBP\": \"Киев - Реєстрація авто з пробігом в МРЕВ\",\n"
	        + "    \"nID_Subject\": \"20045\",\n"
	        + "    \"sUserTaskName\": \"Надання послуги: Огляд авто\",\n"
	        + "    \"nID\": \"20005\"\n"
	        + "  },\n"
	        + "  {\n"
	        + "    \"sDateStart\": \"2015-07-20T15:45:00\",\n"
	        + "    \"sDateEdit\": \"2015-07-06T23:25:15\",\n"
	        + "    \"sTaskDate\": \"2015-07-06T23:27:18\",\n"
	        + "    \"sDateFinish\": \"2015-07-20T16:00:00\",\n"
	        + "    \"nID_FlowSlot\": \"7\",\n"
	        + "    \"sNameBP\": \"Киев - Реєстрація авто з пробігом в МРЕВ\",\n"
	        + "    \"nID_Subject\": \"20045\",\n"
	        + "    \"sUserTaskName\": \"Надання послуги: Огляд авто\",\n"
	        + "    \"nID\": \"20010\"\n"
	        + "  }\n"
	        + "]\n"
	        + "\n```\n"
	        + "\n"
	        + "https://test.region.igov.org.ua/wf/service/action/flow/getFlowSlotTickets?sLogin=kermit&bEmployeeUnassigned=true\n"
	        + "\n```json\n"
	        + "[\n"
	        + "  {\n"
	        + "    \"sDateStart\": \"2015-08-03T08:00:00\",\n"
	        + "    \"sDateEdit\": \"2015-07-30T23:10:58\",\n"
	        + "    \"sTaskDate\": \"2015-07-30T23:50:07\",\n"
	        + "    \"sDateFinish\": \"2015-08-03T08:15:00\",\n"
	        + "    \"nID_FlowSlot\": \"20086\",\n"
	        + "    \"sNameBP\": \"Днепропетровск - Реєстрація авто з пробігом в МРЕВ\",\n"
	        + "    \"nID_Subject\": \"20045\",\n"
	        + "    \"sUserTaskName\": \"Друк держ.номерів\",\n"
	        + "    \"nID\": \"20151\"\n"
	        + "  },\n"
	        + "  {\n"
	        + "    \"sDateStart\": \"2015-08-03T08:15:00\",\n"
	        + "    \"sDateEdit\": \"2015-07-31T21:00:56\",\n"
	        + "    \"sTaskDate\": \"2015-07-31T21:01:19\",\n"
	        + "    \"sDateFinish\": \"2015-08-03T08:30:00\",\n"
	        + "    \"nID_FlowSlot\": \"20023\",\n"
	        + "    \"sNameBP\": \"Киев - Реєстрація авто з пробігом в МРЕВ\",\n"
	        + "    \"nID_Subject\": \"20045\",\n"
	        + "    \"sUserTaskName\": \"Перевірка наявності обтяжень\",\n"
	        + "    \"nID\": \"20357\"\n"
	        + "  }\n"
	        + "]\n"
	        + "\n```\n"
	        + "\n"
	        + "https://test.region.igov.org.ua/wf/service/action/flow/getFlowSlotTickets?sLogin=kermit&bEmployeeUnassigned=true&sDate=2015-07-20\n"
	        + "\n```json\n"
	        + "[\n"
	        + "  {\n"
	        + "    \"sDateStart\": \"2015-07-20T15:15:00\",\n"
	        + "    \"sDateEdit\": \"2015-07-06T11:03:52\",\n"
	        + "    \"sTaskDate\": \"2015-07-30T10:03:43\",\n"
	        + "    \"sDateFinish\": \"2015-07-20T15:30:00\",\n"
	        + "    \"nID_FlowSlot\": \"6\",\n"
	        + "    \"sNameBP\": \"Киев - Реєстрація авто з пробігом в МРЕВ\",\n"
	        + "    \"nID_Subject\": \"20045\",\n"
	        + "    \"sUserTaskName\": \"Надання послуги: Огляд авто\",\n"
	        + "    \"nID\": \"20005\"\n"
	        + "  },\n"
	        + "  {\n"
	        + "    \"sDateStart\": \"2015-07-20T15:45:00\",\n"
	        + "    \"sDateEdit\": \"2015-07-06T23:25:15\",\n"
	        + "    \"sTaskDate\": \"2015-07-06T23:27:18\",\n"
	        + "    \"sDateFinish\": \"2015-07-20T16:00:00\",\n"
	        + "    \"nID_FlowSlot\": \"7\",\n"
	        + "    \"sNameBP\": \"Киев - Реєстрація авто з пробігом в МРЕВ\",\n"
	        + "    \"nID_Subject\": \"20045\",\n"
	        + "    \"sUserTaskName\": \"Надання послуги: Огляд авто\",\n"
	        + "    \"nID\": \"20010\"\n"
	        + "  }\n"
	        + "]\n"
	        + "\n```\n" )
    @RequestMapping(value = "/getFlowSlotTickets", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public
    @ResponseBody
    String getFlowSlotTickets(
	    @ApiParam(value = "имя пользователя для которого необходимо вернуть тикеты", required = true) @RequestParam(value = "sLogin") String sLogin,
	    @ApiParam(value = "опциональный параметр (false по умолчанию). Если true - возвращать тикеты не заассайненые на пользователей", required = false) @RequestParam(value = "bEmployeeUnassigned", required = false, defaultValue = "false") Boolean bEmployeeUnassigned,
	    @ApiParam(value = "опциональный параметр в формате yyyy-MM-dd. Дата за которую выбирать тикеты. При выборке проверяется startDate тикета (без учета времени. только дата). Если день такой же как и у указанное даты - такой тикет добавляется в результат.", required = false) @RequestParam(value = "sDate", required = false) String sDate
    ) throws Exception {

		List<Map<String, String>> res = oFlowService.getFlowSlotTickets(sLogin, bEmployeeUnassigned, sDate);

        String jsonRes = JSONValue.toJSONString(res);
        LOG.info("Result:{}", jsonRes);
        return jsonRes;
    }
}