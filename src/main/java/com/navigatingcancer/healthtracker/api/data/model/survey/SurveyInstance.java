package com.navigatingcancer.healthtracker.api.data.model.survey;

import com.navigatingcancer.healthtracker.api.data.model.AbstractDocument;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import lombok.Data;
import org.apache.commons.codec.binary.Base64;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "survey_instances")
public class SurveyInstance extends AbstractDocument {
  private Long patientId;
  private Long clinicId;
  private String surveyId;
  private SortedMap<String, Object> surveyParameters;

  @Indexed(unique = true)
  private String hash;

  public SurveyInstance(
      Long clinicId, Long patientId, String surveyId, Map<String, Object> params) {
    this.patientId = patientId;
    this.clinicId = clinicId;
    this.surveyId = surveyId;
    this.surveyParameters = new TreeMap<>(params);
    this.hash = this.hashString();
  }

  public SurveyInstance() {}

  private byte[] hash() {
    String paramValues = surveyParameters.entrySet().toString();
    String hashString = String.format("%s:%s:%s:%s", patientId, clinicId, surveyId, paramValues);
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");

      return md.digest(hashString.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(
          String.format("Unable to hash surveyInstance with id: %s", this.getId()), e);
    }
  }

  public String hashString() {
    return Base64.encodeBase64String(this.hash());
  }

  @Override
  public int hashCode() {
    return ByteBuffer.wrap(this.hash()).getInt();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    final SurveyInstance other = (SurveyInstance) obj;

    return this.hashCode() == other.hashCode();
  }
}
