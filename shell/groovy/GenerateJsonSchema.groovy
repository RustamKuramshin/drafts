#!/usr/bin/env groovy

@Grab('com.fasterxml.jackson.core:jackson-databind:2.18.3')
@Grab('com.fasterxml.jackson.module:jackson-module-jsonSchema:2.18.3')

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator
import java.util.regex.Pattern

if (args.length != 1) {
    println "Использование: groovy GenerateJsonSchema.groovy <Путь_к_class_файлу>"
    System.exit(1)
}

def classFile = new File(args[0])
if (!classFile.exists() || !classFile.name.endsWith('.class')) {
    println "❌ Указанный файл не существует или не является .class файлом: ${classFile}"
    System.exit(1)
}

// Определяем classpath и имя класса
def classPathRoot = findClasspathRoot(classFile)
def fullyQualifiedClassName = toFullyQualifiedClassName(classFile, classPathRoot)

def loader = new URLClassLoader([classPathRoot.toURI().toURL()] as URL[], this.class.classLoader)

try {
    def clazz = Class.forName(fullyQualifiedClassName, true, loader)
    def mapper = new ObjectMapper()
    def schemaGen = new JsonSchemaGenerator(mapper)
    def schema = schemaGen.generateSchema(clazz)

    def schemaJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema)
    def output = new File("./${clazz.simpleName}.schema.json")
    output.text = schemaJson
    println "✅ JSON Schema сохранена: ${output.absolutePath}"
} catch (Exception e) {
    println "❌ Ошибка: ${e.message}"
    e.printStackTrace()
}

// === Утилиты ===

File findClasspathRoot(File classFile) {
    def parts = classFile.getAbsolutePath().split(Pattern.quote(File.separator))
    def index = parts.findLastIndexOf { it == 'main' || it == 'classes' }
    if (index == -1 || index < 2) {
        throw new RuntimeException("Невозможно определить корень classpath из пути: ${classFile}")
    }
    return new File(parts.take(index + 1).join(File.separator))
}

String toFullyQualifiedClassName(File classFile, File root) {
    def relativePath = classFile.absolutePath - root.absolutePath
    return relativePath
            .replace(File.separator, ".")
            .replaceFirst("^\\.+", "")
            .replaceAll(/\.class$/, "")
}
