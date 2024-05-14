package org.example.creator;

import org.example.enums.FilenamesEnum;
import org.example.models.JavaClass;
import org.example.models.ReleaseInfo;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class FileCreator {
    private FileCreator(){}

    private static String enumToDirectoryName(@NotNull FilenamesEnum fileEnum) {
        return switch (fileEnum) {
            case TRAINING -> "training/";
            case TESTING -> "testing/";
            default -> "metrics/";
        };
    }

    private static String enumToFilename(@NotNull FilenamesEnum fileEnum, int csvIndex) {

        return switch (fileEnum) {
            case TRAINING -> "_TR" + csvIndex;
            case TESTING -> "_TE" + csvIndex;
            case METRICS -> "_metrics";
            case CURRENT -> "_current_classes";
        };

    }

    private static @NotNull File createANewFile(String projName, FilenamesEnum fileEnum, int fileIndex, String endPath) throws IOException {
        String enumFilename = enumToFilename(fileEnum, fileIndex);
        String dirPath = "retrieved_data/" + projName + "/" + enumToDirectoryName(fileEnum);

        String pathname = dirPath + projName + enumFilename + endPath;
        File dir = new File(dirPath);
        File file = new File(pathname);

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
                    "IS_BUGGY,\n");


            writeDataOnFile(rcList, fw, false);
        }
    }
    private static void writeDataOnFile(List<ReleaseInfo> riList, FileWriter fw, boolean isArff) throws IOException {
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

            writeDataOnFile(riList, fw, true);

        }
    }


}