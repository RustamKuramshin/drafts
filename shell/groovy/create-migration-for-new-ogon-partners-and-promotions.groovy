@GrabConfig(systemClassLoader=true)

@Grab(group='org.postgresql', module='postgresql', version='42.2.5')
import groovy.sql.Sql

import groovy.text.GStringTemplateEngine

import groovy.transform.Field

@Grab('org.apache.commons:commons-csv:1.10.0')
import org.apache.commons.csv.CSVRecord

@Grab('org.slf4j:slf4j-api:1.7.30')
@Grab('org.slf4j:slf4j-log4j12:1.7.30')
@Grab('log4j:log4j:1.2.17')
import org.apache.log4j.BasicConfigurator
import org.apache.log4j.Logger

import java.nio.file.Files
import java.nio.file.Paths

@Field
def commonUtils = (new GroovyShell()).parse(new File('CommonUtils.groovy'))

BasicConfigurator.configure()
@Field
def log = Logger.getLogger("migration-generator")

def getDataForPartnersBlock(Sql sql, List<CSVRecord> newPartnerDates) {
    def list = []

    def query = '''
        SELECT DISTINCT part.name, part.short_name, part.picture_id, part.partner_view_id
        FROM promotion promo
        LEFT JOIN partner part ON promo.partner_id = part.id
        WHERE part.name = ? AND date_trunc('day', promo.start_date) = date_trunc('day', ?::timestamp);
    '''

    newPartnerDates.each { pd ->
        sql.eachRow(query, [pd.partner_name, pd.date]) { row ->
            list << [
                name         : escapeXml(row['name']),
                shortName    : escapeXml(row['short_name']),
                pictureId    : row['picture_id'],
                partnerViewId: row['partner_view_id']
            ]
        }
    }

    if (list.size() != newPartnerDates.size()) {
        log.error("Не всех партнёров удалось найти в БД")
        throw new RuntimeException("Не всех партнёров удалось найти в БД")
    }

    return list
}

def getDataForPromotionsBlock(Sql sql, List<CSVRecord> newPartnerDates) {
    def list = []

    def query = '''
        SELECT promo.id, promo.ogon_id, promo.partner_id, promo.status, promo.creation_date, 
               promo.modification_date, promo.properties_mask, promo.state, promo.name, promo.description, 
               promo.preview_text, promo.benefit_text, promo.full_rules, promo.annotation, promo.expiration_date, 
               promo.announcement_description, promo.ogon_promotion_view_id, promo.start_date,
               part.short_name, part.picture_id
        FROM promotion promo
        LEFT JOIN partner part ON promo.partner_id = part.id
        WHERE part.name = ? AND date_trunc('day', promo.start_date) = date_trunc('day', ?::timestamp)
        ORDER BY promo.start_date ASC;
    '''

    newPartnerDates.each { pd ->
        sql.eachRow(query, [pd.partner_name, pd.date]) { row ->
            list << [
                id                     : UUID.randomUUID(),
                ogonId                 : row['ogon_id'],
                partnerShortName       : escapeXml(row['short_name']),
                partnerPictureId       : row['picture_id'],
                status                 : row['status'],
                creationDate           : row['creation_date'],
                modificationDate       : row['modification_date'],
                propertiesMask         : row['properties_mask'],
                state                  : row['state'],
                name                   : escapeXml(row['name']),
                description            : row['description'],
                previewText            : row['preview_text'],
                benefitText            : row['benefit_text'],
                fullRules              : row['full_rules'],
                annotation             : row['annotation'],
                startDate              : "${row['start_date']} +00:00",
                expirationDate         : "${row['expiration_date']} +00:00",
                announcementDescription: row['announcement_description'],
                ogonPromotionViewId    : row['ogon_promotion_view_id']
            ]
        }
    }

    return list
}

def getDataForPromotionsGroupOfferTypesBlock(Sql sql, List<CSVRecord> newPartnerDates, List<CSVRecord> newPromotions) {
    def list = []

    def promotionsData = getDataForPromotionsBlock(sql, newPartnerDates)

    promotionsData.each { pd ->

        commonUtils.findCSVRecordByFieldValue(
            newPromotions,
            'promotion_view_id',
            pd['ogonPromotionViewId']
        ).each { csvRec ->
            list << [
                ogonPromotionViewId: pd['ogonPromotionViewId'],
                groupOfferType     : csvRec.get('group_offer_type')
            ]
        }
    }

    return list
}

def getDataForCategoryPromotionsBlock(Sql sql, List<CSVRecord> newPartnerDates, List<CSVRecord> newPromotions) {
    def list = []

    def query = '''
        SELECT c.id
        FROM category c
        WHERE c.ogon_id = CAST(? AS bigint);
    '''

    def promotionsData = getDataForPromotionsBlock(sql, newPartnerDates)

    promotionsData.each { pd ->
        commonUtils.findCSVRecordByFieldValue(
            newPromotions,
            'promotion_view_id',
            pd['ogonPromotionViewId']
        ).each { csvRec ->
            String ogonCategoryId = csvRec['category_id']

            sql.eachRow(query, [ogonCategoryId]) { row ->
                list << [
                    ogonPromotionViewId: pd['ogonPromotionViewId'],
                    categoryId: row['id']
                ]
            }
        }
    }

    return list
}

def main() {
    def requiredFiles = ['partner-connection-dates.csv', 'new-promotions.csv', 'migration-template.gtpl', 'settings.yml']

    if (!commonUtils.checkRequiredFiles(requiredFiles)) {
        log.error("Не все необходимые файлы присутствуют. Завершение работы.")
        return
    }

    commonUtils.checkEnvironmentVariables(["DB_USERNAME", "DB_PASSWORD"])

    log.info("Все необходимые файлы присутствуют.")

    def settings = commonUtils.readSettings('settings.yml')

    log.info("Настройки успешно загружены: ${settings}")

    def newPartnerDates = commonUtils.parseCsv('partner-connection-dates.csv')
    assert newPartnerDates.size() > 0

    log.info("Файл с новыми партнёрами успешно загружен ${newPartnerDates}")

    def newPromotions = commonUtils.parseCsv('new-promotions.csv')
    assert newPromotions.size() > 0

    log.info("Файл с новыми промоушенами успешно загружен ${newPromotions}")

    Sql sql = commonUtils.connectToDatabase(settings['database'])

    log.info("Соединение с базой данных установленно")

    log.info("Формирование коллекции данных для заполнения блока шаблона <Вставка в таблицу partner>")
    def partnersData = getDataForPartnersBlock(sql, newPartnerDates)

    log.info("Формирование коллекции данных для заполнения блока шаблона <Вставка в таблицу promotion>")
    def promotionsData = getDataForPromotionsBlock(sql, newPartnerDates)

    log.info("Формирование коллекции данных для заполнения блока шаблона <Вставка в таблицу promotion_group_offer_type>")
    def promotionsGroupOfferTypesData = getDataForPromotionsGroupOfferTypesBlock(sql, newPartnerDates, newPromotions)

    log.info("Формирование коллекции данных для заполнения блока шаблона <Вставка в таблицу category_promotion>")
    def categoryPromotionsData = getDataForCategoryPromotionsBlock(sql, newPartnerDates, newPromotions)

    // Чтение шаблона из файла
    def templatePath = settings['template']['fileName']
    def templateContent = new String(Files.readAllBytes(Paths.get(templatePath)))

    // Рендеринг XML-файла миграции
    def engine = new GStringTemplateEngine()
    def template = engine.createTemplate(templateContent).make(
        [
            partners                : partnersData,
            promotions              : promotionsData,
            promotionGroupOfferTypes: promotionsGroupOfferTypesData,
            categoryPromotions      : categoryPromotionsData
        ]
    )
    def xmlOutput = template.toString()

    log.info(xmlOutput)

    String outputFileName = settings['template']['outputFileName']

    // Удаление файла миграции, созданного ранее
    new File(outputFileName).delete()

    // Создание нового файла миграции
    def xmlFile = new File(outputFileName)
    xmlFile.text = xmlOutput

    sql.close()
}

main()