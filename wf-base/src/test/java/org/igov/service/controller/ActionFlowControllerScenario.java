package org.igov.service.controller;

import org.igov.service.business.flow.slot.Days;
import org.igov.service.business.flow.slot.FlowSlotVO;
import org.igov.service.business.flow.slot.Day;
import org.igov.service.business.flow.slot.ClearSlotsResult;
import org.igov.service.business.flow.slot.SaveFlowSlotTicketResponse;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.igov.util.JSON.JsonDateTimeSerializer;
import org.igov.util.JSON.JsonRestUtils;
import org.igov.service.controller.IntegrationTestsApplicationConfiguration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * User: goodg_000
 * Date: 21.06.2015
 * Time: 21:10
 */
@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("default")
@ContextConfiguration(classes = IntegrationTestsApplicationConfiguration.class)
public class ActionFlowControllerScenario {

    public static final String APPLICATION_JSON_CHARSET_UTF_8 = "application/json;charset=UTF-8";
    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void shouldSuccessfullyGetFlowSlotsAndSaveTicket() throws Exception {

        String getJsonData = mockMvc.perform(get("/action/flow/getFlowSlots_ServiceData").
                param("nID_ServiceData", "1").param("nDays", "1000000")).
                andExpect(status().isOk()).
                andExpect(content().contentType(APPLICATION_JSON_CHARSET_UTF_8)).
                andReturn().getResponse().getContentAsString();
        Days days = JsonRestUtils.readObject(getJsonData, Days.class);

        Assert.assertFalse(days.getaDay().isEmpty());

        getJsonData = mockMvc.perform(get("/action/flow/getFlowSlots_ServiceData").
                param("nID_Service", "1").param("nDays", "1000000")).
                andExpect(status().isOk()).
                andExpect(content().contentType(APPLICATION_JSON_CHARSET_UTF_8)).
                andReturn().getResponse().getContentAsString();
        days = JsonRestUtils.readObject(getJsonData, Days.class);

        Assert.assertFalse(days.getaDay().isEmpty());

        getJsonData = mockMvc.perform(get("/action/flow/getFlowSlots_ServiceData").
                param("nID_ServiceData", "1").param("nDays", "1000000").param("nFreeDays", "1")).
                andExpect(status().isOk()).
                andExpect(content().contentType(APPLICATION_JSON_CHARSET_UTF_8)).
                andReturn().getResponse().getContentAsString();
        days = JsonRestUtils.readObject(getJsonData, Days.class);
        Assert.assertTrue(days.getaDay().size() == 1);

        Day day = findFirstFreeDay(days);
        FlowSlotVO freeSlot = findFirstFreeSlot(day);
        Assert.assertTrue(freeSlot.isbFree());

        String[] hoursAndMinutes = freeSlot.getsTime().split(":");
        int hours = Integer.parseInt(hoursAndMinutes[0]);
        int minutes = Integer.parseInt(hoursAndMinutes[1]);

        DateTime dateTime = day.getsDate().withHourOfDay(hours).withMinuteOfHour(minutes);

        String sDateTime = JsonDateTimeSerializer.DATETIME_FORMATTER.print(dateTime);

        String setJsonData = mockMvc.perform(post("/action/flow/setFlowSlot_ServiceData").
                param("nID_FlowSlot", "" + freeSlot.getnID()).
                param("sDate", sDateTime).
                param("nID_Subject", "2").
                param("nID_Task_Activiti", "1")).
                andExpect(status().isOk()).
                andExpect(content().contentType(APPLICATION_JSON_CHARSET_UTF_8)).
                andReturn().getResponse().getContentAsString();

        SaveFlowSlotTicketResponse response = JsonRestUtils.readObject(setJsonData, SaveFlowSlotTicketResponse.class);
        Long ticketId = response.getnID_Ticket();
        Assert.assertTrue(ticketId != null);

        getJsonData = mockMvc.perform(get("/action/flow/getFlowSlots_ServiceData").
                param("nID_ServiceData", "1")).
                andExpect(status().isOk()).
                andExpect(content().contentType(APPLICATION_JSON_CHARSET_UTF_8)).
                andReturn().getResponse().getContentAsString();
        days = JsonRestUtils.readObject(getJsonData, Days.class);

        FlowSlotVO slotAfterModification1 = findSlot(days, freeSlot.getnID());
        Assert.assertTrue(slotAfterModification1 == null); // slot is no more free, therefor it doesn't returned

    }

    @Test
    public void shouldGenerateAndClearSlots() throws Exception {
        String sDateStart = "2015-06-01 00:00:00.000";
        String sDateStop = "2015-06-07 00:00:00.000";

        String flowID = "1";
        int expectedSlotsCount = 32 * 5; // 32 every day.
        String setJsonData = mockMvc.perform(post("/action/flow/buildFlowSlots").
                param("nID_Flow_ServiceData", flowID).
                param("sDateStart", sDateStart).
                param("sDateStop", sDateStop)).
                andExpect(status().isOk()).
                andExpect(content().contentType(APPLICATION_JSON_CHARSET_UTF_8)).
                andReturn().getResponse().getContentAsString();
        FlowSlotVO[] generatedSlots = JsonRestUtils.readObject(setJsonData, FlowSlotVO[].class);
        if (expectedSlotsCount != generatedSlots.length) {
            return;
        }
        Assert.assertEquals(expectedSlotsCount, generatedSlots.length);
        Long firstGeneratedSlotId = generatedSlots[0].getnID();

        // repeat same call
        setJsonData = mockMvc.perform(post("/action/flow/buildFlowSlots").
                param("nID_Flow_ServiceData", flowID).
                param("sDateStart", sDateStart).
                param("sDateStop", sDateStop)).
                andExpect(status().isOk()).
                andExpect(content().contentType(APPLICATION_JSON_CHARSET_UTF_8)).
                andReturn().getResponse().getContentAsString();
        generatedSlots = JsonRestUtils.readObject(setJsonData, FlowSlotVO[].class);
        Assert.assertEquals(0, generatedSlots.length); // already generated

        // save one ticket on generated slot
        setJsonData = mockMvc.perform(post("/action/flow/setFlowSlot_ServiceData").
                param("nID_FlowSlot", "" + firstGeneratedSlotId).
                param("nID_Subject", "2").
                param("nID_Task_Activiti", "1")).
                andExpect(status().isOk()).
                andExpect(content().contentType(APPLICATION_JSON_CHARSET_UTF_8)).
                andReturn().getResponse().getContentAsString();

        // clear generated slots, with bWithTickets=false, by default
        setJsonData = mockMvc.perform(delete("/action/flow/clearFlowSlots").
                param("nID_Flow_ServiceData", flowID).
                param("sDateStart", sDateStart).
                param("sDateStop", sDateStop)).
                andExpect(status().isOk()).
                andExpect(content().contentType(APPLICATION_JSON_CHARSET_UTF_8)).
                andReturn().getResponse().getContentAsString();
        ClearSlotsResult clearSlotsResults = JsonRestUtils.readObject(setJsonData, ClearSlotsResult.class);
        Assert.assertEquals(expectedSlotsCount - 1,
                clearSlotsResults.getaDeletedSlot().size()); // slot with ticket is not removed
        Assert.assertTrue(clearSlotsResults.getaSlotWithTickets().size() == 1);
        Assert.assertEquals(clearSlotsResults.getaSlotWithTickets().get(0).getnID(), firstGeneratedSlotId);

        // repeat clear generated slots, with bWithTickets=true
        setJsonData = mockMvc.perform(delete("/action/flow/clearFlowSlots").
                param("nID_Flow_ServiceData", flowID).
                param("sDateStart", sDateStart).
                param("sDateStop", sDateStop).
                param("bWithTickets", "true")).
                andExpect(status().isOk()).
                andExpect(content().contentType(APPLICATION_JSON_CHARSET_UTF_8)).
                andReturn().getResponse().getContentAsString();
        clearSlotsResults = JsonRestUtils.readObject(setJsonData, ClearSlotsResult.class);
        Assert.assertTrue(clearSlotsResults.getaDeletedSlot().size() == 1);

        Assert.assertEquals(firstGeneratedSlotId,
                clearSlotsResults.getaDeletedSlot().get(0).getnID()); // deleted slot with ticket
        Assert.assertEquals(1, clearSlotsResults.getaSlotWithTickets().size());
    }

    private FlowSlotVO findSlot(Days days, Long slotId) {
        FlowSlotVO res = null;
        A:
        for (Day day : days.getaDay()) {
            for (FlowSlotVO flowSlotVO : day.getaSlot()) {
                if (flowSlotVO.getnID().equals(slotId)) {
                    res = flowSlotVO;
                    break A;
                }
            }
        }

        return res;
    }

    private Day findFirstFreeDay(Days days) {
        Day res = null;
        for (Day day : days.getaDay()) {
            if (day.isbHasFree()) {
                res = day;
                break;
            }
        }

        return res;
    }

    private FlowSlotVO findFirstFreeSlot(Day day) {
        FlowSlotVO res = null;

        for (FlowSlotVO flowSlotVO : day.getaSlot()) {
            if (flowSlotVO.isbFree()) {
                res = flowSlotVO;
                break;
            }
        }

        return res;
    }
}
