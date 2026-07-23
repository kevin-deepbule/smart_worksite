package com.xd.smartworksite.task.application;

import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.task.domain.GenerateTask;
import com.xd.smartworksite.task.domain.TaskStatus;
import com.xd.smartworksite.task.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskSubmissionApplicationService {
    private static final int MAX_ERROR_LENGTH = 2000;

    private final TaskRepository taskRepository;
    private final TaskOutboxApplicationService taskOutboxApplicationService;

    public TaskSubmissionApplicationService(TaskRepository taskRepository,
                                            TaskOutboxApplicationService taskOutboxApplicationService) {
        this.taskRepository = taskRepository;
        this.taskOutboxApplicationService = taskOutboxApplicationService;
    }

    @Transactional
    public GenerateTask createPendingTask(Long projectId, String taskType, String bizType,
                                          Long bizId, String currentStage, int maxRetryCount) {
        GenerateTask task = new GenerateTask();
        task.setProjectId(projectId);
        task.setTaskType(taskType);
        task.setBizType(bizType);
        task.setBizId(bizId);
        task.setStatus(TaskStatus.PENDING.name());
        task.setCurrentStage(currentStage);
        task.setRetryCount(0);
        task.setMaxRetryCount(Math.max(0, maxRetryCount));
        task.setCancelRequested(false);
        taskRepository.insertTask(task);
        if (task.getId() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "task id was not generated");
        }
        return taskRepository.findById(task.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SYSTEM_ERROR, "task is not readable"));
    }

    @Transactional
    public GenerateTask queuePendingTask(Long taskId, String currentStage, Long updatedBy, String reason) {
        int updated = taskRepository.queuePending(taskId, currentStage, updatedBy);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "pending task queue state changed");
        }
        GenerateTask task = requireTask(taskId);
        taskOutboxApplicationService.enqueueTask(task, reason);
        return task;
    }

    @Transactional
    public void failWaitingTask(Long taskId, String currentStage, String errorMessage, Long updatedBy) {
        int updated = taskRepository.failWaiting(taskId, currentStage, normalizeError(errorMessage), updatedBy);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "waiting task failure state changed");
        }
    }

    @Transactional
    public GenerateTask resetFailedTaskToPending(Long taskId, String currentStage, Long updatedBy) {
        int updated = taskRepository.resetFailedToPending(taskId, currentStage, updatedBy);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "failed task cannot be reset to pending");
        }
        return requireTask(taskId);
    }

    private GenerateTask requireTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "task not found"));
    }

    private String normalizeError(String message) {
        if (message == null || message.isBlank()) {
            return "task failed";
        }
        String value = message.trim();
        return value.length() <= MAX_ERROR_LENGTH ? value : value.substring(0, MAX_ERROR_LENGTH);
    }
}
