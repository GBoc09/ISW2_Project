package org.example.creator;

import org.example.enums.FilenamesEnum;
import org.example.models.ClassifierEvaluation;
import org.example.models.JavaClass;
import org.example.models.ReleaseInfo;
import org.example.utils.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class FileCreator {
    private FileCreator(){}

    private static @NotNull File createANewFile(String projName, FilenamesEnum fileEnum, int fileIndex, String endPath) throws IOException {
        String enumFilename = FileUtils.enumToFilename(fileEnum, fileIndex);
        Path dirPath = Path.of("retrieved_data/", projName, FileUtils.enumToDirectoryName(fileEnum));

        Path pathname = Path.of(dirPath.toString(), projName + enumFilename + endPath);
        File dir = new File(dirPath.toUri());
        File file = new File(pathname.toUri());

        if (!dir.exists() && !file.mkdirs()) {
            throw new RuntimeException(); //Exception: dir creation impossible
        }

        if (file.exists() && !file.delete()) {
            throw new IOException(); //Exception: file deletion impossible
        }
        return file;
    }
    public static void writeOnCsv(String projName, List<ReleaseInfo> rcList, FilenamesEnum csvEnum, int csvIndex) throws IOException {

        File file = createANewFile(projName, csvEnum, csvIndex, ".csv");

        try (FileWriter fw = new FileWriter(file)) {
            fw.write("VERSION," +
                    "JAVA_CLASS," +
                    "SIZE," +
                    "LOC_ADDED," +
                    "MAX_LOC_ADDED," +
                    "AVG_LOC_ADDED," +
                    "LOC_DELETED," +
                    "MAX_LOC_DELETED," +
                    "AVG_LOC_DELETED," +
                    "CHURN," +
                    "MAX_CHURN," +
                    "AVG_CHURN," +
                    "FIXED_DEFECTS," +
                    "NUMBER_OF_COMMITS," +
                    "NUMBER_OF_AUTHORS," +
                    "IS_BUGGY\n");


            writeClassesDataOnFile(rcList, fw, false);
        }
    }
    public static void writeEvaluationDataOnCsv(String projName, List<ClassifierEvaluation> classifierEvaluationList) throws IOException {

        File file = createANewFile(projName, FilenamesEnum.EVALUATING, 0, ".csv");

        try(FileWriter fw = new FileWriter(file)) {

            fw.write("DATASET," +
                    "#TRAINING_RELEASES," +
                    "%TRAINING_INSTANCES," +
                    "CLASSIFIER," +
                    "FEATURE_SELECTION," +
                    "BALANCING," +
                    "COST_SENSITIVE," +
                    "PRECISION," +
                    "RECALL," +
                    "AUC," +
                    "KAPPA," +
                    "TRUE_POSITIVE," +
                    "FALSE_POSITIVE," +
                    "TRUE_NEGATIVE," +
                    "FALSE_NEGATIVE\n");

            for(ClassifierEvaluation classifierEvaluation: classifierEvaluationList) {

                fw.write(projName + ","); //DATASET
                fw.write(classifierEvaluation.getWalkForwardIterationIndex() + ","); //#TRAINING_RELEASES
                fw.write(classifierEvaluation.getTrainingPercent() + ","); //%TRAINING_INSTANCES
                fw.write(classifierEvaluation.getClassifier() + ","); //CLASSIFIER
                fw.write(classifierEvaluation.getFeatureSelection().toString() + ","); //FEATURE_SELECTION
                fw.write(classifierEvaluation.getSampling().toString() + ","); //BALANCING
                fw.write(classifierEvaluation.getCostSensitiveType().toString() + ","); //COST_SENSITIVE
                fw.write(classifierEvaluation.getPrecision() + ","); //PRECISION
                fw.write(classifierEvaluation.getRecall() + ","); //RECALL
                fw.write(classifierEvaluation.getAuc() + ","); //AUC
                fw.write(classifierEvaluation.getKappa() + ","); //KAPPA
                fw.write(classifierEvaluation.getTp() + ","); //TRUE_POSITIVE
                fw.write(classifierEvaluation.getFp() + ","); //FALSE_POSITIVE
                fw.write(classifierEvaluation.getTn() + ","); //TRUE_NEGATIVE
                fw.write(classifierEvaluation.getFn() + "\n"); //FALSE_NEGATIVE
            }
        }
    }


    private static void writeClassesDataOnFile(List<ReleaseInfo> riList, FileWriter fw, boolean isArff) throws IOException {
        int count;
        for(ReleaseInfo releaseInfo: riList) {
            count = 0;
            for(JavaClass javaClass: releaseInfo.getJavaClasses()) {
                if(!isArff) {
                    fw.write(releaseInfo.getRelease().getIndex() + ","); //VERSION
                    fw.write(javaClass.getName() + ","); //JAVA_CLASS
                }
                fw.write(javaClass.getMetrics().getSize() + ","); //SIZE
                fw.write(javaClass.getMetrics().getLocAdded() + ","); //LOC_ADDED
                fw.write(javaClass.getMetrics().getMaxLocAdded() + ","); //MAX_LOC_ADDED
                fw.write(javaClass.getMetrics().getAvgLocAdded() + ","); //AVG_LOC_ADDED
                fw.write(javaClass.getMetrics().getLocDeleted() + ","); //LOC_DELETED
                fw.write(javaClass.getMetrics().getMaxLocDeleted() + ","); //MAX_LOC_DELETED
                fw.write(javaClass.getMetrics().getAvgLocDeleted() + ","); //AVG_LOC_DELETED
                fw.write(javaClass.getMetrics().getChurn() + ","); //CHURN
                fw.write(javaClass.getMetrics().getMaxChurn() + ","); //MAX_CHURN
                fw.write(javaClass.getMetrics().getAvgChurn() + ","); //AVG_CHURN
                fw.write(javaClass.getMetrics().getFixedDefects() + ","); //FIXED_DEFECTS
                fw.write(javaClass.getCommits().size() + ","); //NUMBER_OF_COMMITS
                fw.write(javaClass.getMetrics().getnAuth() + ","); //NUMBER_OF_AUTHORS
                fw.write(javaClass.getMetrics().isBuggy()); //IS_BUGGY

                fw.write("\n");

                if(javaClass.getMetrics().isBuggyness()) {
                    count++;
                }
            }
            releaseInfo.setBuggyClasses(count);
        }
    }
    public static void writeOnArff(String projName, List<ReleaseInfo> riList, FilenamesEnum filenamesEnum, int fileIndex) throws IOException {

        File file = createANewFile(projName, filenamesEnum, fileIndex, ".arff");

        try(FileWriter fw = new FileWriter(file)) {

            fw.write("@relation " + file.getName() + "\n");
            fw.write("@attribute SIZE numeric\n");
            fw.write("@attribute LOC_ADDED numeric\n");
            fw.write("@attribute MAX_LOC_ADDED numeric\n");
            fw.write("@attribute AVG_LOC_ADDED numeric\n");
            fw.write("@attribute LOC_DELETED numeric\n");
            fw.write("@attribute MAX_LOC_DELETED numeric\n");
            fw.write("@attribute AVG_LOC_DELETED numeric\n");
            fw.write("@attribute CHURN numeric\n");
            fw.write("@attribute MAX_CHURN numeric\n");
            fw.write("@attribute AVG_CHURN numeric\n");
            fw.write("@attribute FIXED_DEFECTS numeric\n");
            fw.write("@attribute NUMBER_OF_COMMITS numeric\n");
            fw.write("@attribute NUMBER_OF_AUTHORS numeric\n");
            fw.write("@attribute IS_BUGGY {'True', 'False'}\n");
            fw.write("@data\n");

            writeClassesDataOnFile(riList, fw, true);



        }
    }


}