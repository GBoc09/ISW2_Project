package org.example.models;

import org.example.enums.CostSensitiveEnum;
import org.example.enums.FeatureSelectionEnum;
import org.example.enums.SamplingEnum;

public class ClassifierEvaluation {
    private String projName;
    private int walkForwardIterationIndex;
    private double trainingPercent;
    private String classifier;
    private FeatureSelectionEnum featureSelection;
    private SamplingEnum sampling;
    private CostSensitiveEnum costSensitive;
    private double precision;
    private double recall;
    private double auc;
    private double kappa;
    private double tp;
    private double fp;
    private double tn;
    private double fn;

    public ClassifierEvaluation(String projName, int index, String classifier, FeatureSelectionEnum featureSelection, SamplingEnum sampling, CostSensitiveEnum costSensitive) {
        this.projName = projName;
        this.walkForwardIterationIndex = index;
        this.classifier = classifier;
        this.featureSelection = featureSelection;
        this.sampling = sampling;
        this.costSensitive = costSensitive;

        this.trainingPercent = 0.0;
        this.precision = 0;
        this.recall = 0;
        this.auc = 0;
        this.kappa = 0;
        this.tp = 0;
        this.fp = 0;
        this.tn = 0;
        this.fn = 0;

    }


    public String getProjName() {
        return projName;
    }

    public void setProjName(String projName) {
        this.projName = projName;
    }

    public int getWalkForwardIterationIndex() {
        return walkForwardIterationIndex;
    }

    public void setWalkForwardIterationIndex(int walkForwardIterationIndex) {
        this.walkForwardIterationIndex = walkForwardIterationIndex;
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public FeatureSelectionEnum getFeatureSelection() {
        return featureSelection;
    }

    public void setFeatureSelection(FeatureSelectionEnum featureSelection) {
        this.featureSelection = featureSelection;
    }

    public SamplingEnum getSampling() {
        return sampling;
    }

    public void setSampling(SamplingEnum sampling) {
        this.sampling = sampling;
    }


    public CostSensitiveEnum getCostSensitiveType() {
        return costSensitive;
    }


    public void setCostSensitive(CostSensitiveEnum costSensitive) {
        this.costSensitive = costSensitive;
    }


    public double getPrecision() {
        return precision;
    }

    public void setPrecision(double precision) {
        this.precision = precision;
    }

    public double getRecall() {
        return recall;
    }

    public void setRecall(double recall) {
        this.recall = recall;
    }

    public double getAuc() {
        return auc;
    }

    public void setAuc(double auc) {
        this.auc = auc;
    }

    public double getKappa() {
        return kappa;
    }

    public void setKappa(double kappa) {
        this.kappa = kappa;
    }

    public double getTrainingPercent() {
        return trainingPercent;
    }

    public void setTrainingPercent(double trainingPercent) {
        this.trainingPercent = trainingPercent;
    }

    public double getTp() {
        return tp;
    }

    public void setTp(double tp) {
        this.tp = tp;
    }


    public double getFp() {
        return fp;
    }

    public void setFp(double fp) {
        this.fp = fp;
    }


    public double getTn() {
        return tn;
    }


    public void setTn(double tn) {
        this.tn = tn;
    }


    public double getFn() {
        return fn;
    }


    public void setFn(double fn) {
        this.fn = fn;
    }
}