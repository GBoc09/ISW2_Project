package org.example.utils;

import org.example.models.Version;
import org.example.retrievers.VersionRetriever;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;

public class VersionUtils {
    private VersionUtils() {}

//    public static @Nullable Version retrieveNextRelease(VersionRetriever versionRetriever, LocalDate date) {
//        for(Version version : versionRetriever.getProjVersions()) {
//            LocalDate releaseDate = version.getDate();
//            if(!releaseDate.isBefore(date)) {
//                return version;
//            }
//        }
//        return null;
//    }

}
