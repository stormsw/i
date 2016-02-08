package org.igov.model.action.item;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.igov.model.core.Entity;

import javax.persistence.Column;

/**
 * User: goodg_000 Date: 04.05.2015 Time: 22:58
 */
@javax.persistence.Entity
public class ServiceType extends Entity {

    @JsonProperty(value = "sName")
    @Column(name = "sName", nullable = false)
    private String name;

    @JsonProperty(value = "sNote")
    @Column(name = "sNote", nullable = false)
    private String note;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
