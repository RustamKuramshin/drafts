package com.rustam.dev.other

data class Person(var name: String, var age: Int)

fun main() {

    val person1 = Person("John", 20)
    val person2 = Person("Doe", 30)

    val personSet = setOf(person1, person2)

    person1.name = "Jack"

    if (person1 in personSet) {
        println("found")
    } else {
        println("NOT found")
    }

}