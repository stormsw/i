package org.igov.model.subject.message;

import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.igov.model.core.Entity;
import org.igov.util.JSON.JsonDateTimeDeserializer;
import org.igov.util.JSON.JsonDateTimeSerializer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import javax.persistence.Transient;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.igov.model.subject.SubjectContact;

@javax.persistence.Entity
public class SubjectMessage extends Entity {

    private static final long serialVersionUID = -5269544412868933212L;

    @JsonProperty(value = "sHead")
    @Column(name = "sHead", length = 200, nullable = false)
    private String head;

    @JsonProperty(value = "sBody")
    @Column(name = "sBody", nullable = false)
    private String body;

    @JsonProperty(value = "sDate")
    @JsonSerialize(using = JsonDateTimeSerializer.class)
    @JsonDeserialize(using = JsonDateTimeDeserializer.class)
    @Type(type = DATETIME_TYPE)
    @Column(name = "sDate", nullable = false)
    private DateTime date;

    
    @JsonProperty(value = "nID_Subject")
    @Column(name = "nID_Subject", nullable = false, columnDefinition = "int default 0")
    private Long id_subject;
    
    //@Transient
    @JsonProperty(value = "sMail")
    @Column(name = "sMail", length = 100)
    private String mail;
    
    @JsonProperty(value="oMail")
    @ManyToOne
    @JoinColumn(name="nID_SubjectContact_Mail")
    @Cascade({CascadeType.SAVE_UPDATE})
    private SubjectContact oMail;

    @JsonProperty(value = "sContacts")
    @Column(name = "sContacts", length = 200)
    private String contacts;

    @JsonProperty(value = "sData")
    @Column(name = "sData", length = 200)
    private String data;

    @JsonProperty(value = "oSubjectMessageType")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "nID_SubjectMessageType", nullable = false)
    private SubjectMessageType subjectMessageType = SubjectMessageType.DEFAULT;
    
    @JsonProperty(value = "sBody_Indirectly")
    @Column(name = "sBody_Indirectly")
    private String sBody_Indirectly; 
    
    @JsonProperty(value = "nID_HistoryEvent_Service")
    @Column(name = "nID_HistoryEvent_Service", nullable = true)
    private Long nID_HistoryEvent_Service;

    public String getHead() {
        return head;
    }

    public void setHead(String head) {
        this.head = head;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public DateTime getDate() {
        return date;
    }

    public void setDate(DateTime date) {
        this.date = date;
    }

    public Long getId_subject() {
        return id_subject;
    }

    public void setId_subject(Long id_subject) {
        this.id_subject = id_subject;
    }

    public String getMail() {
       return ((this.oMail != null) ? ((this.oMail.getsValue() != null) ? this.oMail.getsValue() : ""): ""); 
    }
    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getContacts() {
        return contacts;
    }

    public void setContacts(String contacts) {
        this.contacts = contacts;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public SubjectMessageType getSubjectMessageType() {
        return subjectMessageType;
    }

    public void setSubjectMessageType(SubjectMessageType subjectMessageType) {
        this.subjectMessageType = subjectMessageType;
    }

	public String getsBody_Indirectly() {
		return sBody_Indirectly;
	}

	public void setsBody_Indirectly(String sBody_Indirectly) {
		this.sBody_Indirectly = sBody_Indirectly;
	}
    
	public Long getnID_HistoryEvent_Service() {
		return nID_HistoryEvent_Service;
	}

	public void setnID_HistoryEvent_Service(Long nID_HistoryEvent_Service) {
		this.nID_HistoryEvent_Service = nID_HistoryEvent_Service;
	}

    public SubjectContact getoMail() {
        return oMail;
    }

    public void setoMail(SubjectContact oMail) {
        this.oMail = oMail;
    }

    
}
