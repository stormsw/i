package org.igov.run.schedule;

import org.igov.service.exception.CommonServiceException;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.igov.service.business.escalation.EscalationService;

import java.util.Date;

public class JobEscalation extends IAutowiredSpringJob {

    private final static Logger LOG = LoggerFactory.getLogger(JobEscalation.class);
    @Autowired
    private EscalationService escalationService;

    public void execute(JobExecutionContext context) throws JobExecutionException {
        LOG.info("In QuartzJob - executing JOB at {} by context.getTrigger().getName()={}",
                new Date(), context.getTrigger().getName());
        try {
            //TODO: ��� ����� �������� ����� ������� ���������!
            escalationService.runEscalationAll();
        } catch (CommonServiceException oException) {
            LOG.error("Bad: ", oException.getMessage());
            LOG.debug("FAIL:", oException);
        }
    }
}