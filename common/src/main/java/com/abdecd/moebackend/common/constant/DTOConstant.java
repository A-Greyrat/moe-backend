package com.abdecd.moebackend.common.constant;

public interface DTOConstant {
    int PERSON_NAME_LENGTH_MAX = 40;
    int PERSON_NAME_LENGTH_MIN = 1;
    int PASSWORD_LENGTH_MAX = 344;
    int STRING_LENGTH_MAX = 200;
    int COMMENT_LENGTH_MAX = 4000;
    int FEEDBACK_LENGTH_MAX = 10000;
    int CAPTCHA_LENGTH = 4;
    int EMAIL_VERIFY_CODE_LENGTH = 6;
    String TAGS_REGEXP = "^[^;]+(?:;[^;]+)*$";
    String PERSON_NAME_REGEX = "^[^@ ]+$";
}
