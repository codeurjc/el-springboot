package io.elastest.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import io.elastest.model.Enums.LevelEnum;
import io.elastest.model.Trace.TraceView;

//@Entity
public class LogTrace {
    public interface LogTraceView {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    @JsonProperty("id")
    @JsonView({ TraceView.class, LogTraceView.class })
    protected Long id;

    @JsonView({ TraceView.class, LogTraceView.class })
    @Column(name = "message")
    @JsonProperty("message")
    private String message;

    @Column(name = "level")
    @JsonProperty("level")
    @JsonView({ TraceView.class, LogTraceView.class })
    private LevelEnum level;

    @JsonView({ LogTraceView.class })
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "logTrace")
    @JoinColumn(name = "trace")
    @JsonIgnoreProperties(value = "logTrace")
    Trace trace;

    /* ******************** */
    /* *** Constructors *** */
    /* ******************** */

    public LogTrace() {
    }

    /* *********************** */
    /* *** Getters/Setters *** */
    /* *********************** */

    public LogTrace(Long id, String message, LevelEnum level, Trace trace) {
        super();
        this.id = id == null ? 0 : id;
        this.message = message;
        this.level = level;
        this.trace = trace;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LevelEnum getLevel() {
        return level;
    }

    public void setLevel(LevelEnum level) {
        this.level = level;
    }

    public Trace getTrace() {
        return trace;
    }

    public void setTrace(Trace trace) {
        this.trace = trace;
    }

    /* ************** */
    /* *** Others *** */
    /* ************** */

    @Override
    public String toString() {
        return "LogTrace [message=" + message + ", level=" + level + "]";
    }

}
