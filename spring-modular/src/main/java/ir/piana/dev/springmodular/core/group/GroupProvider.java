package ir.piana.dev.springmodular.core.group;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.List;

public interface GroupProvider {
    List<GroupModel> getGroups();
    String getGroupsJsonString() throws RuntimeException;
    String getGroupsYamlString() throws RuntimeException;
}
