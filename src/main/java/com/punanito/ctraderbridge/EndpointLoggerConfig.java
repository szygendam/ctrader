package com.punanito.ctraderbridge;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Configuration
public class EndpointLoggerConfig {

    @Bean
    public CommandLineRunner logEndpoints(ApplicationContext ctx) {
        return args -> {

            RequestMappingHandlerMapping mapping =
                    ctx.getBean(RequestMappingHandlerMapping.class);

            System.out.println("\n===== LIST OF ALL ENDPOINTS =====\n");

            mapping.getHandlerMethods().forEach((info, method) -> {

                String className = method.getBeanType().getSimpleName();
                String methodName = method.getMethod().getName();

                // SPRING 2.6+ / 3.x
                if (info.getPathPatternsCondition() != null) {
                    info.getPathPatternsCondition().getPatterns()
                            .forEach(pattern -> info.getMethodsCondition().getMethods()
                                    .forEach(httpMethod ->
                                            System.out.printf("%-6s %-40s %s#%s%n",
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
                                            System.out.printf("%-6s %-40s %s#%s%n",
                                                    httpMethod,
                                                    url,
                                                    className,
                                                    methodName
                                            )
                                    )
                            );
                }

            });

            System.out.println("\n=================================\n");
        };
    }
}
