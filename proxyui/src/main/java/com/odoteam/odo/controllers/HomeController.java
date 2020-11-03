package com.odoteam.odo.controllers;

import com.groupon.odo.proxylib.Constants;
import com.groupon.odo.proxylib.HistoryService;
import com.groupon.odo.proxylib.SQLService;
import com.groupon.odo.proxylib.Utils;
import com.odoteam.odo.containers.HttpProxyContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.filter.HiddenHttpMethodFilter;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.Filter;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Controller
@ComponentScan(
        basePackages = {"com.odoteam.odo.controllers"},
        excludeFilters = {@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = HttpProxyContainer.class)}
)
@EnableAutoConfiguration(exclude = {ServletWebServerFactoryAutoConfiguration.class})
@PropertySources(value = {@PropertySource("classpath:application.properties")})
public class HomeController {
    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    @PostConstruct
    public void init() {
        try {
            SQLService.getInstance().updateSchema("/migrations");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void destroy() {
        logger.info("Running destroy");
        try {
            SQLService.getInstance().stopServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GetMapping(value = "/")
    public String home() {
        return "redirect:profiles";
    }

    @Bean
    public ServletWebServerFactory servletContainer() throws Exception {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();

        int apiPort = Utils.getSystemPort(Constants.SYS_API_PORT);
        factory.setPort(apiPort);
        factory.getSession().setTimeout(Duration.ofMinutes(10));
        factory.setContextPath("/odo");
        factory.setBaseDirectory(new File("./tmp"));
        List<TomcatConnectorCustomizer> cs = new ArrayList();
        cs.add(tomcatConnectorCustomizers());
        factory.setTomcatConnectorCustomizers(cs);

        if (Utils.getEnvironmentOptionValue(Constants.SYS_LOGGING_DISABLED) != null) {
            HistoryService.getInstance().disableHistory();
        }
        return factory;
    }

    @Bean
    public TomcatConnectorCustomizer tomcatConnectorCustomizers() {
        return connector -> {
            connector.setMaxPostSize(-1);
            connector.setProperty("relaxedQueryChars", "<>[\\]^`{|}");
            connector.setProperty("relaxedPathChars", "<>[\\]^`{|}");
        };
    }

    // TODO check if used
    @Bean
    public Filter hiddenHttpMethodFilter() {
        return new HiddenHttpMethodFilter();
    }

    public static void main(String[] args) {
        SpringApplication.run(HomeController.class, args);
        SpringApplication.run(HttpProxyContainer.class, args);
    }
}
