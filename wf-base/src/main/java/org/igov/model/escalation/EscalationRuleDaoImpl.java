package org.igov.model.escalation;

import org.igov.model.core.GenericEntityDao;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class EscalationRuleDaoImpl extends GenericEntityDao<EscalationRule>
        implements EscalationRuleDao {
    protected EscalationRuleDaoImpl() {
        super(EscalationRule.class);
    }

    @Override
    @Transactional
    public EscalationRule saveOrUpdate(Long nID, String sID_BP, String sID_userTask,
            String sCondition, String soData,
            String sPatternFile, EscalationRuleFunction ruleFunction) {

        EscalationRule rule = nID != null ? findByIdExpected(nID) : new EscalationRule();
        if (nID != null && rule == null) {//??
            rule = new EscalationRule();
            rule.setId(nID);
        }
        rule.setsID_BP(sID_BP);
        rule.setsID_UserTask(sID_userTask);
        if (sCondition != null) {
            rule.setsCondition(sCondition);
        }
        if (soData != null) {
            rule.setSoData(soData);
        }
        if (sPatternFile != null) {
            rule.setsPatternFile(sPatternFile);
        }
        if (ruleFunction != null) {
            //            EscalationRuleFunction ruleFunction = new EscalationRuleFunction();
            //            ruleFunction.setId(nID);
            rule.setoEscalationRuleFunction(ruleFunction);
        }
        saveOrUpdate(rule);
        return rule;
    }
}
