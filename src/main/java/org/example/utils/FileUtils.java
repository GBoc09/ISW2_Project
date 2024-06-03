package org.example.utils;

import org.example.enums.FilenamesEnum;
import org.jetbrains.annotations.NotNull;

public class FileUtils {
    private FileUtils() {}

    public static String getArffFilename(FilenamesEnum fileEnum, String projName, int index) {
        return projName + enumToFilename(fileEnum, index) + ".arff";
    }

    public static String enumToFilename(@NotNull FilenamesEnum fileEnum, int index) {

        return switch (fileEnum) {
            case TRAINING -> "_TRAINING_" + index;
            case TESTING -> "_TESTING_" + index;
            case METRICS -> "_metrics";
            case ACUME ->  "_ACUME" + index;
            case EVALUATING -> "_classifiers_report_";
        };

    }

    public static String enumToDirectoryName(@NotNull FilenamesEnum fileEnum) {

        return switch (fileEnum) {
            case TRAINING -> "training";
            case TESTING -> "testing";
            case EVALUATING -> "evaluating";
            default -> "metrics";
        };

    }
}
