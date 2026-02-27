import groovy.json.JsonSlurper

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

client = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .build()

metricsForDeteting = "custom:"

def getAllMetricsRq = HttpRequest.newBuilder()
        .uri(new URI("https://hjp51166.live.dynatrace.com/api/v1/timeseries?source=custom"))
        .header("Authorization", "Api-Token ...")
        .GET()
        .build()

println("Get all custom metrics")
def respGetAllMetrics = client.send(getAllMetricsRq, HttpResponse.BodyHandlers.ofString())
def respGetAllMetricsBody = respGetAllMetrics.body()

def allMetricsJson = new JsonSlurper().parseText(respGetAllMetricsBody)

println(allMetricsJson)

println("Find and delete metrics: ${metricsForDeteting}")

allMetricsJson.each {
    def timeseriesId = it['timeseriesId'].toString()
    if (timeseriesId.contains(metricsForDeteting)) {
        println("Delete metric: ${timeseriesId}")

        def deleteMetricRq = HttpRequest.newBuilder()
                .uri(new URI("https://hjp51166.live.dynatrace.com/api/v1/timeseries/${timeseriesId}"))
                .header("Authorization", "Api-Token ...")
                .DELETE()
                .build()

        def respDeleteMetric = client.send(deleteMetricRq, HttpResponse.BodyHandlers.ofString())

        println("Response status code: ${respDeleteMetric.statusCode()}")
        def respDeleteMetricBody = respDeleteMetric.body()
        if (respDeleteMetricBody) {
            println("Response body: ${respDeleteMetric.body()}")
        }
    }
}