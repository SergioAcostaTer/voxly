package com.pigs.voxly.application.evaluation;

import com.pigs.voxly.application.evaluation.dto.EvaluationResponse;
import com.pigs.voxly.application.identity.ports.CurrentUserProvider;
import com.pigs.voxly.application.sessions.SessionStatusStreamService;
import com.pigs.voxly.domain.evaluation.Evaluation;
import com.pigs.voxly.domain.evaluation.EvaluationErrors;
import com.pigs.voxly.domain.evaluation.EvaluationId;
import com.pigs.voxly.domain.evaluation.EvaluationRepository;
import com.pigs.voxly.domain.identity.UserId;
import com.pigs.voxly.domain.sessions.SessionErrors;
import com.pigs.voxly.domain.sessions.SessionId;
import com.pigs.voxly.domain.sessions.SessionRepository;
import com.pigs.voxly.sharedKernel.domain.results.ResultT;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final SessionRepository sessionRepository;
    private final CurrentUserProvider currentUserProvider;
    private final EvaluationProcessor evaluationProcessor;
    private final SessionStatusStreamService sessionStatusStreamService;

    public EvaluationService(
            EvaluationRepository evaluationRepository,
            SessionRepository sessionRepository,
            CurrentUserProvider currentUserProvider,
            EvaluationProcessor evaluationProcessor,
            SessionStatusStreamService sessionStatusStreamService
    ) {
        this.evaluationRepository = evaluationRepository;
        this.sessionRepository = sessionRepository;
        this.currentUserProvider = currentUserProvider;
        this.evaluationProcessor = evaluationProcessor;
        this.sessionStatusStreamService = sessionStatusStreamService;
    }

    public ResultT<EvaluationResponse> getEvaluation(UUID evaluationId) {
        var userIdOpt = currentUserProvider.getUserId();
        if (userIdOpt.isEmpty()) {
            return ResultT.failure(SessionErrors.NOT_OWNER);
        }

        var evaluationOpt = evaluationRepository.findById(EvaluationId.from(evaluationId));
        if (evaluationOpt.isEmpty()) {
            return ResultT.failure(EvaluationErrors.evaluationNotFoundById(evaluationId));
        }

        var evaluation = evaluationOpt.get();

        // Verify ownership through session
        var sessionOpt = sessionRepository.findByIdAndUserId(
                evaluation.getSessionId(),
                UserId.from(userIdOpt.get())
        );
        if (sessionOpt.isEmpty()) {
            return ResultT.failure(SessionErrors.NOT_OWNER);
        }

        return ResultT.success(EvaluationResponse.fromDomain(evaluation));
    }

    public ResultT<EvaluationResponse> getEvaluationBySessionId(UUID sessionId) {
        var userIdOpt = currentUserProvider.getUserId();
        if (userIdOpt.isEmpty()) {
            return ResultT.failure(SessionErrors.NOT_OWNER);
        }

        // Verify session ownership
        var sessionOpt = sessionRepository.findByIdAndUserId(
                SessionId.from(sessionId),
                UserId.from(userIdOpt.get())
        );
        if (sessionOpt.isEmpty()) {
            return ResultT.failure(SessionErrors.sessionNotFoundById(sessionId));
        }

        var evaluationOpt = evaluationRepository.findBySessionId(SessionId.from(sessionId));
        if (evaluationOpt.isEmpty()) {
            return ResultT.failure(EvaluationErrors.EVALUATION_NOT_FOUND);
        }

        return ResultT.success(EvaluationResponse.fromDomain(evaluationOpt.get()));
    }

    @Transactional
    public ResultT<EvaluationResponse> startEvaluation(UUID sessionId) {
        var userIdOpt = currentUserProvider.getUserId();
        if (userIdOpt.isEmpty()) {
            return ResultT.failure(SessionErrors.NOT_OWNER);
        }

        var sessionOpt = sessionRepository.findByIdAndUserId(
                SessionId.from(sessionId),
                UserId.from(userIdOpt.get())
        );
        if (sessionOpt.isEmpty()) {
            return ResultT.failure(SessionErrors.sessionNotFoundById(sessionId));
        }

        var session = sessionOpt.get();

        // Check if evaluation already exists
        var existingEval = evaluationRepository.findBySessionId(session.getId());
        if (existingEval.isPresent()) {
            return ResultT.success(EvaluationResponse.fromDomain(existingEval.get()));
        }

        // Create new evaluation
        var evalResult = Evaluation.create(
                session.getId(),
                UserId.from(userIdOpt.get()),
                session.getSessionType().getName()
        );

        if (evalResult.isFailure()) {
            return ResultT.failure(evalResult.getError());
        }

        var evaluation = evalResult.getValue();
        evaluationRepository.save(evaluation);
        sessionStatusStreamService.publishEvaluationUpdate(evaluation);

        // Trigger async processing
        evaluationProcessor.processAsync(evaluation.getId().getValue());

        return ResultT.success(EvaluationResponse.fromDomain(evaluation));
    }
}
