package com.pigs.voxly.domain.sessions;

import com.pigs.voxly.sharedKernel.domain.results.Error;

import java.util.UUID;

public final class SessionErrors {

    private SessionErrors() {}

    public static final Error SESSION_NOT_FOUND = Error.notFound("Session.NotFound", "Session not found");

    public static Error sessionNotFoundById(UUID id) {
        return Error.notFound("Session.NotFoundById", "Session with ID '" + id + "' not found");
    }

    public static final Error NO_MEDIA_UPLOADED = Error.validation("Session.NoMedia", "No media file has been uploaded");

    public static final Error MEDIA_ALREADY_UPLOADED = Error.validation("Session.MediaExists", "Media file already uploaded");

    public static final Error INVALID_STATUS_TRANSITION = Error.validation("Session.InvalidStatusTransition", "Invalid session status transition");

    public static final Error CANNOT_MODIFY_ANALYZING = Error.validation("Session.CannotModify", "Cannot modify session while analysis is in progress");

    public static final Error CANNOT_DELETE_ANALYZING = Error.validation("Session.CannotDelete", "Cannot delete session while analysis is in progress");

    public static final Error NOT_OWNER = Error.forbidden("Session.NotOwner", "You do not have permission to access this session");

    public static final Error ANALYSIS_ALREADY_REQUESTED = Error.validation("Session.AnalysisRequested", "Analysis has already been requested for this session");

    public static final Error CANNOT_ANALYZE_DRAFT = Error.validation("Session.CannotAnalyzeDraft", "Cannot analyze a session without uploaded media");
}
