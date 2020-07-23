package com.aabdulaziz.converter.json2java;

import java.util.Set;

public class ClassGenerator {

	public static String buildConstantsClass(String packageName, String className, Set<String> fieldSet) {
		String packageLiteral = "%PACKAGE%";
		String classNameLiteral = "%CLASSNAME%";
		String constantsLiteral = "%CONSTANTS%";

		String classTemplate = "package " + packageLiteral + "; \n\n" + "public final class " + classNameLiteral + " { "
				+ "\n" + constantsLiteral + "}" + "\n";

		classTemplate = classTemplate.replace(packageLiteral, packageName);
		classTemplate = classTemplate.replace(classNameLiteral, className);
		classTemplate = classTemplate.replace(constantsLiteral, buildConstants(fieldSet));

		return classTemplate;
	}

	private static String buildConstants(Set<String> fieldSet) {
		String constantLiteral = "%CONSTANT%";
		String constantsTemplate = "\tpublic static final String " + constantLiteral + " = \"" + constantLiteral
				+ "\";\n";
		StringBuilder stringBuilder = new StringBuilder();

		for (String field : fieldSet) {
			stringBuilder.append(constantsTemplate.replace(constantLiteral, field));
		}

		return stringBuilder.toString();
	}

}