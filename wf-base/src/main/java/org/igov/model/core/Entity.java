package org.igov.model.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.sf.brunneng.jom.annotations.Identifier;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Abstract entity.
 * <p/>
 * User: goodg_000
 * Date: 04.05.2015
 * Time: 21:51
 */
@MappedSuperclass
public abstract class Entity {

    protected static final String DATETIME_TYPE = "org.jadira.usertype.dateandtime.joda.PersistentDateTime";

    @JsonProperty(value = "nID")
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "nID")
    private Long id;

    @Identifier
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

}
