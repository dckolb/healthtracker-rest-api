package com.navigatingcancer.healthtracker.api.data.model;

/* NOTE 1 : The names of these categories NEED TO match the Drool rules in reources/rules */
/* NOTE 2 : The order of these categories also defines the priority */
public enum HealthTrackerStatusCategory {
  TRIAGE,
  ACTION_NEEDED,
  WATCH_CAREFULLY,
  NO_ACTION_NEEDED,
  PENDING,
  COMPLETED,
  MN_RULES;

  public static String categoryNiceName(HealthTrackerStatusCategory cat) {
    String res;
    if (cat == null) {
      res = "Active";
    } else {
      switch (cat) {
        case ACTION_NEEDED:
          res = "Action Needed";
          break;
        case NO_ACTION_NEEDED:
          res = "No Action Needed";
          break;
        case WATCH_CAREFULLY:
          res = "Watch Carefully";
          break;
        case COMPLETED:
          res = "Completed";
          break;
        case PENDING:
          res = "Pending";
          break;
        case TRIAGE:
          res = "In Triage";
          break;
        default:
          res = "Active";
          break;
      }
    }
    return res;
  }
}
