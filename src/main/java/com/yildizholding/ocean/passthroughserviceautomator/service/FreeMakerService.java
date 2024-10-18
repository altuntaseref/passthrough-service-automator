package com.yildizholding.ocean.passthroughserviceautomator.service;

import com.yildizholding.ocean.passthroughserviceautomator.model.FreeMakeModel;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.StringWriter;

@Slf4j
@Service
public class FreeMakerService {

    public StringWriter createJavaClassesFromTemplates(FreeMakeModel model) {
        try {
            Configuration cfg = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
            Template template = cfg.getTemplate(model.getTemplateFilePath());
            FileWriter myWriter = new FileWriter(model.getFileName());
            StringWriter stringWriter = new StringWriter();

            template.process(model.getArgs(), stringWriter);

            myWriter.write(stringWriter.toString());
            myWriter.close();
            return stringWriter;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }

    }

}
