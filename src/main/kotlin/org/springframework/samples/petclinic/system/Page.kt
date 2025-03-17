package org.springframework.samples.petclinic.system

class Page<T>(values: List<T>, pageable: Pageable? = null) {
    val content: List<T> = values
    val totalPages: Int = 1

    fun toList(): List<T> = content

    val totalElements: Int get() = content.size

    val isEmpty: Boolean get() = content.isEmpty()

    fun iterator(): Iterator<T> = content.iterator()
}

object Pageable {
	fun unpaged(): Pageable? = null
}

object PageRequest {
	fun of(i: Int, pageSize: Int): Pageable? = null
}
