package org.example.creator;

import org.example.enums.CsvNamesEnum;
import org.example.models.JavaClass;
import org.example.models.ReleaseCommits;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class FileCreator {
    private static String enumToString(CsvNamesEnum csvEnum, int csvIndex) {

        return switch (csvEnum) {
            case TRAINING -> "_TR" + csvIndex;
            case TESTING -> "_TE" + csvIndex;
            case BUGGY -> "_buggy_classes";
            case CURRENT -> "_current_classes";
        };

    }

    public static void writeOnCsv(String projName, List<ReleaseCommits> rcList, CsvNamesEnum csvEnum, int csvIndex) throws IOException {

        String csvNameStr = enumToString(csvEnum, csvIndex);

        try(FileWriter fw = new FileWriter(projName + csvNameStr + ".csv")) {

            fw.write("VERSION_INDEX,JAVA_CLASS,SIZE,IS_BUGGY");

            int count;

            for(ReleaseCommits rc: rcList) {
                count = 0;
                for(JavaClass javaClass: rc.getJavaClasses()) {
                    fw.write("\n");

                    fw.write(rc.getRelease().getIndex() + ",");
                    fw.write(javaClass.getName() + ",");
                    fw.write(javaClass.getMetrics().getSize() + ",");
                    fw.write(javaClass.getMetrics().isBuggy());
                    /*cell1.setCellValue(javaClass.getRelease().getId());
                    cell2.setCellValue(javaClass.getSize());
                    cell3.setCellValue(javaClass.getNr());
                    cell4.setCellValue(javaClass.getnAuth());
                    cell5.setCellValue(javaClass.getLocAdded());
                    cell6.setCellValue(javaClass.getMaxLocAdded());
                    cell7.setCellValue(javaClass.getAvgLocAdded());
                    cell8.setCellValue(javaClass.getChurn());
                    cell9.setCellValue(javaClass.getMaxChurn());
                    cell10.setCellValue(javaClass.getAvgChurn());*/

                    if(javaClass.getMetrics().isBuggyness()) {
                        count++;
                    }
                }

                System.out.println("Numero di classi buggy for " + rc.getRelease().getName() + ": " + count);
            }
        }
    }
}
