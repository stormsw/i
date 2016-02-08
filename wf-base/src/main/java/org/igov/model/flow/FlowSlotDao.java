package org.igov.model.flow;

import org.joda.time.DateTime;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.igov.model.core.EntityDao;

/**
 * User: goodg_000
 * Date: 21.06.2015
 * Time: 15:44
 */
public interface FlowSlotDao extends EntityDao<FlowSlot> {

    /**
     * Gets flow slots by service data ID ordered by date in given interval
     *
     * @param nID_ServiceData            ID services data of slot flow.
     * @param nID_SubjectOrganDepartment id department
     * @param startDate                  start date of interval (inclusive)
     * @param stopDate                   end date of interval (exclusive)
     * @return flow slots
     */
    List<FlowSlot> findFlowSlotsByServiceData(Long nID_ServiceData, Long nID_SubjectOrganDepartment,
            DateTime startDate, DateTime stopDate);

    /**
     * Gets flow slots by BP ID ordered by date in given interval
     *
     * @param sID_BP                     id of business process
     * @param nID_SubjectOrganDepartment id department
     * @param startDate                  start date of interval (inclusive)
     * @param stopDate                   end date of interval (exclusive)
     * @return flow slots
     */
    List<FlowSlot> findFlowSlotsByBP(String sID_BP, Long nID_SubjectOrganDepartment, DateTime startDate,
                      DateTime stopDate);

    /**
     * @param nID_Flow_ServiceData ID of flow
     * @param startDate            start date of interval (inclusive)
     * @param stopDate             end date of interval (exclusive)
     * @return flow slots
     */
    List<FlowSlot> findFlowSlotsByFlow(Long nID_Flow_ServiceData, DateTime startDate, DateTime stopDate);

    /**
     * Gets flow slots in given interval
     *
     * @param nID_Flow_ServiceData ID of flow
     * @param startDate            start date of interval (inclusive)
     * @param stopDate             stop date of interval (inclusive)
     * @return set of date times
     */
    Set<DateTime> findFlowSlotsDates(Long nID_Flow_ServiceData, DateTime startDate, DateTime stopDate);

    /**
     * Updates slots with new duration
     *
     * @param nID_Flow_ServiceData
     * @param dates
     * @param newDuration
     * @return count of slots updated
     */
    int updateSlots(Long nID_Flow_ServiceData, Collection<DateTime> dates, String newDuration);

}
