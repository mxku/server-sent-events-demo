package com.manishk.sse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.file.Files;
import org.springframework.integration.dsl.support.GenericHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
@RestController
public class SeDemoApplication {

	private final Map<String,SseEmitter> sses = new ConcurrentHashMap<>();

	@Bean
	IntegrationFlow inboundFlow(@Value("${input-dir:file://${user.home}/Desktop/in}") File in){
		return IntegrationFlows.from(Files.inboundAdapter(in).autoCreateDirectory(true),
				poller -> poller.poller(p -> p.fixedRate(1000L)))
				.transform(File.class,File::getAbsolutePath)
				.handle(String.class, (path, map) -> {
                    sses.forEach((k,sse) -> {
						try {
							sse.send(path);
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					});
                    return null;
                }).get();
	}

	@GetMapping("files/{name}")
	SseEmitter filesCreatedInFolder(@PathVariable String name){
		SseEmitter sseEmitter = new SseEmitter(60*1000L);
		sseEmitter.onCompletion(() -> sses.remove(name));
		sseEmitter.onTimeout(sseEmitter::complete);
		sses.put(name,sseEmitter);
		return sseEmitter;
	}

	public static void main(String[] args) {
		SpringApplication.run(SeDemoApplication.class, args);
	}
}
