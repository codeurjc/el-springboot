package io.elastest.model;

import javax.persistence.Column;
import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import io.elastest.model.Trace.TraceView;

//@Entity
public class ComposedMetricTrace extends MetricTrace {
    @JsonView({ TraceView.class, MetricTraceView.class })
    @Column(name = "units", columnDefinition = "TEXT", length = 65535)
    @JsonProperty("units")
    String units;

    public ComposedMetricTrace(Long id, String name, String content,
            Trace trace, String units) {
        super(id, name, content, trace);
        this.units = units;
    }

    public ComposedMetricTrace() {
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    @Override
    public String toString() {
        return "ComposedMetricTrace [units=" + units + ", id=" + id + "]";
    }

}
