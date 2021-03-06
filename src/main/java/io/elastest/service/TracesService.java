package io.elastest.service;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.elastest.dao.TraceRepository;
import io.elastest.model.Enums.LevelEnum;
import io.elastest.model.Enums.StreamType;
import io.elastest.model.Trace;
import io.elastest.util.Utils;
import io.krakens.grok.api.Grok;
import io.krakens.grok.api.GrokCompiler;

@Service
public class TracesService {
    public final Logger logger = getLogger(lookup().lookupClass());

    private final TraceRepository traceRepository;
    private final QueueService queueService;

    GrokCompiler grokCompiler;

    @Value("${grok.patterns.file.path}")
    private String grokPatternsFilePath;

    String javaLogLevelExpression = "%{JAVALOGLEVEL:level}";
    String containerNameExpression = "%{CONTAINERNAME:containerName}";
    String componentExecAndComponentServiceExpression = "^(?<component>(test|sut|dynamic))_?(?<exec>\\d*)(_(?<componentService>[^_]*(?=_\\d*)?))?";
    String cleanMessageExpression = "^([<]\\d*[>].*)?(?>test_\\d*|sut_\\d*|dynamic_\\d*)\\D*(?>_exec)\\[.*\\][:][\\s]";

    String startsWithTestOrSutExpression = "^(test|sut)(_)?(\\d*)(.*)?";

    String dockbeatStream = "et_dockbeat";

    @Autowired
    public TracesService(TraceRepository traceRepository,
            QueueService queueService) {
        this.traceRepository = traceRepository;
        this.queueService = queueService;
    }

    @PostConstruct
    private void init() throws IOException {
        grokCompiler = GrokCompiler.newInstance();
        grokCompiler.registerDefaultPatterns();

        InputStream inputStream = getClass()
                .getResourceAsStream("/" + grokPatternsFilePath);
        grokCompiler.register(inputStream, StandardCharsets.UTF_8);

    }

    public Map<String, String> processGrokExpression(String message,
            String expression) {
        Grok compiledPattern = grokCompiler.compile(expression);
        Map<String, Object> map = compiledPattern.match(message).capture();

        // As <String,String> Map
        return map.entrySet().stream().collect(Collectors
                .toMap(Map.Entry::getKey, e -> (String) e.getValue()));
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

    public Trace matchesLevelAndContainerNameFromMessage(Trace trace,
            String message) {
        if (message != null) {
            // Level
            Map<String, String> levelMap = processGrokExpression(message,
                    javaLogLevelExpression);
            try {
                LevelEnum level = LevelEnum.fromValue(levelMap.get("level"));
                trace.setLevel(level);
            } catch (Exception e) {

            }

            // Container Name
            Map<String, String> containerNameMap = processGrokExpression(
                    message, containerNameExpression);
            String containerName = containerNameMap.get("containerName");
            if (containerName != null) {
                trace.setContainerName(containerName);
            }
        }
        return trace;
    }

    public void saveTrace(Trace trace) {
        synchronized (this.traceRepository) {
            this.traceRepository.save(trace);
        }
    }

    /* *********** */
    /* *** TCP *** */
    /* *********** */

    public void processTcpTrace(String message, Date timestamp) {
        logger.debug("Processing trace {}", message);

        if (message != null && !message.isEmpty()) {
            try {
                Trace trace = new Trace();
                trace.setEtType("et_logs");
                trace.setStream("default_log");
                trace.setStreamType(StreamType.LOG);

                // Timestamp
                DateFormat df = new SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                String timestampAsISO8061 = df.format(timestamp);
                trace.setTimestamp(timestampAsISO8061);

                // If message, set level and container name
                trace = this.matchesLevelAndContainerNameFromMessage(trace,
                        message);

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

                if (trace.getComponentService() != null) {
                    trace.setComponent(trace.getComponent() + "_"
                            + trace.getComponentService());
                }

                logger.debug("Trace: {}", trace);
                this.saveTrace(trace);
                this.queueService.sendTrace(trace);
            } catch (Exception e) {
                logger.error("Error on processing TCP trace {}: ", message, e);
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

    @SuppressWarnings("unchecked")
    public void processBeatTrace(Map<String, Object> dataMap,
            boolean fromDockbeat) {
        logger.debug("Processing trace {}", dataMap.toString());
        if (dataMap != null && !dataMap.isEmpty()) {
            try {
                Trace trace = setInitialBeatTraceData(dataMap);

                if (fromDockbeat) {
                    trace.setStream(dockbeatStream);
                }
                // If message, set level and container name
                trace = this.matchesLevelAndContainerNameFromMessage(trace,
                        (String) dataMap.get("message"));

                String component = trace.getComponent();

                // Docker
                String[] containerNameTree = new String[] { "docker",
                        "container", "name" };
                String containerName = (String) Utils.getMapFieldByTreeList(
                        dataMap, Arrays.asList(containerNameTree));

                if (containerName != null) {
                    trace.setContainerName(containerName);
                    // Metricbeat
                    if (dataMap.get("metricset") != null) {
                        if (component != null) {
                            trace.setComponent(component + "_" + containerName);
                        }
                    } else {// Filebeat
                        if (component == null) {
                            // from etm filebeat, discard non sut/test
                            // containers
                            if (!containerName
                                    .matches(startsWithTestOrSutExpression)) {
                                logger.error(
                                        "Filebeat trace without component and container name {} does not matches sut/test, discarding",
                                        containerName);
                                return;
                            }
                        } else {
                            trace.setComponent(component + "_" + containerName);
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
                if (trace.getContainerName() != null) {
                    Map<String, String> componentExecAndComponentServiceMap = processGrokExpression(
                            trace.getContainerName(),
                            componentExecAndComponentServiceExpression);
                    if (componentExecAndComponentServiceMap != null
                            && !componentExecAndComponentServiceMap.isEmpty()) {
                        trace.setExec(componentExecAndComponentServiceMap
                                .get("exec"));
                        trace.setComponent(componentExecAndComponentServiceMap
                                .get("component"));
                        trace.setComponentService(
                                componentExecAndComponentServiceMap
                                        .get("componentService"));
                    }
                }

                trace = cleanCommonFields(trace, trace.getMessage());

                if (trace.getMessage() != null) {
                    trace.setStreamType(StreamType.LOG);
                }

                // Its Metric
                if (trace.getStreamType() == null
                        || !trace.getStreamType().equals(StreamType.LOG)) {
                    // Dockbeat
                    if(trace.getStream() == null) {
                        return;
                    }
                    if (trace.getStream().equals(dockbeatStream)) {
                        if (trace.getContainerName()
                                .matches(startsWithTestOrSutExpression)) {
                            trace.setStreamType(StreamType.COMPOSED_METRICS);
                            if (trace.getComponentService() != null) {
                                trace.setComponent(trace.getComponent() + "_"
                                        + trace.getComponentService());
                            }
                            trace.setEtType((String) dataMap.get("type"));
                            trace.setMetricName(trace.getEtType());
                            trace.setContentFromLinkedHashMap(
                                    (LinkedHashMap<Object, Object>) dataMap
                                            .get(trace.getEtType()));

                        } else {
                            logger.error(
                                    "Dockbeat trace container name {} does not matches sut/test, discarding",
                                    trace.getContainerName());
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

                            String metricName = metricsetModule + "_"
                                    + metricsetName;
                            trace.setEtType(metricName);
                            trace.setMetricName(metricName);

                            String[] contentTree = new String[] {
                                    metricsetModule, metricsetName };
                            LinkedHashMap<Object, Object> content = (LinkedHashMap<Object, Object>) Utils
                                    .getMapFieldByTreeList(dataMap,
                                            Arrays.asList(contentTree));

                            trace.setContentFromLinkedHashMap(content);

                            if (trace.getStreamType() == null) {
                                trace.setStreamType(
                                        StreamType.COMPOSED_METRICS);
                            }

                        }
                    }
                } else { // log
                    trace.setEtType("et_logs");

                    if (trace.getStream() == null) {
                        trace.setStream("default_log");
                    }

                    if (trace.getComponentService() != null) {
                        trace.setComponent(trace.getComponent() + "_"
                                + trace.getComponentService());
                    }
                }

                logger.debug("Trace: {}", trace);
                this.saveTrace(trace);
                this.queueService.sendTrace(trace);
            } catch (Exception e) {
                logger.error("Error on processing Beat trace {}: ", dataMap, e);
            }
        }
    }

    /* ************ */
    /* *** HTTP *** */
    /* ************ */

    @SuppressWarnings("unchecked")
    public void processHttpTrace(Map<String, Object> dataMap) {
        if (dataMap != null && !dataMap.isEmpty()) {
            List<String> messages = (List<String>) dataMap.get("messages");
            // Multiple messages
            if (messages != null) {
                logger.debug("Is multiple message trace. Spliting...");
                for (String message : messages) {
                    Map<String, Object> currentMap = new HashMap<>();
                    currentMap.putAll(dataMap);
                    currentMap.remove("messages");
                    currentMap.put("message", message);
                    this.processHttpTrace(currentMap);
                    return;
                }
            } else {
                this.processBeatTrace(dataMap, false);
            }
        }
    }
}
