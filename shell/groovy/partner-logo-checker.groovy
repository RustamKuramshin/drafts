@GrabConfig(systemClassLoader=true)

@Grab(group='org.postgresql', module='postgresql', version='42.2.5')
import groovy.sql.Sql

import groovy.transform.Field

@Grab('org.apache.commons:commons-csv:1.10.0')
import org.apache.commons.csv.CSVRecord

@Grab('org.slf4j:slf4j-api:1.7.30')
@Grab('org.slf4j:slf4j-log4j12:1.7.30')
@Grab('log4j:log4j:1.2.17')
import org.apache.log4j.BasicConfigurator
import org.apache.log4j.Logger

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

import java.nio.charset.StandardCharsets

@Field
def commonUtils = (new GroovyShell()).parse(new File('CommonUtils.groovy'))

BasicConfigurator.configure()
@Field
def log = Logger.getLogger("logo-checker")

def getPictureUrlById(String pictureId) {
    final String V1_OBJECT_DOWNLOAD = "v1/object/download/"
    final String OGON_IMAGES_STORAGE_URL = "http://backend-ecosystem-object-storage-server-http:8081/"
    final String OGON_IMAGES_CDN_URL = "https://cdn-frontend.gazprombonus.ru/unsafe/gravity_ce:q_80:dpr_2:format_webp:resize_fill_0_0_1/"

    String rawFileName = OGON_IMAGES_STORAGE_URL + V1_OBJECT_DOWNLOAD + pictureId

    String fileName = Base64.getEncoder().encodeToString(rawFileName.getBytes(StandardCharsets.UTF_8))

    return OGON_IMAGES_CDN_URL + fileName + ".webp"
}

def getPicture(String pictureUrl) {
    HttpClient client = HttpClient.newHttpClient()
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(pictureUrl))
        .build()

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString())

    int statusCode = response.statusCode()

    return statusCode
}

def getPartnersFromDb(Sql sql) {
    def list = []

    def query = '''
        SELECT p.id, p.name, p.short_name, p.picture_id
        FROM partner p;
    '''

    sql.eachRow(query) { row ->
        list << [
            id           : row['id'],
            name         : row['name'],
            shortName    : row['short_name'],
            pictureId    : row['picture_id'],
        ]
    }

    return list
}

def main() {

    boolean csvMode = false

    boolean dbMode = true

    if (csvMode) {

        def requiredFiles = ['partner.csv']

        if (!commonUtils.checkRequiredFiles(requiredFiles)) {
            log.error("Не все необходимые файлы присутствуют. Завершение работы.")
            return
        }

        log.info("Все необходимые файлы присутствуют.")

        log.info("Чтение списка партнёров из csv.")

        List<CSVRecord> partnersFromCsv = commonUtils.parseCsv('partner.csv')

        log.info("Проверка возможности получения логотипов партнёров")

        partnersFromCsv.each {
            if (it.picture_id != null && it.picture_id != "") {
                String pictureUrl = getPictureUrlById(it.picture_id)
                int statusCode = getPicture(pictureUrl)
                if (statusCode != 200) {
                    println("${it.id} - ${it.name} - ${it.picture_id} - $pictureUrl - $statusCode")
                }
            }
        }

        log.info("Проверка логотипов завершена")
    }

    if (dbMode) {

        def requiredFiles = ['settings.yml']

        if (!commonUtils.checkRequiredFiles(requiredFiles)) {
            log.error("Не все необходимые файлы присутствуют. Завершение работы.")
            return
        }

        commonUtils.checkEnvironmentVariables(["DB_USERNAME", "DB_PASSWORD"])

        log.info("Все необходимые файлы присутствуют.")

        def settings = commonUtils.readSettings('settings.yml')

        log.info("Настройки успешно загружены: ${settings}")

        Sql sql = commonUtils.connectToDatabase(settings['database'])

        log.info("Соединение с базой данных установленно")

        log.info("Получение списка партнеров из БД")
        def partnersFromDb = getPartnersFromDb(sql)

        log.info("Проверка возможности получения логотипов партнёров")

        partnersFromDb.each {
            if (it.pictureId != null && it.pictureId != "") {

                String pictureUrl = getPictureUrlById(it.picture_Id)
                int statusCode = getPicture(pictureUrl)

                println("${it.id} - ${it.name} - ${it.pictureId} - $pictureUrl - $statusCode")
            }
        }

        log.info("Проверка логотипов завершена")
    }
}

main()