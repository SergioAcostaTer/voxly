package com.pigs.voxly.application.sessions;

import java.io.IOException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.pigs.voxly.application.evaluation.EvaluationService;
import com.pigs.voxly.application.identity.ports.CurrentUserProvider;
import com.pigs.voxly.application.sessions.dto.CreateSessionRequest;
import com.pigs.voxly.application.sessions.dto.SessionListResponse;
import com.pigs.voxly.application.sessions.dto.SessionResponse;
import com.pigs.voxly.application.sessions.dto.UpdateSessionRequest;
import com.pigs.voxly.application.shared.ports.StorageService;
import com.pigs.voxly.domain.identity.UserId;
import com.pigs.voxly.domain.sessions.Session;
import com.pigs.voxly.domain.sessions.SessionErrors;
import com.pigs.voxly.domain.sessions.SessionId;
import com.pigs.voxly.domain.sessions.SessionRepository;
import com.pigs.voxly.domain.sessions.enumerations.SessionType;
import com.pigs.voxly.domain.sessions.valueobjects.MediaFile;
import com.pigs.voxly.domain.sessions.valueobjects.SessionTitle;
import com.pigs.voxly.sharedKernel.domain.pagination.PagedRequest;
import com.pigs.voxly.sharedKernel.domain.results.Result;
import com.pigs.voxly.sharedKernel.domain.results.ResultT;
import com.pigs.voxly.sharedKernel.validation.FileValidator;

@Service
@Transactional(readOnly = true)
public class SessionService {

    private final SessionRepository sessionRepository;
    private final StorageService storageService;
    private final CurrentUserProvider currentUserProvider;
    private final EvaluationService evaluationService;

    public SessionService(
            SessionRepository sessionRepository,
            StorageService storageService,
            CurrentUserProvider currentUserProvider,
            EvaluationService evaluationService) {
        this.sessionRepository = sessionRepository;
        this.storageService = storageService;
        this.currentUserProvider = currentUserProvider;
        this.evaluationService = evaluationService;
    }

    @Transactional
    public ResultT<SessionResponse> createSession(CreateSessionRequest request) {
        var userIdOpt = currentUserProvider.getUserId();
        if (userIdOpt.isEmpty()) {
            return ResultT.failure(SessionErrors.NOT_OWNER);
        }

        var titleResult = SessionTitle.create(request.title());
        if (titleResult.isFailure()) {
            return ResultT.failure(titleResult.getError());
        }

        var sessionType = SessionType.fromName(request.sessionType());

        var sessionResult = Session.create(
                UserId.from(userIdOpt.get()),
                titleResult.getValue(),
                request.description(),
                sessionType,
                request.language());

        if (sessionResult.isFailure()) {
            return ResultT.failure(sessionResult.getError());
        }

        var session = sessionResult.getValue();
        sessionRepository.save(session);

        return ResultT.success(SessionResponse.fromDomain(session));
    }

    public ResultT<SessionResponse> getSession(UUID sessionId) {
        var userIdOpt = currentUserProvider.getUserId();
        if (userIdOpt.isEmpty()) {
            return ResultT.failure(SessionErrors.NOT_OWNER);
        }

        var sessionOpt = sessionRepository.findByIdAndUserId(
                SessionId.from(sessionId),
                UserId.from(userIdOpt.get()));

        if (sessionOpt.isEmpty()) {
            return ResultT.failure(SessionErrors.sessionNotFoundById(sessionId));
        }

        return ResultT.success(SessionResponse.fromDomain(sessionOpt.get()));
    }

    public ResultT<SessionListResponse> getUserSessions(int page, int size) {
        var userIdOpt = currentUserProvider.getUserId();
        if (userIdOpt.isEmpty()) {
            return ResultT.failure(SessionErrors.NOT_OWNER);
        }

        var pagedRequest = PagedRequest.of(page, size);
        var sessions = sessionRepository.findByUserId(UserId.from(userIdOpt.get()), pagedRequest);

        return ResultT.success(SessionListResponse.fromPagedList(sessions));
    }

    @Transactional
    public ResultT<SessionResponse> updateSession(UUID sessionId, UpdateSessionRequest request) {
        var userIdOpt = currentUserProvider.getUserId();
        if (userIdOpt.isEmpty()) {
            return ResultT.failure(SessionErrors.NOT_OWNER);
        }

        var sessionOpt = sessionRepository.findByIdAndUserId(
                SessionId.from(sessionId),
                UserId.from(userIdOpt.get()));

        if (sessionOpt.isEmpty()) {
            return ResultT.failure(SessionErrors.sessionNotFoundById(sessionId));
        }

        var session = sessionOpt.get();

        SessionTitle newTitle = session.getTitle();
        if (request.title() != null && !request.title().isBlank()) {
            var titleResult = SessionTitle.create(request.title());
            if (titleResult.isFailure()) {
                return ResultT.failure(titleResult.getError());
            }
            newTitle = titleResult.getValue();
        }

        String newDescription = request.description() != null ? request.description() : session.getDescription();
        SessionType newType = request.sessionType() != null ? SessionType.fromName(request.sessionType())
                : session.getSessionType();

        var updateResult = session.updateDetails(newTitle, newDescription, newType);
        if (updateResult.isFailure()) {
            return ResultT.failure(updateResult.getError());
        }

        sessionRepository.save(session);
        return ResultT.success(SessionResponse.fromDomain(session));
    }

    @Transactional
    public ResultT<SessionResponse> uploadMedia(UUID sessionId, MultipartFile file) {
        var userIdOpt = currentUserProvider.getUserId();
        if (userIdOpt.isEmpty()) {
            return ResultT.failure(SessionErrors.NOT_OWNER);
        }

        var sessionOpt = sessionRepository.findByIdAndUserId(
                SessionId.from(sessionId),
                UserId.from(userIdOpt.get()));

        if (sessionOpt.isEmpty()) {
            return ResultT.failure(SessionErrors.sessionNotFoundById(sessionId));
        }

        var session = sessionOpt.get();

        // Validate file
        var contentType = file.getContentType();
        var validationResult = FileValidator.isAudioContentType(contentType)
                ? FileValidator.validateAudio(file.getOriginalFilename(), contentType, file.getSize())
                : FileValidator.validateVideo(file.getOriginalFilename(), contentType, file.getSize());

        if (validationResult.isFailure()) {
            return ResultT.failure(validationResult.getError());
        }

        // Store file
        try {
            var directory = "sessions/" + sessionId;
            var storeResult = storageService.store(
                    file.getInputStream(),
                    file.getOriginalFilename(),
                    file.getContentType(),
                    directory);

            if (storeResult.isFailure()) {
                return ResultT.failure(storeResult.getError());
            }

            var mediaFile = MediaFile.create(
                    storeResult.getValue(),
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize());

            var uploadResult = session.uploadMedia(mediaFile);
            if (uploadResult.isFailure()) {
                // Clean up stored file
                storageService.delete(storeResult.getValue());
                return ResultT.failure(uploadResult.getError());
            }

            sessionRepository.save(session);
            return ResultT.success(SessionResponse.fromDomain(session));

        } catch (IOException e) {
            return ResultT.failure(com.pigs.voxly.sharedKernel.domain.results.Error.failure(
                    "Session.UploadFailed", "Failed to upload media: " + e.getMessage()));
        }
    }

    @Transactional
    public Result deleteSession(UUID sessionId) {
        var userIdOpt = currentUserProvider.getUserId();
        if (userIdOpt.isEmpty()) {
            return Result.failure(SessionErrors.NOT_OWNER);
        }

        var sessionOpt = sessionRepository.findByIdAndUserId(
                SessionId.from(sessionId),
                UserId.from(userIdOpt.get()));

        if (sessionOpt.isEmpty()) {
            return Result.failure(SessionErrors.sessionNotFoundById(sessionId));
        }

        var session = sessionOpt.get();

        var prepareResult = session.prepareForDeletion();
        if (prepareResult.isFailure()) {
            return Result.failure(prepareResult.getError());
        }

        // Delete files from storage
        for (String path : prepareResult.getValue()) {
            storageService.delete(path);
        }

        sessionRepository.delete(session);
        return Result.success();
    }

    @Transactional
    public Result requestAnalysis(UUID sessionId) {
        var userIdOpt = currentUserProvider.getUserId();
        if (userIdOpt.isEmpty()) {
            return Result.failure(SessionErrors.NOT_OWNER);
        }

        var sessionOpt = sessionRepository.findByIdAndUserId(
                SessionId.from(sessionId),
                UserId.from(userIdOpt.get()));

        if (sessionOpt.isEmpty()) {
            return Result.failure(SessionErrors.sessionNotFoundById(sessionId));
        }

        var session = sessionOpt.get();
        var result = session.requestAnalysis();

        if (result.isSuccess()) {
            sessionRepository.save(session);

            // Detached, idempotent pipeline start; processing continues after page reloads.
            var evaluationResult = evaluationService.startEvaluation(sessionId);
            if (evaluationResult.isFailure()) {
                return Result.failure(evaluationResult.getError());
            }
        }

        return result;
    }
}
