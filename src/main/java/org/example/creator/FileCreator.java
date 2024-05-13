package org.example.creator;

import org.example.enums.CsvNamesEnum;
import org.example.models.JavaClass;
import org.example.models.ReleaseCommits;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class FileCreator {
    private FileCreator(){}
    private static String enumToString(@NotNull CsvNamesEnum csvEnum, int csvIndex) {

        return switch (csvEnum) {
            case TRAINING -> "_TR" + csvIndex;
            case TESTING -> "_TE" + csvIndex;
            case BUGGY -> "_buggy_classes";
            case CURRENT -> "_current_classes";
        };
    }

    public static void writeOnCsv(String projName, List<ReleaseCommits> rcList, CsvNamesEnum csvEnum, int csvIndex) throws IOException {

        String csvNameStr = enumToString(csvEnum, csvIndex);
        String pathname = projName + csvNameStr + ".csv";

        File file = new File(pathname);

        if(file.exists() && !file.delete()) {
            throw new IOException(); //Exception: file deletion impossible
        }
        try(FileWriter fw = new FileWriter(pathname)) {
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
                    "IS_BUGGY");

            int count;

            for(ReleaseCommits rc: rcList) {
                count = 0;
                for(JavaClass javaClass: rc.getJavaClasses()) {
                    fw.write("\n");

                    fw.write(rc.getRelease().getIndex() + ",");
                    fw.write(javaClass.getName() + ",");
                    fw.write(javaClass.getMetrics().getSize() + ",");
                    fw.write(javaClass.getMetrics().getLocAdded() + ",");
                    fw.write(javaClass.getMetrics().getMaxLocAdded() + ",");
                    fw.write(javaClass.getMetrics().getAvgLocAdded() + ",");
                    fw.write(javaClass.getMetrics().getLocDeleted() + ",");
                    fw.write(javaClass.getMetrics().getMaxLocDeleted() + ",");
                    fw.write(javaClass.getMetrics().getAvgLocDeleted() + ",");
                    fw.write(javaClass.getMetrics().getChurn() + ",");
                    fw.write(javaClass.getMetrics().getMaxChurn() + ",");
                    fw.write(javaClass.getMetrics().getAvgChurn() + ",");
                    fw.write(javaClass.getMetrics().getFixedDefects() + ",");
                    fw.write(javaClass.getCommits().size() + ",");
                    fw.write(javaClass.getMetrics().isBuggy());
                    if(javaClass.getMetrics().isBuggyness()) {
                        count++;
                    }
                }

                rc.setBuggyClasses(count);
            }
        }
    }
}