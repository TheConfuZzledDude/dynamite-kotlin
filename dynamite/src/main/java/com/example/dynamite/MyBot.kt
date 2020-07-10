package com.example.dynamite



import com.softwire.dynamite.bot.Bot
import com.softwire.dynamite.game.Gamestate
import com.softwire.dynamite.game.Move
import java.util.HashMap
import kotlinx.serialization.*
import kotlinx.serialization.json.*


@Serializable
data class TwoMoveState(val move1: Pair<Move, Move>, val move2: Pair<Move, Move>)

@Serializable
data class Entry(var probability: Double, var observations: Double) {
}

infix fun Double.entryWith(other: Double): Entry {
    return Entry(this, other)
}

fun <A, B> cartesianProduct(
    a: Collection<A>,
    b: Collection<B>
): List<Pair<A, B>> {
    return a.flatMap { l ->
        b.map { r -> l to r }
    }
}

val possibleStates = cartesianProduct(Move.values().toList(), Move.values().toList())
val twoMoveStates = cartesianProduct(possibleStates, possibleStates)


var dynamiteSupply = 100

class MyBot : Bot {
    var markovProbabilities = hashMapOf<TwoMoveState, HashMap<Move, Entry>>()

    init {
        twoMoveStates.associateTo(markovProbabilities) { (a, b) ->

            val probabilities = hashMapOf<Move, Entry>()
            Move.values().associateTo(probabilities) {
                val entry = (1.0 / 5.0) entryWith 0.0
                it to entry
            }
            TwoMoveState(a, b) to probabilities
        }
    }

    fun randomMove(): Move {
        val possibleMoves =
            if (dynamiteSupply > 0) listOf(
                Move.R,
                Move.S,
                Move.P,
                Move.D
            ) else listOf(
                Move.R,
                Move.P,
                Move.S
            )
        val nextMove = possibleMoves.shuffled().first()
        if (nextMove == Move.D) dynamiteSupply--

        return nextMove
    }

    fun updateMarkov(gamestate: Gamestate): Move {
        val effectiveRounds = gamestate.rounds.map {
            it.p1 to it.p2
        }

        effectiveRounds.takeLast(3).also { (prevRound1, prevRound2, currentRound) ->
            var prevState = TwoMoveState(prevRound1, prevRound2)
            val opponentMove = currentRound.second

//            println("PrevState: $prevState - Opponent's Move $opponentMove - Our Move - ${currentRound.first}")

            markovProbabilities[prevState]?.forEach { (_, entry) ->
                entry.observations *= 0.3
            }
            markovProbabilities[prevState]
                ?.get(opponentMove).also {
                    if (it != null) {
                        it.observations += 1
                    }
                }

            val total = markovProbabilities[prevState]?.toList()?.fold(0.0) { acc, (_, entry) ->
                acc + entry.observations
            }.run { requireNotNull(this) }


            val currentState = TwoMoveState(prevRound2,currentRound)

            markovProbabilities[currentState]?.forEach { _, entry ->
                entry.probability = entry.observations / total
            }

            val maxEntry = requireNotNull(markovProbabilities[currentState]?.maxBy {
                it.value.probability
            })

            val minEntry = requireNotNull(markovProbabilities[currentState]?.minBy {
                it.value.probability
            })

//          println("Max: $maxEntry, Min: $minEntry")

            if (maxEntry.value.probability == minEntry.value.probability) {
                return randomMove()
            }
            val nextMove = getBeatingMove(maxEntry.key)
            if (nextMove == Move.D) {
                if (dynamiteSupply <= 0) {
                    return randomMove()
                }
                else {
                    dynamiteSupply--
                }
            }
            return nextMove
        }

    }

    fun getBeatingMove(move: Move): Move {
        return when (move) {
            Move.D -> Move.W
            Move.R -> Move.P
            Move.P -> Move.S
            Move.S -> Move.R
            Move.W -> listOf(Move.R, Move.S, Move.P).shuffled().first()
        }
    }

    override fun makeMove(gamestate: Gamestate): Move {
        return if (gamestate.rounds.size < 3) {
            randomMove()
        } else {
            updateMarkov(gamestate)
        }

        // Are you debugging?
        // Put a breakpoint in this method to see when we make a move
    }

    init {
        // Are you debugging?
        // Put a breakpoint on the line below to see when we start a new match
        println("Started new match")
    }
}