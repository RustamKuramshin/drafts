package ru.kuramshindev.ru.kuramshindev

class Cat: AbstractAnimal {

    val tailLength: Int

    constructor(age: Int, name: String, tailLength: Int) : super(age = age, name = name) {
        this.tailLength = tailLength
    }

    fun say() = println("Meoo")
}