package org.example.retrievers;

import org.example.enums.*;
import org.example.utils.AcumeUtils;
import org.example.models.ClassifierEvaluation;
import org.jetbrains.annotations.NotNull;
import org.example.utils.FileUtils;
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
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SMOTE;
import weka.filters.supervised.instance.SpreadSubsample;

import java.nio.file.Path;
import java.util.*;

import static org.example.creator.FileCreator.writeCsvForAcume;

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

        for(int i=1; i<=this.numIter; i++) {    //For each iteration

            computeIteration(projName, classifiersListMap, i);
        }

        List<ClassifierEvaluation> classifierEvaluationList = new ArrayList<>();

        for(Map.Entry<String, List<ClassifierEvaluation>> classifierName: classifiersListMap.entrySet()) {
            classifierEvaluationList.addAll(classifiersListMap.get(classifierName.getKey()));
        }

        return classifierEvaluationList;
    }

    /** This method iterates through all possible combinations of classifiers, feature selection modes, sampling modes and
     * cost-sensitive modes. For each combination, it calls the useClassifier method to evaluate the classifier and adds
     * the result to the classifiersListMap map. */
    private void computeIteration(String projName, Map<String, List<ClassifierEvaluation>> classifiersListMap, int i) throws Exception {
        for(ClassifierEnum classifierName: ClassifierEnum.values()) { //For each classifier
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
                AttributeSelection filter = getBestFirstAttributeSelection(training, "-D 1 -N 5");

                classifier = getFilteredClassifier(classifier, filter); /* This line of code is responsible for creating
                a new classifier that also incorporates a specific filter, which will be used for the training model. */
            }
            case BEST_FIRST_BACKWARD -> {
                //FEATURE SELECTION WITH BEST FIRST BACKWARD TECNIQUE
                AttributeSelection filter = getBestFirstAttributeSelection(training, "-D 0 -N 5");

                classifier = getFilteredClassifier(classifier, filter);
            }
            case NONE -> {
                //NO FEATURE SELECTION
            }
        }

        int[] nominalCounts = training.attributeStats(training.numAttributes() - 1).nominalCounts;
        int numberOfFalse = nominalCounts[1];
        int numberOfTrue = nominalCounts[0];

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
                double proportionOfMajorityValue = (double) numberOfFalse / (numberOfFalse + numberOfTrue);

                Resample resample = new Resample();
                resample.setInputFormat(training);
                String options = "-B 1.0 -S 1 -Z " + proportionOfMajorityValue * 2 * 100;
                resample.setOptions(Utils.splitOptions(options));

                classifier = getFilteredClassifier(classifier, resample);
            }
            case SMOTE -> {
                double percentSMOTE;    //Percentage of oversampling (e.g. a percentage of 100% will cause a doubling of the instances of the minority class)
                if(numberOfTrue==0 || numberOfTrue > numberOfFalse){
                    percentSMOTE = 0;
                }else{
                    percentSMOTE = (100.0*(numberOfFalse-numberOfTrue))/numberOfTrue;
                }
                SMOTE smote = new SMOTE();
                smote.setInputFormat(training);
                smote.setOptions(Utils.splitOptions("-C 1 -K 5 -P " + percentSMOTE + " -S 1"));
                if(numberOfTrue > 1)    //It is impossible assign 0 neighbors
                    smote.setNearestNeighbors(Math.min(numberOfTrue - 1, 5));   //In this way, we avoid the problem that SMOTE needs al least 5 true instances.
                else
                    break;

                classifier = getFilteredClassifier(classifier, smote);
            }
            case NONE -> {
                //NO SAMPLING
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

        assert classifier != null;
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

        String size = "SIZE";
        String isBuggy = "IS_BUGGY";
        List<AcumeUtils> acumeUtilsList = new ArrayList<>();

        int sizeIndex = testing.attribute(size).index();
        int isBuggyIndex = testing.attribute(isBuggy).index();

        int trueClassifierIndex = testing.classAttribute().indexOfValue("True");

        if(trueClassifierIndex != -1){
            for(int i = 0; i < testing.numInstances(); i++){
                int sizeValue = (int) testing.instance(i).value(sizeIndex);
                int valueIndex = (int) testing.instance(i).value(isBuggyIndex);
                String buggyness =  testing.attribute(isBuggyIndex).value(valueIndex);
                String buggy;
                buggy = writeBuggy(buggyness);
                double[] distribution = classifier.distributionForInstance(testing.instance(i));
                AcumeUtils acumeUtils = new AcumeUtils(i, sizeValue, distribution[trueClassifierIndex], buggy);
                acumeUtilsList.add(acumeUtils);
            }
        }
        writeCsvForAcume(projName, classifierName, featureSelection, sampling, costSensitive, index, acumeUtilsList);

        return simpleRandomForest;
    }
    private String writeBuggy(String buggyness) {

        if(buggyness.equals("True")){
            return  "YES";
        }else{
            return  "NO";
        }

    }


    private static AttributeSelection getBestFirstAttributeSelection(Instances training, String quotedOptionString) throws Exception {
        AttributeSelection filter = new AttributeSelection();
        BestFirst search = new BestFirst();
        search.setOptions(Utils.splitOptions(quotedOptionString));
        filter.setSearch(search);
        filter.setInputFormat(training);
        return filter;
    }

    @NotNull
    private static Classifier getFilteredClassifier(Classifier classifier, Filter filter) {
        FilteredClassifier filteredClassifier = new FilteredClassifier();

        filteredClassifier.setClassifier(classifier);
        filteredClassifier.setFilter(filter);

        return filteredClassifier;
    }

    private Classifier getClassifierByEnum(@NotNull ClassifierEnum classifierName){
        switch (classifierName) {
            case IBK -> {
                return new IBk();
            }
            case NAIVE_BAYES -> {
                return new NaiveBayes();
            }
            case RANDOM_FOREST -> {
                return new RandomForest();
            }
        }

        return null;
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
