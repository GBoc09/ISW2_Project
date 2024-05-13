package org.example.retrievers;

import org.example.models.Version;
import org.example.utils.JSONUtils;
import org.example.utils.VersionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;


/** With this class we take the version */
public class VersionRetriever {
    public static final String RELEASE_DATE = "releaseDate";
    List<Version> projVersions;

    public List<Version> getProjVersions() {
        return projVersions;
    }

    public VersionRetriever(String projName) {
        /*Fills the arraylist with releases dates and orders them
        Ignores releases with missing dates */
        try {
            getVersions(projName);
            VersionUtils.printVersion(projVersions);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private void getVersions(String projName) throws IOException {
        String url = "https://issues.apache.org/jira/rest/api/2/project/" + projName;
        JSONObject json = JSONUtils.readJsonFromUrl(url);
        JSONArray versions = json.getJSONArray("versions");
        this.projVersions = createVersionArray(versions);
        sortRelease(this.projVersions);
        setIndex(this.projVersions);
    }
    private void setIndex(@NotNull List<Version> versions) {
        int i = 0;
        for(Version versionInfo : versions) {
            versionInfo.setIndex(i);
            i++;
        }
    }

    public List<Version> getAffectedVersions(@NotNull JSONArray versions) {
        String id;
        List<Version> affectedVersions = new ArrayList<>();
        for (int i = 0; i < versions.length(); i++ ) {
            if(versions.getJSONObject(i).has(RELEASE_DATE) && versions.getJSONObject(i).has("id")) {
                id = versions.getJSONObject(i).get("id").toString();
                Version v = searchVersion(id);
                if(v == null) throw new RuntimeException(); //TODO Create a new exception or ignore the case with v == null
                affectedVersions.add(v);
            }
        }
        return affectedVersions;
    }
    private @Nullable Version searchVersion(String id) {
        for(Version version: this.projVersions) {
            if(Objects.equals(version.getId(), id)) {
                return version;
            }
        }
        return null;
    }

    private @NotNull List<Version> createVersionArray(@NotNull JSONArray versions) {
        List<Version> versionList = new ArrayList<>();
        for (int i = 0; i < versions.length(); i++ ) {
            String name = "";
            String id = "";
            if(versions.getJSONObject(i).has(RELEASE_DATE)) {
                if (versions.getJSONObject(i).has("name"))
                    name = versions.getJSONObject(i).get("name").toString();
                if (versions.getJSONObject(i).has("id"))
                    id = versions.getJSONObject(i).get("id").toString();
                addRelease(versions.getJSONObject(i).get(RELEASE_DATE).toString(), name, id, versionList);
            }
        }
        return versionList;
    }
    private void sortRelease(@NotNull List<Version> releases) {
        releases.sort(Comparator.comparing(Version::getDate));
    }
    public void addRelease(String strDate, String name, String id, @NotNull List<Version> releases) {
        LocalDate date = LocalDate.parse(strDate);
        Version newRelease = new Version(id, name, date);
        releases.add(newRelease);
    }

}