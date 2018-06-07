package io.elastest.model;

import javax.persistence.Column;
import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import io.elastest.model.Trace.TraceView;

//@Entity
public class AtomicMetricTrace extends MetricTrace {

    @JsonView({ TraceView.class, MetricTraceView.class })
    @Column(name = "unit")
    @JsonProperty("unit")
    String unit;

    public AtomicMetricTrace() {
    }

    public AtomicMetricTrace(Long id, String name, String content, Trace trace,
            String unit) {
        super(id, name, content, trace);
        this.unit = unit;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    @Override
    public String toString() {
        return "AtomicMetricTrace [unit=" + unit + ", id=" + id + "]";
    }

}
