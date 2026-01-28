package com.punanito.ctraderbridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Configuration
public class EndpointLoggerConfig {

    private static final Logger logger = LoggerFactory.getLogger(EndpointLoggerConfig.class);

    @Bean
    public CommandLineRunner logEndpoints(ApplicationContext ctx) {
        return args -> {

            RequestMappingHandlerMapping mapping =
                    ctx.getBean(RequestMappingHandlerMapping.class);

            logger.info("\n===== LIST OF ALL ENDPOINTS =====\n");

            mapping.getHandlerMethods().forEach((info, method) -> {

                String className = method.getBeanType().getSimpleName();
                String methodName = method.getMethod().getName();

                // SPRING 2.6+ / 3.x
                if (info.getPathPatternsCondition() != null) {
                    info.getPathPatternsCondition().getPatterns()
                            .forEach(pattern -> info.getMethodsCondition().getMethods()
                                    .forEach(httpMethod ->
                                            logger.info("{} {} {} {} ",
                                                    httpMethod,
                                                    pattern.getPatternString(),
                                                    className,
                                                    methodName
                                            )
                                    )
                            );
                }

                // SPRING < 2.6 fallback
                else if (info.getPatternsCondition() != null) {
                    info.getPatternsCondition().getPatterns()
                            .forEach(url -> info.getMethodsCondition().getMethods()
                                    .forEach(httpMethod ->
                                            logger.info("%-6s %-40s %s#%s%n",
                                                    httpMethod,
                                                    url,
                                                    className,
                                                    methodName
                                            )
                                    )
                            );
                }

            });

            logger.info("\n=================================\n");
        };
    }
}
