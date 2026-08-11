package kallos.repository

import kallos.model.Remainder

class RemainderRepository {
    private val _remainders = mutableListOf<Remainder>()
    val remainders: List<Remainder> get() = _remainders.toList()

    fun addRemainder(remainder: Remainder) {
        _remainders.add(remainder)
    }

    fun editRemainder(remainder: Remainder) {
        val index = _remainders.indexOfFirst { it.id == remainder.id }
        if (index != -1) _remainders[index] = remainder
    }

    fun deleteRemainder(remainderId: String) {
        _remainders.removeAll { it.id == remainderId }
    }

    fun findById(remainderId: String): Remainder? =
        _remainders.find { it.id == remainderId }

    fun replaceAll(remainders: List<Remainder>) {
        _remainders.clear()
        _remainders.addAll(remainders)
    }
}
