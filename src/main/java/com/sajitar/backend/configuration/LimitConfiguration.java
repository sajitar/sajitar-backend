package com.sajitar.backend.configuration;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.sajitar.backend.domain.validation.Limit.LimitValidator;

@Configuration
class LimitConfiguration extends LimitValidator {

	protected LimitConfiguration(@Value("${sajitar.domain.validation.limit.max}") final Integer max) {
		LimitValidator.max = ObjectUtils.getIfNull(max, 100);
	}

}
