package com.aabdulaziz.converter.json2java;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author ahmedehab5010
 *
 */
@Mojo(name = "json2java", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class Json2JavaMojo extends AbstractMojo {

	@Parameter(required = true, readonly = true)
	private String resourcesPath;

	@Parameter(required = true, readonly = true)
	private String constantName;

	@Parameter(required = true, readonly = true)
	private String packageName;

	@Parameter(required = true, readonly = true)
	private String outputPath;

	private static final String JSON_EXTENSION = ".json";

	public void execute() throws MojoExecutionException {
		Map<String, String> classAndJsonMap = jsonFileToClassAndJsonStringMap();

		classAndJsonMap.forEach((className, json) -> {
			String classString = createClassString(className, json);
			try {
				Files.write(Paths.get(outputPath + "/" + className + ".java"),
						classString.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE,
						StandardOpenOption.TRUNCATE_EXISTING);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	private Map<String, String> jsonFileToClassAndJsonStringMap() {
		Map<String, String> classAndJsonStringMap = new HashMap<>();
		try (Stream<Path> paths = Files.walk(Paths.get(Json2JavaMojo.class.getResource(resourcesPath).toURI()))) {
			paths.filter(p -> p.toString().endsWith(JSON_EXTENSION)).forEach(p -> {
				try {
					// The filename, used as the class name
					classAndJsonStringMap.put(p.getFileName().toString().replace(JSON_EXTENSION, ""),
							Files.readAllLines(p).stream().collect(Collectors.joining("\n"))); // JSON as a string
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
		}
		return classAndJsonStringMap;
	}

	private String createClassString(String className, String json) {
		JsonFactory factory = new JsonFactory();

		ObjectMapper mapper = new ObjectMapper(factory);
		JsonNode rootNode = null;
		try {
			rootNode = mapper.readTree(json);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		Set<String> fieldSet = new HashSet<>();
		rootNode.elements().forEachRemaining(field -> {
			fieldSet.add(field.get("code").toString().replace("\"", ""));
		});
		return ClassGenerator.buildConstantsClass(packageName, className, fieldSet);
	}
}
