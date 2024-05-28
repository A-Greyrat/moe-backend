package com.abdecd.moebackend.business.dao.entity;

import com.abdecd.moebackend.common.constant.MessageConstant;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Accessors(chain = true)
@Data
public class SearchVideoGroupEntity {
    private Long id;
    private String title;
    private String uploaderName;
    private String description;
    private List<String> tags;
    private Byte type;
    private String year;
    private Double weight;
    private List<String> suggestion;

    public void refreshSuggestion() {
        suggestion = new ArrayList<>();
        suggestion.add(title);
        if (!uploaderName.equals(MessageConstant.ADMIN)) suggestion.add(uploaderName);
//        suggestion.addAll(tags);
    }
}
