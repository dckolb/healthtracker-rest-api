package com.navigatingcancer.healthtracker.api.processor.model;

import com.google.common.collect.ImmutableMap;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
public class SymptomDetails implements Comparable<SymptomDetails> {

    private String severity; // Ex : Mild, Medium, Severe
    private Integer rawSeverity; // Ex : 4, 7, 9
    private String symptomType;
    private String comment;
    private List<String> detailList = new ArrayList<>();
    @Setter(AccessLevel.NONE)
    private String title;

    /*
      "symptomDetails": [
        {
          "title": "Severe pain (9)",
          "detailList":[
            "It interferes with my daily activities very much.\n",
            "It occurs almost constantly"
          ]
          "symptom": "pain",
          "severity": "severe",
          "comment": "joint pain",
        }
      ]
     */

    private static final Map<String, Pattern> SEVERITY_COMPARATOR
            = ImmutableMap.of(
            "very severe",  Pattern.compile(""),
            "severe",       Pattern.compile("^(very severe)$"),
            "moderate",     Pattern.compile("^(very severe|severe)$"),
            "mild",         Pattern.compile("^(very severe|severe|moderate)$")
            );

    @Override
    public int compareTo(SymptomDetails other) {
        if((this.severity == null || other.severity == null) || (this.severity.equals(other.severity)) ) {
            return ObjectUtils.compare(this.symptomType, other.symptomType);
        } else {
            Pattern p1 = SEVERITY_COMPARATOR.get(this.severity.toLowerCase());
            if(p1 == null)
                return -1;

            if(SEVERITY_COMPARATOR.get(other.severity.toLowerCase()) == null)
                return 1; // any valid severity > invalid severity

            Matcher matcher = p1.matcher(other.severity.toLowerCase());
            return matcher.matches() ? -1 : 1;
        }
    }

    public static SymptomDetails createSymptomDetailsWithNoSymptoms() {
        SymptomDetails symptomDetails = new SymptomDetails();
        symptomDetails.setSymptomType("");
        symptomDetails.setSeverity("none");
        symptomDetails.setRawSeverity(0);
        symptomDetails.title = "No side effects";
        return symptomDetails;
    }

    public void setSeverity(String newSeverity) {
        this.severity = newSeverity;
    }

    public String updateTitle(boolean followsCtcaeStandard) {
        String severitySuffix = "";
        if(!followsCtcaeStandard && symptomType.equalsIgnoreCase("pain")) {
            // For non MN and if SymptomType == 'pain', then title is "Very severe pain (9)"
            severitySuffix = String.format(" (%s)", rawSeverity.toString());
        }

        String descriptiveSeverity = severity;
        if(severity != null && severity.equalsIgnoreCase("None") && !symptomType.equalsIgnoreCase("activityFunction")) {
            descriptiveSeverity = "No";
        }

        String descriptiveSymptomType = SurveyDictionary.descriptiveSymptomType(symptomType);

        title = String.format("%s %s%s", descriptiveSeverity, descriptiveSymptomType, severitySuffix);

        return title;
    }
}
