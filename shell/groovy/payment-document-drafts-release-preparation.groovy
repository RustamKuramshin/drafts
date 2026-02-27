import groovy.io.FileType
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.xml.XmlSlurper

import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.security.cert.X509Certificate

/**
 * Определения
 */

// Читаем переданные в скрипт параметры
tuzUsername              = System.getenv("TUZ_USERNAME")
tuzPassword              = System.getenv("TUZ_PASSWORD")
mainBranchSigma          = System.getenv("MODIFIED_BRANCH")
nexusUrl                 = System.getenv("NEXUS_URL")
jenkinsUrl               = System.getenv("JENKINS_URL")
draftRepoSshUrl          = System.getenv("DRAFT_REPO_SSH_URL")
thisIsJenkinsPipeline    = System.getenv("THIS_IS_JENKINS_PIPELINE").toBoolean()
releaseBranchAlpha       = System.getenv("RELEASE_BRANCH_ALPHA")
draftRepoGitUrl          = System.getenv("DRAFT_REPO_GIT_URL")
jiraIssue                = System.getenv("JIRA_ISSUE")
awaitBuildJenkinsJob     = System.getenv("AWAIT_BUILD_JENKINS_JOB").toBoolean()
jiraUrl                  = System.getenv("JIRA_URL")
modifiedBranchCommitHash = System.getenv("MODIFIED_BRANCH_COMMIT_HASH")

try {
    // Доступ к чтению свойств может быть заблокирован внутри Jenkins
    println("Версия JVM: ${System.getProperty("java.version")}")
    println("Версия Groovy: ${GroovySystem.version}")
} catch (Exception ex) {
    println(ex)
}

def getHttpClient() {
    TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                void checkClientTrusted(
                        X509Certificate[] certs, String authType) {
                }

                void checkServerTrusted(
                        X509Certificate[] certs, String authType) {
                }
            }
    }

    SSLContext sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, trustAllCerts, new SecureRandom())

    return HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .sslContext(sslContext)
            .build()

}

httpClient = getHttpClient()

def basicAuthHeader(String username, String password) {
    return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
}

def execShell(String command, Boolean ignoreError = false) {
    println("Ожидаем окончания выполнения команды: ${command}")
    def stdOut = new StringBuilder(), stdErr = new StringBuilder()
    def proc = command.trim().execute()
    proc.consumeProcessOutput(stdOut, stdErr)
    proc.waitForOrKill(1800000)
    if (
    (stdErr.toString() != "" && !ignoreError &&
            // Стоп-слова, по которым понимаем, что все хорошо
            !stdOut.toString().contains("BUILD SUCCESS") &&
            !stdErr.toString().contains("LF will be replaced by CRLF") &&
            !stdErr.toString().contains("Create pull request") &&
            !stdErr.toString().contains("HTTP/1.1 201 Created") &&
            !stdErr.toString().contains("[new branch]") &&
            !stdErr.toString().contains("[deleted]") &&
            !stdErr.toString().contains("Switched to a new branch") &&
            !stdErr.toString().contains("HTTPS connections may not be secure")
    ) ||
            // Стоп-слова, по которым понимаем, что есть ошибка
            stdOut.toString().contains("[ERROR]") ||
            stdOut.toString().contains("BUILD FAILURE")) {
        println("Выполнение команды закончилось ОШИБКОЙ:")
        println("std out> $stdOut")
        println("std error> $stdErr")
        throw new Exception("Command completed with error")
    } else {
        println("Выполнение команды закончилось УСПЕХОМ:")
        println("std out> $stdOut")
        println("std error> $stdErr")
    }

    return [stdOut.toString().trim(), stdErr.toString().trim()]
}

def getMaven() {
    if (System.properties['os.name'].toString().toLowerCase().contains('windows')) {
        return "mvn.cmd"
    } else {
        return "mvn"
    }
}

def buildUri(String url, Map<String, String> queryParams = null, Boolean encode = false) {

    String query = null

    if (encode) {
        query = encodeQuery(queryParams)
    } else {
        def queryList = queryParams.collect { "${it.key}=${it.value}" }
        query = queryList.join("&")
    }

    return queryParams ? "${url}?${query}" : url
}

def encodeQuery(Map<String, String> queryParams) {
    return queryParams.collect {"${it.key}=${URLEncoder.encode(it.value, StandardCharsets.UTF_8)}"}.join("&")
}

def urlWithAuth(String url, String username, String password) {
    return url.replace("://", "://${username}:${password}@")
}

def httpGet(String url, Map<String, String> queryParams = null, String... headers) {
    def uri = buildUri(url, queryParams)
    def httpRequest = HttpRequest.newBuilder()
            .uri(new URI(uri))
            .headers(headers)
            .GET()
            .build()

    println("Request:")
    println("GET ${uri}")
    printHeaders(httpRequest.headers())
    def resp = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
    println("Response:")
    println(resp.statusCode())
    printHeaders(resp.headers())
    def respBody = resp.body()
    if (respBody) {
        println(respBody)
    }
    return [resp.statusCode(), resp.headers(), respBody]
}

def httpPost(String url, Map<String, String> queryParams = null, String body = null, Boolean encode = false, String... headers) {
    def httpRequestBuilder = HttpRequest.newBuilder()
    def uri = buildUri(url, queryParams, encode)
    httpRequestBuilder.uri(new URI(uri))
    httpRequestBuilder.headers(headers)
    if (body) {
        httpRequestBuilder.POST(HttpRequest.BodyPublishers.ofString(body))
    } else {
        httpRequestBuilder.POST(HttpRequest.BodyPublishers.noBody())
    }

    def httpRequest = httpRequestBuilder.build()
    println("Request:")
    println("POST ${uri}")
    printHeaders(httpRequest.headers())
    if (body) {
        println(body)
    }
    def resp = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
    println("Response:")
    println(resp.statusCode())
    printHeaders(resp.headers())
    def respBody = (String) resp.body()
    if (respBody) {
        println(respBody)
    }
    return [resp.statusCode(), resp.headers(), respBody]
}

def printHeaders(HttpHeaders headers) {
    headers.map().each {
        println("${it.key}: ${it.value.join(";")}")
    }
}

def stringToJson(String str) {
    return new JsonSlurper().parseText(str)
}

def getJenkinsCrumb() {
    def respCrumb = httpGet(
            "${jenkinsUrl}/crumbIssuer/api/json",
            "Authorization", basicAuthHeader("${tuzUsername}", "${tuzPassword}")
    )

    def crumb = stringToJson(respCrumb.last().toString())

    return ["crumbRequestField": "${crumb.crumbRequestField}" as String, "crumb": "${crumb.crumb}" as String]
}

def buildJenkinsJob(String jobUrl, Map<String, String> jobParams, String paramsPlace = "query", Boolean await = false) {
    println("Запускаем job'у: ${jobUrl}")
    println("С параметрами: ${jobParams}")
    def jenkinsCrumb = getJenkinsCrumb()
    println("Получен crumb из jenkins: ${jenkinsCrumb}")

    ArrayList<Object> respBuild = null

    if (paramsPlace == "query") {
        respBuild = httpPost(
                "${jobUrl}/buildWithParameters",
                jobParams,
                null,
                true,
                "Authorization", basicAuthHeader("${tuzUsername}", "${tuzPassword}"),
                jenkinsCrumb.crumbRequestField, jenkinsCrumb.crumb
        )
    } else if (paramsPlace == "body"){

        def value = "${["parameter": jobParams.collect {["name":"${it.key}", "value":"${it.key}"]}]}"

        def form = ["json": "'${new JsonBuilder(value).toPrettyString()}'"]

        def f = form.collect {"${it.key}=${URLEncoder.encode(it.value, StandardCharsets.UTF_8)}"}.first()

        respBuild = httpPost(
                "${jobUrl}/build",
                null,
                f,
                "Content-Type","application/x-www-form-urlencoded",
                "Authorization", basicAuthHeader("${tuzUsername}", "${tuzPassword}"),
                jenkinsCrumb.crumbRequestField, jenkinsCrumb.crumb
        )

    } else {
        throw new Exception("Указано неизвестное место для установки параметров джобы")
    }

    if (await) {
        def respHeaders = respBuild[1] as HttpHeaders
        def location = respHeaders.allValues("location").first()
        String executableJobUrl = null
        while (true) {
            try {
                def respLocation = httpGet(
                        "${location}/api/json",
                        "Authorization", basicAuthHeader("${tuzUsername}", "${tuzPassword}"),
                        jenkinsCrumb.crumbRequestField, jenkinsCrumb.crumb
                )

                def executable = stringToJson(respLocation.last().toString()).executable
                if (executable) {
                    executableJobUrl = executable.url
                    break
                }

            } catch (Exception ex) {
                println(ex)
            }

            sleep(10000)
        }

        sleep(10000)

        String jobStatus = null

        while (true) {
            try {
                def respJobInfo = httpGet(
                        "${executableJobUrl}/api/json",
                        "Authorization", basicAuthHeader("${tuzUsername}", "${tuzPassword}"),
                        jenkinsCrumb.crumbRequestField, jenkinsCrumb.crumb
                )

                jobStatus = (String) stringToJson(respJobInfo.last().toString()).result
            } catch (Exception ex) {
                println(ex)
                sleep(10000)
                continue
            }

            if (jobStatus) {
                println("JOB ${jobUrl} STATUS: ${jobStatus}")

                if (jobStatus == "SUCCESS") {
                    break
                } else if (jobStatus == "FAILURE" || jobStatus == "ABORTED") {
                    throw new Exception("Jenkins build failed")
                } else if (jobStatus == "UNSTABLE") {
                    def respLocation2 = httpGet(
                            "${location}/api/json",
                            "Authorization", basicAuthHeader("${tuzUsername}", "${tuzPassword}"),
                            jenkinsCrumb.crumbRequestField, jenkinsCrumb.crumb
                    )
                    if (respLocation2.first().toString().toInteger() == 404) {
                        break
                    }
                }
            }

            sleep(10000)
        }
    }
}

def getArtifactLatestVersionFromNexus(String artifactId) {
    def respGetArtifactInfoFromNexusBody = httpGet(
            "${nexusUrl}/service/local/lucene/search",
            ["q": "${artifactId}", "collapseresults": "true"],
            "Authorization", basicAuthHeader("${tuzUsername}", "${tuzPassword}"),
            "Accept", "application/json,application/vnd.siesta-error-v1+json,application/vnd.siesta-validation-errors-v1+json"
    ).last().toString()

    def artifactInfoFromNexusJson = stringToJson(respGetArtifactInfoFromNexusBody)

    def latestArtifactVersionFromNexus = artifactInfoFromNexusJson.data[0].latestRelease

    if (!latestArtifactVersionFromNexus) {
        println("latestArtifactVersionFromNexus is null")
        println("Nexus response:")
        println(respGetArtifactInfoFromNexusBody)
    } else {
        println("latestArtifactVersionFromNexus from nexus: ${latestArtifactVersionFromNexus}")
    }

    return (String) latestArtifactVersionFromNexus
}

def replaceInAllPomFiles(String oldStr, String newStr) {
    println("Рекурсивный обход всех pom.xml")
    def rootDir = new File("../../")
    def counter = 0
    rootDir.eachFileRecurse(FileType.FILES) {
        if (it.name.contains("pom") && !it.path.contains("target")) {
            if (it.text.contains(oldStr)) {
                println("Найден pom.xml с вхождением подстроки ${oldStr}: ${it}")
                println("Обновляем pom.xml...")
                def newPomContent = it.text.replace(oldStr, newStr)
                it.text = newPomContent
                println("pom.xml обновлен")
                counter++
            }
        }
    }

    println("Обновлено ${counter} файлов pom.xml")
}

def getVersionFromPom(String pomPath) {
    def pom = new File(pomPath).text
    def project = new XmlSlurper().parseText(pom)
    def version = (String) project.version
    println("Версия в pom-файле ${pomPath} равна ${version}")
    return version
}

def getDependencyVersionFromPom(String artifactId, String pomPath) {
    String version = null
    def pom = new File(pomPath).text
    def project = new XmlSlurper().parseText(pom)
    for (dependency in project.dependencies.dependency) {
        if (((String) dependency.artifactId) == artifactId) {
            version = (String) dependency.version
            break
        }
    }

    println("В pom-файле ${pomPath} версия зависимости ${artifactId} равна ${version}")
    return version
}

def isExistGitBranchRemote(String branch) {
    def commandRes = execShell("git ls-remote --heads origin ${branch}")[0].trim()
    return !commandRes.isEmpty()
}

def isExistGitBranchLocal(String branch) {
    def commandRes = execShell("git branch --list ${branch}")[0].trim()
    return !commandRes.isEmpty()
}

def deleteLocalGitBranch(String branch) {
    execShell("git branch -D ${branch}", true)
}

def deleteRemoteGitBranch(String branch) {
    execShell("git push origin --delete ${branch}", true)
}

def mergeUpdateFromRemoteBranch(String branch) {
    execShell("git merge origin/${branch}")
}

def prepareRepo() {
    execShell("git config http.sslVerify false", true)

    if (!thisIsJenkinsPipeline) {
        gitCommitAllChanges("${jiraIssue}_Commit_all")
    }

    if (thisIsJenkinsPipeline) {
        // Устанавливаем креды для удаленной репы
        execShell("git remote remove origin")
        execShell("git remote add origin ${urlWithAuth(draftRepoGitUrl, tuzUsername, tuzPassword)}")
    }
}

def setPipelineBranch() {
    // Встанем на ветку чтобы выйти из detached head
    println("Становимся на main ветку: ${mainBranchSigma}")
    execShell("git pull origin ${mainBranchSigma}", true)
    execShell("git checkout ${mainBranchSigma}", true)

    if (!isExistGitBranchRemote("${releaseBranchAlpha}")) {
        println("Релизная ветка не найдена в удаленном репо, создаем ее")
        execShell("git checkout -b ${releaseBranchAlpha}")
        execShell("git push -u origin ${releaseBranchAlpha}")
    } else {
        println("Переключаемся на релизную ветку")
        execShell("git fetch origin", true)
        execShell("git checkout ${releaseBranchAlpha}")

        println("Получаем последний тэг")
        def lastTag = execShell("git describe --tags --abbrev=0", true)[0]

        println("Возвращаемся на main-ветку")
        execShell("git checkout ${mainBranchSigma}", true)

        println("Удаляем релизную ветку в локальном репо")
        execShell("git tag -d ${lastTag}", true)
        deleteLocalGitBranch(releaseBranchAlpha)
        println("Удаляем релизную ветку в удаленном репо")
        execShell("git push --delete origin ${lastTag}", true)
        deleteRemoteGitBranch(releaseBranchAlpha)

        println("Создаем релизную ветку ${releaseBranchAlpha}")
        execShell("git checkout -b ${releaseBranchAlpha}", true)
        println("Переносим тэг")
        execShell("git tag -a ${lastTag} -m release", true)
        println("Пушим релизную ветку в удаленный репо")
        execShell("git push -u origin ${releaseBranchAlpha}", true)
        println("Пушим тэг")
        execShell("git push origin ${lastTag}", true)
    }

    // Проверка последнего коммита в ветке
    if (!execShell("git branch --contains ${modifiedBranchCommitHash}", true)[0].contains("${releaseBranchAlpha}")) {
        throw new Exception("Релизная ветка ${releaseBranchAlpha} не была обновлена из ветки ${mainBranchSigma}")
    }
}

def setDataSpaceCoreVersion() {
    println("Устанавливаем версию dataspace-core")

    String currentVersion = getDependencyVersionFromPom("paydocdrafts-model-sdk", '../../payment-document-drafts-service/pom.xml')

    String versionForReplace = null

    try {
        println("Пытаемся прочитать версию из файла dataspace-core-build-version")
        versionForReplace = new File("../../dataspace-core-build-version").text.trim()
    } catch (Exception ex) {
        println(ex)
        println("Пытаемся получить версию из nexus")
        versionForReplace = getArtifactLatestVersionFromNexus("payment-document-drafts-dataspace-core")
    }

    if (versionForReplace.isEmpty()) {
        throw new Exception("НЕ удалось определить версию dataspace-core")
    }

    replaceInAllPomFiles(currentVersion, versionForReplace)
}

def setNewGeneralVersion() {
    println("Устанавливаем новую общую версию")
    println("Получаем последнюю выпущенную версию из Nexus")
    def latestGeneralVersionFromNexus = getArtifactLatestVersionFromNexus("payment-document-drafts-dto")
    println("Последняя выпущенная версия в Nexus: ${latestGeneralVersionFromNexus}")

    println("Читаем общую версию из pom.xml")
    def currentProjectVersion = getVersionFromPom('../../payment-document-common-libs/pom.xml')
    println("Общая версия из pom.xml: ${currentProjectVersion}")

    String newGeneralVersion = null

    if (latestGeneralVersionFromNexus) {
        def majorVersion = (latestGeneralVersionFromNexus =~ /(\d+)\.(\d+)\.(\d+)/)[0][1].toString().toInteger()
        def minorVersion = (latestGeneralVersionFromNexus =~ /(\d+)\.(\d+)\.(\d+)/)[0][2].toString().toInteger()
        def buildVersion = (latestGeneralVersionFromNexus =~ /(\d+)\.(\d+)\.(\d+)/)[0][3].toString().toInteger()
        println("Разбор выпущенной общей версии:")
        println(majorVersion)
        println(minorVersion)
        println(buildVersion)

        def newBuildVersion = buildVersion + 1

        newGeneralVersion = "${majorVersion}.${minorVersion}.${newBuildVersion}"

    } else {
        newGeneralVersion = "1.0.0"
    }

    println("Новая общая версия")
    println(newGeneralVersion)
    replaceInAllPomFiles(currentProjectVersion, newGeneralVersion)
    // Если были добавлены новые модули, то следует еще обновить общую версию
    replaceInAllPomFiles("GENERAL-SNAPSHOT", newGeneralVersion)
}

def deployArtifactsToNexus() {
    println("clean и deploy в Nexus")
    def MVN_CLEAN_DEPLOY = "${getMaven()} clean deploy -s ../../settings-alpha.xml -Dalpha.login=CAB-SA-DVO01455 -Dalpha.password=XoEvm!W7wR7Klq5Sfod5jnYk2 -Dmaven.test.skip=true -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validity.dates=true"

    execShell("${MVN_CLEAN_DEPLOY} -f ../../payment-document-drafts-dto")
    execShell("${MVN_CLEAN_DEPLOY} -f ../../payment-document-common-libs")
    execShell("${MVN_CLEAN_DEPLOY} -f ../../payment-document-api")
}

def commitAllReleaseIssues(String releaseIssue, String projectKey) {

    println("releaseIssue=${releaseIssue}, projectKey=${projectKey}")

    def releaseIssueListFile = new File("../../release-issue-list-file.txt")
    releaseIssueListFile.text = "${releaseIssue}\n"
    gitCommitAllChanges("${releaseIssue}_release_issue")

    def respIssueData = httpGet(
            "${jiraUrl}/rest/api/2/issue/${releaseIssue}",
            "Authorization", basicAuthHeader("${tuzUsername}", "${tuzPassword}")
    )

    def issueData = stringToJson(respIssueData.last().toString())

    issueData.fields.issuelinks.each {
        if (!it.inwardIssue) {
            println("Не удалось определить inwardIssue для: ${it}")
        } else {
            if ((it.inwardIssue.key as String).contains(projectKey)) {
                println("Коммитим issue: ${it.inwardIssue.key}")
                releaseIssueListFile.text += "${it.inwardIssue.key}\n"
                gitCommitAllChanges("${it.inwardIssue.key}_release_issue")
            }
        }
    }
}

def gitCommitAllChanges(String msg) {
    execShell("git add -A")
    execShell("git commit -m ${msg}")
}

def scriptMain() {

    Boolean isRelease = false

    if (mainBranchSigma ==~ /^release\/.+/) {
        isRelease = true
    }

    prepareRepo()

    setPipelineBranch()

    if (isRelease) {
        def releaseIssue = (mainBranchSigma =~ /^release\/(.+)/)[0][1].toString()
        def projectKey = (releaseIssue =~ /(.+)-.+/)[0][1].toString()
        commitAllReleaseIssues(releaseIssue, projectKey)
        jiraIssue = releaseIssue
    }

    setDataSpaceCoreVersion()

    setNewGeneralVersion()

    deployArtifactsToNexus()

    println("Коммитим и пушим")
    gitCommitAllChanges("${jiraIssue}_Commit_all")
    execShell("git push -u origin HEAD")

    def pauseMs = 30000
    println("Делаем паузу ${pauseMs / 1000} сек. перед запуском пайплайна")
    sleep(pauseMs)

    def currentBranch = execShell("git rev-parse --symbolic-full-name --abbrev-ref HEAD").first()

    try {
        buildJenkinsJob(
                "${jenkinsUrl}/job/PPRBTRIP/job/Synapse/job/SynapseBuilder",
                [
                        "jiraTicketKey"             : "${jiraIssue}",
                        "synapseGitUrlSources"      : "${draftRepoSshUrl}",
                        "synapseGitBranchSources"   : "${currentBranch}",
                        "synapsePrjList"            : "configs,payment-document-drafts-service,payment-document-entrypoint-api",
                        "toolJDKVersion"            : "openjdk-11.0.2_linux",
                        "needsPublishToRegistry"    : "true",
                        "needsPublishToNexus"       : "true",
                        "nexusSegmentArray"         : "payment-document-draft",
                        "needsOSS"                  : "true",
                        "needsSAST"                 : "true",
                        "needsSonar"                : "true",
                        "disableJiraInteraction"    : "true",
                        "dockerRegistryURL"         : "registry.delta.sbrf.ru",
                        "needsCustomMavenSettingXml": "true",
                        "additionalSonarProps"      : "-DskipTests=true",
                        "additionalBuildProps"      : "-DskipTests=true"
                ],
                "query",
                awaitBuildJenkinsJob as Boolean
        )
    } finally {
        execShell("git checkout ${mainBranchSigma}", true)
    }
}


scriptMain()

return this