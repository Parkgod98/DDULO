package com.apitest.ddulo.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Configuration
public class SwaggerConfig {
	
	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
				.info(new Info().title("뚫어")
						.description("""
	                            <p>지하철 탑승 시 병목현상을 해결하는 서비스 <b><뚫어></b>의 API 문서 입니다.</p>
	                            <h4>Team : 뚫어뻥</h4>
	                            <ul>
		                            <li><strong>이름</strong> : <a href="#" target="_blank">Github</a></li>
	                            </ul>
	                            """)
	                    .version("v0.0.1")
	                    .contact(new Contact()
	                            .name("주소")
	                            .url("#"))
						.license(new License().name("Apache 2.0").url("http://springdoc.org")));
	}
}
