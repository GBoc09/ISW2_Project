package org.example.utils;

import org.example.models.ReleaseInfo;
import org.example.models.Version;
import org.example.retrievers.VersionRetriever;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.util.List;

public class VersionUtils {
    private VersionUtils() {}
    public static void printVersion(List<Version> versionList) {
        for(Version version: versionList) {
            System.out.println("Version: " +
                    + version.getIndex() + ", "
                    + version.getName() + ", "
                    + version.getDate());
        }
    }

    public static @Nullable Version retrieveNextRelease(VersionRetriever versionRetriever, LocalDate date) {
        for(Version version : versionRetriever.getProjVersions()) {
            LocalDate releaseDate = version.getDate();
            if(!releaseDate.isBefore(date)) {
                return version;
            }
        }
        return null;
    }
    public static @Nullable ReleaseInfo retrieveCommitRelease(VersionRetriever versionRetriever, LocalDate date, @NotNull List<ReleaseInfo> rcList) {
        Version version = retrieveNextRelease(versionRetriever, date);

        for(ReleaseInfo rc: rcList) {
            if(rc.getRelease() == version) return rc;
        }

        return null;
    }
}
