#!/usr/bin/env groovy

if (args.length != 1) {
    println "Usage: ./analyzePackage.groovy path/to/package"
    System.exit(1)
}

def packagePath = args[0]
def baseDir = new File(packagePath)

if (!baseDir.exists() || !baseDir.isDirectory()) {
    println "Invalid package path: $packagePath"
    System.exit(1)
}

// Рекурсивно собираем все .java файлы
def getJavaFilesRecursively = { File dir ->
    def javaFiles = []
    dir.eachFileRecurse { file ->
        if (file.name.endsWith(".java")) {
            javaFiles << file
        }
    }
    return javaFiles
}

def javaFiles = getJavaFilesRecursively(baseDir)

if (javaFiles.isEmpty()) {
    println "No Java files found in: $packagePath"
    System.exit(0)
}

Set<String> allClassNames = []
Map<String, File> classFileMap = [:]
Map<String, Boolean> hasFields = [:]

// Собираем имена классов и проверяем на наличие полей
javaFiles.each { file ->
    def text = file.text
    def matcher = text =~ /class\s+([A-Za-z0-9_]+)/
    if (matcher.find()) {
        def className = matcher.group(1)
        allClassNames << className
        classFileMap[className] = file

        def fieldMatcher = text =~ /(?:private|protected|public)?\s+(?!class|interface)[\w<>]+\s+\w+\s*(=|;)/
        hasFields[className] = fieldMatcher.find()
    }
}

Set<String> usedClasses = [] as Set

// Ищем упоминания классов в других файлах
javaFiles.each { file ->
    def text = file.text
    allClassNames.each { className ->
        if (text.contains(className) && !file.name.contains(className + ".java")) {
            usedClasses << className
        }
    }
}

println "\n🔍 Классы без полей (пустые):"
hasFields.each { className, hasField ->
    if (!hasField) {
        println " - $className (${classFileMap[className].path})"
    }
}

println "\n🧹 Неиспользуемые классы (в рамках пакета):"
(allClassNames - usedClasses).each { unusedClass ->
    println " - $unusedClass (${classFileMap[unusedClass].path})"
}
