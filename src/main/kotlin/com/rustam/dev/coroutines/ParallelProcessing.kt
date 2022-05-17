package com.rustam.dev.coroutines

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

// Время одного элемента в чанке
const val itemTime = 10L

// Размер чанка -> [ [.....], [....], [...], [..], [.] ]
const val chunkSize = 100

// Размер буфера -> (<---------->)
const val bufferSize = 10

// Счетчик обработанных строк
val counter = AtomicInteger(0)

suspend fun main() {

    println("Приблизительно понадобится: ${(chunkSize * itemTime)/1000} сек")

    // Инициализируем буферизированный канал с указанной емкостью
    val bufferedChannel = Channel<List<String>>(bufferSize)

    // Засекаем время
    val start = System.currentTimeMillis()

    // Открываем скоуп корутин
    coroutineScope {
        // Запускаем корутину, которая прочитает файл
        // и отправит чанки в канал
        launch {
            // Читаем строки в файле
            File("data.txt").useLines { sequence ->

                val stringList = sequence.toList()
                println("Файл содержит ${stringList.size} строк")
                println("Будет создано ${stringList.size/chunkSize} чанков")

                // Делим на чанки и отправляем каждый чанк в канал
                stringList.chunked(chunkSize).forEach {
                    println("Отправлено в канал")
                    bufferedChannel.send(it)
                }

                // Закрываем канал
                bufferedChannel.close()
            }
        }

        // Запускаем корутину, которая читает чанки из канала и отправляет их на исполнение
        launch {
            for (chunk in bufferedChannel) {
                println("Получено из канала")
                // Каждый чанк будет обрабатываться отдельной корутиной
                launch {
                    processChunk(chunk)
                }
            }
        }
    }

    println("За ${(System.currentTimeMillis() - start)/1000F} сек обработано $counter строк")
}

suspend fun processChunk(chunk: List<String>) {
    println("Обработка...")
    chunk.forEach {
        delay(itemTime)
        println(it)
        counter.incrementAndGet()
    }
}