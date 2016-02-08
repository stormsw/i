package org.igov.service.business.action.task.core;

import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.task.Task;

import java.text.SimpleDateFormat;

public enum TaskReportField {

    REQUEST_NUMBER("1", "${nID_Task}") {
        @Override
        public String replaceValue(String currentRow, Task curTask, SimpleDateFormat sDateFormat) {
            return currentRow.replace(this.getPattern(), curTask.getId());
        }

		@Override
		public String replaceValue(String currentRow,
				HistoricTaskInstance curTask, SimpleDateFormat sDateFormat) {
			return currentRow.replace(this.getPattern(), curTask.getId());
		}
    },
    CREATE_DATE("2", "${sDateCreate}") {
        @Override
        public String replaceValue(String currentRow, Task curTask, SimpleDateFormat sDateFormat) {
            return currentRow.replace(this.getPattern(), sDateFormat.format(curTask.getCreateTime()));
        }

		@Override
		public String replaceValue(String currentRow,
				HistoricTaskInstance curTask, SimpleDateFormat sDateFormat) {
			return currentRow.replace(this.getPattern(), sDateFormat.format(curTask.getCreateTime()));
		}
    };

    private String id;
    private String pattern;

    TaskReportField(String id, String pattern) {
        this.id = id;
        this.pattern = pattern;
    }

    public static TaskReportField getReportFieldForId(String id) {
        for (TaskReportField curr : TaskReportField.values()) {
            if (curr.getId().equals(id)) {
                return curr;
            }
        }
        return null;
    }

    public String getId() {
        return id;
    }

    public String getPattern() {
        return pattern;
    }

    public abstract String replaceValue(String currentRow, Task curTask, SimpleDateFormat sDateFormat);
    
    public abstract String replaceValue(String currentRow, HistoricTaskInstance curTask, SimpleDateFormat sDateFormat);

}
