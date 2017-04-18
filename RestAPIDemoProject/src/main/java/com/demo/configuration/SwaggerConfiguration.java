package com.demo.configuration;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfiguration {
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.demo.controller"))
                .paths(PathSelectors.any())
                .build()
                .apiInfo(getApiInfo());
    }

    private ApiInfo getApiInfo() {
        return new ApiInfo(
                "Swagger Api Documentation",
                "This is api documentation for prototype of big data class",
                "Beta",
                "https://github.com/aapeshave/big_data",
                contact(),
                "Github",
                "https://github.com/aapeshave/big_data"
        );
    }

    private Contact contact() {
        return new Contact(
                "Ajinkya Peshave",
                "https://github.com/aapeshave/big_data",
                "aapeshave@gmail.com"
        );
    }
}
