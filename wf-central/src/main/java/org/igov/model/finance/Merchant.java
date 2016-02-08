package org.igov.model.finance;

import org.igov.model.subject.organ.SubjectOrgan;
import org.igov.model.core.NamedEntity;

import javax.persistence.*;

@Entity
@AttributeOverrides({ @AttributeOverride(name = "name", column = @Column(name = "sName", nullable = true)) })
public class Merchant extends NamedEntity {

    @Column(nullable = false, unique = true)
    private String sID;

    @Column
    private String sURL_CallbackStatusNew;

    @Column
    private String sURL_CallbackPaySuccess;

    @Column
    private String sPrivateKey;

    //OKPO
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "nID_SubjectOrgan")
    private SubjectOrgan owner;

    @Column(length = 10)
    private String sID_Currency;

    public String getsID() {
        return sID;
    }

    public void setsID(String sID) {
        this.sID = sID;
    }

    public String getsURL_CallbackStatusNew() {
        return sURL_CallbackStatusNew;
    }

    public void setsURL_CallbackStatusNew(String sURL_CallbackStatusNew) {
        this.sURL_CallbackStatusNew = sURL_CallbackStatusNew;
    }

    public String getsURL_CallbackPaySuccess() {
        return sURL_CallbackPaySuccess;
    }

    public void setsURL_CallbackPaySuccess(String sURL_CallbackPaySuccess) {
        this.sURL_CallbackPaySuccess = sURL_CallbackPaySuccess;
    }

    public SubjectOrgan getOwner() {
        return owner;
    }

    public void setOwner(SubjectOrgan owner) {
        this.owner = owner;
    }

    public String getsPrivateKey() {
        return sPrivateKey;
    }

    public void setsPrivateKey(String sPrivateKey) {
        this.sPrivateKey = sPrivateKey;
    }

    public String getsID_Currency() {
        return sID_Currency;
    }

    public void setsID_Currency(String sID_Currency) {
        this.sID_Currency = sID_Currency;
    }
}
