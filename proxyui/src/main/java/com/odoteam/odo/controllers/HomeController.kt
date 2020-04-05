package com.odoteam.odo.controllers

import com.groupon.odo.proxylib.Constants
import com.groupon.odo.proxylib.HistoryService
import com.groupon.odo.proxylib.SQLService
import com.groupon.odo.proxylib.Utils
import com.odoteam.odo.containers.HttpProxyContainer
import org.apache.catalina.connector.Connector
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.servlet.server.ServletWebServerFactory
import org.springframework.context.annotation.*
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.filter.HiddenHttpMethodFilter
import java.io.File
import java.time.Duration
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import javax.servlet.Filter

@Controller
@ComponentScan(
        basePackages = ["com.odoteam.odo.controllers"],
        excludeFilters = [ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = [HttpProxyContainer::class])]
)
@EnableAutoConfiguration(exclude = [ServletWebServerFactoryAutoConfiguration::class])
@PropertySources(value = [PropertySource("classpath:application.properties")])
class HomeController {

    private val logger = LoggerFactory.getLogger(HomeController::class.java)

    @PostConstruct
    fun init() {
        try {
            SQLService.getInstance().updateSchema("/migrations")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @PreDestroy
    fun destroy() {
        logger.info("Running destroy")
        try {
            SQLService.getInstance().stopServer()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @GetMapping(value = ["/"])
    fun home(): String {
        return "redirect:profiles"
    }

    @Bean
    fun servletContainer(): ServletWebServerFactory {
        val factory = TomcatServletWebServerFactory()
        val apiPort = Utils.getSystemPort(Constants.SYS_API_PORT)
        factory.port = apiPort
        factory.session.timeout = Duration.ofMinutes(10)
        factory.contextPath = "/odo"
        factory.setBaseDirectory(File("./tmp"))
        val cs = mutableListOf<TomcatConnectorCustomizer?>()
        cs.add(tomcatConnectorCustomizers())
        factory.tomcatConnectorCustomizers = cs
        if (Utils.getEnvironmentOptionValue(Constants.SYS_LOGGING_DISABLED) != null) {
            HistoryService.getInstance().disableHistory()
        }
        return factory
    }

    @Bean
    fun tomcatConnectorCustomizers(): TomcatConnectorCustomizer {
        return TomcatConnectorCustomizer { connector: Connector ->
            connector.maxPostSize = -1
            connector.setProperty("relaxedQueryChars", "<>[\\]^`{|}")
            connector.setProperty("relaxedPathChars", "<>[\\]^`{|}")
        }
    }

    // TODO check if used
    @Bean
    fun hiddenHttpMethodFilter(): Filter {
        return HiddenHttpMethodFilter()
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(HomeController::class.java, *args)
    SpringApplication.run(HttpProxyContainer::class.java, *args)
}