package io.elastest.service;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.aicer.grok.dictionary.GrokDictionary;
import org.aicer.grok.util.Grok;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.elastest.model.Enums.LevelEnum;
import io.elastest.model.Enums.StreamType;
import io.elastest.dao.TraceRepository;
import io.elastest.model.Trace;
import io.elastest.util.Utils;

@Service
public class TracesService {
    public final Logger log = getLogger(lookup().lookupClass());

    private final TraceRepository traceRepository;

    final GrokDictionary dictionary = new GrokDictionary();

    @Value("${grok.patterns.file}")
    private String grokPatternsFilePath;

    String javaLogLevelExpression = "%{JAVALOGLEVEL:level}";
    String containerNameExpression = "%{CONTAINERNAME:containerName}";
    String componentExecAndComponentServiceExpression = "^(?<component>(test|sut|dynamic))_?(?<exec>\\d*)(_(?<componentService>[^_]*(?=_\\d*)?))?";
    String cleanMessageExpression = "^([<]\\d*[>].*)?(?>test_\\d*|sut_\\d*|dynamic_\\d*)\\D*(?>_exec)\\[.*\\][:][\\s]";

    String startsWithTestOrSutExpression = "/^(test|sut)(_)?(\\d*)(.*)?/";

    public TracesService(TraceRepository traceRepository) {
        this.traceRepository = traceRepository;
    }

    @PostConstruct
    private void init() {
        dictionary.addBuiltInDictionaries();
        dictionary.addDictionary(new File(grokPatternsFilePath));
        dictionary.bind();
    }

    public Map<String, String> processGrokExpression(String message,
            String expression) {
        Grok compiledPattern = dictionary.compileExpression(expression);
        return compiledPattern.extractNamedGroups(message);
    }

    public Trace cleanCommonFields(Trace trace, String message) {
        // Message
        if (message != null) {
            message = message.replaceAll(cleanMessageExpression, "");
            trace.setMessage(message);
        }

        // Change containerName and component dashes to underscores
        if (trace.getContainerName() != null) {
            trace.setContainerName(
                    trace.getContainerName().replaceAll("-", "_"));
        }
        if (trace.getComponent() != null) {
            trace.setComponent(trace.getComponent().replaceAll("-", "_"));
        }

        if (trace.getComponentService() != null) {
            trace.setComponentService(
                    trace.getComponentService().replaceAll("-", "_"));
        }
        return trace;
    }

    /* *********** */
    /* *** TCP *** */
    /* *********** */

    public void processTcpTrace(String message, Date timestamp) {
        log.debug("Processing trace {}", message);

        if (message != null && !message.isEmpty()) {
            try {
                Trace trace = new Trace();
                trace.setEtType("et_logs");
                trace.setStream("default_log");
                trace.setStreamType(StreamType.LOG);

                // Timestamp
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
                String timestampAsISO8061 = df.format(timestamp);
                trace.setTimestamp(timestampAsISO8061);

                // Level
                Map<String, String> levelMap = processGrokExpression(message,
                        javaLogLevelExpression);
                try {
                    LevelEnum level = LevelEnum
                            .fromValue(levelMap.get("level"));
                    trace.setLevel(level);
                } catch (Exception e) {

                }

                // Container Name
                Map<String, String> containerNameMap = processGrokExpression(
                        message, containerNameExpression);
                trace.setContainerName(containerNameMap.get("containerName"));

                // Exec, Component and Component Service
                Map<String, String> componentExecAndComponentServiceMap = processGrokExpression(
                        trace.getContainerName(),
                        componentExecAndComponentServiceExpression);
                if (componentExecAndComponentServiceMap != null
                        && !componentExecAndComponentServiceMap.isEmpty()) {
                    trace.setExec(
                            componentExecAndComponentServiceMap.get("exec"));
                    trace.setComponent(componentExecAndComponentServiceMap
                            .get("component"));
                    trace.setComponentService(
                            componentExecAndComponentServiceMap
                                    .get("componentService"));
                }

                trace = cleanCommonFields(trace, message);

                log.debug("Trace: {}", trace);
                this.traceRepository.save(trace);
            } catch (Exception e) {
                log.error("Error on processing TCP trace: ", e);
            }
        }
    }

    /* ************* */
    /* *** Beats *** */
    /* ************* */
    public Trace setInitialBeatTraceData(Map<String, Object> dataMap) {
        Trace trace = new Trace();
        trace.setComponent((String) dataMap.get("component"));
        trace.setComponentService((String) dataMap.get("componentService"));
        trace.setContainerName((String) dataMap.get("containerName"));
        trace.setEtType((String) dataMap.get("et_type"));
        trace.setExec((String) dataMap.get("exec"));
        trace.setLevel(LevelEnum.fromValue((String) dataMap.get("level")));
        trace.setMessage((String) dataMap.get("message"));
        trace.setMetricName((String) dataMap.get("metricName"));
        trace.setStream((String) dataMap.get("stream"));
        trace.setStreamType(
                StreamType.fromValue((String) dataMap.get("stream_type")));
        trace.setTimestamp((String) dataMap.get("@timestamp"));
        trace.setUnit((String) dataMap.get("unit"));
        trace.setUnits((String) dataMap.get("units"));

        return trace;

    }

    public void processBeatTrace(Map<String, Object> dataMap) {
        log.debug("Processing trace {}", dataMap.toString());
        // TODO dockbeat input
        if (dataMap != null && !dataMap.isEmpty()) {
            try {
                Trace trace = setInitialBeatTraceData(dataMap);

                String component = (String) dataMap.get("component");
                if (component == null) {
                    return;
                }

                trace.setComponent(component);

                // Docker
                String[] containerNameTree = new String[] { "docker",
                        "container", "name" };
                String containerName = (String) Utils.getMapFieldByTreeList(
                        dataMap, Arrays.asList(containerNameTree));

                trace.setContainerName(containerName);
                if (containerName != null) {
                    // Metricbeat
                    if (dataMap.get("metricset") == null) {
                        trace.setComponent(component + "_" + containerName);
                    } else {// Filebeat
                        if (!containerName
                                .matches(startsWithTestOrSutExpression)) {
                            return;
                        }
                        if (dataMap.get("json") != null) {

                            String[] jsonLogTree = new String[] { "json",
                                    "log" };

                            String message = (String) Utils
                                    .getMapFieldByTreeList(dataMap,
                                            Arrays.asList(jsonLogTree));

                            if (message != null) {
                                trace.setMessage(message);
                            }

                        } else {
                            String log = (String) dataMap.get("log");
                            if (log != null) {
                                trace.setMessage(log);
                            }

                        }
                    }
                }
                // Exec, Component and Component Service
                Map<String, String> componentExecAndComponentServiceMap = processGrokExpression(
                        trace.getContainerName(),
                        componentExecAndComponentServiceExpression);
                if (componentExecAndComponentServiceMap != null
                        && !componentExecAndComponentServiceMap.isEmpty()) {
                    trace.setExec(
                            componentExecAndComponentServiceMap.get("exec"));
                    trace.setComponent(componentExecAndComponentServiceMap
                            .get("component"));
                    trace.setComponentService(
                            componentExecAndComponentServiceMap
                                    .get("componentService"));
                }

                trace = cleanCommonFields(trace, trace.getMessage());

                if (trace.getMessage() != null) {
                    trace.setStreamType(StreamType.LOG);
                }

                if (trace.getStreamType() == null
                        || !trace.getStreamType().equals(StreamType.LOG)) {
                    // Dockbeat
                    if (trace.getStream().equals("et_dockbeat")) {
                        if (trace.getContainerName()
                                .matches("(\\D*\\d*_\\D*_\\d*)|(\\D*_\\d*)")) {
                            trace.setStreamType(StreamType.COMPOSED_METRICS);
                            if (trace.getComponentService() != null) {
                                trace.setComponent(trace.getComponent() + "_"
                                        + trace.getComponentService());
                            }
                        } else {
                            return;
                        }
                    } else {
                        if (dataMap.get("metricset") != null) {
                            String[] metricsetModuleTree = new String[] {
                                    "metricset", "module" };
                            String metricsetModule = (String) Utils
                                    .getMapFieldByTreeList(dataMap,
                                            Arrays.asList(metricsetModuleTree));

                            String[] metricsetNameTree = new String[] {
                                    "metricset", "name" };
                            String metricsetName = (String) Utils
                                    .getMapFieldByTreeList(dataMap,
                                            Arrays.asList(metricsetNameTree));

                            trace.setEtType(
                                    metricsetModule + "_" + metricsetName);
                            
                        }
                    }
                }

                log.debug("Trace: {}", trace);
                this.traceRepository.save(trace);
            } catch (Exception e) {
                log.error("Error on processing TCP trace: ", e);
            }
        }
    }

}
