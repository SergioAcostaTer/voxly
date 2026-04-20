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

    @Transactional
    public ResultT<SessionResponse> createSessionWithMedia(CreateSessionRequest request, MultipartFile file) {
        var userIdOpt = currentUserProvider.getUserId();
        if (userIdOpt.isEmpty()) {
            return ResultT.failure(SessionErrors.NOT_OWNER);
        }

        var titleResult = SessionTitle.create(request.title());
        if (titleResult.isFailure()) {
            return ResultT.failure(titleResult.getError());
        }

        var sessionType = SessionType.fromName(request.sessionType());

        var validationResult = validateMediaFile(file);
        if (validationResult.isFailure()) {
            return ResultT.failure(validationResult.getError());
        }

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
        var uploadResult = storeAndAttachMedia(session, file);
        if (uploadResult.isFailure()) {
            return ResultT.failure(uploadResult.getError());
        }

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

        var validationResult = validateMediaFile(file);
        if (validationResult.isFailure()) {
            return ResultT.failure(validationResult.getError());
        }

        var uploadResult = storeAndAttachMedia(session, file);
        if (uploadResult.isFailure()) {
            return ResultT.failure(uploadResult.getError());
        }

        sessionRepository.save(session);
        return ResultT.success(SessionResponse.fromDomain(session));
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

    private Result validateMediaFile(MultipartFile file) {
        var contentType = file.getContentType();
        var normalizedFileName = normalizeUploadedFileName(file.getOriginalFilename(), contentType);
        boolean isAudioUpload = FileValidator.isAudioContentType(contentType)
                || (!FileValidator.isVideoContentType(contentType)
                        && FileValidator.isAudioFileName(normalizedFileName));
        return isAudioUpload
                ? FileValidator.validateAudio(normalizedFileName, contentType, file.getSize())
                : FileValidator.validateVideo(normalizedFileName, contentType, file.getSize());
    }

    private Result storeAndAttachMedia(Session session, MultipartFile file) {
        try {
            var directory = "sessions/" + session.getId().getValue();
            var normalizedFileName = normalizeUploadedFileName(file.getOriginalFilename(), file.getContentType());
            var storeResult = storageService.store(
                    file.getInputStream(),
                    normalizedFileName,
                    file.getContentType(),
                    directory);

            if (storeResult.isFailure()) {
                return Result.failure(storeResult.getError());
            }

            var mediaFile = MediaFile.create(
                    storeResult.getValue(),
                    normalizedFileName,
                    file.getContentType(),
                    file.getSize());

            var uploadResult = session.uploadMedia(mediaFile);
            if (uploadResult.isFailure()) {
                storageService.delete(storeResult.getValue());
                return Result.failure(uploadResult.getError());
            }

            return Result.success();
        } catch (IOException e) {
            return Result.failure(com.pigs.voxly.sharedKernel.domain.results.Error.failure(
                    "Session.UploadFailed", "Failed to upload media: " + e.getMessage()));
        }
    }

    private String normalizeUploadedFileName(String originalFileName, String contentType) {
        String fallbackFileName = originalFileName == null || originalFileName.isBlank()
                ? "upload"
                : originalFileName;

        String normalizedContentType = contentType == null ? "" : contentType.trim().toLowerCase();
        if (!fallbackFileName.toLowerCase().endsWith(".weba")) {
            return fallbackFileName;
        }

        if (normalizedContentType.startsWith("audio/webm") || normalizedContentType.startsWith("video/webm")) {
            return fallbackFileName.substring(0, fallbackFileName.length() - 5) + ".webm";
        }

        return fallbackFileName;
    }
}
