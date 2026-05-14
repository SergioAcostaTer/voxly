package com.pigs.voxly.infrastructure.evaluation;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.pigs.voxly.application.shared.ports.PostureAnalysisService;
import com.pigs.voxly.sharedKernel.domain.results.ResultT;

@Service
@ConditionalOnProperty(name = "posture.analyzer.enabled", havingValue = "false", matchIfMissing = true)
public class MockPostureAnalysisService implements PostureAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(MockPostureAnalysisService.class);

    @Override
    public ResultT<PostureAnalysisResult> analyze(Path videoPath, Consumer<Double> progressCallback) {
        log.info("Mock posture analysis for: {}", videoPath.getFileName());
        progressCallback.accept(0.5);
        var result = new PostureAnalysisResult(
                85.0,
                100,
                "B",
                "[]",
                "{}",
                "[]",
                List.of("Stand tall and maintain open posture throughout your presentation."),
                null);
        progressCallback.accept(1.0);
        return ResultT.success(result);
    }
}
