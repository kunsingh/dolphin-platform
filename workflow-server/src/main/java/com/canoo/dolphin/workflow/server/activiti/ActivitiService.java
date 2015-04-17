package com.canoo.dolphin.workflow.server.activiti;

import com.canoo.dolphin.server.BeanManager;
import com.canoo.dolphin.workflow.server.model.*;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.PvmTransition;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.pvm.process.TransitionImpl;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Scope("session")
public class ActivitiService {

    @Inject
    private RuntimeService runtimeService;

    @Inject
    private ManagementService managementService;

    @Inject
    private BeanManager manager;

    @Inject
    private RepositoryService repositoryService;


    public WorkflowViewModel setupWorkflowViewModel() {
        WorkflowViewModel workflowViewModel = manager.create(WorkflowViewModel.class);
        workflowViewModel.setProcessList(setupProcessList());
        return workflowViewModel;
    }

    public ProcessInstance createProcessInstance(String processInstanceId) {

        List<org.activiti.engine.runtime.ProcessInstance> list = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).list();
       return                                list.isEmpty() ? null : map(list.get(0));
    }

    private ProcessDefinition map(org.activiti.engine.repository.ProcessDefinition processDefinition) {
        ProcessDefinition mappedInstance = manager.create(ProcessDefinition.class);
        mappedInstance.setLabel(processDefinition.getId());
        mappedInstance.setName(processDefinition.getName());
        List<org.activiti.engine.runtime.ProcessInstance> instances = runtimeService.createProcessInstanceQuery().processDefinitionId(processDefinition.getId()).list();
        for (org.activiti.engine.runtime.ProcessInstance instance : instances) {
            mappedInstance.getProcessInstances().add(mapLight(instance));
        }
        return mappedInstance;
    }

    private ProcessInstance map(org.activiti.engine.runtime.ProcessInstance processInstance) {
        List<Activity> activities = createActivities(processInstance.getProcessDefinitionId());
        ProcessInstance mappedInstance = manager.create(ProcessInstance.class);
        mappedInstance.setLabel(processInstance.getId());
        mappedInstance.getActivities().addAll(activities);
        if (!activities.isEmpty()) {
            mappedInstance.setStartActivity(activities.get(0));
        }
        return mappedInstance;
    }

    private BaseProcessInstance mapLight(org.activiti.engine.runtime.ProcessInstance processInstance) {
        BaseProcessInstance mappedInstance = manager.create(BaseProcessInstance.class);
        mappedInstance.setLabel(processInstance.getId());
        return mappedInstance;
    }

    private List<Activity> createActivities(String processDefinitionId) {
        ProcessDefinitionEntity processDefinitionEntity = managementService.executeCommand(new DeployProcessDefinitionCommand(processDefinitionId));
        List<ActivityImpl> activities = processDefinitionEntity.getActivities();
        return createActivityList(activities);
    }

    private List<Activity> createActivityList(List<ActivityImpl> activityImpls) {
        final List<Activity> result = new ArrayList<>();
        final Map<String, Activity> activitiesById = new HashMap<>();
        for (final ActivityImpl activityImpl : activityImpls) {
            Activity activity = createActivityWithoutTransitions(activityImpl);
            activitiesById.put(activityImpl.getId(), activity);
            result.add(activity);
        }
        for (ActivityImpl activityImpl : activityImpls) {
            createOutGoingTransitions(activityImpl, activitiesById);
        }
        return result;
    }

    private Activity createActivityWithoutTransitions(ActivityImpl activityImpl) {
        final Activity activity = manager.create(Activity.class);
        activity.setActivityId(activityImpl.getId());
        Map<String, Object> properties = activityImpl.getProperties();
        activity.setType((String) properties.get("type"));
        activity.setActivityName((String) properties.get("name"));
        activity.setDescription((String) properties.get("documentation"));
        return activity;
    }

    private void createOutGoingTransitions(ActivityImpl sourceImpl, Map<String, Activity> idMappings) {
        List<PvmTransition> outgoingTransitions = sourceImpl.getOutgoingTransitions();
        for (PvmTransition transitionImpl : outgoingTransitions) {
            Activity source = idMappings.get(transitionImpl.getSource().getId());
            Transition transition = manager.create(Transition.class);
            Map<String, Object> properties = ((TransitionImpl) transitionImpl).getProperties();
            String targetId = transitionImpl.getDestination().getId();
            transition.setSource(source);
            transition.setTarget(idMappings.get(targetId));
            transition.setActivityId(transitionImpl.getId());
            transition.setTransitionName((String) properties.get("name"));
            transition.setConditionText((String) properties.get("conditionText"));
            transition.setDescription((String) properties.get("documentation"));
            source.getOutgoingTransitions().add(transition);
        }
    }

    private ProcessList setupProcessList() {
        final List<org.activiti.engine.repository.ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();
        final ProcessList processList = manager.create(ProcessList.class);
        processList.getProcessDefinitions().addAll(processDefinitions.parallelStream().map(this::map).collect(Collectors.toList()));
        return processList;
    }
}
