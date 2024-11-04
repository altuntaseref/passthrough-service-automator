package ${packageName}.config;

import com.yildizholding.ocean.common.logging.InboundRequestLoggingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class InboundRequestLoggingFilterConfig {
@Bean
@SuppressWarnings("unchecked")
public FilterRegistrationBean inboundRequestLoggingFilter() {
FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
filterRegistrationBean.setFilter(new InboundRequestLoggingFilter());
filterRegistrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
return filterRegistrationBean;
}
}
