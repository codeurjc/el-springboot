package io.elastest.service;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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

    public TracesService(TraceRepository traceRepository) {
        this.traceRepository = traceRepository;
    }

    @PostConstruct
    private void init() {
        dictionary.addBuiltInDictionaries();
        dictionary.addDictionary(new File(grokPatternsFilePath));
        dictionary.bind();
    }

    public void processTcpTrace(String message, Date timestamp) {
        log.debug("Processing trace {}", message);

        if (message != null && !message.isEmpty()) {
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
                LevelEnum level = LevelEnum.fromValue(levelMap.get("level"));
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
            trace.setExec(componentExecAndComponentServiceMap.get("exec"));
            trace.setComponent(
                    componentExecAndComponentServiceMap.get("component"));
            trace.setComponentService(componentExecAndComponentServiceMap
                    .get("componentService"));

            // Message
            message = message.replaceAll(cleanMessageExpression, "");
            trace.setMessage(message);
            log.debug("Trace: {}", trace);
            
            this.traceRepository.save(trace);
        }
    }

    public Map<String, String> processGrokExpression(String message,
            String expression) {
        Grok compiledPattern = dictionary.compileExpression(expression);
        return compiledPattern.extractNamedGroups(message);
    }

    public void processBeatTrace() {

    }

}
