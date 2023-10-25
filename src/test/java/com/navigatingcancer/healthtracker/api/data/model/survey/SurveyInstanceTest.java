package com.navigatingcancer.healthtracker.api.data.model.survey;

import java.util.TreeMap;
import org.junit.Assert;
import org.junit.Test;

public class SurveyInstanceTest {

  @Test
  public void hashInstance_createsConsistentHashRegardlessOfParamOrder() {
    var params1 = new TreeMap<String, Object>();
    params1.put("foo", "bar");
    params1.put("biz", "baz");
    var instance1 = new SurveyInstance(1L, 2L, "3", params1);

    var params2 = new TreeMap<String, Object>();
    params2.put("biz", "baz");
    params2.put("foo", "bar");
    var instance2 = new SurveyInstance(1L, 2L, "3", params2);

    Assert.assertEquals(instance1, instance2);
    Assert.assertEquals(instance1.getHash(), instance2.getHash());

    var params3 = new TreeMap<String, Object>();
    params3.put("foo", "bar");
    params3.put("boz", "baz");
    var instance3 = new SurveyInstance(1L, 2L, "3", params3);

    Assert.assertNotEquals(instance1, instance3);
    Assert.assertNotEquals(instance1.getHash(), instance3.getHash());
  }

  @Test
  public void hashInstance_variesWhenValueIsDifferent() {
    var params1 = new TreeMap<String, Object>();
    params1.put("foo", "bar");
    var instance1 = new SurveyInstance(1L, 2L, "3", params1);

    var params2 = new TreeMap<String, Object>();
    params2.put("foo", "baz");
    var instance2 = new SurveyInstance(1L, 2L, "3", params2);

    Assert.assertNotEquals(instance1, instance2);
    Assert.assertNotEquals(instance1.getHash(), instance2.getHash());
  }
}
