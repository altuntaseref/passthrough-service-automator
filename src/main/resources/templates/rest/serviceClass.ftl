package ${packageName}.service;
import ${packageName}.config.${systemName}ServiceConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.Map;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ${className} {

private final  ${systemName}ServiceConfig serviceConfig;
private final RestTemplate restTemplate;

}
