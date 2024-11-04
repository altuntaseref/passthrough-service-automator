package ${packageName}.controller;

import ${packageName}.service.${serviceClassName};
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ${className} {

private final ${serviceClassName} ${serviceVarName};

}
