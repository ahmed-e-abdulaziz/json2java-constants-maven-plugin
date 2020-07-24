package com.aabdulaziz.converter.json2java;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.model.Resource;

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

	@Parameter(readonly = true, defaultValue = "${project.build.directory}")
	private String targetFolder;

	@Parameter(required = true, readonly = true)
	private String constantKey;

	@Parameter(required = true, readonly = true)
	private String packageName;

	@Parameter(required = true, readonly = true)
	private String outputPath;

	@Parameter(required = false, defaultValue = "false", readonly = true)
	private boolean ordered;

	@Parameter(defaultValue = "${project.resources}", required = true, readonly = true)
	private List<Resource> resources;

	private static final String JSON_EXTENSION = ".json";

	public void execute() throws MojoExecutionException {
		String generationPath = separatorsToSystem(targetFolder + File.separator + outputPath);
		new File(generationPath).mkdirs();
		Map<String, String> classAndJsonMap = jsonFileToClassAndJsonStringMap();
		classAndJsonMap.forEach((className, json) -> {
			String classString = createClassString(className, json);
			try {
				Path javaClassPath = Paths.get(generationPath + File.separator + className + ".java");
				Files.write(javaClassPath, classString.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE,
						StandardOpenOption.TRUNCATE_EXISTING);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	private Map<String, String> jsonFileToClassAndJsonStringMap() {
		Map<String, String> classAndJsonStringMap = new HashMap<>();
		try (Stream<Path> paths = Files.walk(Paths.get(resources.get(0).getDirectory()))) {
			paths.filter(p -> p.toString().endsWith(JSON_EXTENSION)).forEach(p -> {
				try {
					// The filename, used as the class name
					classAndJsonStringMap.put(p.getFileName().toString().replace(JSON_EXTENSION, ""),
							Files.readAllLines(p).stream().collect(Collectors.joining("\n"))); // JSON as a string
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		} catch (IOException e) {
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
		Set<String> fieldSet = ordered ? new TreeSet<>(String::compareTo) : new LinkedHashSet<>();
		rootNode.elements().forEachRemaining(field -> {
			fieldSet.add(field.get(constantKey).toString().replace("\"", ""));
		});
		return ClassGenerator.buildConstantsClass(packageName, className, fieldSet);
	}

	private String separatorsToSystem(String res) {
		if (res == null)
			return null;
		if (File.separatorChar == '\\') {
			// From Windows to Linux/Mac
			return res.replace('/', File.separatorChar);
		} else {
			// From Linux/Mac to Windows
			return res.replace('\\', File.separatorChar);
		}
	}
}
