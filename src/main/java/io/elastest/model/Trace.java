package io.elastest.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonView;

@Entity
public class Trace {
    public interface TraceView {
    }

    @Id
    String exec;

    @JsonView({ TraceView.class })
    @Column(name = "component")
    @JsonProperty("component")
    String component;

    String etType;

    Date timestamp;

    String stream;

    String containerName;

    StreamType streamType;

    /* ******************** */
    /* *** Constructors *** */
    /* ******************** */

    public Trace() {
    }

    public Trace(String exec, String component, String etType, Date timestamp,
            String stream, String containerName, StreamType streamType) {
        super();
        this.exec = exec;
        this.component = component;
        this.etType = etType;
        this.timestamp = timestamp;
        this.stream = stream;
        this.containerName = containerName;
        this.streamType = streamType;
    }

    public enum StreamType {
        LOG("log"),

        COMPOSED_METRICS("composed_metrics"),

        ATOMIC_METRIC("atomic_metric");

        private String value;

        StreamType(String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }

        @JsonCreator
        public static StreamType fromValue(String text) {
            for (StreamType b : StreamType.values()) {
                if (String.valueOf(b.value).equals(text)) {
                    return b;
                }
            }
            return null;
        }
    }

    /* *********************** */
    /* *** Getters/Setters *** */
    /* *********************** */

    public String getExec() {
        return exec;
    }

    public void setExec(String exec) {
        this.exec = exec;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getEtType() {
        return etType;
    }

    public void setEtType(String etType) {
        this.etType = etType;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public StreamType getStreamType() {
        return streamType;
    }

    public void setStreamType(StreamType streamType) {
        this.streamType = streamType;
    }

}
