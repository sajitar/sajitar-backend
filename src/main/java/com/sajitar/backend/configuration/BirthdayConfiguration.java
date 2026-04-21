package com.sajitar.backend.configuration;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.sajitar.backend.domain.validation.profile.Birthday.BirthdayValidator;

@Configuration
class BirthdayConfiguration extends BirthdayValidator {

	protected BirthdayConfiguration(@Value("${sajitar.domain.validation.profile.birthday.min-age-years}") final Integer minAgeYears) {
		BirthdayValidator.minAgeYears = ObjectUtils.getIfNull(minAgeYears, 18);
	}

}
