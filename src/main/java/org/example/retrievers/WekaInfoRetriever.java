package org.example.retrievers;

import org.example.enums.*;
import org.example.models.ClassifierEvaluation;
import org.jetbrains.annotations.NotNull;
import org.example.utils.FileUtils;
import org.example.creator.FileCreator;
import weka.attributeSelection.*;
import weka.classifiers.Classifier;
import weka.classifiers.CostMatrix;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.CostSensitiveClassifier;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.supervised.instance.ClassBalancer;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SpreadSubsample;

import java.nio.file.Path;
import java.util.*;

public class WekaInfoRetriever {

    private final String projName;
    private final int numIter;

    public WekaInfoRetriever(String projName, int numIter) {
        this.projName = projName;
        this.numIter = numIter;
    }

    public List<ClassifierEvaluation> retrieveClassifiersEvaluation(String projName) throws Exception {

        Map<String, List<ClassifierEvaluation>> classifiersListMap = new HashMap<>();
        for(ClassifierEnum classifierName: ClassifierEnum.values()) {
            classifiersListMap.put(classifierName.name(), new ArrayList<>());
        }

        for(int i=1; i<=this.numIter; i++) {

            for(ClassifierEnum classifierName: ClassifierEnum.values()) {
                for (FeatureSelectionEnum featureSelectionEnum : FeatureSelectionEnum.values()) {   //Iterate on all feature selection mode
                    for (SamplingEnum samplingEnum : SamplingEnum.values()) {       //Iterate on all sampling mode
                        for (CostSensitiveEnum costSensitiveEnum : CostSensitiveEnum.values()) {    //Iterate on all cost sensitive mode
                            //Evaluate the classifier
                            classifiersListMap.get(classifierName.name())  //Get the list associated to the actual classifier
                                    .add(useClassifier(i, projName, classifierName, featureSelectionEnum, samplingEnum, costSensitiveEnum)); //Evaluate the classifier
                        }
                    }
                }
            }
        }

        List<ClassifierEvaluation> classifierEvaluationList = new ArrayList<>();

        for(String classifierName: classifiersListMap.keySet()) {
            classifierEvaluationList.addAll(classifiersListMap.get(classifierName));
        }

        return classifierEvaluationList;
    }

    private @NotNull ClassifierEvaluation useClassifier(int index, String projName, ClassifierEnum classifierName, @NotNull FeatureSelectionEnum featureSelection, @NotNull SamplingEnum sampling, CostSensitiveEnum costSensitive) throws Exception {

        Classifier classifier = getClassifierByEnum(classifierName);

        DataSource source1 = new DataSource(Path.of("retrieved_data", projName, "training", FileUtils.getArffFilename(FilenamesEnum.TRAINING, projName, index)).toString());
        DataSource source2 = new DataSource(Path.of("retrieved_data", projName, "testing",  FileUtils.getArffFilename(FilenamesEnum.TESTING, projName, index)).toString());
        Instances training = source1.getDataSet();
        Instances testing = source2.getDataSet();

        int numAttr = training.numAttributes();
        training.setClassIndex(numAttr - 1);
        testing.setClassIndex(numAttr - 1);

        Evaluation eval = new Evaluation(testing);

        //FEATURE SELECTION
        switch (featureSelection) {
            case BEST_FIRST_FORWARD -> {
                //FEATURE SELECTION WITH BEST FIRST FORWARD TECNIQUE
                AttributeSelection filter = getBestFirstAttributeSelection("-D 1 -N 5");

                classifier = getFilteredClassifier(classifier, filter);
            }
            case BEST_FIRST_BACKWARD -> {
                //FEATURE SELECTION WITH BEST FIRST BACKWARD TECNIQUE
                AttributeSelection filter = getBestFirstAttributeSelection("-D 0 -N 5");
                classifier = getFilteredClassifier(classifier, filter);
            }
        }

        //SAMPLING
        switch (sampling) {
            case UNDERSAMPLING -> {
                //VALIDATION WITH UNDERSAMPLING
                SpreadSubsample spreadSubsample = new SpreadSubsample();
                spreadSubsample.setInputFormat(training);
                spreadSubsample.setOptions(Utils.splitOptions("-M 1.0"));

                classifier = getFilteredClassifier(classifier, spreadSubsample);
            }
            case OVERSAMPLING -> {
                //VALIDATION WITH OVERSAMPLING
                int[] nominalCounts = training.attributeStats(training.numAttributes() - 1).nominalCounts;
                int numberOfFalse = nominalCounts[1];
                int numberOfTrue = nominalCounts[0];
                double proportionOfMajorityValue = (double) numberOfFalse / (numberOfFalse + numberOfTrue);

                Resample resample = new Resample();
                resample.setInputFormat(training);
                String options = "-B 1.0 -S 1 -Z " + proportionOfMajorityValue * 2 * 100;
                resample.setOptions(Utils.splitOptions(options));

                classifier = getFilteredClassifier(classifier, resample);
            }
            case SMOTE -> {
                /*TODO SMOTE smote = new SMOTE();
                smote.setInputFormat(training);
                smote.setClassValue("1");
                smote.setPercentage(percentSMOTE);*/


            }
        }

        //COST SENSITIVE
        if (Objects.requireNonNull(costSensitive) == CostSensitiveEnum.SENSITIVE_LEARNING) {
            //COST SENSITIVE WITH SENSITIVE LEARNING
            CostSensitiveClassifier costSensitiveClassifier = new CostSensitiveClassifier();
            costSensitiveClassifier.setMinimizeExpectedCost(true);
            CostMatrix costMatrix = getCostMatrix();
            costSensitiveClassifier.setCostMatrix(costMatrix);
            costSensitiveClassifier.setClassifier(classifier);

            classifier = costSensitiveClassifier;
        }

        classifier.buildClassifier(training);
        eval.evaluateModel(classifier, testing);

        ClassifierEvaluation simpleRandomForest = new ClassifierEvaluation(this.projName, index, classifierName.name(), featureSelection, sampling, costSensitive);
        simpleRandomForest.setTrainingPercent(100.0 * training.numInstances() / (training.numInstances() + testing.numInstances()));
        simpleRandomForest.setPrecision(eval.precision(0));
        simpleRandomForest.setRecall(eval.recall(0));
        simpleRandomForest.setAuc(eval.areaUnderROC(0));
        simpleRandomForest.setKappa(eval.kappa());
        simpleRandomForest.setTp(eval.numTruePositives(0));
        simpleRandomForest.setFp(eval.numFalsePositives(0));
        simpleRandomForest.setTn(eval.numTrueNegatives(0));
        simpleRandomForest.setFn(eval.numFalseNegatives(0));
        return simpleRandomForest;
    }

    private static AttributeSelection getBestFirstAttributeSelection(String quotedOptionString) throws Exception {
        AttributeSelection filter = new AttributeSelection();
        BestFirst search = new BestFirst();
        search.setOptions(Utils.splitOptions(quotedOptionString));
        filter.setSearch(search);
        return filter;
    }

    @NotNull
    private static Classifier getFilteredClassifier(Classifier classifier, Filter filter) {
        FilteredClassifier filteredClassifier = new FilteredClassifier();

        filteredClassifier.setClassifier(classifier);
        filteredClassifier.setFilter(filter);

        classifier = filteredClassifier;
        return classifier;
    }

    private Classifier getClassifierByEnum(@NotNull ClassifierEnum classifierName) throws Exception{
        switch (classifierName) {
            case IBK -> {
                IBk iBk = new IBk();
                iBk.setOptions(Utils.splitOptions("-K 1 -W 0 -A \"weka.core.neighboursearch.LinearNNSearch -A \\\"weka.core.EuclideanDistance -R first-last\\\"\""));
                return iBk;
            }
            case NAIVE_BAYES -> {
                return new NaiveBayes();
            }
            case RANDOM_FOREST -> {
                RandomForest randomForest = new RandomForest();
                randomForest.setOptions(Utils.splitOptions("-P 100 -I 100 -num-slots 1 -K 0 -M 1.0 -V 0.001 -S 1"));
                return randomForest;
            }
        }

        throw new RuntimeException();
    }

    private static CostMatrix getCostMatrix() {
        double weightFalsePositive = 1.0;
        double weightFalseNegative = 10.0;
        CostMatrix costMatrix = new CostMatrix(2);
        costMatrix.setCell(0, 0, 0.0);
        costMatrix.setCell(1, 0, weightFalsePositive);
        costMatrix.setCell(0, 1, weightFalseNegative);
        costMatrix.setCell(1, 1, 0.0);
        return costMatrix;
    }


}
