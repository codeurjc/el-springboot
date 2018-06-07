package io.elastest.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import io.elastest.model.Trace.TraceView;

//@Entity
//@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class MetricTrace {
    public interface MetricTraceView {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    @JsonProperty("id")
    @JsonView({ TraceView.class, MetricTraceView.class })
    protected Long id;

    @JsonView({ TraceView.class, MetricTraceView.class })
    @Column(name = "name")
    @JsonProperty("name")
    private String name;

    @JsonView({ TraceView.class, MetricTraceView.class })
    @Column(name = "content", columnDefinition = "TEXT", length = 65535)
    @JsonProperty("name")
    private String content;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "metricTrace")
    @JoinColumn(name = "trace")
    @JsonIgnoreProperties(value = "metricTrace")
    Trace trace;

    /* ******************** */
    /* *** Constructors *** */
    /* ******************** */

    public MetricTrace() {
    }

    /* *********************** */
    /* *** Getters/Setters *** */
    /* *********************** */

    public MetricTrace(Long id, String name, String content, Trace trace) {
        super();
        this.id = id == null ? 0 : id;
        this.name = name;
        this.content = content;
        this.trace = trace;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    /* ************** */
    /* *** Others *** */
    /* ************** */

    @Override
    public String toString() {
        return "MetricTrace [name=" + name + ", content=" + content + "]";
    }

}
