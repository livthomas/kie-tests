package org.kie.perf.scenario.load;

import java.util.List;

import org.kie.api.event.process.DefaultProcessEventListener;
import org.kie.api.event.process.ProcessCompletedEvent;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.perf.SharedMetricRegistry;
import org.kie.perf.jbpm.JBPMController;
import org.kie.perf.jbpm.constant.ProcessStorage;
import org.kie.perf.jbpm.constant.UserStorage;
import org.kie.perf.scenario.IPerfTest;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

public class LHumanTaskProcess implements IPerfTest {

    private JBPMController jc;

    private Timer startProcess;
    private Timer startTaskDuration;
    private Timer completeTaskDuration;
    private Meter completedProcess;

    @Override
    public void init() {
        jc = JBPMController.getInstance();
        jc.addProcessEventListener(new DefaultProcessEventListener() {
            @Override
            public void afterProcessCompleted(ProcessCompletedEvent event) {
                completedProcess.mark();
            }
        });

        jc.createRuntimeManager(ProcessStorage.HumanTask.getPath());
    }

    @Override
    public void initMetrics() {
        MetricRegistry metrics = SharedMetricRegistry.getInstance();
        startProcess = metrics.timer(MetricRegistry.name(LHumanTaskProcess.class, "scenario.process.start.duration"));
        startTaskDuration = metrics.timer(MetricRegistry.name(LHumanTaskProcess.class, "scenario.task.start.duration"));
        completeTaskDuration = metrics.timer(MetricRegistry.name(LHumanTaskProcess.class, "scenario.task.complete.duration"));
        completedProcess = metrics.meter(MetricRegistry.name(LHumanTaskProcess.class, "scenario.process.completed"));
    }

    @Override
    public void execute() {
        Timer.Context context;

        context = startProcess.time();
        RuntimeEngine runtimeEngine = jc.getRuntimeEngine();
        KieSession ksession = runtimeEngine.getKieSession();
        ProcessInstance pi = ksession.startProcess(ProcessStorage.HumanTask.getProcessDefinitionId());
        context.stop();

        TaskService taskService = runtimeEngine.getTaskService();
        List<Long> tasks = taskService.getTasksByProcessInstanceId(pi.getId());
        Long taskSummaryId = tasks.get(0);

        context = startTaskDuration.time();
        taskService.start(taskSummaryId, UserStorage.PerfUser.getUserId());
        context.stop();

        context = completeTaskDuration.time();
        taskService.complete(taskSummaryId, UserStorage.PerfUser.getUserId(), null);
        context.stop();
    }

    @Override
    public void close() {
        jc.tearDown();
    }

}
