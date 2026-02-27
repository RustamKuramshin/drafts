import groovy.sql.Sql
import groovy.transform.Field

@Grab('org.apache.commons:commons-csv:1.10.0')
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord

@Grab('org.slf4j:slf4j-api:1.7.30')
@Grab('org.slf4j:slf4j-log4j12:1.7.30')
@Grab('log4j:log4j:1.2.17')
import org.apache.log4j.BasicConfigurator
import org.apache.log4j.Logger
import org.yaml.snakeyaml.Yaml

BasicConfigurator.configure()
@Field
def log = Logger.getLogger("common-utils")

// Проверка наличия файлов
def checkRequiredFiles(files) {
    files.each { String fileName ->
        if (!new File(fileName).exists()) {
            log.error("Отсутствует необходимый файл: $fileName")
            return false
        }
    }
    return true
}

// Проверка требуемых переменных окружения
def checkEnvironmentVariables(envs) {

    def systemEnvs = System.getenv()

    envs.each { String envName ->
        if ((systemEnvs[envName] == null) || systemEnvs[envName].empty) {
            throw new RuntimeException("Не задан env ${envName}")
        }
    }
}

// Функция для парсинга csv-файла
List<CSVRecord> parseCsv(String fileName) {
    def rows = null

    new File(fileName).withReader { r ->
        def records = CSVFormat.RFC4180.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setDelimiter(";")
            .build()
            .parse(r)
            .records

        rows = records
    }
    return rows
}

// Функция для чтения настроек из YAML-файла
def readSettings(String fileName) {
    def yaml = new Yaml()
    def inputStream = new FileInputStream(new File(fileName))
    def data = yaml.load(inputStream)
    return data
}

// Экранирование строк для xml
String escapeXml(String input) {
    if (input == null) {
        return null
    }

    return input.replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&apos;&apos;")
}

// Подключение к БД
def connectToDatabase(dbConfig) {
    def env = System.getenv()

    Sql.newInstance(
        dbConfig['url'],
        env["DB_USERNAME"],
        env["DB_PASSWORD"],
        dbConfig['driver']
    )
}

// Поиск строк в csv по значению в столбце
List<CSVRecord> findCSVRecordByFieldValue(List<CSVRecord> csvRecordList, String field, Object value) {

    def found = []

    for (rec in csvRecordList) {
        if (rec[field] == value.toString()) {
            found << rec
        }
    }

    return found
}