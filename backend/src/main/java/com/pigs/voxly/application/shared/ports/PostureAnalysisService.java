package com.pigs.voxly.application.shared.ports;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import com.pigs.voxly.sharedKernel.domain.results.ResultT;

public interface PostureAnalysisService {

    ResultT<PostureAnalysisResult> analyze(Path videoPath, Consumer<Double> progressCallback);

    default ResultT<PostureAnalysisResult> analyze(Path videoPath) {
        return analyze(videoPath, p -> {});
    }

    record PostureAnalysisResult(
            double finalScore,
            int maxScore,
            String grade,
            String gestureSummariesJson,
            String penaltyBreakdownJson,
            String timelineJson,
            List<String> recommendations,
            String renderedVideoUrl) {
    }
}
