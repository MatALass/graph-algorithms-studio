package com.matalass.graphroutestudio.animation;

import com.matalass.graphroutestudio.algorithms.AlgorithmResult;
import com.matalass.graphroutestudio.ui.GraphCanvas;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.util.Duration;

import java.util.List;

public class AnimationController {
    private final GraphCanvas canvas;
    private AlgorithmResult result;
    private List<AlgorithmResult.AnimationStep> steps;

    private final IntegerProperty currentStep = new SimpleIntegerProperty(-1);
    private final IntegerProperty totalSteps = new SimpleIntegerProperty(0);
    private final BooleanProperty playing = new SimpleBooleanProperty(false);
    private final DoubleProperty speed = new SimpleDoubleProperty(1.0);

    private Timeline timeline;
    private Runnable onStepChange;

    public AnimationController(GraphCanvas canvas) {
        this.canvas = canvas;
    }

    public void load(AlgorithmResult result) {
        stop();
        this.result = result;
        this.steps = result.getSteps();
        totalSteps.set(steps.size());
        currentStep.set(-1);
        canvas.clearAnimationStep();
    }

    public void play() {
        if (result == null || steps.isEmpty() || playing.get()) {
            return;
        }
        if (currentStep.get() >= steps.size() - 1) {
            currentStep.set(-1);
        }
        playing.set(true);
        scheduleNext();
    }

    public void pause() {
        playing.set(false);
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }

    public void stop() {
        pause();
        currentStep.set(-1);
        canvas.clearAnimationStep();
    }

    public void stepForward() {
        pause();
        if (currentStep.get() < steps.size() - 1) {
            currentStep.set(currentStep.get() + 1);
            applyStep();
        }
    }

    public void stepBack() {
        pause();
        if (currentStep.get() > 0) {
            currentStep.set(currentStep.get() - 1);
            applyStep();
        } else if (currentStep.get() == 0) {
            currentStep.set(-1);
            canvas.clearAnimationStep();
        }
    }

    public void goToStep(int idx) {
        pause();
        currentStep.set(Math.max(-1, Math.min(steps.size() - 1, idx)));
        if (currentStep.get() >= 0) {
            applyStep();
        } else {
            canvas.clearAnimationStep();
        }
    }

    private void scheduleNext() {
        double ms = 1000.0 / Math.max(0.1, speed.get());
        timeline = new Timeline(new KeyFrame(Duration.millis(ms), event -> {
            if (!playing.get()) {
                return;
            }
            int next = currentStep.get() + 1;
            if (next >= steps.size()) {
                playing.set(false);
                return;
            }
            currentStep.set(next);
            applyStep();
            if (next < steps.size() - 1) {
                scheduleNext();
            } else {
                playing.set(false);
            }
        }));
        timeline.play();
    }

    private void applyStep() {
        if (steps == null || currentStep.get() < 0 || currentStep.get() >= steps.size()) {
            return;
        }
        AlgorithmResult.AnimationStep step = steps.get(currentStep.get());
        canvas.showAnimationStep(step);
        if (onStepChange != null) {
            onStepChange.run();
        }
    }

    public IntegerProperty currentStepProperty() { return currentStep; }
    public IntegerProperty totalStepsProperty() { return totalSteps; }
    public BooleanProperty playingProperty() { return playing; }
    public DoubleProperty speedProperty() { return speed; }
    public int getCurrentStep() { return currentStep.get(); }
    public int getTotalSteps() { return totalSteps.get(); }
    public boolean isPlaying() { return playing.get(); }

    public String getCurrentDescription() {
        int i = currentStep.get();
        if (steps == null || i < 0 || i >= steps.size()) {
            return "";
        }
        return steps.get(i).description();
    }

    public void setOnStepChange(Runnable onStepChange) {
        this.onStepChange = onStepChange;
    }
}
