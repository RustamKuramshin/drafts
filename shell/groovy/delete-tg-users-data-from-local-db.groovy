@GrabConfig(systemClassLoader=true)

@Grab(group='org.postgresql', module='postgresql', version='42.2.5')

import groovy.sql.Sql

println "Удаление данных telegram-пользователей из базы данных axiomconfbot"

void deleteTgUsersData(Sql sql) {
    def tables = [
            'time_capsule',
            'telegram_user_quiz_progress',
            'telegram_user'
    ]

    tables.each { table ->
        println "Очистка таблицы: ${table}"
        sql.execute("DELETE FROM " + table)
    }
}

def dbUrl = "jdbc:postgresql://localhost:25432/axiomconfbot"
def dbUser = 'pgadmin'
def dbPassword = 'pgadmin'

println "Подключение к базе данных axiomconfbot"

def sql = Sql.newInstance(dbUrl, dbUser, dbPassword, 'org.postgresql.Driver')
try {
    deleteTgUsersData(sql)
    println "Данные пользователей успешно отчищены."
} catch (Exception e) {
    println "Ошибка работы с базой данных axiomconfbot: ${e.message}"
} finally {
    sql.close()
}