package org.example.retrievers;

import org.example.models.Version;
import org.example.utils.JSONUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** With this class we take the version */
public class VersionRetriever {
    private  List<Version> projVersions;
    private static final String URL = "https://issue.apache.org/jira/rest/api/2/project/";
    private static final String VERSIONS = "versions";
    private static final String RELEASE_DATE = "releaseDate";
    private static final String ID = "id";
    private static final String NAME = "name";

    public List<Version> getProjVersions() {
        return projVersions;
    }
    public VersionRetriever(String projectName){
        //Populate the ArrayList with releases dates and rearranges them
        //Ignores releases with missing dates
        try{
            getVersions(projectName);
        }catch (IOException | URISyntaxException e){
            throw new RuntimeException(e);
        }
    }

    private void getVersions(String projName) throws IOException, URISyntaxException {

        String url = "https://issues.apache.org/jira/rest/api/2/project/" + projName;
        JSONObject json = JSONUtils.readJsonFromUrl(url);
        JSONArray versions = json.getJSONArray("versions");

        this.projVersions = createVersionArray(versions);

        setIndex(this.projVersions);
    }

    private @NotNull ArrayList<Version> createVersionArray(JSONArray jsonArrayVersion) {

        ArrayList<Version> versionInfoArrayList = new ArrayList<>();
        for(int i = 0; i < jsonArrayVersion.length(); i++){
            String name = "";
            String id = "";
            if(jsonArrayVersion.getJSONObject(1).has(RELEASE_DATE)){

                if(jsonArrayVersion.getJSONObject(1).has(NAME))
                    name = jsonArrayVersion.getJSONObject(1).get(NAME).toString();
                if(jsonArrayVersion.getJSONObject(1).has(ID))
                    id = jsonArrayVersion.getJSONObject(1).get(ID).toString();
                addRelease(jsonArrayVersion.getJSONObject(1).get(RELEASE_DATE).toString(), name, id, versionInfoArrayList);

            }
        }

        return versionInfoArrayList;
    }
    private void addRelease(String date, String name, String id, ArrayList<Version> versionInfoArrayList) {

        LocalDate localDate = LocalDate.parse(date);
        Version newReleaseInfo = new Version(id, name, localDate);
        versionInfoArrayList.add(newReleaseInfo);

    }

    /** Get VERSION info from issues */
    public ArrayList<Version> getAffectedVersions(@NotNull JSONArray versions) {
        String id;
        ArrayList<Version> affectedVersions = new ArrayList<>();
        for (int i = 0; i < versions.length(); i++ ) {
            if(versions.getJSONObject(i).has(RELEASE_DATE) && versions.getJSONObject(i).has("id")) {
                id = versions.getJSONObject(i).get("id").toString();
                Version v = searchVersion(id);
                if(v == null) continue;

                affectedVersions.add(v);
            }
        }
        return affectedVersions;
    }

    private void setIndex(@NotNull List<Version> versions) {
        int i = 0;
        for(Version version : versions) {
            version.setIndex(i);
            i++;
        }
    }

    /** Research of the version */
    private @Nullable Version searchVersion(String id) {

        for(Version versionInfo: this.projVersions){
            if(Objects.equals(versionInfo.getId(), id)){
                return versionInfo;
            }
        }
        return null;
    }
    public void setProjectVersions(ArrayList<Version> projVersions) {
        this.projVersions = projVersions;
    }
}
