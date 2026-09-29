package com.sajitar.backend.adapter.in.web;

import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.webmvc.autoconfigure.error.BasicErrorController;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver;
import org.springframework.boot.webmvc.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("${spring.web.error.path:${error.path:/error}}")
class InternalServerErrorController extends BasicErrorController {

    InternalServerErrorController(
            final ErrorAttributes errorAttributes,
            final WebProperties webProperties,
            final ObjectProvider<ErrorViewResolver> errorViewResolvers) {
        super(errorAttributes, webProperties.getError(), errorViewResolvers.orderedStream().toList());
    }

    @Override
    @RequestMapping
    public ResponseEntity<Map<String, Object>> error(final HttpServletRequest request) {
        if (getStatus(request) == HttpStatus.INTERNAL_SERVER_ERROR) {
            logInternalServerError(request);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return super.error(request);
    }

    @Override
    @RequestMapping(produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView errorHtml(final HttpServletRequest request, final HttpServletResponse response) {
        if (getStatus(request) == HttpStatus.INTERNAL_SERVER_ERROR) {
            logInternalServerError(request);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            return null;
        }
        return super.errorHtml(request, response);
    }

    @Override
    protected boolean isIncludeStackTrace(final HttpServletRequest request, final MediaType produces) {
        return false;
    }

    private void logInternalServerError(final HttpServletRequest request) {
        if (request.getAttribute(RequestDispatcher.ERROR_EXCEPTION) instanceof Throwable exception) {
            log.error("Internal server error", exception);
            return;
        }
        log.error("Internal server error");
    }

}
