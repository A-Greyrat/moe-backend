package com.abdecd.moebackend.business.exceptionhandler;

import com.abdecd.moebackend.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.*;

@RestController
public class MyErrorController extends BasicErrorController {

    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    public MyErrorController(ErrorAttributes errorAttributes,
                             ServerProperties serverProperties,
                             List<ErrorViewResolver> errorViewResolvers) {
        super(errorAttributes, serverProperties.getError(), errorViewResolvers);
    }


    @Override
    public ModelAndView errorHtml(HttpServletRequest request,
                                  HttpServletResponse response) {
        HttpStatus status = getStatus(request);
        Map<String, Object> model = Collections
                .unmodifiableMap(getErrorAttributes(request, getErrorAttributeOptions(request, MediaType.TEXT_HTML)));
        try {
            String errMsg = (String) model.get("error");
            if (errMsg.equals("Internal Server Error")) errMsg = "unknown error";
            objectMapper.writeValue(response.getOutputStream(), Result.error(status.value(), errMsg));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {
        Map<String, Object> body = getErrorAttributes(request, getErrorAttributeOptions(request, MediaType.ALL));

        var errMsg = body.get("error");
        if (errMsg.equals("Internal Server Error")) errMsg = "unknown error";
        Map<String, Object> resultBody = new HashMap<>(16);
        resultBody.put("code", body.get("status"));
        resultBody.put("msg", errMsg);
        resultBody.put("data", "");
        return new ResponseEntity<>(resultBody, Objects.requireNonNull(HttpStatus.resolve((Integer) body.get("status"))));
    }
}